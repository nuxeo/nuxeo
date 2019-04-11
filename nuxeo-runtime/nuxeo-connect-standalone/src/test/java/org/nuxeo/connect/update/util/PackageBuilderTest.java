/*
 * (C) Copyright 2012-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        builder.name("nuxeo-automation").version("5.3.2").type(PackageType.ADDON);
        builder.title("Nuxeo Automation").description("The automation framework");
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
            xmap.load(new ByteArrayInputStream(xml.getBytes()));
        } catch (IOException e) {
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
