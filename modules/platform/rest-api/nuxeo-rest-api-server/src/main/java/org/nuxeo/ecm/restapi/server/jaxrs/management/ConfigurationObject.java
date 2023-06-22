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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;

import org.nuxeo.common.Environment;
import org.nuxeo.ecm.restapi.jaxrs.io.management.ConfigurationProperties;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.launcher.config.ConfigurationConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * Dumps the configuration properties and runtime properties.
 * <p>
 * Properties are redacted in two steps:
 * <ul>
 * <li>Properties are redacted based on keywords in their keys
 * <li>Properties are then redacted matching their values against various regex patterns.
 * </ul>
 *
 * @since 2021.40
 */
@WebObject(type = ManagementObject.MANAGEMENT_OBJECT_PREFIX + "configuration")
@Produces(APPLICATION_JSON)
public class ConfigurationObject extends AbstractResource<ResourceTypeImpl> {

    protected Properties configurationProps;

    @GET
    public ConfigurationProperties doGet() throws IOException {
        var runtimeProps = Framework.getProperties();
        return new ConfigurationProperties(getConfigurationProperties(), runtimeProps);
    }

    protected Properties getConfigurationProperties() throws IOException {
        if (configurationProps == null) {
            configurationProps = new Properties();
            var configProps = Path.of(Framework.getProperty(Environment.NUXEO_CONFIG_DIR))
                                  .resolve(ConfigurationConstants.FILE_CONFIGURATION_PROPERTIES);
            try (var input = Files.newInputStream(configProps)) {
                configurationProps.load(input);
            }
        }
        return configurationProps;
    }
}
