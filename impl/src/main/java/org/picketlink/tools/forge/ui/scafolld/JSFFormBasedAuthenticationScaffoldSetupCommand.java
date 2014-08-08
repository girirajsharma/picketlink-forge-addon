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
package org.picketlink.tools.forge.ui.scafolld;

import org.jboss.forge.addon.javaee.ejb.EJBFacet;
import org.jboss.forge.addon.javaee.ejb.ui.EJBSetupWizard;
import org.jboss.forge.addon.javaee.faces.FacesFacet;
import org.jboss.forge.addon.javaee.faces.ui.FacesSetupWizard;
import org.jboss.forge.addon.javaee.servlet.ServletFacet;
import org.jboss.forge.addon.javaee.servlet.ui.ServletSetupWizard;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.ui.command.PrerequisiteCommandsProvider;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.result.navigation.NavigationResultBuilder;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.picketlink.tools.forge.PicketLinkBaseFacet;
import org.picketlink.tools.forge.ui.SetupWizard;
import org.picketlink.tools.forge.ui.http.SecurityFilterSetupWizard;

import javax.inject.Inject;

import static org.picketlink.tools.forge.util.ResourceUtil.createJavaResourceIfNecessary;
import static org.picketlink.tools.forge.util.ResourceUtil.createSecurityInitializerifNecessary;
import static org.picketlink.tools.forge.util.ResourceUtil.createWebResourceIfNecessary;

/**
 * @author Pedro Igor
 */
public class JSFFormBasedAuthenticationScaffoldSetupCommand extends AbstractProjectCommand implements PrerequisiteCommandsProvider {

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    private SecurityFilterSetupWizard securityFilterSetupWizard;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(JSFFormBasedAuthenticationScaffoldSetupCommand.class)
            .name("PicketLink JSF Form Authentication Scaffold: Setup")
            .description("Provides a template for a JSF project using Form-based Authentication..")
            .category(Categories.create("picketlink"));
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {

    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project selectedProject = getSelectedProject(context);

        createSecurityInitializerifNecessary(selectedProject);
        createJavaResourceIfNecessary(selectedProject, "LoginController.java", "/scaffold/jsfformauthc/classes/LoginController.java");

        createWebResourceIfNecessary(selectedProject, "index.html", "/scaffold/jsfformauthc/index.html");
        createWebResourceIfNecessary(selectedProject, "home.xhtml", "/scaffold/jsfformauthc/home.xhtml");
        createWebResourceIfNecessary(selectedProject, "loginWithBean.xhtml", "/scaffold/jsfformauthc/loginWithBean.xhtml");
        createWebResourceIfNecessary(selectedProject, "protected/private.xhtml", "/scaffold/jsfformauthc/protected/private.xhtml");

        return Results.success("Scaffold was setup successfully.");
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

        if (project != null) {
            if (!project.hasFacet(FacesFacet.class)) {
                builder.add(FacesSetupWizard.class);
            }

            if (!project.hasFacet(EJBFacet.class)) {
                builder.add(EJBSetupWizard.class);
            }

            if (!project.hasFacet(PicketLinkBaseFacet.class)) {
                builder.add(SetupWizard.class);
            }

            if (!project.hasFacet(ServletFacet.class)) {
                builder.add(ServletSetupWizard.class);
            }

            this.securityFilterSetupWizard.setUrlPattern("/protected/*");

            builder.add(this.securityFilterSetupWizard);
        }

        return builder.build();
    }
}
