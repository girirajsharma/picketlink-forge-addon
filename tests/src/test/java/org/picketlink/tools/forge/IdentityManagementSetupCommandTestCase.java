package org.picketlink.tools.forge;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.javaee.jpa.JPAFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.shrinkwrap.descriptor.api.persistence.PersistenceCommonDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence.PersistenceUnitCommon;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.picketlink.tools.forge.operations.PersistenceOperations;

import javax.inject.Inject;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
public class IdentityManagementSetupCommandTestCase extends AbstractTestCase {

    @Inject
    private PersistenceOperations persistenceManager;

    @Test
    @Ignore("See why JPAFacet is not being installed. This only happen when executing from here. Shell is fine.")
    public void testDefaultSetup() throws Exception {
        assertSuccessfulResult(
            executeShellCommand("picketlink-identity-management-setup --named default.config --identityStoreType JPA")
        );

        Project selectedProject = getSelectedProject();
        JPAFacet jpaFacet = selectedProject.getFacet(JPAFacet.class);
        PersistenceCommonDescriptor persistenceUnitConfig = (PersistenceCommonDescriptor) jpaFacet.getConfig();
        List<PersistenceUnitCommon> persistenceUnits = persistenceUnitConfig.getAllPersistenceUnit();

        assertFalse(persistenceUnits.isEmpty());

        PersistenceUnitCommon persistenceUnitCommon = persistenceUnits.get(0);

        List allClazz = persistenceUnitCommon.getAllClazz();

        assertTrue(allClazz.containsAll(this.persistenceManager.getBasicIdentityModelEntityTypes(selectedProject, null)));
    }
}