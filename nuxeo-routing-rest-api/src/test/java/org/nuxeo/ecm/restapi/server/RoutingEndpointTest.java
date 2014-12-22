/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.restapi.server;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.jaxrs.io.JsonHelper;
import org.nuxeo.ecm.restapi.server.jaxrs.RoutingRequest;
import org.nuxeo.ecm.restapi.test.BaseTest;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

import com.sun.jersey.api.client.ClientResponse;

/**
 * @since 7.1
 */
@RunWith(FeaturesRunner.class)
@Jetty(port = 18090)
@Features(RestServerFeature.class)
@Deploy("org.nuxeo.ecm.platform.restapi.server.routing")
public class RoutingEndpointTest extends BaseTest {

    HttpServletRequest req = mock(HttpServletRequest.class);

    HttpServletResponse resp = mock(HttpServletResponse.class);

    @Test
    public void testWorkflowEndpoint() throws Exception {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator jg = JsonHelper.createJsonGenerator(out);
        jg.writeObject(new RoutingRequest());

        // When i do a get request on the workflow endpoint
        ClientResponse response = getResponse(RequestType.POST, "/workflow", out.toString());

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

    }
}
