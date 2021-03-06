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
package org.picketlink.tools.forge.operations;

import org.jboss.forge.addon.configuration.Configuration;
import org.jboss.forge.addon.configuration.facets.ConfigurationFacet;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.picketlink.config.SecurityConfigurationBuilder;
import org.picketlink.event.SecurityConfigurationEvent;
import org.picketlink.tools.forge.ConfigurationOperations;
import org.picketlink.tools.forge.ui.idm.IdentityStoreType;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Set;

import static org.picketlink.tools.forge.ConfigurationOperations.Properties.PICKETLINK_IDENTITY_CONFIGURATION_NAME;
import static org.picketlink.tools.forge.ConfigurationOperations.Properties.PICKETLINK_IDENTITY_STORE_TYPE;
import static org.picketlink.tools.forge.ConfigurationOperations.Properties.PICKETLINK_STATELESS_IDENTITY;
import static org.picketlink.tools.forge.ConfigurationOperations.Properties.PICKETLINK_TOP_LEVEL_PACKAGE_NAME;

/**
 * @author Pedro Igor
 */
public class ConfigurationOperationsImpl implements ConfigurationOperations {

    private static final String SECURITY_CONFIGURATION_CLASS_NAME = "SecurityConfiguration";
    private static final String RESOURCE_PRODUCER_CLASS_NAME = "Resources";

    @Inject
    private AttributedTypeOperations attributedTypeManager;

    public JavaResource newConfiguration(Project selectedProject) {
        StringBuilder methodBodyBuilder = new StringBuilder();
        StringBuilder configuration = createIdentityBeanConfiguration(selectedProject);
        StringBuilder identityManagementConfiguration = createIdentityManagementConfiguration(selectedProject);

        configuration.append(identityManagementConfiguration.toString());

        if (configuration.length() > 0) {
            methodBodyBuilder
                .append("SecurityConfigurationBuilder builder = event.getBuilder();\n")
                .append("\n")
                .append("builder\n")
                .append(configuration.toString());
        }

        return newConfiguration(selectedProject, methodBodyBuilder);
    }

    @Override
    public JavaResource newResourceProducer(Project selectedProject) {
        Configuration configuration = getConfiguration(selectedProject);

        String identityStoreType = configuration.getString(PICKETLINK_IDENTITY_STORE_TYPE.name(), IdentityStoreType.jpa.name());

        if (!IdentityStoreType.jpa.name().equals(identityStoreType)) {
            return null;
        }

        JavaSourceFacet javaFacet = selectedProject.getFacet(JavaSourceFacet.class);

        String topLevelPackageName = getConfiguration(selectedProject).getString(PICKETLINK_TOP_LEVEL_PACKAGE_NAME.name(), DEFAULT_TOP_LEVEL_PACKAGE);
        JavaResource javaResource = javaFacet
            .getBasePackageDirectory()
            .getOrCreateChildDirectory(topLevelPackageName)
            .getChildOfType(JavaResource.class, RESOURCE_PRODUCER_CLASS_NAME + ".java");
        String packageName = javaFacet.calculatePackage(javaResource);

        JavaClassSource javaSource = Roaster.create(JavaClassSource.class)
            .setName(RESOURCE_PRODUCER_CLASS_NAME)
            .setPublic()
            .setPackage(packageName);

        FieldSource<JavaClassSource> entityManagerProducerField = javaSource
            .addField()
            .setPrivate()
            .setType(EntityManager.class)
            .setName("entityManager");

        entityManagerProducerField.addAnnotation(PersistenceContext.class);
        entityManagerProducerField.addAnnotation(Produces.class);

        javaResource.setContents(javaSource);

        return javaResource;
    }

    private StringBuilder createIdentityBeanConfiguration(Project selectedProject) {
        StringBuilder config = new StringBuilder();
        boolean statelessIdentity = getConfiguration(selectedProject).getBoolean(PICKETLINK_STATELESS_IDENTITY.name(), false);

        if (statelessIdentity) {
            config
                .append("\t.identity()\n")
                .append("\t\t.stateless()\n");
        }

        return config;
    }

