/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuno Cunha (ncunha@nuxeo.com)
 */

package org.nuxeo.ecm.automation.core.operations.document;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.webengine.model.exceptions.IllegalParameterException;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, MockitoFeature.class })
@Deploy({ "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.automation.core:test-operations.xml" })
public class AddPermissionTest {

    @Inject
    protected AutomationService automationService;

    @Inject
    protected CoreSession session;

    @Test
    @Deploy("org.nuxeo.ecm.automation.core:test-allow-virtual-user.xml")
    public void shouldFailWhenPermissionDoesNotExist() throws OperationException {
        DocumentModel doc = session.createDocumentModel("/", "src", "File");
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        List<String> users = Arrays.asList("foo");
        String invalidPermission = "<a href=www.evil.com>New";
        Map<String, Object> params = getParametersForAddOperation(null, users, null, invalidPermission, null, null,
                null, false, false, null);
        try {
            automationService.run(ctx, AddPermission.ID, params);
            fail(String.format("Calling %s with an invalid permission %s should fail.", AddPermission.ID,
                    invalidPermission));
        } catch (OperationException e) {
            assertEquals(
	            String.format("Permission %s is invalid.", invalidPermission),
		    e.getCause().getCause().getMessage());
        }
    }

    protected Map<String, Object> getParametersForAddOperation(String user, List<String> users, String email,
            String permission, String aclName, Calendar begin, Calendar end, boolean blockInheritance, boolean notify,
            String comment) {
        Map<String, Object> params = new HashMap<>();
        params.put("user", user);
        params.put("users", users);
        params.put("email", email);
        params.put("permission", permission);
        params.put("aclName", aclName);
        params.put("begin", begin);
        params.put("end", end);
        params.put("blockInheritance", blockInheritance);
        params.put("notify", notify);
        params.put("comment", comment);
        return params;
    }
}
