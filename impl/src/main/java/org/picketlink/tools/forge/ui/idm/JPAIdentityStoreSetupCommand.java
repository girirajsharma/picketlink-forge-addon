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

import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;
import org.picketlink.tools.forge.PersistenceManager;

import javax.inject.Inject;

/**
 * @author Pedro Igor
 */
public class JPAIdentityStoreSetupCommand extends AbstractProjectCommand implements UIWizardStep {

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    private PersistenceManager persistenceManager;

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {

    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        this.persistenceManager.configure(getSelectedProject(context));
        return Results.success("JPA Identity Store successfully configured.");
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
        return null;
    }
}
