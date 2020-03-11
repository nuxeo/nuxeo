/*
 * (C) Copyright 2015-2019 Nuxeo (http://nuxeo.com/) and others.
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

import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Service holding runtime configuration properties.
 *
 * @since 7.4
 */
public interface ConfigurationService {

    String LIST_SEPARATOR = ",";

    /**
     * Returns the given property value if any.
     *
     * @param key the property key
     * @since 11.1
     */
    Optional<String> getString(String key);

    /**
     * Returns the given property value if any, otherwise returns the given default value.
     *
     * @param key the property key
     * @param defaultValue the default value for this key
     * @since 11.1
     */
    String getString(String key, String defaultValue);

    /**
     * Returns the given property value if any.
     *
     * @param key the property key
     * @since 11.1
     */
    Optional<Integer> getInteger(String key);

    /**
     * Returns the given property value if any, otherwise returns the given default value.
     *
     * @param key the property key
     * @param defaultValue the default value for this key
     * @since 11.1
     */
    int getInteger(String key, int defaultValue);

    /**
     * Returns the given property value if any.
     *
     * @param key the property key
     * @since 11.1
     */
    Optional<Long> getLong(String key);

    /**
     * Returns the given property value if any, otherwise returns the given default value.
     *
     * @param key the property key
     * @param defaultValue the default value for this key
     * @since 11.1
     */
    long getLong(String key, long defaultValue);

    /**
     * Returns the given property value if any.
     *
     * @param key the property key
     * @since 11.1
     */
    Optional<Boolean> getBoolean(String key);

    /**
     * Returns true if given property exists and is true when compared to a boolean value.
     *
     * <pre>
     * prop=true  | isBooleanTrue("prop") = true
     * prop=trUe  | isBooleanTrue("prop") = true
     * prop=false | isBooleanTrue("prop") = false
     * prop=any   | isBooleanTrue("prop") = false
     * prop=      | isBooleanTrue("prop") = false
     * </pre>
     *
     * @since 11.1
     */
    boolean isBooleanTrue(String key);

    /**
     * Returns true if given property exists and is false when compared to a boolean value.
     *
     * <pre>
     * prop=false | isBooleanFalse("prop") = true
     * prop=fAlse | isBooleanFalse("prop") = true
     * prop=true  | isBooleanFalse("prop") = false
     * prop=any   | isBooleanFalse("prop") = false
     * prop=      | isBooleanFalse("prop") = false
     * </pre>
     *
     * @since 11.1
     */
    boolean isBooleanFalse(String key);

    /**
     * Returns the given property value if any.
     *
     * @param key the property key
     * @since 11.1
     */
    Optional<Duration> getDuration(String key);

    /**
     * Returns the given property value if any, otherwise returns the given default value.
     *
     * @param key the property key
     * @param defaultValue the default value for this key
     * @since 11.1
     */
    Duration getDuration(String key, Duration defaultValue);

    /**
     * Returns the given property value if any, otherwise null.
     *
     * @param key the property key
     * @deprecated since 11.1, use {@link #getString(String)} instead
     */
    @Deprecated
    String getProperty(String key);

    /**
     * Returns the given property value if any, otherwise returns the given default value.
     *
     * @param key the property key
     * @param defaultValue the default value for this key
     * @deprecated since 11.1, use {@link #getString(String, String)} instead
     */
    @Deprecated
    String getProperty(String key, String defaultValue);

    /**
     * Returns the properties with key starting with the given namespace.
     *
     * @param namespace the namespace
     * @return a map of properties with trimmed keys (namespace removed)
     * @since 10.3
     */
    Map<String, Serializable> getProperties(String namespace);

    /**
     * Returns true if given property is true when compared to a boolean value.
     *
     * @deprecated since 11.1, use {@link #isBooleanTrue(String)} instead
     */
    @Deprecated
    boolean isBooleanPropertyTrue(String key);

    /**
     * Returns true if given property is false when compared to a boolean value.
     * <p>
     * Returns also true if property is not blank and is not equals to true.
     *
     * @deprecated since 11.1, use {@link #isBooleanFalse(String)} instead
     */
    @Deprecated
    boolean isBooleanPropertyFalse(String key);

    /**
     * Returns the json string representing the properties with key starting with the given namespace.
     *
     * @param namespace the namespace of the properties
     * @since 10.3
     */
    String getPropertiesAsJson(String namespace) throws IOException;

}
