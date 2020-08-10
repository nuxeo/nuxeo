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
package org.nuxeo.ecm.automation.test.service;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.webengine.test.WebEngineFeatureCore;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Test invalid contributions to codec extension points.
 *
 * @since 11.3
 */

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, WebEngineFeatureCore.class })
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.io")
public class TestInvalidContributions {

    protected void checkStartupError(String message) {
        List<String> errors = Framework.getRuntime().getMessageHandler().getMessages(Level.ERROR);
        assertEquals(1, errors.size());
        assertEquals(message, errors.get(0));
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.io:test-invalid-codec.xml")
    public void testInvalidCodec() {
        checkStartupError("Failed to register codec on 'org.nuxeo.ecm.automation.io.services.IOComponent': "
                + "error initializing class 'org.nuxeo.ecm.automation.core.AutomationComponent' "
                + "(java.lang.ClassCastException: class org.nuxeo.ecm.automation.core.AutomationComponent "
                + "cannot be cast to class org.nuxeo.ecm.automation.io.services.codec.ObjectCodec "
                + "(org.nuxeo.ecm.automation.core.AutomationComponent and org.nuxeo.ecm.automation.io.services.codec.ObjectCodec "
                + "are in unnamed module of loader 'app')).");
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.io:test-invalid-codec-notfound.xml")
    public void testInvalidCodecNotFound() {
        checkStartupError("Failed to register codec on 'org.nuxeo.ecm.automation.io.services.IOComponent': "
                + "error initializing class 'org.nuxeo.ecm.automation.server.test.NonExistingCodec' "
                + "(java.lang.ClassNotFoundException: org.nuxeo.ecm.automation.server.test.NonExistingCodec).");
    }

}
