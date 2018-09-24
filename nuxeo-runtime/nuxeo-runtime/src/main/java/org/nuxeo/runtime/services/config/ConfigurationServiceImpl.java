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

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.logging.DeprecationLogger;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 7.4
 */
public class ConfigurationServiceImpl extends DefaultComponent implements ConfigurationService {

    protected static final Log log = LogFactory.getLog(ConfigurationServiceImpl.class);

    public static final String CONFIGURATION_EP = "configuration";

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * XXX remove once we are able to get such a cached map from DefaultComponent
     *
     * @since 10.3
     */
    protected volatile Map<String, ConfigurationPropertyDescriptor> descriptors;

    /**
     * XXX remove once we are able to get such a cached map from DefaultComponent.
     * <p>
     * We'd ideally need a <T extends Descriptor> Map<String, T> getDescriptors(String xp) with cache method.
     *
     * @since 10.3
     */
    protected Map<String, ConfigurationPropertyDescriptor> getDescriptors() {
        Map<String, ConfigurationPropertyDescriptor> d = descriptors;
        if (d == null) {
            synchronized (this) {
                d = descriptors;
                if (d == null) {
                    List<ConfigurationPropertyDescriptor> descs = getDescriptors(CONFIGURATION_EP);
                    descriptors = d = descs.stream().collect(Collectors.toMap(desc -> desc.getId(), desc -> desc));
                }
            }
        }
        return d;
    }

    @Override
    public String getProperty(String key) {
        return getProperty(key, null);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        ConfigurationPropertyDescriptor conf = getDescriptors().get(key);
        if (conf == null) {
            return defaultValue;
        }
        String value = conf.getValue();
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
            synchronized (this) {
                descriptors = null;
            }
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
            synchronized (this) {
                descriptors = null;
            }
            super.unregisterContribution(contribution, extensionPoint, contributor);
        }
    }

    @Override
    public Map<String, Serializable> getProperties(String namespace) {
        if (StringUtils.isEmpty(namespace)) {
            return null;
        }
        if (namespace.charAt(namespace.length() - 1) == '.') {
            throw new IllegalArgumentException("namespace cannot end with a dot");
        }
        return getDescriptors().values()
                               .stream()
                               .filter(desc -> startsWithNamespace(desc.getName(), namespace))
                               .collect(Collectors.toMap(desc -> desc.getId().substring(namespace.length() + 1),
                                       desc -> desc.getValue() != null && desc.list
                                               ? desc.getValue().split(LIST_SEPARATOR) : desc.getValue()));
    }

    @Override
    public String getPropertiesAsJson(String namespace) throws IOException {
        return MAPPER.writeValueAsString(getProperties(namespace));
    }

    /**
     * Returns true if a string starts with a namespace.
     *
     * @param string a string
     * @param namespace
     * @since 10.3
     */
    protected static boolean startsWithNamespace(String string, String namespace) {
        int nl = namespace.length();
        return string.length() > nl && string.charAt(nl) == '.' && string.startsWith(namespace);
    }

}
