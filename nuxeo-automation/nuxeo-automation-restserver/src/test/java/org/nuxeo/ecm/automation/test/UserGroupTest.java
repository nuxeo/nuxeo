/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.automation.test;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

import com.google.inject.Inject;
import com.sun.jersey.api.client.ClientResponse;

/**
 * Tests the users and groups Rest endpoints
 *
 * @since 5.7.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class UserGroupTest extends BaseTest {

    @Inject
    UserManager um;

    @Test
    public void itCanFetchAUser() throws Exception {
        // Given the user1

        // When I call the Rest endpoint
        ClientResponse response = getResponse(RequestType.GET, "/user/user1");

        // Then it returns the Json
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = mapper.readTree(response.getEntityInputStream());

        assertEquals("user", node.get("entity-type").getValueAsText());
        assertEquals("user1", node.get("id").getValueAsText());
        assertEquals("John",
                node.get("properties").get("firstName").getValueAsText());
        assertEquals("Lennon",
                node.get("properties").get("lastName").getValueAsText());

    }

}
