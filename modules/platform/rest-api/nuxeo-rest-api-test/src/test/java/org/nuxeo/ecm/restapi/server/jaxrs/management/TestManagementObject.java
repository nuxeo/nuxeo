/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.restapi.server.jaxrs.management;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.platform.usermanager.NuxeoGroupImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.restapi.test.ManagementBaseTest;
import org.nuxeo.http.test.handler.HttpStatusCodeHandler;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * @since 2025.4
 */
@Deploy("org.nuxeo.ecm.platform.restapi.test.test")
public class TestManagementObject extends ManagementBaseTest {

    protected static final String SIMPLE_GROUP_NAME = "simple";

    protected static final String LEELA_USERNAME = "leela";

    protected static final String LEELA_PASSWORD = "pwd";

    @Inject
    protected UserManager userManager;

    @Inject
    protected TransactionalFeature txFeature;

    @Before
    public void before() {
        // a simple group
        if (userManager.getGroup(SIMPLE_GROUP_NAME) != null) {
            userManager.deleteGroup(SIMPLE_GROUP_NAME);
        }
        NuxeoGroup group = new NuxeoGroupImpl(SIMPLE_GROUP_NAME);
        userManager.createGroup(group.getModel());

        // a simple user
        if (userManager.getPrincipal(LEELA_USERNAME) != null) {
            userManager.deleteUser(LEELA_USERNAME);
        }
        DocumentModel user = userManager.getBareUserModel();
        user.setPropertyValue("user:username", LEELA_USERNAME);
        user.setPropertyValue("user:password", LEELA_PASSWORD);
        user.setPropertyValue("user:groups", (Serializable) List.of(SIMPLE_GROUP_NAME));
        userManager.createUser(user);

        txFeature.nextTransaction();
    }

    @Test
    public void testAdministratorUserIsAuthorized() {
        httpClient.buildGetRequest("/management/dummy")
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(HttpServletResponse.SC_OK, status.intValue()));
    }

    @Test
    public void testLeelaUserIsUnauthorized() {
        httpClient.buildGetRequest("/management/dummy")
                  .credentials(LEELA_USERNAME, LEELA_PASSWORD)
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(HttpServletResponse.SC_FORBIDDEN, status.intValue()));
    }

    @Test
    @WithFrameworkProperty(name = ManagementObject.MANAGEMENT_API_USER_PROPERTY, value = LEELA_USERNAME)
    public void testLeelaUserIsAuthorizedWhenConfigured() {
        httpClient.buildGetRequest("/management/dummy")
                  .credentials(LEELA_USERNAME, LEELA_PASSWORD)
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(HttpServletResponse.SC_OK, status.intValue()));
    }

    @Test
    @WithFrameworkProperty(name = ManagementObject.MANAGEMENT_API_GROUP_PROPERTY, value = SIMPLE_GROUP_NAME)
    public void testSimpleGroupIsAuthorizedWhenConfigured() {
        httpClient.buildGetRequest("/management/dummy")
                  .credentials(LEELA_USERNAME, LEELA_PASSWORD)
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(HttpServletResponse.SC_OK, status.intValue()));
    }

    @Test
    @WithFrameworkProperty(name = ManagementObject.MANAGEMENT_API_GROUP_PROPERTY, value = SIMPLE_GROUP_NAME
            + ",another-group")
    public void testSimpleGroupIsAuthorizedWhenSeveralGroupConfigured() {
        httpClient.buildGetRequest("/management/dummy")
                  .credentials(LEELA_USERNAME, LEELA_PASSWORD)
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(HttpServletResponse.SC_OK, status.intValue()));
    }
}
