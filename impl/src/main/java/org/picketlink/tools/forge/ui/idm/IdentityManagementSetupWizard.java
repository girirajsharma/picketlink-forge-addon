package org.picketlink.tools.forge.ui.idm;

import org.jboss.forge.addon.configuration.Configuration;
import org.jboss.forge.addon.configuration.facets.ConfigurationFacet;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.facets.constraints.FacetConstraints;
import org.jboss.forge.addon.javaee.jpa.ui.setup.JPASetupWizard;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.ui.command.CommandExecutionListener;
import org.jboss.forge.addon.ui.command.UICommand;
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
import org.picketlink.tools.forge.PicketLinkFacetIDM;

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
public class IdentityManagementSetupWizard extends AbstractProjectCommand implements UIWizard {

    private static final String DEFAULT_IDENTITY_CONFIGURATION_NAME = "default.config";

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    private ConfigurationOperations configurationOperations;

    @Inject
    @WithAttributes(label = "Configuration Name", required = true, description = "Identity configuration name", defaultValue = DEFAULT_IDENTITY_CONFIGURATION_NAME)
    private UIInput<String> named;

    @Inject
    @WithAttributes(label = "Identity Store Type", required = true, description = "The identity store to be used.")
    private UISelectOne<IdentityStoreType> identityStoreType;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(IdentityManagementSetupWizard.class)
            .name("PicketLink Identity Management: Setup")
            .description("Configure Identity Management to your project.")
            .category(Categories.create("picketlink"));
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        builder.add(this.named);

        this.identityStoreType.setValueChoices(asList(IdentityStoreType.values()));
        this.identityStoreType.setDefaultValue(IdentityStoreType.jpa);
        builder.add(this.identityStoreType);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project selectedProject = getSelectedProject(context.getUIContext());
        Configuration configuration = selectedProject.getFacet(ConfigurationFacet.class).getConfiguration();

        configuration.setProperty(ConfigurationOperations.Properties.IDENTITY_CONFIGURATION_NAME.name(), this.named.getValue());
        configuration.setProperty(ConfigurationOperations.Properties.IDENTITY_STORE_TYPE.name(), this.identityStoreType.getValue().name());

        this.configurationOperations.newConfiguration(selectedProject);

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
    public NavigationResult next(UINavigationContext context) throws Exception {
        Project project = getSelectedProject(context);

        context.getUIContext().addCommandExecutionListener(new CommandExecutionListener() {
            @Override
            public void preCommandExecuted(UICommand command, UIExecutionContext context) {

            }

            @Override
            public void postCommandExecuted(UICommand command, UIExecutionContext context, Result result) {
                System.out.print(result);
            }

            @Override
            public void postCommandFailure(UICommand command, UIExecutionContext context, Throwable failure) {

            }
        });

        if (project != null && IdentityStoreType.jpa.equals(this.identityStoreType.getValue())) {
            return context.navigateTo(JPASetupWizard.class);
        }

        return null;
    }
}