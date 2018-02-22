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
package org.nuxeo.runtime.registry;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class RegistryTest {

    @Inject
    protected HotDeployer hotDeployer;

    @Test
    public void testReload() throws Exception {
        hotDeployer.deploy("org.nuxeo.runtime.test.tests:CompA.xml");

        assertNull(Framework.getRuntime().getComponent(new ComponentName("CompA")));

        hotDeployer.deploy("org.nuxeo.runtime.test.tests:CompB.xml");

        assertNull(Framework.getRuntime().getComponent(new ComponentName("CompA")));

        hotDeployer.deploy("org.nuxeo.runtime.test.tests:CompC.xml");

        assertNotNull(Framework.getRuntime().getComponent(new ComponentName("CompA")));
        assertNotNull(Framework.getRuntime().getComponent(new ComponentName("CompB")));
        assertNotNull(Framework.getRuntime().getComponent(new ComponentName("CompC")));

        // test the unregister methods - without doing a registry reset
        hotDeployer.undeploy("org.nuxeo.runtime.test.tests:CompC.xml", "org.nuxeo.runtime.test.tests:CompA.xml",
                "org.nuxeo.runtime.test.tests:CompB.xml");

        assertNull(Framework.getRuntime().getComponent(new ComponentName("CompA")));
        assertNull(Framework.getRuntime().getComponent(new ComponentName("CompB")));
        assertNull(Framework.getRuntime().getComponent(new ComponentName("CompC")));

        hotDeployer.deploy("org.nuxeo.runtime.test.tests:CompA.xml");

        assertNull(Framework.getRuntime().getComponent(new ComponentName("CompA")));

        hotDeployer.deploy("org.nuxeo.runtime.test.tests:CompB.xml");

        assertNull(Framework.getRuntime().getComponent(new ComponentName("CompA")));

        hotDeployer.deploy("org.nuxeo.runtime.test.tests:CompC.xml");

        assertNotNull(Framework.getRuntime().getComponent(new ComponentName("CompA")));
        assertNotNull(Framework.getRuntime().getComponent(new ComponentName("CompB")));
        assertNotNull(Framework.getRuntime().getComponent(new ComponentName("CompC")));

    }

    /**
     * Deploy then reset and redeploy the same component
     *
     * @throws Exception
     */
    @Test
    public void testReset() throws Exception {

        hotDeployer.deploy("org.nuxeo.runtime.test.tests:CompC.xml");

        assertNotNull(Framework.getRuntime().getComponent(new ComponentName("CompC")));

        // test the reset instead of unregsitering each component
        Framework.getRuntime().getComponentManager().reset();
        Framework.getRuntime().getComponentManager().start();

        assertNull(Framework.getRuntime().getComponent(new ComponentName("CompC")));

        hotDeployer.deploy("org.nuxeo.runtime.test.tests:CompC.xml");

        assertNotNull(Framework.getRuntime().getComponent(new ComponentName("CompC")));
    }

}
