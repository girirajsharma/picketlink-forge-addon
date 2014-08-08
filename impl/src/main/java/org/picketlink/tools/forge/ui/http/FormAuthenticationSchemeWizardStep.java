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
import org.jboss.forge.addon.javaee.servlet.ServletFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;
import org.jboss.shrinkwrap.descriptor.api.webapp30.WebAppDescriptor;
import org.picketlink.authentication.web.AuthenticationFilter;
import org.picketlink.tools.forge.ConfigurationOperations;

import javax.inject.Inject;

import static org.picketlink.tools.forge.util.ResourceUtil.createWebResourceIfNecessary;

/**
 * @author Pedro Igor
 */
public class FormAuthenticationSchemeWizardStep extends AbstractProjectCommand implements UIWizardStep{

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    @WithAttributes(label = "Authentication Login Page", required = true, description = "The page URI representing the login page.", defaultValue = "/login.html")
    private UIInput<String> loginPage;

    @Inject
    @WithAttributes(label = "Authentication Error Page", required = true, description = "The page URI that will be used to redirect the user on unsuccessful authentication.", defaultValue = "/loginFailed.html")
    private UIInput<String> errorPage;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(SecurityFilterSetupWizard.class)
            .name("PicketLink HTTP Form Authentication: Setup")
            .description("Enables HTTP Form Authentication to your project.")
            .category(Categories.create("picketlink"));
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        builder.add(this.errorPage);
        builder.add(this.loginPage);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project selectedProject = getSelectedProject(context);
        Configuration configuration = selectedProject.getFacet(ConfigurationFacet.class).getConfiguration();
        String securityFilterName = configuration.getString(ConfigurationOperations.Properties.PICKETLINK_SECURITY_FILTER_NAME.name());
        ServletFacet servletFacet = selectedProject.getFacet(ServletFacet.class);
        WebAppDescriptor config = (WebAppDescriptor) servletFacet.getConfig();

        config
            .getOrCreateFilter()
            .filterName(securityFilterName)
            .removeAllInitParam()
            .createInitParam()
            .paramName("authType")
            .paramValue(AuthenticationFilter.AuthType.FORM.name());

        config
            .getOrCreateFilter()
            .filterName(securityFilterName)
            .createInitParam()
            .paramName("form-login-page")
            .paramValue(this.loginPage.getValue());

        createWebResourceIfNecessary(selectedProject, this.loginPage.getValue(), "/scaffold/jsfformauthc/login.html");

        config
            .getOrCreateFilter()
            .filterName(securityFilterName)
            .createInitParam()
            .paramName("form-error-page")
            .paramValue(this.errorPage.getValue());

        servletFacet.saveConfig(config);

        createWebResourceIfNecessary(selectedProject, this.errorPage.getValue(), "/scaffold/jsfformauthc/loginFailed.html");

        return Results.success("HTTP Form-based Authentication has been installed.");
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
        return null;
    }
}
