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

import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.junit.Test;
import org.picketlink.event.SecurityConfigurationEvent;

import javax.enterprise.event.Observes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Pedro Igor
 */
public class JavaSourceTestCase {

    @Test
    public void testSecurityConfiguration() {
        JavaClassSource javaClass = Roaster.create(JavaClassSource.class)
            .setName("SecurityConfiguration")
            .setPublic();

        javaClass
            .addMethod()
            .setName("onInit")
            .setPublic()
            .setReturnTypeVoid()
            .setBody("if (true) {}")
            .addParameter(SecurityConfigurationEvent.class, "event")
            .addAnnotation(Observes.class);

        assertFalse(javaClass.hasSyntaxErrors());

        assertEquals("\n" +
            "import org.picketlink.event.SecurityConfigurationEvent;\n" +
            "import javax.enterprise.event.Observes;\n" +
            "\n" +
            "public class SecurityConfiguration\n" +
            "{\n" +
            "\n" +
            "   public void onInit(@Observes SecurityConfigurationEvent event)\n" +
            "   {\n" +
            "      if (true)\n" +
            "      {\n" +
            "      }\n" +
            "   }\n" +
            "}", javaClass.toString());

        System.out.println(javaClass.toString());
    }

}
