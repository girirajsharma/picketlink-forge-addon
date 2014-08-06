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

import org.jboss.forge.addon.configuration.Configuration;
import org.jboss.forge.addon.configuration.facets.ConfigurationFacet;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.dependencies.builder.DependencyQueryBuilder;
import org.jboss.forge.addon.javaee.jpa.JPAFacet;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.shrinkwrap.descriptor.api.persistence.PersistenceCommonDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence.PersistenceUnitCommon;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Provides methods to manipulate project's persistence unit in order to properly configure the JPA Identity Store.</p>
 *
 * @author Pedro Igor
 */
public class PersistenceManager {

    private static final String JPA_ANNOTATION_PACKAGE = "org.picketlink.idm.jpa.annotations";
    private static final String BASIC_IDENTITY_ENTITY_MODEL_PACKAGE_NAME = "org.picketlink.idm.jpa.model.sample.simple";

    @Inject
    private DependencyResolver dependencyResolver;

    public void configure(Project selectedProject) {
        Configuration configuration = selectedProject.getFacet(ConfigurationFacet.class).getConfiguration();
        List<String> entityTypes;

        if (isConfigureBasicIdentityModel(configuration)) {
            entityTypes = getBasicIdentityModelEntityTypes(selectedProject);
        } else {
            entityTypes = getProjectEntityTypes(selectedProject);
        }

        JPAFacet jpaFacet = selectedProject.getFacet(JPAFacet.class);
        PersistenceCommonDescriptor persistenceUnitConfig = (PersistenceCommonDescriptor) jpaFacet.getConfig();
        List<PersistenceUnitCommon> persistenceUnits = persistenceUnitConfig.getAllPersistenceUnit();

        if (!persistenceUnits.isEmpty()) {
            PersistenceUnitCommon persistenceUnit = persistenceUnits.iterator().next();

            for (String entityType : entityTypes) {
                addEntityClass(persistenceUnit, entityType);
            }
        }

        jpaFacet.saveConfig(persistenceUnitConfig);
    }

    public List<String> getProjectEntityTypes(Project selectedProject) {
        JavaSourceFacet javaSourceFacet = selectedProject.getFacet(JavaSourceFacet.class);
        return findEntityTypes(javaSourceFacet.getBasePackageDirectory().getFullyQualifiedName(), javaSourceFacet.getBasePackage());
    }

    public ArrayList<String> getBasicIdentityModelEntityTypes(Project selectedProject) {
        PicketLinkFacetIDM picketLinkFacetIDM = selectedProject.getFacet(PicketLinkFacetIDM.class);
        DependencyQueryBuilder query = DependencyQueryBuilder
            .create(DependencyBuilder
                .create(MavenDependencies.PICKETLINK_IDM_SIMPLE_SCHEMA_DEPENDENCY)
                .setVersion(picketLinkFacetIDM.getPicketLinkVersion())
                .getCoordinate());

        Dependency dependency = this.dependencyResolver.resolveArtifact(query);
        FileResource<?> artifact = dependency.getArtifact();

        return findEntityTypes(artifact.getFullyQualifiedName(), BASIC_IDENTITY_ENTITY_MODEL_PACKAGE_NAME);
    }

    private ArrayList<String> findEntityTypes(String rootPath, final String packageName) {
        final ArrayList<String> entityTypes = new ArrayList<>();

        try (FileSystem fs = FileSystems.newFileSystem(Paths.get(rootPath), null)) {
            final String packagePath = File.separator + packageName.replace('.', File.separatorChar);

            Files.walkFileTree(fs.getPath(packagePath), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String filePath = file.toString();
                    String suffix = ".class";

                    if (filePath.endsWith(suffix)) {
                        filePath = filePath.substring(0, filePath.indexOf(suffix));

                        if (filePath.startsWith(File.separator)) {
                            filePath = filePath.substring(1);
                        }

                        String typeName = filePath.replace(File.separatorChar, '.');

                        try {
                            Class<?> type = Thread.currentThread().getContextClassLoader().loadClass(typeName);

                            if (isMappedEntity(type)) {
                                entityTypes.add(type.getName());
                            }
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException("Could not load type [" + typeName + "].", e);
                        }
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Could not find entity types.", e);

        }

        return entityTypes;
    }

    private boolean isConfigureBasicIdentityModel(Configuration configuration) {
        return configuration.containsKey(ConfigurationOperations.Properties.PICKETLINK_IDENTITY_BASIC_MODEL
            .name()) && configuration.getBoolean(ConfigurationOperations.Properties.PICKETLINK_IDENTITY_BASIC_MODEL.name());
    }

    private void addEntityClass(PersistenceUnitCommon pu, String entityType) {
        if (!pu.getAllClazz().contains(entityType)) {
            pu.clazz(entityType);
        }
    }

    private boolean isMappedEntity(Class<?> cls) {
        while (!cls.equals(Object.class)) {
            for (Annotation a : cls.getAnnotations()) {
                if (a.annotationType().getName().startsWith(JPA_ANNOTATION_PACKAGE)) {
                    return true;
                }
            }

            // No class annotation was found, check the fields
            for (Field f : cls.getDeclaredFields()) {
                for (Annotation a : f.getAnnotations()) {
                    if (a.annotationType().getName().startsWith(JPA_ANNOTATION_PACKAGE)) {
                        return true;
                    }
                }
            }

            // Check the superclass
            cls = cls.getSuperclass();
        }

        return false;
    }
}
