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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFacet;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.shell.test.ShellTest;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.Dependencies;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.After;
import org.junit.Before;

import javax.inject.Inject;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * @author Pedro Igor
 */
public abstract class AbstractTestCase {

    @Inject
    private ShellTest shellTest;

    @Inject
    private ProjectFactory projectFactory;

    private Project selectedProject;

    @Deployment
    @Dependencies({
        @AddonDependency(name = "org.jboss.forge.addon:shell-test-harness"),
        @AddonDependency(name = "org.jboss.forge.addon:maven"),
        @AddonDependency(name = "org.jboss.forge.addon:javaee"),
        @AddonDependency(name = "org.picketlink.tools.forge:picketlink-forge-addon")
    })
    public static ForgeArchive deploy() {
        return ShrinkWrap
            .create(ForgeArchive.class)
            .addClass(AbstractTestCase.class)
            .addClass(MyUser.class)
            .addBeansXML()
            .addAsAddonDependencies(
                AddonDependencyEntry.create("org.jboss.forge.addon:shell-test-harness"),
                AddonDependencyEntry.create("org.jboss.forge.addon:projects"),
                AddonDependencyEntry.create("org.jboss.forge.addon:maven"),
                AddonDependencyEntry.create("org.jboss.forge.addon:javaee"),
                AddonDependencyEntry.create("org.jboss.forge.furnace.container:cdi"),
                AddonDependencyEntry.create("org.picketlink.tools.forge:picketlink-forge-addon"));
    }

    @Before
    public void onBefore() throws Exception {
        this.selectedProject = this.projectFactory.createTempProject(Arrays.<Class<? extends ProjectFacet>>asList(JavaSourceFacet.class));

        JavaSourceFacet javaFacet = this.selectedProject.getFacet(JavaSourceFacet.class);

        JavaResource childOfType = javaFacet.getBasePackageDirectory()
            .getChildOfType(JavaResource.class, MyUser.class.getSimpleName() + ".java");

        childOfType.setContents(new FileInputStream("/pedroigor/java/workspace/jboss/picketlink/picketlink-forge-addon/tests/src/test/java/org/picketlink/tools/forge/MyUser.java"));

        this.shellTest.getShell().setCurrentResource(this.selectedProject.getRoot());

        Result result = executeShellCommand("picketlink-setup");

        assertFalse(Failed.class.isInstance(result));
    }

    @After
    public void onAfter() throws Exception {
        this.shellTest.clearScreen();
    }

    protected Result executeShellCommand(String command) {
        try {
            Result result = getShellTest()
                .execute((command),
                    100000,
                    TimeUnit.SECONDS);

            this.selectedProject = this.projectFactory.findProject(this.selectedProject.getRoot());

            return result;
        } catch (TimeoutException e) {
            fail("Command [" + command + "] timeout.");
        }

        return null;
    }

    protected void assertSuccessfulResult(Result result) {
        assertFalse(Failed.class.isInstance(result));
    }

    protected ShellTest getShellTest() {
        return this.shellTest;
    }

    protected ProjectFactory getProjectFactory() {
        return this.projectFactory;
    }

    protected Project getSelectedProject() {
        return this.selectedProject;
    }
}
