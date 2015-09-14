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

import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Registry for JSF configuration contributions.
 *
 * @since 7.4
 */
public class ConfigurationPropertyRegistry extends SimpleContributionRegistry<ConfigurationPropertyDescriptor> {

    private static final Log log = LogFactory.getLog(ConfigurationPropertyRegistry.class);

    protected Properties properties = Framework.getRuntime().getConfigurationProperties();

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
        String value = contrib.getValue();
        properties.setProperty(name, value);
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

}
