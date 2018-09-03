/*
 * (C) Copyright 2015-2017 Nuxeo (http://nuxeo.com/) and others.
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.logging.DeprecationLogger;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @since 7.4
 */
public class ConfigurationServiceImpl extends DefaultComponent implements ConfigurationService {

    protected static final Log log = LogFactory.getLog(ConfigurationServiceImpl.class);

    public static final String CONFIGURATION_EP = "configuration";

    public static final String COMPONENT_NAME = "org.nuxeo.runtime.config";

    @Override
    public String getProperty(String key) {
        return getProperty(key, null);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        Descriptor desc = getDescriptor(CONFIGURATION_EP, key);
        if (desc == null) {
            return defaultValue;
        }
        String value = ((ConfigurationPropertyDescriptor) getDescriptor(CONFIGURATION_EP, key)).getValue();
        return value != null ? value : defaultValue;
    }

    @Override
    public boolean isBooleanPropertyTrue(String key) {
        String value = getProperty(key);
        return Boolean.parseBoolean(value);
    }

    @Override
    public boolean isBooleanPropertyFalse(String key) {
        String value = getProperty(key);
        return StringUtils.isNotBlank(value) && !Boolean.parseBoolean(value);
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (CONFIGURATION_EP.equals(extensionPoint)) {
            ConfigurationPropertyDescriptor configurationPropertyDescriptor = (ConfigurationPropertyDescriptor) contribution;
            String key = configurationPropertyDescriptor.getName();
            if (Framework.getProperties().containsKey(key)) {
                String message = "Property '" + key + "' should now be contributed to extension "
                        + "point 'org.nuxeo.runtime.ConfigurationService', using target 'configuration'";
                DeprecationLogger.log(message, "7.4");
                Framework.getRuntime().getMessageHandler().addWarning(message);
            }
            super.registerContribution(contribution, extensionPoint, contributor);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (CONFIGURATION_EP.equals(extensionPoint)) {
            super.unregisterContribution(contribution, extensionPoint, contributor);
        }
    }

    @Override
    protected String getName() {
        return COMPONENT_NAME;
    }

}
