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
package org.nuxeo.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.runtime.services.config.ConfigurationService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * @since 11.5
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestComponentEnablement {

    protected static final String COMP_NAME = "org.nuxeo.runtime.enable.test";

    protected static final String TEST_PROP_NAME = "nuxeo.property.test.enablement";

    protected static final String SERVICE_TEST_PROP_NAME = "nuxeo.test.dummyBooleanProperty";

    @Inject
    protected ConfigurationService cs;

    @Inject
    protected HotDeployer deployer;

    protected void checkEnabled(boolean enabled) {
        RegistrationInfo ri = Framework.getRuntime()
                                       .getComponentManager()
                                       .getRegistrationInfo(new ComponentName(COMP_NAME));
        if (enabled) {
            assertNotNull(ri);
            assertEquals(enabled, ri.isEnabled());
            assertEquals(!enabled, ri.isDisabled());
        } else {
            assertNull(ri);
        }
        assertEquals(enabled, cs.isBooleanTrue(SERVICE_TEST_PROP_NAME));
    }

    @Test
    public void testDefaultConf() throws Exception {
        deployer.deploy("org.nuxeo.runtime.test.tests:enable-test-contrib.xml");
        checkEnabled(false);
    }

    @Test
    public void testDefaultConfWithDefault() throws Exception {
        deployer.deploy("org.nuxeo.runtime.test.tests:enable-test-contrib-default.xml");
        checkEnabled(true);
    }

    @Test
    public void testDefaultConfDisable() throws Exception {
        deployer.deploy("org.nuxeo.runtime.test.tests:disable-test-contrib-default.xml");
        checkEnabled(false);
    }

    @Test
    @WithFrameworkProperty(name = TEST_PROP_NAME, value = "true")
    public void testEnabledConf() throws Exception {
        deployer.deploy("org.nuxeo.runtime.test.tests:disable-test-contrib-default.xml");
        checkEnabled(false);
    }

    @Test
    @WithFrameworkProperty(name = TEST_PROP_NAME, value = "true")
    public void testEnabledConfWithDefault1() throws Exception {
        deployer.deploy("org.nuxeo.runtime.test.tests:enable-test-contrib-default.xml");
        checkEnabled(true);
    }

    @Test
    public void testEnabledConfWithDefault2() throws Exception {
        deployer.deploy("org.nuxeo.runtime.test.tests:enable-test-contrib-default.xml");
        checkEnabled(true);
    }

    @Test
    @WithFrameworkProperty(name = TEST_PROP_NAME, value = "true")
    public void testEnableConfDisabled() throws Exception {
        deployer.deploy("org.nuxeo.runtime.test.tests:disable-test-contrib-default.xml");
        checkEnabled(false);
    }

    @Test
    @WithFrameworkProperty(name = TEST_PROP_NAME, value = "false")
    public void testDisabledConf() throws Exception {
        deployer.deploy("org.nuxeo.runtime.test.tests:enable-test-contrib.xml");
        checkEnabled(false);
    }

    @Test
    @WithFrameworkProperty(name = TEST_PROP_NAME, value = "false")
    public void testDisabledConfWithDefault() throws Exception {
        deployer.deploy("org.nuxeo.runtime.test.tests:enable-test-contrib-default.xml");
        checkEnabled(false);
    }

    @Test
    @WithFrameworkProperty(name = TEST_PROP_NAME, value = "false")
    public void testDisabledConfDisabled() throws Exception {
        deployer.deploy("org.nuxeo.runtime.test.tests:disable-test-contrib-default.xml");
        checkEnabled(true);
    }

}
