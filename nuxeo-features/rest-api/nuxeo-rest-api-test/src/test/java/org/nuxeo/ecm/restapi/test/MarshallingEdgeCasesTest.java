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
 *     Thomas Roger
 */

package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.multipart.impl.MultiPartWriter;

/**
 * @since 9.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@Jetty(port = 18090)
@Deploy("org.nuxeo.ecm.platform.restapi.test.test")
public class MarshallingEdgeCasesTest extends BaseTest {

    @Test
    public void unauthenticatedEndpointShouldReturnJSON() throws IOException {
        ClientConfig config = new DefaultClientConfig();
        config.getClasses().add(MultiPartWriter.class);
        client = Client.create(config);
        ClientResponse r = client.resource("http://localhost:18090/api/v1/")
                                 .path("foo")
                                 .path("unauthenticated")
                                 .accept(MediaType.APPLICATION_JSON)
                                 .get(ClientResponse.class);
        assertEquals(200, r.getStatus());
        JsonNode node = mapper.readTree(r.getEntityInputStream());
        assertEquals("bar", node.get("foo").getTextValue());
    }

    @Test
    public void rollbackedtransactionShouldStillReturnJSON() throws IOException {
        ClientResponse r = getResponse(RequestType.GET, "foo/rollback");
        assertEquals(200, r.getStatus());
        JsonNode node = mapper.readTree(r.getEntityInputStream());
        assertEquals("bar", node.get("foo").getTextValue());
    }

}
