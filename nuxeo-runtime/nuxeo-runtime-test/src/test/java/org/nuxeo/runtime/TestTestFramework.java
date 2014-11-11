/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime;

import java.net.URL;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;


public class TestTestFramework extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        runtime = Framework.getRuntime();
        assertNotNull(runtime);
    }

    public void testInitialize() {
        assertNotNull(runtime.getHome());
    }

    public void testSetProperty() {
        runtime.getProperties().put("toto", "titi");
        assertEquals("titi", runtime.getProperty("toto"));
    }

    public void testLookupBundleUrl() throws Exception {

        urls = new URL[] {
                new URL("file:/repo/org/nuxeo/nuxeo-runtime-1.4-SNAPSHOT.jar"),
                new URL("file:/projects/nuxeo-core-test/bin/test"),
                new URL("file:/projects/nuxeo-core-test/bin/main"),
                new URL("file:/projects/nuxeo-common/target/test-classes"),
                new URL("file:/projects/nuxeo-common/target/classes"),
        };
        assertEquals(urls[0], lookupBundleUrl("nuxeo-runtime"));
        assertEquals(urls[2], lookupBundleUrl("nuxeo-core-test"));
        assertEquals(urls[4], lookupBundleUrl("nuxeo-common"));
    }

    public void testIsVersionSuffix() {
        assertTrue(isVersionSuffix(""));
        assertTrue(isVersionSuffix("-1.4-SNAPSHOT"));
        assertTrue(isVersionSuffix("-1.3.2"));
        assertTrue(isVersionSuffix("-1.3.2.jar"));
        assertFalse(isVersionSuffix("-api-5.1.1.jar"));
    }

}
