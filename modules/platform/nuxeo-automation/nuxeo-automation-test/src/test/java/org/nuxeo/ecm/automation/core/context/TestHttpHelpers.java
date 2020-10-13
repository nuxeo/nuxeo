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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.test.AutomationServerFeature;
import org.nuxeo.ecm.automation.test.repository.AutomationRepositoryInit;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.webengine.test.WebEngineFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainerFeature;

/**
 * @since 7.3
 */
@RunWith(FeaturesRunner.class)
@Features({ WebEngineFeature.class, AutomationServerFeature.class })
@Deploy("org.nuxeo.ecm.platform.url.api")
@Deploy("org.nuxeo.ecm.platform.url.core")
@Deploy("org.nuxeo.ecm.platform.types")
@Deploy("org.nuxeo.ecm.automation.test.test:test-servletcontainer-contrib.xml")
@RepositoryConfig(init = AutomationRepositoryInit.class, cleanup = Granularity.METHOD)
public class TestHttpHelpers {

    @Inject
    CoreSession session;

    @Inject
    AutomationService automationService;

    @Inject
    protected ServletContainerFeature servletContainerFeature;

    protected OperationContext ctx;

    @Before
    public void createOperationContext() {
        ctx = new OperationContext(session);
    }

    @After
    public void closeOperationContext() {
        ctx.close();
    }

    protected String getBaseURL() {
        int port = servletContainerFeature.getPort();
        return "http://localhost:" + port;
    }

    @Test
    public void canUseHttpHelperGET() throws OperationException, IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("script", "Context.result = HTTP.call(\"Administrator\",\"Administrator\",\"GET\", \"" + getBaseURL()
                + "/dummy/string\");");
        automationService.run(ctx, "RunScript", params);
        String result = ((Blob) ctx.get("result")).getString();
        assertEquals("dummy", result);
    }

    @Test
    public void canUseHttpHelperPOST() throws OperationException, IOException {
        String data = "dummy data";
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-type", MediaType.APPLICATION_JSON);
        Map<String, Object> params = new HashMap<>();
        ctx.put("data", data);
        ctx.put("headers", headers);
        params.put("script", "Context.result = HTTP.call(\"Administrator\",\"Administrator\",\"POST\", \""
                + getBaseURL() + "/dummy/post\", Context.data, Context.headers);");
        automationService.run(ctx, "RunScript", params);
        String result = ((Blob) ctx.get("result")).getString();
        assertEquals("dummy data", result);
    }

    @Test
    public void canUseHttpHelperGETStringBlob() throws OperationException, IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("script", "Context.result = HTTP.call(\"Administrator\",\"Administrator\",\"GET\", \"" + getBaseURL()
                + "/dummy/stringblob\");");
        automationService.run(ctx, "RunScript", params);
        String result = ((Blob) ctx.get("result")).getString();
        assertEquals("dummy string blob", result);
    }

    @Test
    public void canUseHttpHelperGETBlob() throws OperationException, IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("script", "Context.result = HTTP.call(\"Administrator\",\"Administrator\",\"GET\", \"" + getBaseURL()
                + "/dummy/blob\");");
        automationService.run(ctx, "RunScript", params);
        Blob result = ((Blob) ctx.get("result"));
        assertNotNull(result);
        assertTrue(result.getLength() > 0);
    }

}
