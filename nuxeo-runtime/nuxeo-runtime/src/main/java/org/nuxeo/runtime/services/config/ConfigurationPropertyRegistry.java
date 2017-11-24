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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
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
            String message = "Property '" + key + "' should now be contributed to extension "
                    + "point 'org.nuxeo.runtime.ConfigurationService', using target 'configuration'";
            DeprecationLogger.log(message, "7.4");
            Framework.getRuntime().getMessageHandler().addWarning(message);
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
