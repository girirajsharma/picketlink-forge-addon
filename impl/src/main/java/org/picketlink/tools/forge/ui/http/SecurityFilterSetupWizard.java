/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.picketlink.tools.forge.ui.http;

import org.jboss.forge.addon.configuration.Configuration;
import org.jboss.forge.addon.configuration.facets.ConfigurationFacet;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.javaee.servlet.ServletFacet;
import org.jboss.forge.addon.javaee.servlet.ui.ServletSetupWizard;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.ui.command.PrerequisiteCommandsProvider;
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
import org.jboss.forge.addon.ui.result.navigation.NavigationResultBuilder;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizard;
import org.jboss.shrinkwrap.descriptor.api.webapp30.WebAppDescriptor;
import org.picketlink.authentication.web.AuthenticationFilter;
import org.picketlink.tools.forge.ConfigurationOperations;
import org.picketlink.tools.forge.PicketLinkFacetBase;

import javax.inject.Inject;
import java.util.Arrays;

import static org.picketlink.authentication.web.AuthenticationFilter.AuthType;

/**
 * @author Pedro Igor
 */
@FacetConstraint(value = PicketLinkFacetBase.class)
public class SecurityFilterSetupWizard extends AbstractProjectCommand implements UIWizard, PrerequisiteCommandsProvider {

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    private ConfigurationOperations configurationOperations;

    @Inject @WithAttributes(label = "Security Filter Name", required = true, description = "The name of the PicketLink filter", defaultValue = "SecurityFilter")
    private UIInput<String> filterName;

    @Inject @WithAttributes(label = "Authentication Scheme", required = true, description = "Select the HTTP Authentication Scheme")
    private UISelectOne<AuthType> authcScheme;

    @Inject
    @WithAttributes(label = "URL Protection Pattern", required = true, description = "Specifies an URL pattern that will be used to protect your project resources..", defaultValue = "/*")
    private UIInput<String> urlPattern;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(SecurityFilterSetupWizard.class)
            .name("PicketLink Security Filter: Setup")
            .description("Configures a Security Filter to your Web Application.")
            .category(Categories.create("picketlink"));
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        builder.add(this.filterName);

        this.authcScheme.setValueChoices(Arrays.asList(AuthType.values()));
        this.authcScheme.setDefaultValue(AuthType.FORM);

        builder.add(this.authcScheme);

        builder.add(this.urlPattern);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project selectedProject = getSelectedProject(context);
        Configuration configuration = selectedProject.getFacet(ConfigurationFacet.class).getConfiguration();

        configuration.setProperty(ConfigurationOperations.Properties.PICKETLINK_SECURITY_FILTER_NAME.name(), this.filterName.getValue());
        configuration.setProperty(ConfigurationOperations.Properties.PICKETLINK_SECURITY_FILTER_AUTHC_SCHEME.name(), this.authcScheme.getValue().name());
        configuration.setProperty(ConfigurationOperations.Properties.PICKETLINK_SECURITY_FILTER_URL_PATTERN.name(), this.urlPattern.getValue());

        ServletFacet servletFacet = selectedProject.getFacet(ServletFacet.class);
        WebAppDescriptor config = (WebAppDescriptor) servletFacet.getConfig();

        config
            .getOrCreateFilter()
                .filterName(this.filterName.getValue())
                .filterClass(AuthenticationFilter.class.getName());

        config
            .getOrCreateFilterMapping()
                .filterName(this.filterName.getValue())
                .removeAllUrlPattern()
                .urlPattern(this.urlPattern.getValue());

        servletFacet.saveConfig(config);

        this.configurationOperations.newConfiguration(selectedProject);

        return Results.success("Security Filter has been installed.");
    }

    @Override
    protected ProjectFactory getProjectFactory() {
        return this.projectFactory;
    }

    @Override
    protected boolean isProjectRequired() {
        return true;
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        if (AuthType.FORM.equals(this.authcScheme.getValue())) {
            return context.navigateTo(FormAuthenticationSchemeWizardStep.class);
        }

        if (AuthType.BASIC.equals(this.authcScheme.getValue())) {
            return context.navigateTo(BasicAuthenticationSchemeWizardStep.class);
        }

        if (AuthType.DIGEST.equals(this.authcScheme.getValue())) {
            return context.navigateTo(DigestAuthenticationSchemeWizardStep.class);
        }

        return null;
    }

    @Override
    public NavigationResult getPrerequisiteCommands(UIContext context) {
        NavigationResultBuilder builder = NavigationResultBuilder.create();

        builder.add(ServletSetupWizard.class);

        return builder.build();
    }

    public void setUrlPattern(String urlPattern) {
        this.urlPattern.setDefaultValue(urlPattern);
    }
}
