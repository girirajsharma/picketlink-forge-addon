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

import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.DependencyQuery;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.dependencies.builder.DependencyQueryBuilder;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.MetadataFacet;
import org.jboss.forge.addon.projects.facets.PackagingFacet;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;
import org.picketlink.idm.model.AttributedType;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
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

/**
 * <p>Provides methods to manipulate project's persistence unit in order to properly configure the JPA Identity Store.</p>
 *
 * @author Pedro Igor
 */
public class AttributedTypeManager {

    @Inject
    private DependencyResolver dependencyResolver;

    public Set<String> getAttributedTypes(final Project selectedProject) {
        final Resource<?> artifactResource = performBuild(selectedProject);
        return findAttributedTypes(selectedProject, artifactResource);
    }

    private Resource<?> performBuild(Project selectedProject) {
        PackagingFacet packagingFacet = selectedProject.getFacet(PackagingFacet.class);

        return packagingFacet
            .createBuilder()
            .addArguments("clean", "install")
            .runTests(false)
            .build();
    }

    private Set<String> findAttributedTypes(final Project selectedProject, Resource<?> projectArtifact) {
        final Set<String> entityTypes = new HashSet<>();

        try (FileSystem fs = FileSystems.newFileSystem(Paths.get(projectArtifact.getFullyQualifiedName()), null)) {
            JavaSourceFacet javaFacet = selectedProject.getFacet(JavaSourceFacet.class);
            final MavenFacet mavenFacet = selectedProject.getFacet(MavenFacet.class);
            final String basePackagePath = javaFacet.getBasePackage().replace('.', File.separatorChar);
            final String packageRootPath = getPackageRootPath(mavenFacet);

            String fileSystemPath = packageRootPath + basePackagePath;

            Files.walkFileTree(fs.getPath(fileSystemPath), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String filePath = file.toString();
                    String suffix = ".class";

                    if (filePath.endsWith(suffix)) {
                        filePath = filePath.substring(0, filePath.indexOf(suffix));

                        if (filePath.startsWith(File.separator)) {
                            filePath = filePath.substring(1);
                        }

                        filePath = filePath.substring(filePath.indexOf(basePackagePath));

                        String typeName = filePath.replace(File.separatorChar, '.');
                        URLClassLoader classLoader = getProjectClassLoader(selectedProject);

                        try {
                            Class<?> type = classLoader.loadClass(typeName);

                            if (isAttributedType(type, classLoader)) {
                                entityTypes.add(type.getName());
                            }
                        } catch (Exception e) {
                            throw new RuntimeException("Could not load type [" + typeName + "].", e);
                        } finally {
                            if (classLoader != null) {
                                try {
                                    classLoader.close();
                                } catch (IOException ignore) {
                                }
                            }
                        }
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
//            throw new RuntimeException("Could not find entity types.", e);
        }

        return entityTypes;
    }

    private String getPackageRootPath(MavenFacet mavenFacet) {
        if ("war".equalsIgnoreCase(mavenFacet.getModel().getPackaging())) {
            return "/WEB-INF/classes/";
        }

        return "/";
    }

    private URLClassLoader getProjectClassLoader(Project selectedProject) {
        try {
            MavenFacet mavenFacet = selectedProject.getFacet(MavenFacet.class);
            MetadataFacet metadataFacet = selectedProject.getFacet(MetadataFacet.class);
            DependencyQuery projectDependencyQuery = create(metadataFacet.getOutputDependency(), mavenFacet.getModel().getPackaging());
            FileResource<?> projectArtifact = getProjectArtifact(selectedProject);
            List<URL> dependenciesURL = new ArrayList<>();

            dependenciesURL.add(new URL(formatJarUrl(projectArtifact, getPackageRootPath(mavenFacet))));

            for (Dependency dependency: dependencyResolver.resolveDependencies(projectDependencyQuery)) {
                dependenciesURL.add(new URL(formatJarUrl(dependency.getArtifact(), "/")));
            }

            return new URLClassLoader(dependenciesURL.toArray(new URL[dependenciesURL.size()]));
        } catch (Exception e) {
            throw new RuntimeException("Could not create project's class loader.", e);
        }
    }

    private FileResource<?> getProjectArtifact(Project selectedProject) {
        MavenFacet mavenFacet = selectedProject.getFacet(MavenFacet.class);
        MetadataFacet metadataFacet = selectedProject.getFacet(MetadataFacet.class);
        DependencyQuery projectDependencyQuery = create(metadataFacet.getOutputDependency(), mavenFacet.getModel().getPackaging());

        return dependencyResolver.resolveArtifact(projectDependencyQuery).getArtifact();
    }

    private String formatJarUrl(FileResource<?> artifact, String packageRootPath) {
        return "jar:file:" + artifact.getFullyQualifiedName() + "!" + packageRootPath;
    }

    private boolean isAttributedType(Class<?> cls, ClassLoader classLoader) {
        while (!cls.equals(Object.class)) {
            if (loadClass(AttributedType.class.getName(), classLoader).isAssignableFrom(cls)) {
                return true;
            }

            // Check the superclass
            cls = cls.getSuperclass();
        }

        return false;
    }

    private Class<?> loadClass(String name, ClassLoader classLoader) {
        try {
            return classLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not find type [" + name + "].", e);
        }
    }

    private DependencyQuery create(Dependency dependency, String packaging) {
        return DependencyQueryBuilder
            .create(DependencyBuilder
                .create(dependency)
                .setPackaging(packaging)
                .getCoordinate());
    }
}
