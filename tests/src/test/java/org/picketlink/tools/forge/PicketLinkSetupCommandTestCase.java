package org.picketlink.tools.forge;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.picketlink.tools.forge.ConfigurationOperations.DEFAULT_TOP_LEVEL_PACKAGE;

@RunWith(Arquillian.class)
public class PicketLinkSetupCommandTestCase extends AbstractTestCase {

    @Inject
    private MavenDependencies mavenDependencies;

    @Test
    public void testSetupWithoutParameters() throws Exception {
        assertSuccessfulResult(
            executeShellCommand("picketlink-setup")
        );

        assertCommandResult(getLatestVersion(), true, null);
    }

    private Coordinate getLatestVersion() {
        return this.mavenDependencies.resolveLatestVersion();
    }

    @Test
    public void testSetupWithVersion() throws Exception {
        Coordinate latestVersion = getLatestVersion();
        List<Coordinate> availableVersions = this.mavenDependencies.resolveVersions(false);
        Coordinate selectedVersion = null;

        for (Coordinate version : availableVersions) {
            if (!version.equals(latestVersion) && version.getVersion().contains("2.7.0")) {
                selectedVersion = version;
                break;
            }
        }

        assertNotNull(selectedVersion);

        assertSuccessfulResult(
            executeShellCommand("picketlink-setup --version " + selectedVersion.getVersion())
        );

        assertCommandResult(selectedVersion, false, null);
    }

    @Test
    public void testSetupWithSnapshotVersion() throws Exception {
        List<Coordinate> availableVersions = this.mavenDependencies.resolveVersions(true);
        Coordinate selectedVersion = null;

        for (Coordinate version : availableVersions) {
            if (version.getVersion().equals("2.7.0-SNAPSHOT")) {
                selectedVersion = version;
                break;
            }
        }

        assertNotNull(selectedVersion);

        assertSuccessfulResult(
            executeShellCommand("picketlink-setup --showSnapshots --version " + selectedVersion.getVersion())
        );

        assertCommandResult(selectedVersion, false, null);
    }

    @Test
    public void testSetupWithTopLevelPackage() throws Exception {
        String packageName = "custom";

        assertSuccessfulResult(
            executeShellCommand("picketlink-setup --topLevelPackage " + packageName)
        );

        assertCommandResult(getLatestVersion(), false, packageName);
    }

    public void assertCommandResult(Coordinate expectedVersion, boolean assertDependencies, String packageName) throws Exception {
        Project selectedProject = getSelectedProject();
        PicketLinkFacetBase picketlinkFacet = selectedProject.getFacet(PicketLinkFacetBase.class);

        assertNotNull(picketlinkFacet);

        assertEquals(expectedVersion.getVersion(), picketlinkFacet.getPicketLinkVersion());

        if (assertDependencies) {
            DependencyFacet dependencyFacet = selectedProject.getFacet(DependencyFacet.class);

            assertTrue(dependencyFacet.hasDirectManagedDependency(MavenDependencies.PICKETLINK_BOM_DEPENDENCY));
            assertTrue(dependencyFacet.hasEffectiveDependency(MavenDependencies.PICKETLINK_API_DEPENDENCY));
            assertTrue(dependencyFacet.hasEffectiveDependency(MavenDependencies.PICKETLINK_IMPL_DEPENDENCY));
            assertTrue(dependencyFacet.hasEffectiveDependency(MavenDependencies.PICKETLINK_IDM_API_DEPENDENCY));
            assertTrue(dependencyFacet.hasEffectiveDependency(MavenDependencies.PICKETLINK_IDM_IMPL_DEPENDENCY));
        }

        JavaSourceFacet javaFacet = selectedProject.getFacet(JavaSourceFacet.class);

        if (packageName == null) {
            packageName = DEFAULT_TOP_LEVEL_PACKAGE;
        }

        assertNotNull(javaFacet.getJavaResource(javaFacet.getBasePackage() + "." + packageName + ".Securityconfiguration"));
        assertNotNull(javaFacet.getJavaResource(javaFacet.getBasePackage() + "." + packageName + ".Resources"));
    }
}