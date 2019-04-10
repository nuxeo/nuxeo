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
package org.nuxeo.ecm.restapi.server.jaxrs.drive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.restapi.test.BaseTest;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests the {@link NuxeoDriveObject}.
 *
 * @since 9.10
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@Deploy("org.nuxeo.drive.rest.api")
@ServletContainer(port = 18090)
public class NuxeoDriveObjectTest extends BaseTest {

    @Test
    public void testGetConfiguration() throws URISyntaxException, FileNotFoundException, IOException {

        try (CloseableClientResponse response = getResponse(RequestType.GET, "/drive/configuration")) {
            assertEquals(404, response.getStatus());
        }

        File testConfigurationFile = new File(
                Thread.currentThread().getContextClassLoader().getResource("nuxeo-drive-config.json").toURI());
        File serverConfigurationFile = new File(Environment.getDefault().getConfig(), "nuxeo-drive-config.json");
        FileUtils.copyFile(testConfigurationFile, serverConfigurationFile);

        try (CloseableClientResponse response = getResponse(RequestType.GET, "/drive/configuration")) {
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
}
