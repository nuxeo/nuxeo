/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * @since 11.1
 */
public class TestNuxeoRuntimeService {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void before() {
        // check runtime is not yet available
        assertNull(Framework.getRuntime());
    }

    @After
    public void after() throws InterruptedException {
        var runtime = Framework.getRuntime();
        if (runtime != null) {
            runtime.stop();
        }
    }

    @Test
    public void testRuntimeWithDeployAll() {
        // create a runtime and start it
        NuxeoRuntimeService runtime = new NuxeoRuntimeService.Builder(folder.getRoot()).deployAll().start();

        assertEquals(1, runtime.bundleContexts.size());
        assertTrue(runtime.bundleContexts.containsKey("org.nuxeo.runtime"));
        assertEquals(9, runtime.bundleContexts.get("org.nuxeo.runtime").components.size());

        assertNotNull(Framework.getRuntime());
        assertNotNull(runtime.getService(ConfigurationService.class));
    }

    @Test
    public void testRuntimeWithDeployBundle() {
        // create a runtime and start it
        NuxeoRuntimeService runtime = new NuxeoRuntimeService.Builder(folder.getRoot()).deploy("org.nuxeo.runtime")
                                                                                       .start();

        assertEquals(1, runtime.bundleContexts.size());
        assertTrue(runtime.bundleContexts.containsKey("org.nuxeo.runtime"));
        assertEquals(9, runtime.bundleContexts.get("org.nuxeo.runtime").components.size());

        assertNotNull(Framework.getRuntime());
        assertNotNull(runtime.getService(ConfigurationService.class));
    }

    @Test
    public void testRuntimeWithDeployComponent() {
        // create a runtime and start it
        NuxeoRuntimeService runtime = new NuxeoRuntimeService.Builder(folder.getRoot()).deploy(
                "org.nuxeo.runtime:OSGI-INF/ConfigurationService.xml").start();

        assertEquals(1, runtime.bundleContexts.size());
        assertTrue(runtime.bundleContexts.containsKey("org.nuxeo.runtime"));
        assertEquals(1, runtime.bundleContexts.get("org.nuxeo.runtime").components.size());
        assertEquals("org.nuxeo.runtime.ConfigurationService",
                runtime.bundleContexts.get("org.nuxeo.runtime").components.get(0).getName());

        assertNotNull(Framework.getRuntime());
        assertNotNull(runtime.getService(ConfigurationService.class));
    }

    @Test
    public void testRuntimeWithDeployComponentNotDeclaredInBundle() {
        // create a runtime and start it
        NuxeoRuntimeService runtime = new NuxeoRuntimeService.Builder(
                folder.getRoot()).deployAll()
                                 .deploy("org.nuxeo.runtime.test:test-configuration-service-contrib.xml")
                                 .start();

        assertEquals(2, runtime.bundleContexts.size());
        assertTrue(runtime.bundleContexts.containsKey("org.nuxeo.runtime"));
        assertEquals(9, runtime.bundleContexts.get("org.nuxeo.runtime").components.size());
        assertTrue(runtime.bundleContexts.containsKey("org.nuxeo.runtime.test"));
        assertEquals(1, runtime.bundleContexts.get("org.nuxeo.runtime.test").components.size());
        assertEquals("org.nuxeo.runtime.test.configuration.contrib",
                runtime.bundleContexts.get("org.nuxeo.runtime.test").components.get(0).getName());

        assertNotNull(Framework.getRuntime());
        assertNotNull(runtime.getService(ConfigurationService.class));
        assertTrue(runtime.getService(ConfigurationService.class).isBooleanTrue("nuxeo.runtime.service.test"));
    }

    @Test
    public void testRuntimeFailedWithAbsentBundle() {
        try {
            // create a runtime and start it
            new NuxeoRuntimeService.Builder(folder.getRoot()).deployAll().deploy("org.nuxeo.rendition").start();
            fail("Nuxeo Runtime start should have failed");
        } catch (RuntimeServiceException e) {
            assertEquals("Bundle: org.nuxeo.rendition is not present in Nuxeo Runtime class loader", e.getMessage());
        }
    }

    @Test
    public void testRuntimeFailedWithAbsentComponent() {
        try {
            // create a runtime and start it
            new NuxeoRuntimeService.Builder(folder.getRoot()).deployAll()
                                                             .deploy("org.nuxeo.runtime:ListenerExtension.xml")
                                                             .start();
            fail("Nuxeo Runtime start should have failed");
        } catch (RuntimeServiceException e) {
            assertEquals("Component: ListenerExtension.xml was not found in bundle: org.nuxeo.runtime", e.getMessage());
        }
    }

    @Test
    public void testRuntimeContextIsolation() {
        // create a runtime and start it
        NuxeoRuntimeService runtime = new NuxeoRuntimeService.Builder(folder.getRoot()).deployAll().start();
        NuxeoRuntimeContext runtimeContext = runtime.bundleContexts.get("org.nuxeo.runtime");
        // resource should not be found
        var resource = runtimeContext.getResource("test-configuration-service-contrib.xml");
        assertNull(resource);
    }

}
