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
package org.nuxeo.ecm.restapi.jaxrs.io.management;

import java.util.Properties;

/**
 * @since 2021.40
 */
public class ConfigurationProperties {

    protected final Properties configuredProperties;

    protected final Properties runtimeProperties;

    public ConfigurationProperties(Properties configuredProperties, Properties runtimeProperties) {
        this.configuredProperties = configuredProperties;
        this.runtimeProperties = runtimeProperties;
    }

    public Properties getConfiguredProperties() {
        return configuredProperties;
    }

    public Properties getRuntimeProperties() {
        return runtimeProperties;
    }
}
