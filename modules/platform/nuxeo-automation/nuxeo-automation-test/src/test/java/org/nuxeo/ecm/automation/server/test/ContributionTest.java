/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.automation.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.server.AutomationServer;
import org.nuxeo.ecm.automation.server.RestBinding;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.webengine.test.WebEngineFeatureCore;
import org.nuxeo.runtime.logging.DeprecationLogger;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, WebEngineFeatureCore.class, LogCaptureFeature.class })
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.io")
@Deploy("org.nuxeo.ecm.automation.server")
public class ContributionTest {

    public static final int DEFAULT_BINDINGS = 8;

    @Inject
    AutomationServer server;

    @Inject
    protected HotDeployer hotDeployer;

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

    /**
     * Test registration of a studio generated contribution
     */
    @Test
    @LogCaptureFeature.FilterOn(loggerClass = DeprecationLogger.class, logLevel = "WARN")
    public void testContribution() throws Exception {
        hotDeployer.deploy("org.nuxeo.ecm.automation.server:test-bindings.xml");
        assertEquals(DEFAULT_BINDINGS + 1, server.getBindings().length);
        RestBinding binding = server.getChainBinding("principals");
        assertNull(binding);
        List<String> caughtEvents = logCaptureResult.getCaughtEventMessages();
        assertEquals(1, caughtEvents.size());
        assertEquals(
                "Since version 11.5: Usage of \"disabled\" attribute on RestBinding contribution 'Chain.principals', "
                        + "in extension 'org.nuxeo.ecm.automation.server.test-bindings#bindings', "
                        + "is deprecated: use \"enable\" attribute instead",
                caughtEvents.get(0));

        binding = server.getChainBinding("audit");
        assertFalse(binding.isDisabled);
        assertTrue(binding.isSecure);
        assertTrue(binding.isAdministrator);
        assertEquals(1, binding.groups.length);
        assertEquals("members", binding.groups[0]);
    }

}
