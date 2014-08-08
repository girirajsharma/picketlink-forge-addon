package org.picketlink.tools.forge;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

@RunWith(Arquillian.class)
public class SimpleCommandTestCase extends AbstractTestCase {

    @Inject
    private MavenDependencies mavenDependencies;

    @Test
    public void testSetupWithoutParameters() throws Exception {
        assertSuccessfulResult(
            executeShellCommand("picketlink-setup --identityStoreType file")
        );

        System.out.println(1);
    }
}