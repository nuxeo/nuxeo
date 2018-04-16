/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard <grenard@nuxeo.com>
 */
package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 9.1
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@ServletContainer(port = 18090)
@RepositoryConfig(init = RestServerInit.class)
public class MeTest extends BaseUserTest {

    @Inject
    UserManager um;

    private static final String DUMMY_PASSWORD = "dummy";

    private static final String NEW_PASSWORD = "newPassword";

    private static final String PASSWORD = "user1";

    @Override
    public void doBefore() {
        service = getServiceFor("user1", PASSWORD);
        mapper = new ObjectMapper();
    }

    @Test
    public void testUserCanChangePasswordWithCorrectPassword() {
        // When I change password
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/me/changepassword",
                "{\"oldPassword\": \"" + PASSWORD + "\", \"newPassword\": \"" + NEW_PASSWORD + "\"}")) {

            // Then it returns a OK
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }

        // And I cannot access current user with old password
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/me")) {
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        }

        // When I change I restore password using new password
        service = getServiceFor("user1", NEW_PASSWORD);
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/me/changepassword",
                "{\"oldPassword\": \"" + NEW_PASSWORD + "\", \"newPassword\": \"" + PASSWORD + "\"}")) {

            // Then it returns a OK
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testUserCannotChangePasswordWithIncorrectPassword() {
        // When I change password
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/me/changepassword",
                "{\"oldPassword\": \"" + DUMMY_PASSWORD + "\", \"newPassword\": \"" + NEW_PASSWORD + "\"}")) {

            // Then it returns a UNAUTHORIZED
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        }

        // And the password is unchanged and I can get current user
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/me")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }
    }

}
