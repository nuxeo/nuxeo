/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.common.xmap.registry.Registry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/** @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a> */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestExtensionPoint {

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:BaseXPoint.xml")
    @Deploy("org.nuxeo.runtime.test.tests:OverridingXPoint.xml")
    public void testOverride() {
        ComponentWithXPoint co = Framework.getService(ComponentWithXPoint.class);
        DummyContribution[] contribs = co.getContributions();
        assertEquals(2, contribs.length);
        assertSame(contribs[0].getClass(), DummyContribution.class);
        assertSame(contribs[1].getClass(), DummyContributionOverriden.class);
        assertEquals("XP contrib", contribs[0].message);
        assertEquals("OverXP contrib", contribs[1].message);
        assertEquals("My duty is to override", ((DummyContributionOverriden) contribs[1]).name);
        try {
            co.getComputedRegistry();
            fail("Should have raised IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Unknown registry for extension point 'BaseXPoint--xp'", e.getMessage());
        }
    }

    /**
     * Demonstrates what happens on old overriding contributions that would not have been updated to follow new registry
     * usage.
     */
    @Test
    @Deploy("org.nuxeo.runtime.test.tests:BaseXPoint-withregistry.xml")
    @Deploy("org.nuxeo.runtime.test.tests:OverridingXPoint.xml")
    public void testOverrideRegistry() {
        // old contribs still registered
        ComponentWithXPoint co = Framework.getService(ComponentWithXPoint.class);
        DummyContribution[] contribs = co.getContributions();
        assertEquals(1, contribs.length);
        assertSame(contribs[0].getClass(), DummyContributionOverriden.class);
        assertEquals("OverXP contrib", contribs[0].message);
        assertEquals("My duty is to override", ((DummyContributionOverriden) contribs[0]).name);

        // registry contribs are not using the overridden class
        Registry reg = co.getComputedRegistry();
        assertTrue(reg instanceof MapRegistry);
        MapRegistry mreg = (MapRegistry) reg;
        Map<String, Object> rcontribs = mreg.getContributions();
        assertEquals(2, rcontribs.size());
        assertSame(rcontribs.get("OverXP contrib").getClass(), DummyContributionWithRegistry.class);
        assertSame(rcontribs.get("XP contrib").getClass(), DummyContributionWithRegistry.class);
        assertEquals("OverXP contrib",
                ((DummyContributionWithRegistry) mreg.getContribution("OverXP contrib").get()).message);
        Optional<DummyContributionWithRegistry> c = mreg.getContribution("XP contrib");
        assertEquals("XP contrib", c.get().message);
    }

    /**
     * Demonstrates that old result can still be achieved if API was updated to work with registries instead, by
     * defining a new registry that will feed the original one.
     */
    @Test
    @Deploy("org.nuxeo.runtime.test.tests:BaseXPoint-withregistry.xml")
    @Deploy("org.nuxeo.runtime.test.tests:OverridingXPoint-withregistry.xml")
    public void testOverrideRegistryWithRegistry() {
        ComponentWithXPoint co = Framework.getService(ComponentWithXPoint.class);
        DummyContribution[] contribs = co.getContributions();
        // old contrib to original XP not registered anymore
        assertEquals(0, contribs.length);

        // new contribs relying on registry are using the overridden class
        Registry reg = co.getComputedRegistry();
        assertTrue(reg instanceof MapRegistry);
        MapRegistry mreg = (MapRegistry) reg;
        Map<String, Object> rcontribs = mreg.getContributions();
        assertEquals(2, rcontribs.size());
        assertSame(rcontribs.get("XP contrib").getClass(), DummyContributionWithRegistry.class);
        assertSame(rcontribs.get("OverXP contrib").getClass(), DummyContributionOverridenWithRegistry.class);
        assertEquals("XP contrib", ((DummyContributionWithRegistry) mreg.getContribution("XP contrib").get()).message);
        Optional<DummyContributionOverridenWithRegistry> c = mreg.getContribution("OverXP contrib");
        assertEquals("OverXP contrib", c.get().message);
        assertEquals("My duty is to override", c.get().name);
    }

}
