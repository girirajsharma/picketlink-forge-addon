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
package org.picketlink.tools.forge;

import org.jboss.forge.addon.projects.Project;
import org.picketlink.idm.model.AttributedType;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Provides methods to manipulate project's persistence unit in order to properly configure the JPA Identity Store.</p>
 *
 * @author Pedro Igor
 */
public class AttributedTypeManager {

    public List<String> getAttributedTypes(Project selectedProject) {
        final ArrayList<String> attributedTypes = new ArrayList<>();

        return attributedTypes;
    }

    private boolean isAttributedType(Class<?> cls) {
        while (!cls.equals(Object.class)) {
            if (AttributedType.class.isAssignableFrom(cls)) {
                return true;
            }

            // Check the superclass
            cls = cls.getSuperclass();
        }

        return false;
    }
}
