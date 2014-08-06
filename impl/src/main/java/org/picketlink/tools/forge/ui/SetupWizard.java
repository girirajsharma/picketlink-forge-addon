package org.picketlink.tools.forge.ui;

import org.jboss.forge.addon.configuration.Configuration;
import org.jboss.forge.addon.configuration.facets.ConfigurationFacet;
import org.jboss.forge.addon.convert.Converter;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizard;
import org.picketlink.tools.forge.ConfigurationOperations;
import org.picketlink.tools.forge.MavenDependencies;
import org.picketlink.tools.forge.PicketLinkFacet;
import org.picketlink.tools.forge.PicketLinkFacetBase;
import org.picketlink.tools.forge.ui.idm.IdentityManagementSetupWizard;

import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.util.concurrent.Callable;

import static org.picketlink.tools.forge.ConfigurationOperations.DEFAULT_TOP_LEVEL_PACKAGE;
import static org.picketlink.tools.forge.ConfigurationOperations.Properties.PICKETLINK_TOP_LEVEL_PACKAGE_NAME;

/**
 * <p>This command is used to properly configure the project before any other command is executed. It provides all the necessary
 * configuration in order to properly enable PicketLink to a project.</p>
 *
 * @author Pedro Igor
 */
public class SetupWizard extends AbstractProjectCommand implements UIWizard {

    @Inject
    private FacetFactory facetFactory;

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    private MavenDependencies mavenDependencies;

    @Inject
    private ConfigurationOperations configurationOperations;

    @Inject @WithAttributes(label = "Version", required = true, description = "Select the version of PicketLink", shortName = 'v')
    private UISelectOne<Coordinate> version;

    @Inject @WithAttributes(label = "Show snapshot versions", shortName = 's', description = "Show snapshot versions in the list", defaultValue = "false")
    private UIInput<Boolean> showSnapshots;

    @Inject
    @WithAttributes(label = "Top Level Package", required = true, description = "The top level package where security-related classes will reside", defaultValue = DEFAULT_TOP_LEVEL_PACKAGE)
    private UIInput<String> topLevelPackage;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(SetupWizard.class)
            .name("PicketLink: Setup")
            .description("Configure PicketLink to your project.")
            .category(Categories.create("picketlink"));
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        builder.add(this.showSnapshots);
        initializeUISelectVersions(builder);
        builder.add(this.topLevelPackage);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project selectedProject = getSelectedProject(context);
        PicketLinkFacet selectedModule = this.facetFactory.create(selectedProject, PicketLinkFacetBase.class);

        selectedModule.setPicketLinkVersion(this.version.getValue().getVersion());

        Configuration configuration = selectedProject.getFacet(ConfigurationFacet.class).getConfiguration();

        configuration.setProperty(PICKETLINK_TOP_LEVEL_PACKAGE_NAME.name(), this.topLevelPackage.getValue());

        if (this.facetFactory.install(selectedProject, selectedModule)) {
            createDefaultConfiguration(context, selectedProject);
            return Results.success("PicketLink was successfully configured.");
        }

        return Results.fail("Could not install PicketLink.");
    }

    private void createDefaultConfiguration(UIExecutionContext context, Project selectedProject) throws FileNotFoundException {
        JavaResource configurationResource = this.configurationOperations.newConfiguration(selectedProject);
        context.getUIContext().setSelection(configurationResource);
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
                return mavenDependencies.resolveVersions(showSnapshots.getValue());
            }
        };

        this.version.setValueChoices(coordinatesBuilder);

        this.version.setItemLabelConverter(new Converter<Coordinate,String>() {
            @Override
            public String convert(Coordinate source) {
                return source != null ? String.format("%s", source.getVersion()) : null;
            }
        });

        this.version.setDefaultValue(this.mavenDependencies.resolveLatestVersion());

        builder.add(this.version);
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        return context.navigateTo(IdentityManagementSetupWizard.class);
    }
}