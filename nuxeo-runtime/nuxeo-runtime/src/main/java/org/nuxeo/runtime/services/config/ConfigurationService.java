/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *      Andre Justo
 *      Anahide Tchertchian
 */
package org.nuxeo.runtime.services.config;

import java.util.Map;

/**
 * Service holding runtime configuration properties.
 *
 * @since 7.4
 */
public interface ConfigurationService {

    /**
     * Returns the given property value if any, otherwise null.
     *
     * @param key the property key
     */
    String getProperty(String key);

    /**
     * Returns the given property value if any, otherwise returns the given default value.
     *
     * @param key the property key
     * @param defaultValue the default value for this key
     */
    String getProperty(String key, String defaultValue);

    /**
     * Returns the properties with key starting with the given namespace.
     *
     * @param namespace the namespace
     * @return a map of properties with trimmed keys (namespace removed)
     * @since 10.3
     */
    Map<String, String> getProperties(String namespace);

    /**
     * Returns true if given property is true when compared to a boolean value.
     */
    boolean isBooleanPropertyTrue(String key);

    /**
     * Returns true if given property is false when compared to a boolean value.
     */
    boolean isBooleanPropertyFalse(String key);

}
