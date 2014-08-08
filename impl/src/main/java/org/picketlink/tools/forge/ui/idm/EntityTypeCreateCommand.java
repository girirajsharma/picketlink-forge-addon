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
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.picketlink.idm.jpa.annotations.AttributeValue;
import org.picketlink.idm.jpa.annotations.entity.IdentityManaged;
import org.picketlink.tools.forge.PicketLinkIDMFacet;
import org.picketlink.tools.forge.operations.AttributedTypeOperations;

import javax.inject.Inject;
import javax.persistence.Entity;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.Set;

import static org.picketlink.tools.forge.ConfigurationOperations.Properties.PICKETLINK_TOP_LEVEL_PACKAGE_NAME;
import static org.picketlink.tools.forge.util.ResourceUtil.createJavaResourceIfNecessary;

/**
 * @author Pedro Igor
 */
@FacetConstraint(value = PicketLinkIDMFacet.class)
public class EntityTypeCreateCommand extends AbstractProjectCommand {

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    private AttributedTypeOperations attributedTypeOperations;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(IdentityManagementSetupWizard.class)
            .name("PicketLink JPA Identity Store Entity: Create")
            .description("Creates a JPA Entity for Identity Types.")
            .category(Categories.create("picketlink"));
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project selectedProject = getSelectedProject(context);
        Configuration configuration = selectedProject.getFacet(ConfigurationFacet.class).getConfiguration();
        JavaSourceFacet javaFacet = selectedProject.getFacet(JavaSourceFacet.class);
        String securityPackageName = configuration.getString(PICKETLINK_TOP_LEVEL_PACKAGE_NAME.name());
        String modelPackageName = javaFacet.getBasePackage() + "." + securityPackageName + "." + "model" + "." + "entity";
        Set<String> attributedTypes = this.attributedTypeOperations.getAttributedTypes(selectedProject);
        boolean hasIdentityType = false;
        URLClassLoader projectClassLoader = this.attributedTypeOperations.getProjectClassLoader(selectedProject);

        try {
            for (String attributedTypeName : attributedTypes) {
                JavaResource attributedTypeResource = javaFacet.getJavaResource(attributedTypeName);
                Class identityTypeType = this.attributedTypeOperations
                    .toIdentityType(attributedTypeResource.getJavaType(), projectClassLoader);

                if (identityTypeType != null) {
                    hasIdentityType = true;
                    DirectoryResource modelDirectoryResource = javaFacet.getSourceDirectory()
                        .getOrCreateChildDirectory(modelPackageName.replace('.', File.separatorChar));
                    String entityName = identityTypeType.getSimpleName() + "EntityType";
                    JavaResource entityResource = modelDirectoryResource.getChildOfType(JavaResource.class, entityName + ".java");
                    JavaClassSource entitySource = Roaster.create(JavaClassSource.class)
                        .setPackage(modelPackageName)
                        .setPublic()
                        .setSuperType(modelPackageName + "." + "IdentityTypeEntity")
                        .setName(entityName);

                    entitySource.addAnnotation(Entity.class);
                    entitySource
                        .addAnnotation(IdentityManaged.class)
                        .setClassValue(identityTypeType);

                    for (Field declaredField : identityTypeType.getDeclaredFields()) {
                        if (this.attributedTypeOperations.isAttributeProperty(declaredField)) {
                            entitySource.addField()
                                .setPrivate()
                                .setName(declaredField.getName())
                                .setType(declaredField.getType())
                                .addAnnotation(AttributeValue.class);

                            entitySource.addMethod()
                                .setPublic()
                                .setReturnType(declaredField.getType())
                                .setName("get" + String.valueOf(declaredField.getName().charAt(0)).toUpperCase() + declaredField
                                    .getName()
                                    .substring(1))
                                .setBody("return this." + declaredField.getName() + ";");

                            MethodSource<JavaClassSource> setterMethod = entitySource.addMethod()
                                .setPublic()
                                .setName("set" + String.valueOf(declaredField.getName().charAt(0)).toUpperCase() + declaredField
                                    .getName()
                                    .substring(1));

                            setterMethod.addParameter(declaredField.getType(), declaredField.getName());

                            setterMethod.setBody("this." + declaredField.getName() + " = " + declaredField.getName() + ";");
                        }
                    }

                    entityResource.setContents(entitySource);
                }
            }
        } catch (Exception e) {

        } finally {
            if (projectClassLoader != null) {
                projectClassLoader.close();
            }
        }

        if (hasIdentityType) {
            createJavaResourceIfNecessary(selectedProject, modelPackageName, "PartitionTypeEntity.java", "/scaffold/idm/classes/PartitionTypeEntity.java");
            createJavaResourceIfNecessary(selectedProject, modelPackageName, "IdentityTypeEntity.java", "/scaffold/idm/classes/IdentityTypeEntity.java");
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

}
