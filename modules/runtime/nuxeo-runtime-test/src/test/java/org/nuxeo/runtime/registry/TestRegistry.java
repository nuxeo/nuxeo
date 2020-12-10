/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.runtime.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.common.xmap.registry.SingleRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 11.5
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.runtime.test.tests:registry-framework.xml")
@Deploy("org.nuxeo.runtime.test.tests:registry-contrib-1.xml")
public class TestRegistry {

    @Inject
    protected SampleComponent service;

    protected void hotDeploy(String contrib) throws Exception {
        hotDeploy(true, contrib);
    }

    protected void hotUndeploy(String contrib) throws Exception {
        hotDeploy(false, contrib);
    }

    protected boolean useHotDeployer() {
        return false;
    }

    /**
     * Do not rely on {@link HotDeployer} that restarts the {@link ComponentManager} instead of applying the stash.
     * <p>
     * That allows going through the {@link DefaultComponent#unregisterExtension(org.nuxeo.runtime.model.Extension)}
     * logics.
     */
    protected void hotDeploy(boolean doDeploy, String contrib) throws Exception {
        URL url = Thread.currentThread().getContextClassLoader().getResource(contrib);
        try {
            if (doDeploy) {
                Framework.getRuntime().getContext().deploy(url);
            } else {
                Framework.getRuntime().getContext().undeploy(url);
            }
        } finally {
            Framework.getRuntime().getComponentManager().unstash();
        }
    }

    @Test
    public void testService() throws Exception {
        assertNotNull(service);
        assertFalse(service.registryContributionRegistered);
        assertFalse(service.registryContributionUnregistered);
        hotDeploy("registry-contrib-2.xml");
        assertFalse(service.registryContributionRegistered);
        assertFalse(service.registryContributionUnregistered);
        hotUndeploy("registry-contrib-2.xml");
        assertFalse(service.registryContributionRegistered);
        assertFalse(service.registryContributionUnregistered);
    }

    protected void checkSampleSingle(SingleRegistry reg, String name, String value) {
        Optional<SampleSingleDescriptor> desc = reg.getContribution();
        assertTrue(desc.isPresent());
        assertEquals(name, desc.get().name);
        assertEquals(value, desc.get().value);
    }

    protected void checkInitialSingleRegistry(SingleRegistry reg) throws Exception {
        assertNotNull(reg);
        checkSampleSingle(reg, "sample1", "Sample 1 Value");
    }

    protected void checkOverriddenSingleRegistry(SingleRegistry reg) throws Exception {
        assertNotNull(reg);
        checkSampleSingle(reg, "sample1", "Sample 1 Overridden");
    }

    @Test
    public void testSingleRegistry() throws Exception {
        checkInitialSingleRegistry(service.getSingleRegistry());
        hotDeploy("registry-contrib-2.xml");
        checkOverriddenSingleRegistry(service.getSingleRegistry());
        hotUndeploy("registry-contrib-2.xml");
        checkInitialSingleRegistry(service.getSingleRegistry());
    }

    protected void checkSample(MapRegistry reg, String name, String value, Boolean bool) {
        Optional<SampleDescriptor> desc = reg.getContribution(name);
        assertTrue(desc.isPresent());
        assertEquals(name, desc.get().name);
        assertEquals(value, desc.get().value);
        assertEquals(bool, desc.get().bool);
    }

    protected void checkInitialMapRegistry(MapRegistry reg) throws Exception {
        assertNotNull(reg);
        assertEquals(2, reg.getContributions().size());
        checkSample(reg, "sample1", "Sample 1 Value", true);
        checkSample(reg, "sample2", "Sample 2 Value", null);
    }

    protected void checkOverriddenMapRegistry(MapRegistry reg) throws Exception {
        assertNotNull(reg);
        assertEquals(2, reg.getContributions().size());
        // values have been merged
        checkSample(reg, "sample1", "Sample 1 Additions", true);
        checkSample(reg, "sample2", "", null);
    }

    @Test
    public void testMapRegistry() throws Exception {
        checkInitialMapRegistry(service.getMapRegistry());
        hotDeploy("registry-contrib-2.xml");
        checkOverriddenMapRegistry(service.getMapRegistry());
        hotUndeploy("registry-contrib-2.xml");
        checkInitialMapRegistry(service.getMapRegistry());
    }

    @Test
    public void testCustomRegistry() throws Exception {
        checkInitialMapRegistry(service.getCustomRegistry());
        hotDeploy("registry-contrib-2.xml");
        checkOverriddenMapRegistry(service.getCustomRegistry());
        hotUndeploy("registry-contrib-2.xml");
        checkInitialMapRegistry(service.getCustomRegistry());
    }

    protected void checkLegacy(Map<String, SampleLegacyDescriptor> reg, String name, String value, Boolean bool) {
        assertTrue(reg.containsKey(name));
        SampleDescriptor desc = reg.get(name);
        assertNotNull(desc);
        assertEquals(name, desc.name);
        assertEquals(value, desc.value);
        assertEquals(bool, desc.bool);
    }

    protected void checkInitialLegacyRegistry(Map<String, SampleLegacyDescriptor> reg) throws Exception {
        assertNotNull(reg);
        assertEquals(2, reg.size());
        checkLegacy(reg, "sample1", "Sample 1 Value", true);
        checkLegacy(reg, "sample2", "Sample 2 Value", null);
    }

    protected void checkOverriddenLegacyRegistry(Map<String, SampleLegacyDescriptor> reg) throws Exception {
        assertNotNull(reg);
        assertEquals(2, reg.size());
        // no merge
        checkLegacy(reg, "sample1", "Sample 1 Additions", null);
        checkLegacy(reg, "sample2", "", null);
    }

    @Test
    public void testLegacyRegistry() throws Exception {
        try {
            service.getLegacyRegistry();
            fail("Should have raised IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Unknown registry for extension point 'org.nuxeo.runtime.test.Registry.framework:legacy'",
                    e.getMessage());
        }
        checkInitialLegacyRegistry(service.legacyRegistry);
        hotDeploy("registry-contrib-2.xml");
        checkOverriddenLegacyRegistry(service.legacyRegistry);
        hotUndeploy("registry-contrib-2.xml");
        assertNotNull(service.legacyRegistry);
        if (!useHotDeployer()) {
            // values have been cleared according to SampleComponent#unregister behavior (no hot reload management)
            assertEquals(0, service.legacyRegistry.size());
        } else {
            checkInitialLegacyRegistry(service.legacyRegistry);
        }
    }

}
