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

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.automation.test.repository.AutomationRepositoryInit;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;

/**
 * @since 7.3
 */
@RunWith(FeaturesRunner.class)
@Features({ EmbeddedAutomationServerFeature.class })
@Deploy("org.nuxeo.ecm.platform.url.api")
@Deploy("org.nuxeo.ecm.platform.url.core")
@Deploy("org.nuxeo.ecm.platform.types.api")
@Deploy("org.nuxeo.ecm.platform.types.core")
@Deploy("org.nuxeo.ecm.platform.restapi.io")
@Deploy("org.nuxeo.ecm.platform.restapi.server")
@ServletContainer(port = 18090)
@RepositoryConfig(init = AutomationRepositoryInit.class, cleanup = Granularity.METHOD)
public class TestHttpHelpers {

    @Inject
    CoreSession session;

    @Inject
    AutomationService automationService;

    @Test
    public void canUseHttpHelperGET() throws OperationException, IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("script",
                "Context.result = HTTP.call(\"Administrator\",\"Administrator\",\"GET\", \"http://localhost:18090/api/v1/path/default-domain\");");
        OperationContext ctx = new OperationContext(session);
        automationService.run(ctx, "RunScript", params);
        String result = ((Blob) ctx.get("result")).getString();
        assertNotEquals("Internal Server Error", result);
        assertTrue(result.contains("entity-type"));
    }

    @Test
    public void canUseHttpHelperPOST() throws OperationException, IOException {
        String data = "{\"entity-type\": \"document\",\"type\": \"Workspace\",\"name\":\"newName\",\"properties\": {\"dc:title\":\"My title\",\"dc:description\":\" \"}}";
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-type", "application/json+nxentity");
        Map<String, Object> params = new HashMap<>();
        OperationContext ctx = new OperationContext(session);
        ctx.put("data", data);
        ctx.put("headers", headers);
        params.put("script",
                "Context.result = HTTP.call(\"Administrator\",\"Administrator\",\"POST\", \"http://localhost:18090/api/v1/path/default-domain\", Context.data, Context.headers);");
        automationService.run(ctx, "RunScript", params);
        String result = ((Blob) ctx.get("result")).getString();
        assertNotEquals("Internal Server Error", result);
        assertTrue(result.contains("entity-type"));
    }

    @Test
    public void canUseHttpHelperGETStringBlob() throws OperationException, IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("script",
                "Context.result = HTTP.call(\"Administrator\",\"Administrator\",\"GET\", \"http://localhost:18090/api/v1/path/testBlob/@blob/file:content\");");
        OperationContext ctx = new OperationContext(session);
        automationService.run(ctx, "RunScript", params);
        String result = ((Blob) ctx.get("result")).getString();
        assertNotEquals("Internal Server Error", result);
        assertTrue(result.contains("one"));
    }

    @Test
    public void canUseHttpHelperGETBlob() throws OperationException, IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("script",
                "Context.result = HTTP.call(\"Administrator\",\"Administrator\",\"GET\", \"http://localhost:18090/api/v1/path/testBlob2/@blob/file:content\");");
        OperationContext ctx = new OperationContext(session);
        automationService.run(ctx, "RunScript", params);
        Blob result = ((Blob) ctx.get("result"));
        assertNotNull(result);
        assertTrue(result.getLength() > 0);
    }

}
