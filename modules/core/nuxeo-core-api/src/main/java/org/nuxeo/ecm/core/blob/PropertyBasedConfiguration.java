/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.DurationUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * Basic configuration based on properties.
 *
 * @since 11.1
 */
public class PropertyBasedConfiguration {

    private static final Logger log = LogManager.getLogger(PropertyBasedConfiguration.class);

    public final String systemPropertyPrefix;

    public final Map<String, String> properties;

    public PropertyBasedConfiguration(String systemPropertyPrefix, Map<String, String> properties) {
        this.systemPropertyPrefix = systemPropertyPrefix;
        this.properties = properties;
    }

    /** Gets a string property. */
    public String getProperty(String propertyName) {
        return getProperty(propertyName, null);
    }

    /** Gets a string property, or the given default if undefined or blank. */
    public String getProperty(String propertyName, String defaultValue) {
        return getOptionalProperty(propertyName).orElse(defaultValue);
    }

    /** Gets a long property, or -1 if undefined or blank. */
    public long getLongProperty(String key) {
        return getOptionalLongProperty(key).orElse(-1L);
    }

    /** Gets an integer property, or -1 if undefined or blank. */
    public int getIntProperty(String key) {
        return getIntProperty(key, -1);
    }

    /**
     * Gets an integer property, or the given default if undefined or blank.
     *
     * @since 2023.5
     */
    public int getIntProperty(String key, int defaultValue) {
        return getOptionalIntegerProperty(key).orElse(defaultValue);
    }

    /** Gets a boolean property. */
    public boolean getBooleanProperty(String key) {
        return Boolean.parseBoolean(getProperty(key));
    }

    /**
     * @since 2025.10
     */
    public Optional<Duration> getOptionalDurationProperty(String key) {
        return getOptionalProperty(key).map(s -> {
            try {
                return DurationUtils.parse(s);
            } catch (DateTimeParseException e) {
                log.error("Cannot parse duration {}: {} ", key, s);
                return null;
            }
        });
    }

    /**
     * @since 2025.10
     */
    public Optional<Integer> getOptionalIntegerProperty(String key) {
        return getOptionalProperty(key).map(s -> {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                log.error("Cannot parse int {}: {} ", key, s);
                return null;
            }
        });
    }

    /**
     * @since 2025.10
     */
    public Optional<Long> getOptionalLongProperty(String key) {
        return getOptionalProperty(key).map(s -> {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                log.error("Cannot parse long {}: {} ", key, s);
                return null;
            }
        });
    }

    /**
     * @since 2025.10
     */
    public Optional<String> getOptionalProperty(String propertyName) {
        return Optional.ofNullable(properties.get(propertyName)).filter(StringUtils::isNotBlank).or(() -> {
            if (systemPropertyPrefix == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(Framework.getProperty(systemPropertyPrefix + "." + propertyName))
                           .filter(StringUtils::isNotBlank);
        }).map(String::trim);
    }

}
