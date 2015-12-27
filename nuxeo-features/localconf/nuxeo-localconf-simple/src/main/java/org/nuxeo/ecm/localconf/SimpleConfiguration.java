/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.localconf;

import java.util.Map;

import org.nuxeo.ecm.core.api.localconfiguration.LocalConfiguration;

/**
 * An object that maps keys to values.
 * <p>
 * The mappings can be stored on documents with the facet {@code SimpleConfiguration}.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public interface SimpleConfiguration extends LocalConfiguration<SimpleConfiguration> {

    String SIMPLE_CONFIGURATION_FACET = "SimpleConfiguration";

    String SIMPLE_CONFIGURATION_SCHEMA = "simpleconfiguration";

    String SIMPLE_CONFIGURATION_PARAMETERS_PROPERTY = "sconf:simpleconfigurationparameters";

    String SIMPLE_CONFIGURATION_PARAMETER_KEY = "key";

    String SIMPLE_CONFIGURATION_PARAMETER_VALUE = "value";

    /**
     * Returns the value to which the specified {@code key} is mapped, or {@code null} if there is no mapping for the
     * {@code key}.
     *
     * @param key the key whose associated value is to be returned
     */
    String get(String key);

    /**
     * Returns the value to which the specified key is mapped, or the given default value if there is no mapping for the
     * key.
     *
     * @param key the key whose associated value is to be returned
     * @param defaultValue the value returned if there is no mapping for the key
     */
    String get(String key, String defaultValue);

    /**
     * Associates the specified value with the specified key.
     * <p>
     * If the map previously contained a mapping for the key, the old value is replaced by the specified value.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with {@code key}, or {@code null} if there was no mapping for {@code key}.
     */
    String put(String key, String value);

    /**
     * Copies all of the parameters from the specified map to this Simple configuration
     *
     * @param parameters parameters to be stored in this Simple configuration
     */
    void putAll(Map<String, String> parameters);

}
