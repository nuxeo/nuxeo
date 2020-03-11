/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @since 9.10
 */
public class TestJarUtils {

    public static final String BUILT_JAR_NAME = "built-jar.jar";

    private Path resourcesRoot;

    @Before
    public void setUp() throws Exception {
        resourcesRoot = Paths.get(getClass().getResource("/").toURI());
    }

    @After
    public void tearDown() throws Exception {
        Files.deleteIfExists(resourcesRoot.resolve(BUILT_JAR_NAME));
    }

    @Test
    public void testZipDirectory() throws Exception {
        Path source = resourcesRoot.resolve("TestJarUtils");
        Path target = resourcesRoot.resolve(BUILT_JAR_NAME);
        target = JarUtils.zipDirectory(source, target);

        Manifest manifest = JarUtils.getManifest(target.toFile());
        assertNotNull(manifest);
        Attributes mainAttributes = manifest.getMainAttributes();
        assertNotNull(mainAttributes);
        assertEquals("1.0", mainAttributes.getValue("Manifest-Version"));
        assertEquals("Nuxeo JarUtils test", mainAttributes.getValue("Bundle-Name"));
        assertEquals("2", mainAttributes.getValue("Bundle-ManifestVersion"));
        assertEquals("org.nuxeo.common.jarutils.test;singleton:=true", mainAttributes.getValue("Bundle-SymbolicName"));
    }

}
