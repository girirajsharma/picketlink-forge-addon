package org.picketlink.tools.forge.ui;

import org.jboss.forge.addon.convert.Converter;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.picketlink.tools.forge.MavenDependencies;
import org.picketlink.tools.forge.PicketLinkFacet;
import org.picketlink.tools.forge.PicketLinkFacetBase;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * <p>This command is used to properly configure the project before any other command is executed. It provides all the necessary
 * configuration in order to properly enable PicketLink to a project.</p>
 *
 * @author Pedro Igor
 */
public class SetupCommand extends AbstractProjectCommand {

    @Inject
    private FacetFactory facetFactory;

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    private MavenDependencies mavenDependencies;

    @Inject @WithAttributes(label = "Version", required = true, description = "Select the version of PicketLink", shortName = 'v')
    private UISelectOne<Coordinate> version;

    @Inject @WithAttributes(label = "Show snapshot versions", description = "Show snapshot versions in the list", defaultValue = "false")
    private UIInput<Boolean> showSnapshots;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(SetupCommand.class)
            .name("PicketLink: Setup")
            .description("Configure PicketLink to your project.")
            .category(Categories.create("picketlink"));
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        initializeUISelectVersions(builder);
        builder.add(this.showSnapshots);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project selectedProject = getSelectedProject(context);
        PicketLinkFacet selectedModule = this.facetFactory.create(selectedProject, PicketLinkFacetBase.class);

        selectedModule.setPicketLinkVersion(this.version.getValue().getVersion());

        if (this.facetFactory.install(selectedProject, selectedModule)) {
            return Results.success("PicketLink was successfully configured.");
        }

        return Results.fail("Could not install PicketLink.");
    }

    @Override
    protected boolean isProjectRequired() {
        return true;
    }

    @Override
    protected ProjectFactory getProjectFactory() {
        return this.projectFactory;
    }

    private void initializeUISelectVersions(UIBuilder builder) {
        Callable<Iterable<Coordinate>> coordinatesBuilder = new Callable<Iterable<Coordinate>>() {
            @Override
            public Iterable<Coordinate> call() throws Exception {
                return mavenDependencies.getAvailableVersions(showSnapshots.getValue());
            }
        };

        this.version.setValueChoices(coordinatesBuilder);

        this.version.setItemLabelConverter(new Converter<Coordinate,String>() {
            @Override
            public String convert(Coordinate source) {
                return source != null ? String.format("%s", source.getVersion()) : null;
            }
        });

        final List<Coordinate> availableVersions = this.mavenDependencies.getAvailableVersions(false);

        if (!availableVersions.isEmpty()) {
            Coordinate defaultVersion = availableVersions.get(availableVersions.size() - 1);

            for (int i = availableVersions.size() - 1; i >= 0; i--) {
                String version = availableVersions.get(i).getVersion();

                if (version != null && version.toLowerCase().contains("final")) {
                    defaultVersion = availableVersions.get(i);
                    break;
                }
            }

            this.version.setDefaultValue(defaultVersion);
        }

        builder.add(this.version);
    }
}