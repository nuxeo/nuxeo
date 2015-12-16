/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
