/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.launcher.config.backingservices;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.UnaryOperator;

import org.apache.commons.text.StringEscapeUtils;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.launcher.config.ConfigurationException;
import org.nuxeo.launcher.config.ConfigurationHolder;

/**
 * A backing checker checks for the availability of a backing service.
 *
 * @since 9.2
 * @apiNote Reworked in 11.5.
 */
public interface BackingChecker {

    /**
     * Test if the check has to be done for the given configuration.
     *
     * @param configHolder The current configuration
     * @return true if {@link BackingChecker#check(ConfigurationHolder)} has to be called.
     */
    boolean accepts(ConfigurationHolder configHolder);

    /**
     * Test the availability of the backing service.
     *
     * @param configHolder The current configuration
     * @throws ConfigurationException if backing service is not available.
     */
    void check(ConfigurationHolder configHolder) throws ConfigurationException;

    /**
     * Creates a descriptor instance for the specified file and descriptor class.
     */
    default <T> T getDescriptor(ConfigurationHolder configHolder, String configName, Class<T> klass)
            throws ConfigurationException {
        return getDescriptor(configHolder, configName, klass, UnaryOperator.identity());
    }

    /**
     * Creates a descriptor instance for the specified file and descriptor class. The last parameter is used to register
     * the descriptor classes to XMap when the desired descriptor is an abstract class.
     *
     * @since 2021.10
     */
    default <T> T getDescriptor(ConfigurationHolder configHolder, String configName, Class<T> klass,
            Class<?>... klasses) throws ConfigurationException {
        return getDescriptor(configHolder, configName, klass, UnaryOperator.identity(), klasses);
    }

    /**
     * @since 2021.10
     */
    default <T> T getDescriptor(ConfigurationHolder configHolder, String configName, Class<T> klass,
            UnaryOperator<String> replacer) throws ConfigurationException {
        return getDescriptor(configHolder, configName, klass, replacer, new Class[] {});
    }

    /**
     * Creates a descriptor instance for the specified file and descriptor class.
     */
    @SuppressWarnings("unchecked")
    default <T> T getDescriptor(ConfigurationHolder configHolder, String configName, Class<T> klass,
            UnaryOperator<String> replacer, Class<?>... klasses) throws ConfigurationException {
        Path configPath = configHolder.getConfigurationPath().resolve(configName);
        if (Files.notExists(configPath)) {
            throw new ConfigurationException(
                    "Configuration file: " + configPath + " for class: " + klass.getSimpleName() + "doesn't exist");
        }
        // retrieve template parser to decrypt properties
        var templateParser = configHolder.instantiateTemplateParser().keepEncryptedAsVar(false);
        XMap xmap = new XMap();
        xmap.register(klass);
        List.of(klasses).forEach(xmap::register);
        try {
            String content = Files.readString(configPath, StandardCharsets.UTF_8);
            content = replacer.apply(content);
            // In the ctx of XML contribs, we want to escape any xml characters
            content = templateParser.processText(content, StringEscapeUtils::escapeXml11);
            Object[] nodes = xmap.loadAll(new ByteArrayInputStream(content.getBytes()));
            for (Object node : nodes) {
                if (node != null) {
                    return (T) node;
                }
            }
            throw new ConfigurationException(
                    "No configuration found for class: " + klass.getSimpleName() + " in file:" + configPath);
        } catch (IOException e) {
            throw new ConfigurationException(
                    "Unable to load the configuration for class:" + klass.getSimpleName() + " from file:" + configPath,
                    e);
        }
    }
}
