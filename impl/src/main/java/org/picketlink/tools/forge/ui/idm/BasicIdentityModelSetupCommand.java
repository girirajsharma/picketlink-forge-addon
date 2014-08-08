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
package org.picketlink.tools.forge.ui.idm;

import org.jboss.forge.addon.configuration.Configuration;
import org.jboss.forge.addon.configuration.facets.ConfigurationFacet;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.javaee.ejb.ui.EJBSetupWizard;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
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
import org.jboss.forge.addon.ui.wizard.UIWizard;
import org.picketlink.tools.forge.ConfigurationOperations;
import org.picketlink.tools.forge.MavenDependencies;
import org.picketlink.tools.forge.PicketLinkIDMFacet;

import javax.inject.Inject;

import static org.picketlink.tools.forge.util.ResourceUtil.createSecurityInitializerifNecessary;

/**
 * @author Pedro Igor
 */
@FacetConstraint(value = PicketLinkIDMFacet.class)
public class BasicIdentityModelSetupCommand extends AbstractProjectCommand implements UIWizard {

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    private DependencyInstaller dependencyInstaller;

    @Inject
    @WithAttributes(label = "Creates a Default User", required = true, description = "Indicates if a default user should be created.", defaultValue = "false")
    private UIInput<Boolean> generateDefaultIdentities;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(IdentityManagementSetupWizard.class)
            .name("PicketLink IDM Basic Identity Model: Setup")
            .description("Configure the Basic Identity Model to your PicketLink IDM project.")
            .category(Categories.create("picketlink"));
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        builder.add(this.generateDefaultIdentities);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project selectedProject = getSelectedProject(context);

        if (this.generateDefaultIdentities.getValue()) {
            context.getUIContext().setSelection(createSecurityInitializerifNecessary(selectedProject));
            return Results.success("Default user has been created.");
        }

        Configuration configuration = selectedProject.getFacet(ConfigurationFacet.class).getConfiguration();

        String identityStoreType = configuration.getString(ConfigurationOperations.Properties.PICKETLINK_IDENTITY_STORE_TYPE.name());

        if (IdentityStoreType.jpa.name().equals(identityStoreType)) {
            this.dependencyInstaller.install(selectedProject, MavenDependencies.PICKETLINK_IDM_SIMPLE_SCHEMA_DEPENDENCY);
        }

        return Results.success();
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
        if (this.generateDefaultIdentities.getValue()) {
            return context.navigateTo(EJBSetupWizard.class);
        }

        return null;
    }
}
