/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.context.ContextHelper;
import org.nuxeo.ecm.automation.context.ContextService;
import org.nuxeo.ecm.automation.features.PlatformFunctions;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 7.3
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.features")
@Deploy("org.nuxeo.ecm.automation.test.test:helpers-contrib-test.xml")
public class TestContextHelpers {

    @Inject
    CoreSession session;

    @Inject
    AutomationService automationService;

    @Test
    public void shouldGetContextService() {
        ContextService contextService = Framework.getService(ContextService.class);
        assertNotNull(contextService);
    }

    @Test
    public void shouldGetBuiltInExtension() {
        ContextService contextService = Framework.getService(ContextService.class);
        Map<String, ContextHelper> contextHelperList = contextService.getHelperFunctions();
        assertTrue(contextHelperList.containsKey("Fn"));
        assertNotNull(contextHelperList.get("Fn"));
        assertTrue(contextHelperList.get("Fn") instanceof PlatformFunctions);
    }

    @Test
    public void shouldUseNewExtension() throws OperationException {
        // Get the new extension
        ContextService contextService = Framework.getService(ContextService.class);
        Map<String, ContextHelper> contextHelperList = contextService.getHelperFunctions();
        assertTrue(contextHelperList.containsKey("dummy"));
        assertNotNull(contextHelperList.get("dummy"));

        assertTrue(contextHelperList.get("dummy") instanceof DummyHelper);

        // Use it
        Map<String, Object> params = new HashMap<>();
        params.put("script", "Context.dummy = dummy.helper1();");
        OperationContext ctx = new OperationContext();
        automationService.run(ctx, "RunScript", params);
        assertEquals("hello", ctx.get("dummy"));
    }
}
