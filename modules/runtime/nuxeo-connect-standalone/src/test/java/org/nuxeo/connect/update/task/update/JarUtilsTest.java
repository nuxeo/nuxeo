/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.connect.update.task.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * @since 2025.1
 */
public class JarUtilsTest {

    @Test
    public void testFindJarVersion() {
        var match = JarUtils.findJarVersion("netty-codec-4.1.118.Final.jar");
        assertNotNull(match);
        assertEquals("netty-codec", match.object);
        assertEquals("4.1.118.Final", match.version);
    }

    @Test
    public void testFindJarVersionForSNAPSHOT() {
        var match = JarUtils.findJarVersion("nuxeo-platform-audit-core-2025.0-SNAPSHOT.jar");
        assertNotNull(match);
        assertEquals("nuxeo-platform-audit-core", match.object);
        assertEquals("2025.0-SNAPSHOT", match.version);
    }

    @Test
    public void testFindJarVersionForMilestone() {
        var match = JarUtils.findJarVersion("jersey-client-4.0.0-M1.jar");
        assertNotNull(match);
        assertEquals("jersey-client", match.object);
        assertEquals("4.0.0-M1", match.version);
    }

    // NXP-33139
    @Test
    public void testFindJarVersionWithDashInVersion() {
        var match = JarUtils.findJarVersion("zstd-jni-1.5.5-11.jar");
        assertNotNull(match);
        assertEquals("zstd-jni", match.object);
        assertEquals("1.5.5-11", match.version);
    }

    // NXP-33359
    @Test
    public void testFindJarVersionWithNumberInArtifactNameAfterDash() {
        var match = JarUtils.findJarVersion("nuxeo-3d-viewer-core-2023.2-SNAPSHOT.jar");
        assertNotNull(match);
        assertEquals("nuxeo-3d-viewer-core", match.object);
        assertEquals("2023.2-SNAPSHOT", match.version);
    }

    // NXP-33359
    @Test
    public void testFindJarVersionWithNumberInArtifactNameBeforeDash() {
        var match = JarUtils.findJarVersion("b1-1.1.jar");
        assertNotNull(match);
        assertEquals("b1", match.object);
        assertEquals("1.1", match.version);
    }

    // NXP-33359
    @Test
    public void testFindJarVersionWithDotInArtifactName() {
        var match = JarUtils.findJarVersion("jakarta.servlet-api-6.0.0.jar");
        assertNotNull(match);
        assertEquals("jakarta.servlet-api", match.object);
        assertEquals("6.0.0", match.version);
    }

    // NXP-33359
    @Test
    public void testFindJarVersionWithoutDotInVersion() {
        var match = JarUtils.findJarVersion("oauth-20090531.jar");
        assertNotNull(match);
        assertEquals("oauth", match.object);
        assertEquals("20090531", match.version);
    }

    // NXP-33359
    @Test
    public void testFindJarVersionWithUnderscoreInVersion() {
        var match = JarUtils.findJarVersion("core-3.4.2.v_883_R34x.jar");
        assertNotNull(match);
        assertEquals("core", match.object);
        assertEquals("3.4.2.v_883_R34x", match.version);
    }

    // NXP-33359
    @Test
    public void testFindJarVersionWithVersionInArtifactName() {
        var match = JarUtils.findJarVersion("log4j-1.2-api-2.25.2.jar");
        assertNotNull(match);
        assertEquals("log4j-1.2-api", match.object);
        assertEquals("2.25.2", match.version);
    }

    // NXP-33359
    @Test
    public void testFindJarVersionWithHashInVersion() {
        var match = JarUtils.findJarVersion("jmd-0.8.1-tomasol-3e60e36137.jar");
        assertNotNull(match);
        assertEquals("jmd", match.object);
        assertEquals("0.8.1-tomasol-3e60e36137", match.version);
    }
}
