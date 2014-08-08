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

import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.DependencyQuery;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.dependencies.builder.DependencyQueryBuilder;
import org.jboss.forge.addon.javaee.jpa.JPAFacet;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.projects.facets.MetadataFacet;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.shrinkwrap.descriptor.api.persistence.PersistenceCommonDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence.PersistenceUnitCommon;
import org.picketlink.tools.forge.MavenDependencies;
import org.picketlink.tools.forge.PicketLinkIDMFacet;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptySet;

/**
 * <p>Provides methods to manipulate project's persistence unit in order to properly configure the JPA Identity Store.</p>
 *
 * @author Pedro Igor
 */
public class PersistenceOperations {

    private static final String JPA_ANNOTATION_PACKAGE = "org.picketlink.idm.jpa.annotations";
    private static final String BASIC_IDENTITY_ENTITY_MODEL_PACKAGE_NAME = "org.picketlink.idm.jpa.model.sample.simple";

    @Inject
    private DependencyResolver dependencyResolver;
    private Set<String> basicModelEntities;

    public Set<String> configure(final Project selectedProject) {
        URLClassLoader classLoader = null;

        try {
            classLoader = getProjectClassLoader(selectedProject);
            MavenFacet mavenFacet = selectedProject.getFacet(MavenFacet.class);
            Set<String> projectEntities = findEntityTypes(selectedProject, getProjectArtifact(selectedProject), getPackageRootPath(mavenFacet), classLoader);
            Set<String> basicIdentityModelEntityTypes = getBasicIdentityModelEntityTypes(selectedProject, classLoader);

            if (projectEntities.isEmpty()) {
                projectEntities = basicIdentityModelEntityTypes;
            }

            JPAFacet jpaFacet = selectedProject.getFacet(JPAFacet.class);
            PersistenceCommonDescriptor persistenceUnitConfig = (PersistenceCommonDescriptor) jpaFacet.getConfig();
            List<PersistenceUnitCommon> persistenceUnits = persistenceUnitConfig.getAllPersistenceUnit();

            if (!persistenceUnits.isEmpty()) {
                PersistenceUnitCommon persistenceUnit = persistenceUnits.iterator().next();

                for (String entityType : projectEntities) {
                    addEntityClass(persistenceUnit, entityType);
                }

                ArrayList classes = new ArrayList(persistenceUnit.getAllClazz());

                persistenceUnit.removeAllClazz();

                for (Object entityClass : classes) {
                    if (basicIdentityModelEntityTypes.contains(entityClass.toString())  && !projectEntities.isEmpty()) {
                        continue;
                    }

                    System.out.println(entityClass.toString());

                    addEntityClass(persistenceUnit, entityClass.toString());
                }
            }

            jpaFacet.saveConfig(persistenceUnitConfig);

        } catch (Exception e) {
            throw new RuntimeException("Could not load type.", e);
        } finally {
            if (classLoader != null) {
                try {
                    classLoader.close();
                } catch (IOException ignore) {
                }
            }
        }

        return emptySet();
    }

    public Set<String> getBasicIdentityModelEntityTypes(Project selectedProject, URLClassLoader classLoader) {
        PicketLinkIDMFacet picketLinkIDMFacet = selectedProject.getFacet(PicketLinkIDMFacet.class);
        DependencyQueryBuilder query = DependencyQueryBuilder
            .create(DependencyBuilder
                .create(MavenDependencies.PICKETLINK_IDM_SIMPLE_SCHEMA_DEPENDENCY)
                .setVersion(picketLinkIDMFacet.getPicketLinkVersion())
                .getCoordinate());
        Dependency dependency = this.dependencyResolver.resolveArtifact(query);

        return findEntityTypes(selectedProject, dependency.getArtifact(), File.separator, classLoader);
    }

    private Set<String> findEntityTypes(final Project selectedProject, Resource<?> projectArtifact, final String packageRootPath, final ClassLoader classLoader) {
        final Set<String> entityTypes = new HashSet<>();

        try (FileSystem fs = FileSystems.newFileSystem(Paths.get(projectArtifact.getFullyQualifiedName()), null)) {
            Files.walkFileTree(fs.getPath(packageRootPath), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String filePath = file.toString();

                    String suffix = ".class";

                    if (filePath.endsWith(suffix)) {
                        filePath = filePath.substring(0, filePath.indexOf(suffix));

                        if (filePath.startsWith(File.separator)) {
                            filePath = filePath.substring(1);
                        }

                        if (!filePath.startsWith(File.separator)) {
                            filePath = File.separator + filePath;
                        }

                        filePath = filePath.substring(packageRootPath.length());

                        String typeName = filePath.replace(File.separatorChar, '.');

                        Class<?> type;

                        try {
                            type = classLoader.loadClass(typeName);
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException("Could not load type [" + typeName + "].", e);
                        }

                        if (isMappedEntity(type)) {
                            entityTypes.add(type.getName());
                        }
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignore) {
        }

        return entityTypes;
    }

    private String getPackageRootPath(MavenFacet mavenFacet) {
        if ("war".equalsIgnoreCase(mavenFacet.getModel().getPackaging())) {
            return "/WEB-INF/classes/";
        }

        return "/";
    }

    public URLClassLoader getProjectClassLoader(Project selectedProject) {
        try {
            MetadataFacet metadataFacet = selectedProject.getFacet(MetadataFacet.class);
            MavenFacet mavenFacet = selectedProject.getFacet(MavenFacet.class);
            DependencyQuery projectDependencyQuery = create(metadataFacet.getOutputDependency(), mavenFacet.getModel()
                .getPackaging());
            Resource<?> projectArtifact = getProjectArtifact(selectedProject);
            List<URL> dependenciesURL = new ArrayList<>();

            dependenciesURL.add(new URL(formatJarUrl(projectArtifact, getPackageRootPath(mavenFacet))));

            for (Dependency dependency : dependencyResolver.resolveDependencies(projectDependencyQuery)) {
                dependenciesURL.add(new URL(formatJarUrl(dependency.getArtifact(), "/")));
            }

            DependencyFacet dependencyFacet = selectedProject.getFacet(DependencyFacet.class);

            List<Dependency> effectiveDependencies = dependencyFacet.getEffectiveDependencies();

            for (Dependency dependency : effectiveDependencies) {
                dependency = this.dependencyResolver.resolveArtifact(create(dependency, "jar"));
                dependenciesURL.add(new URL(formatJarUrl(dependency.getArtifact(), "/")));
            }

            return new URLClassLoader(dependenciesURL.toArray(new URL[dependenciesURL.size()]));
        } catch (Exception e) {
            throw new RuntimeException("Could not create project's class loader.", e);
        }
    }

    private Resource<?> getProjectArtifact(Project selectedProject) {
        MavenFacet mavenFacet = selectedProject.getFacet(MavenFacet.class);
        MetadataFacet metadataFacet = selectedProject.getFacet(MetadataFacet.class);
        DependencyQuery projectDependencyQuery = create(metadataFacet.getOutputDependency(), mavenFacet.getModel().getPackaging());

        return dependencyResolver.resolveArtifact(projectDependencyQuery).getArtifact();
    }

    private String formatJarUrl(Resource<?> artifact, String packageRootPath) {
        return "jar:file:" + artifact.getFullyQualifiedName() + "!" + packageRootPath;
    }

    private DependencyQuery create(Dependency dependency, String packaging) {
        return DependencyQueryBuilder
            .create(DependencyBuilder
                .create(dependency)
                .setPackaging(packaging)
                .getCoordinate());
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