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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.Environment;
import org.nuxeo.drive.test.NuxeoDriveJettyFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

/**
 * Tests the {@link NuxeoDriveConfigurationServlet}.
 *
 * @since 8.10-HF20
 */
@RunWith(FeaturesRunner.class)
@Features(NuxeoDriveJettyFeature.class)
@Jetty(port = 18090)
public class TestNuxeoDriveConfigurationServlet {

    protected static final String BASE_URL = "http://localhost:18090";

    protected Client client;

    @Before
    public void setUp() {
        ClientConfig config = new DefaultClientConfig();
        client = Client.create(config);
        client.addFilter(new HTTPBasicAuthFilter("Administrator", "Administrator"));
    }

    @After
    public void tearDown() {
        client.destroy();
    }

    @Test
    public void testServlet() throws URISyntaxException, IOException {

        ClientResponse response = client.resource(BASE_URL)
                                        .path("api")
                                        .path("v1")
                                        .path("drive")
                                        .path("configuration")
                                        .get(ClientResponse.class);
        assertEquals(404, response.getStatus());

        File testConfigurationFile = new File(
                Thread.currentThread().getContextClassLoader().getResource("nuxeo-drive-config.json").toURI());
        File serverConfigurationFile = new File(Environment.getDefault().getConfig(), "nuxeo-drive-config.json");
        FileUtils.copyFile(testConfigurationFile, serverConfigurationFile);

        response = client.resource(BASE_URL)
                         .path("api")
                         .path("v1")
                         .path("drive")
                         .path("configuration")
                         .get(ClientResponse.class);
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getType().toString());
        String json = response.getEntity(String.class);
        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Serializable> options = mapper.readValue(json, Map.class);
        assertNotNull(options);
        assertEquals(10, options.size());
        assertEquals(30, options.get("delay"));
    }
}
