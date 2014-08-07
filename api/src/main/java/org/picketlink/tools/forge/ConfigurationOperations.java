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

import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;

/**
 * @author Pedro Igor
 */
public interface ConfigurationOperations {

    public static final String DEFAULT_TOP_LEVEL_PACKAGE = "security";

    /**
     * <p>Creates a {@link org.jboss.forge.addon.parser.java.resources.JavaResource} providing all the necessary
     * configuration.</p>
     *
     * @param selectedProject
     * @return
     */
    JavaResource newConfiguration(Project selectedProject);

    /**
     * <p>Creates a {@link org.jboss.forge.addon.parser.java.resources.JavaResource} for a resource producer.</p>
     *
     * @param selectedProject
     * @return
     */
    JavaResource newResourceProducer(Project selectedProject);

    public enum Properties {

        PICKETLINK_STATELESS_IDENTITY,
        PICKETLINK_IDENTITY_CONFIGURATION_NAME,
        PICKETLINK_IDENTITY_STORE_TYPE,
        PICKETLINK_IDENTITY_BASIC_MODEL,
        PICKETLINK_TOP_LEVEL_PACKAGE_NAME,
        PICKETLINK_SECURITY_FILTER_NAME,
        PICKETLINK_SECURITY_FILTER_URL_PATTERN,
        PICKETLINK_SECURITY_FILTER_AUTHC_SCHEME
    }
}
