/*
 * (C) Copyright 2012-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Mathieu Guillaume
 *
 */
package org.nuxeo.connect.update.util;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.connect.update.standalone.PackageTestCase;
import org.nuxeo.connect.update.standalone.StandaloneUpdateService;
import org.nuxeo.connect.update.xml.PackageDefinitionImpl;

public class PackageBuilderTest extends PackageTestCase {

    @Test
    public void testPackageBuilder() {
        PackageBuilder builder = new PackageBuilder();
        builder.name("nuxeo-automation").version("5.3.2").type(
                PackageType.ADDON);
        builder.title("Nuxeo Automation").description(
                "The automation framework");
        builder.platform("dm-5.3.2");
        builder.dependency("nuxeo-core:5.3.2");
        builder.conflict("package-that-does-not-exist");
        builder.provide("virtual-package");
        builder.classifier("OpenSource");
        builder.installer("MyInstaller", true);
        builder.addLicense("My License");

        String xml = builder.buildManifest();
        // System.out.println(xml);

        XMap xmap = StandaloneUpdateService.createXmap();
        try {
            PackageDefinitionImpl pdef = (PackageDefinitionImpl) xmap.load(new ByteArrayInputStream(
                    xml.getBytes()));
            // System.out.println(pdef);
        } catch (Exception e) {
            fail("Could not create package definition");
        }

        try {
            File file = builder.build();
            assertTrue(file.exists());
        } catch (IOException e) {
            fail("Could not create package file");
        }
    }

}
