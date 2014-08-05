package org.picketlink.tools.forge.ui.idm;

import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.facets.constraints.FacetConstraints;
import org.jboss.forge.addon.javaee.jpa.JPAFacet;
import org.jboss.forge.addon.javaee.jpa.ui.setup.JPASetupWizard;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.ui.command.PrerequisiteCommandsProvider;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.result.navigation.NavigationResultBuilder;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.picketlink.event.SecurityConfigurationEvent;
import org.picketlink.tools.forge.PicketLinkFacetIDM;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import static java.util.Arrays.asList;

/**
 * <p>This command is used to properly configure the project before any other command is executed. It provides all the necessary
 * configuration in order to properly enable PicketLink to a project.</p>
 *
 * @author Pedro Igor
 */
@FacetConstraints(value = {
    @FacetConstraint(value = PicketLinkFacetIDM.class),
    @FacetConstraint(value = JavaSourceFacet.class)
})
public class IdentityManagementSetupCommand extends AbstractProjectCommand implements PrerequisiteCommandsProvider {

    private static final String DEFAULT_IDENTITY_CONFIGURATION_NAME = "default.config";
    private static final String DEFAULT_TOP_LEVEL_PACKAGE = "security";

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    @WithAttributes(label = "Configuration Name", required = true, description = "Identity configuration name", defaultValue = DEFAULT_IDENTITY_CONFIGURATION_NAME)
    private UIInput<String> named;

    @Inject
    @WithAttributes(label = "Top Level Package", required = true, description = "The top level package where IDM-related classes reside", defaultValue = DEFAULT_TOP_LEVEL_PACKAGE)
    private UIInput<String> topLevelPackage;

    @Inject
    @WithAttributes(label = "Identity Store Type", required = true, description = "The identity store to be used.")
    private UISelectOne<IdentityStoreType> identityStoreType;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(IdentityManagementSetupCommand.class)
            .name("PicketLink Identity Management: Setup")
            .description("Configure Identity Management to your project.")
            .category(Categories.create("picketlink"));
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        builder.add(this.named);
        builder.add(this.topLevelPackage);

        this.identityStoreType.setValueChoices(asList(IdentityStoreType.values()));
        this.identityStoreType.setDefaultValue(IdentityStoreType.FILE);
        builder.add(this.identityStoreType);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project selectedProject = getSelectedProject(context.getUIContext());
        JavaSourceFacet javaSourceFacet = selectedProject.getFacet(JavaSourceFacet.class);

        JavaClassSource javaClass = Roaster.create(JavaClassSource.class)
            .setName("SecurityConfiguration")
            .setPublic();

        javaClass
            .addMethod()
                .setName("onInit")
                .setPublic()
                .setReturnTypeVoid()
                .setBody("Test")
                .addParameter(SecurityConfigurationEvent.class, "event")
                    .addAnnotation(Observes.class);

        JavaResource javaResource = javaSourceFacet.saveJavaSource(javaClass);

        context.getUIContext().setSelection(javaResource);

        return Results.success("PicketLink Identity Management was successfully configured.");
    }

    @Override
    protected boolean isProjectRequired() {
        return true;
    }

    @Override
    protected ProjectFactory getProjectFactory() {
        return this.projectFactory;
    }

    @Override
    public NavigationResult getPrerequisiteCommands(UIContext context) {
        NavigationResultBuilder builder = NavigationResultBuilder.create();
        Project project = getSelectedProject(context);

        if (project != null && !project.hasFacet(JPAFacet.class)) {
            builder.add(JPASetupWizard.class);
        }

        return builder.build();
    }
}