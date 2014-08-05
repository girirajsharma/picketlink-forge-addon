package org.picketlink.tools.forge;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class IdentityManagementSetupCommandTestCase extends AbstractTestCase {

    @Test
    public void testSetup() throws Exception {
        assertSuccessfulResult(
            executeShellCommand("picketlink-identity-management-setup --named default.config --identityStoreType JPA")
        );
    }
}