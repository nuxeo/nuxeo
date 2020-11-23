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
 *      Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.runtime.services.config;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.DurationUtils;
import org.nuxeo.runtime.model.DefaultComponent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;

/**
 * @since 7.4
 */
public class ConfigurationServiceImpl extends DefaultComponent implements ConfigurationService {

    protected static final Logger log = LogManager.getLogger(ConfigurationServiceImpl.class);

    public static final String CONFIGURATION_EP = "configuration";

    protected static final JavaPropsMapper PROPERTIES_MAPPER = new JavaPropsMapper();

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    @Deprecated
    public String getProperty(String key) {
        return getString(key, null);
    }

    @Override
    @Deprecated
    public String getProperty(String key, String defaultValue) {
        return getString(key, defaultValue);
    }

    @Override
    @Deprecated
    public boolean isBooleanPropertyTrue(String key) {
        String value = getProperty(key);
        return Boolean.parseBoolean(value);
    }

    @Override
    @Deprecated
    public boolean isBooleanPropertyFalse(String key) {
        String value = getProperty(key);
        return StringUtils.isNotBlank(value) && !Boolean.parseBoolean(value);
    }

    @Override
    public Map<String, Serializable> getProperties(String namespace) {
        if (StringUtils.isEmpty(namespace)) {
            return null;
        }
        if (namespace.charAt(namespace.length() - 1) == '.') {
            throw new IllegalArgumentException("namespace cannot end with a dot");
        }
        List<ConfigurationPropertyDescriptor> values = getRegistryContributions(CONFIGURATION_EP);
        return values.stream()
                     .filter(desc -> startsWithNamespace(desc.getId(), namespace))
                     .collect(Collectors.toMap(desc -> desc.getId().substring(namespace.length() + 1),
                             desc -> desc.getValue() != null && desc.list ? desc.getValue().split(LIST_SEPARATOR)
                                     : desc.getValue()));
    }

    @Override
    public String getPropertiesAsJson(String namespace) throws IOException {
        // Build properties with indexes for lists
        Properties properties = new Properties();
        getProperties(namespace).forEach((key, value) -> {
            if (value instanceof String[]) {
                int idx = 1;
                for (String v : (String[]) value) {
                    properties.put(String.format("%s.%d", key, idx++), v);
                }
            } else {
                properties.put(key, value);
            }
        });
        return OBJECT_MAPPER.writer()
                            .writeValueAsString(PROPERTIES_MAPPER.readPropertiesAs(properties, ObjectNode.class));
    }

    /**
     * Returns true if a string starts with a namespace.
     *
     * @param string a string
     * @since 10.3
     */
    protected static boolean startsWithNamespace(String string, String namespace) {
        int nl = namespace.length();
        return string.length() > nl && string.charAt(nl) == '.' && string.startsWith(namespace);
    }

    /**
     * @since 11.1
     */
    @Override
    public Optional<String> getString(String key) {
        Optional<ConfigurationPropertyDescriptor> desc = getRegistryContribution(CONFIGURATION_EP, key);
        return desc.map(value -> value.getValue()).filter(StringUtils::isNotBlank);
    }

    /**
     * @since 11.1
     */
    @Override
    public String getString(String key, String defaultValue) {
        return getString(key).orElse(defaultValue);
    }

    /**
     * @since 11.1
     */
    @Override
    public Optional<Integer> getInteger(String key) {
        return getString(key).map(value -> {
            try {
                return Integer.valueOf(value);
            } catch (NumberFormatException e) {
                log.error("Invalid configuration property '{}', '{}' should be a number", key, value, e);
                return null;
            }
        });
    }

    /**
     * @since 11.1
     */
    @Override
    public int getInteger(String key, int defaultValue) {
        return getInteger(key).orElse(defaultValue);
    }

    /**
     * @since 11.1
     */
    @Override
    public Optional<Long> getLong(String key) {
        return getString(key).map(value -> {
            try {
                return Long.valueOf(value);
            } catch (NumberFormatException e) {
                log.error("Invalid configuration property '{}', '{}' should be a number", key, value, e);
                return null;
            }
        });
    }

    /**
     * @since 11.1
     */
    @Override
    public long getLong(String key, long defaultValue) {
        return getLong(key).orElse(defaultValue);
    }

    /**
     * @since 11.1
     */
    @Override
    public Optional<Boolean> getBoolean(String key) {
        return getString(key).map(value -> {
            // don't use Boolean.parseBoolean because we want to enforce typing
            if ("true".equalsIgnoreCase(value)) {
                return TRUE;
            } else if ("false".equalsIgnoreCase(value)) {
                return FALSE;
            } else {
                log.error("Invalid configuration property '{}', '{}' should be a boolean", key, value);
                return null;
            }
        });
    }

    /**
     * @since 11.1
     */
    @Override
    public boolean isBooleanTrue(String key) {
        return getBoolean(key).filter(TRUE::equals).orElse(FALSE);
    }

    /**
     * @since 11.1
     */
    @Override
    public boolean isBooleanFalse(String key) {
        // inverse value as we're getting FALSE
        return getBoolean(key).filter(FALSE::equals).map(value -> !value).orElse(FALSE);
    }

    /**
     * @since 11.1
     */
    @Override
    public Optional<Duration> getDuration(String key) {
        return getString(key).map(value -> {
            try {
                return DurationUtils.parse(value);
            } catch (DateTimeParseException e) {
                log.error("Invalid configuration property '{}', '{}' should be a duration", key, value, e);
                return null;
            }
        });
    }

    /**
     * @since 11.1
     */
    @Override
    public Duration getDuration(String key, Duration defaultValue) {
        return getDuration(key).orElse(defaultValue);
    }

}
