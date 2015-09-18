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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.logging.DeprecationLogger;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Registry for JSF configuration contributions.
 *
 * @since 7.4
 */
public class ConfigurationPropertyRegistry extends SimpleContributionRegistry<ConfigurationPropertyDescriptor> {

    private static final Log log = LogFactory.getLog(ConfigurationPropertyRegistry.class);

    protected Map<String, String> properties = new HashMap<>();

    @Override
    public String getContributionId(ConfigurationPropertyDescriptor contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String key, ConfigurationPropertyDescriptor contrib,
            ConfigurationPropertyDescriptor newOrigContrib) {
        String name = contrib.getName();
        if (StringUtils.isEmpty(name)) {
            log.error("Cannot register configuration property with an empty name");
            return;
        }
        if (Framework.getProperties().containsKey(key)) {
            String message = String.format("Property '" + key + "' should now be contributed to extension "
                    + "point 'org.nuxeo.runtime.ConfigurationService', using target 'configuration'");
            DeprecationLogger.log(message, "7.4");
            Framework.getRuntime().getWarnings().add(message);
        }
        String value = contrib.getValue();
        properties.put(name, value);
        log.info("Registered property with name " + name + " and value " + value);
    }

    @Override
    public void contributionRemoved(String id, ConfigurationPropertyDescriptor origContrib) {
        properties.remove(id);
        log.info("Unregistered property with name " + id);
    }

    @Override
    public ConfigurationPropertyDescriptor clone(ConfigurationPropertyDescriptor orig) {
        return orig.clone();
    }

    @Override
    public void merge(ConfigurationPropertyDescriptor src, ConfigurationPropertyDescriptor dst) {
        dst.merge(src);
    }

    @Override
    public boolean isSupportingMerge() {
        return true;
    }

    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    public String getProperty(String key) {
        return properties.get(key);
    }
}
