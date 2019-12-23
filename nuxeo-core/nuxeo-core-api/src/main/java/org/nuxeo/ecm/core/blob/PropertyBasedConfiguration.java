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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
        String propValue = properties.get(propertyName);
        if (isNotBlank(propValue)) {
            return propValue;
        }
        if (systemPropertyPrefix != null) {
            propValue = Framework.getProperty(systemPropertyPrefix + "." + propertyName);
            if (isNotBlank(propValue)) {
                return propValue;
            }
        }
        return defaultValue;
    }

    /** Gets a long property, or -1 if undefined or blank. */
    public long getLongProperty(String key) {
        String s = getProperty(key);
        long value = -1;
        if (!isBlank(s)) {
            try {
                value = Long.parseLong(s.trim());
            } catch (NumberFormatException e) {
                log.error("Cannot parse long " + key + ": " + s);
            }
        }
        return value;
    }

    /** Gets an integer property, or -1 if undefined or blank. */
    public int getIntProperty(String key) {
        String s = getProperty(key);
        int value = -1;
        if (!isBlank(s)) {
            try {
                value = Integer.parseInt(s.trim());
            } catch (NumberFormatException e) {
                log.error("Cannot parse integer " + key + ": " + s);
            }
        }
        return value;
    }

    /** Gets a boolean property. */
    public boolean getBooleanProperty(String key) {
        return Boolean.parseBoolean(getProperty(key));
    }

}
