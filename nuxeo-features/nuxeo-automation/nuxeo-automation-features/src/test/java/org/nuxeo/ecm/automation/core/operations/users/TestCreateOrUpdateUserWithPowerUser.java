/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.automation.core.operations.users;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * @since 11.1
 */
public class TestCreateOrUpdateUserWithPowerUser extends AbstractTestWithPowerUser {

    @Test
    public void testPowerUserCannotCreateAdministratorUser() throws OperationException {
        Map<String, Object> params = new HashMap<>();
        params.put("username", "bar");
        params.put("groups", new String[] { ADMINISTRATORS_GROUP });
        assertOperationCallFails(params);
    }

    @Test
    public void testPowerUserCannotUpdateAdministratorUser() throws OperationException {
        Map<String, Object> params = new HashMap<>();
        params.put("username", "Administrator");
        Properties properties = new Properties();
        properties.put("firstName", "foo");
        params.put("properties", properties);
        assertOperationCallFails(params);
    }

    @Test
    public void testPowerUserCannotPromoteUserAsAdministrator() throws OperationException {
        Map<String, Object> params = new HashMap<>();
        params.put("username", "fry");
        params.put("groups", new String[] { "subgroup" });
        assertOperationCallFails(params);
    }

    @Test
    public void testPowerUserCannotPromoteHimselfAsAdministrator() throws OperationException {
        NuxeoPrincipal user = userManager.getPrincipal("leela");
        List<String> groups = user.getGroups();
        groups.add(ADMINISTRATORS_GROUP);

        Map<String, Object> params = new HashMap<>();
        params.put("username", "leela");
        params.put("groups", groups);
        assertOperationCallFails(params);
    }

    protected void assertOperationCallFails(Map<String, Object> params) throws OperationException {
        try (CoreSession session = openPowerUserCoreSession(); OperationContext ctx = new OperationContext(session)) {
            automationService.run(ctx, CreateOrUpdateUser.ID, params);
            fail("Should have thrown NuxeoException");
        } catch (NuxeoException e) {
            assertEquals(HttpServletResponse.SC_FORBIDDEN, e.getStatusCode());
            assertEquals(
                    "Failed to invoke operation User.CreateOrUpdate with aliases [Services.CreateUser], User is not allowed to create or edit users",
                    e.getMessage());
        }
    }

    protected CoreSession openPowerUserCoreSession() {
        NuxeoPrincipal principal = userManager.getPrincipal("leela");
        return coreFeature.openCoreSession(principal);
    }
}
