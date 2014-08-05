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
package org.picketlink.tools.forge.ui;

import org.jboss.forge.addon.javaee.jpa.JPAFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.command.AbstractCommandExecutionListener;
import org.jboss.forge.addon.ui.command.UICommand;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.shrinkwrap.descriptor.api.persistence.PersistenceCommonDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence.PersistenceUnitCommon;

import java.util.List;

/**
 * @author Pedro Igor
 */
public class DefaultCommandExecutionListener extends AbstractCommandExecutionListener {

    @Override
    public void postCommandExecuted(UICommand command, UIExecutionContext context, Result result) {
        if (isSuccessfulResult(result)) {
            Project selectedProject = (Project) context.getUIContext().getAttributeMap().get("selectedProject");

            if (selectedProject != null && selectedProject.hasFacet(JPAFacet.class)) {
                PersistenceCommonDescriptor config = (PersistenceCommonDescriptor) selectedProject.getFacet(JPAFacet.class)
                    .getConfig();
                List<PersistenceUnitCommon> persistenceUnits = config.getAllPersistenceUnit();

                if (!persistenceUnits.isEmpty()) {
                    PersistenceUnitCommon pu = persistenceUnits.iterator().next();
                }
            }
        }
    }

    private boolean isSuccessfulResult(Result result) {
        return !Failed.class.isInstance(result);
    }
}
