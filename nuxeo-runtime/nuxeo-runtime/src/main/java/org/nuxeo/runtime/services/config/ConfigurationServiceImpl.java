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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 7.4
 */
public class ConfigurationServiceImpl extends DefaultComponent implements ConfigurationService {

    protected static final Log log = LogFactory.getLog(ConfigurationServiceImpl.class);

    public static final String CONFIGURATION_EP = "configuration";

    protected ConfigurationPropertyRegistry registry = new ConfigurationPropertyRegistry();

    @Override
    public String getProperty(String key) {
        return getProperty(key, null);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        if (registry.hasProperty(key)) {
            return registry.getProperty(key);
        }
        return defaultValue;
    }

    @Override
    public boolean isBooleanPropertyTrue(String key) {
        String value = getProperty(key);
        return Boolean.parseBoolean(value);
    }

    @Override
    public boolean isBooleanPropertyFalse(String key) {
        String value = getProperty(key);
        if (StringUtils.isBlank(value)) {
            return false;
        }
        return !Boolean.parseBoolean(value);
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (CONFIGURATION_EP.equals(extensionPoint)) {
            registry.addContribution((ConfigurationPropertyDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (CONFIGURATION_EP.equals(extensionPoint)) {
            registry.removeContribution((ConfigurationPropertyDescriptor) contribution);
        }
    }

}