    private StringBuilder createIdentityManagementConfiguration(Project selectedProject) {
        StringBuilder config = new StringBuilder();
        Configuration configuration = getConfiguration(selectedProject);
        String identityConfigurationName = configuration.getString(PICKETLINK_IDENTITY_CONFIGURATION_NAME.name(), "default.config");
        String identityStoreType = configuration.getString(PICKETLINK_IDENTITY_STORE_TYPE.name(), IdentityStoreType.jpa.name());

        Set<String> attributedTypes = this.attributedTypeManager.getAttributedTypes(selectedProject);

        if (!attributedTypes.isEmpty()) {
            config
                .append("\t.idmConfig()\n")
                .append("\t\t.named(\"").append(identityConfigurationName).append("\")\n")
                .append("\t\t\t.stores()\n")
                .append("\t\t\t\t.").append(identityStoreType).append("()\n");

            config.append("\t\t\t\t\t.supportType(");

            int i = 0;

            for (String attributedTypeName : attributedTypes) {
                if (i > 0) {
                    config.append(",\n");
                }

                config.append(attributedTypeName).append(".class");

                i++;
            }

            config.append("\t\t\t\t\t)\n");

            config.append("\t\t\t\t\t.supportAllFeatures();");
        }

        return config;
    }

    private JavaResource newConfiguration(Project selectedProject, StringBuilder observerMethodBody) {
        StringBuilder methodBody = observerMethodBody;

        if (methodBody == null) {
            methodBody = new StringBuilder();
        }

        JavaSourceFacet javaFacet = selectedProject.getFacet(JavaSourceFacet.class);

        Configuration configuration = getConfiguration(selectedProject);

        if (methodBody.length() > 0) {
            String topLevelPackageName = configuration.getString(PICKETLINK_TOP_LEVEL_PACKAGE_NAME.name(), DEFAULT_TOP_LEVEL_PACKAGE);
            JavaResource javaResource = javaFacet
                .getBasePackageDirectory()
                .getOrCreateChildDirectory(topLevelPackageName)
                .getChildOfType(JavaResource.class, SECURITY_CONFIGURATION_CLASS_NAME + ".java");
            String packageName = javaFacet.calculatePackage(javaResource);

            JavaClassSource javaSource = Roaster.create(JavaClassSource.class)
                .setName(SECURITY_CONFIGURATION_CLASS_NAME)
                .setPublic()
                .setPackage(packageName);

//            if (authenticationScheme != null) {
//                javaSource.addImport(HTTPAuthenticationScheme.class);
//                javaSource.addImport(PicketLink.class);
//
//                javaSource
//                    .addField()
//                    .setPrivate()
//                    .setName("authenticationScheme")
//                    .setType(AuthType.valueOf(authenticationScheme).getSchemeType())
//                    .addAnnotation(Inject.class);
//
//                MethodSource<JavaClassSource> produceAuthenticationSchemeMethod = javaSource
//                    .addMethod()
//                    .setPublic()
//                    .setReturnType(HTTPAuthenticationScheme.class)
//                    .setName("produceAuthenticationScheme")
//                    .setBody("return authenticationScheme;");
//
//                produceAuthenticationSchemeMethod.addAnnotation(Produces.class);
//                produceAuthenticationSchemeMethod.addAnnotation(PicketLink.class);
//
//            }

            if (methodBody.length() > 0) {
                javaSource.addImport(SecurityConfigurationBuilder.class);

                javaSource
                    .addMethod()
                    .setName("onInit")
                    .setPublic()
                    .setReturnTypeVoid()
                    .setBody(methodBody.toString())
                    .addParameter(SecurityConfigurationEvent.class, "event")
                    .addAnnotation(Observes.class);
            }

            javaResource.setContents(javaSource);
        }

        return null;
    }

    private Configuration getConfiguration(Project selectedProject) {
        ConfigurationFacet configurationFacet = selectedProject.getFacet(ConfigurationFacet.class);
        return configurationFacet.getConfiguration();
    }
}
