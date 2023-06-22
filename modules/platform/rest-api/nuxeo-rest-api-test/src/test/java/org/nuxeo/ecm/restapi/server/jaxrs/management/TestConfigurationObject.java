/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.restapi.server.jaxrs.management;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.restapi.test.ManagementBaseTest;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * @since 2023
 */
public class TestConfigurationObject extends ManagementBaseTest {

    @Before
    public void init() throws IOException {
        // Prepare some configuration
        var home = Framework.getRuntime().getHome().toPath();
        Path configuration = home.resolve("nxserver/config/configuration.properties");
        Framework.getRuntime().setProperty("nuxeo.config.dir", configuration.getParent());
        FileUtils.touch(configuration.toFile());
        try (var out = Files.newOutputStream(configuration)) {
            var configuredProps = new Properties();
            configuredProps.put("blahTokenBlah", "foo");
            configuredProps.put("valueWithBackSlash", "\\qux");
            configuredProps.put("encryptedValue", "{$broken$123}");
            configuredProps.put("basedOnValuePattern", "AKIAI53OIMNYFFMFTEST"); // NOSONAR
            configuredProps.put("kafkaStuff",
                    "org.apache.kafka.common.security.scram.ScramLoginModule required username\\=\"kafkaclient1\" password\\=\"kafkaclient1-secret\";");
            configuredProps.store(out, "test");
        }
    }

    @Test
    @WithFrameworkProperty(name = "superSecret", value = "myBFFname")
    public void testGet() throws IOException {
        try (CloseableClientResponse response = httpClientRule.get("/management/configuration")) {
            assertEquals(SC_OK, response.getStatus());
            String json = response.getEntity(String.class);
            var jsonAssert = JsonAssert.on(json);
            var configuredProps = jsonAssert.get("configuredProperties");
            configuredProps.has("blahTokenBlah").isEquals("***");
            configuredProps.has("valueWithBackSlash").isEquals("\\qux");
            configuredProps.has("encryptedValue").isEquals("***");
            configuredProps.has("basedOnValuePattern").isEquals("AKIAI53-AWS_KEY-TEST");
            configuredProps.has("kafkaStuff")
                           .isEquals(
                                   "org.apache.kafka.common.security.scram.ScramLoginModule required username\\=\"kafkaclient1\" password\\=***");
            jsonAssert.get("runtimeProperties").has("superSecret").isEquals("***");
        }
    }
}
