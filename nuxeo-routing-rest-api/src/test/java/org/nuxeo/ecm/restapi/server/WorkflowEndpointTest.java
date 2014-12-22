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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.nuxeo.ecm.restapi.test.BaseTest;

import com.sun.jersey.api.client.ClientResponse;

/**
 * @since 7.1
 */
public class WorkflowEndpointTest extends BaseTest {

    HttpServletRequest req = mock(HttpServletRequest.class);

    HttpServletResponse resp = mock(HttpServletResponse.class);

    @Test
    public void testWorkflowEndpoint() throws Exception {

        // When i do a get request on the workflow endpoint
        ClientResponse response = getResponse(RequestType.GET, "/workflow");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

    }
}
