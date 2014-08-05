package org.picketlink.tools.forge;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFacet;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.shell.Shell;
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
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;

@RunWith(Arquillian.class)
public class IdentityManagementSetupCommandTestCase {

    @Inject
    private ShellTest shellTest;

    @Inject
    private ProjectFactory projectFactory;

    private Project selectedProject;

    @Deployment
    @Dependencies({
        @AddonDependency(name = "org.jboss.forge.addon:shell-test-harness"),
        @AddonDependency(name = "org.jboss.forge.addon:projects"),
        @AddonDependency(name = "org.jboss.forge.addon:maven"),
        @AddonDependency(name = "org.jboss.forge.furnace.container:cdi"),
        @AddonDependency(name = "org.picketlink.tools.forge:picketlink-forge-addon")
    })
    public static ForgeArchive getDeployment() {
        return ShrinkWrap
            .create(ForgeArchive.class)
            .addBeansXML()
            .addAsAddonDependencies(
                AddonDependencyEntry.create("org.jboss.forge.addon:shell-test-harness"),
                AddonDependencyEntry.create("org.jboss.forge.addon:projects"),
                AddonDependencyEntry.create("org.jboss.forge.addon:maven"),
                AddonDependencyEntry.create("org.jboss.forge.furnace.container:cdi"),
                AddonDependencyEntry.create("org.picketlink.tools.forge:picketlink-forge-addon"));
    }

    @Before
    public void onBefore() throws Exception {
        this.selectedProject = this.projectFactory.createTempProject(Arrays.<Class<? extends ProjectFacet>>asList(JavaSourceFacet.class));

        this.shellTest.getShell().setCurrentResource(this.selectedProject.getRoot());

        Result result = this.shellTest.execute(("picketlink-setup"), 10, TimeUnit.SECONDS);

        assertFalse(Failed.class.isInstance(result));
    }

    @After
    public void onAfter() throws Exception {
        this.shellTest.clearScreen();
    }

    @Test
    public void testSetupShell() throws Exception {
        Shell shell = this.shellTest.getShell();
        Result result = this.shellTest
            .execute(("picketlink-identity-management-setup --named default.config --identityStoreType JPA"),
                10,
                TimeUnit.SECONDS);

        assertFalse(Failed.class.isInstance(result));
    }
}