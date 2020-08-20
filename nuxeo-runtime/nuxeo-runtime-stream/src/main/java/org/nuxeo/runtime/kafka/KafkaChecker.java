/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.runtime.kafka;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.launcher.config.ConfigurationException;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.launcher.config.backingservices.BackingChecker;
import org.nuxeo.lib.stream.log.kafka.KafkaLogManager;

/**
 * @since 11.3
 */
public class KafkaChecker implements BackingChecker {
    private static final Logger log = LogManager.getLogger(KafkaChecker.class);

    private static final String KAFKA_ENABLED_PROP = "kafka.enabled";

    private static final String CONFIG_NAME = "kafka-config.xml";

    @Override
    public boolean accepts(ConfigurationGenerator cg) {
        // not using Boolean.parseValue on purpose, only 'true' must trigger the checker
        if (!"true".equals(cg.getUserConfig().getProperty(KAFKA_ENABLED_PROP))) {
            log.debug("Checker skipped because Kafka is disabled");
            return false;
        }
        return true;
    }

    @Override
    public void check(ConfigurationGenerator cg) throws ConfigurationException {
        KafkaConfigDescriptor config = getConfig(cg);
        try (KafkaLogManager manager = new KafkaLogManager(config.topicPrefix, config.producerProperties.properties,
                config.consumerProperties.properties)) {
            manager.exists("default");
        } catch (Exception e) {
            throw new ConfigurationException("Unable to reach Kafka using: " + config.producerProperties.properties, e);
        }
    }

    protected KafkaConfigDescriptor getConfig(ConfigurationGenerator cg) throws ConfigurationException {
        File configFile = new File(cg.getConfigDir(), CONFIG_NAME);
        if (!configFile.exists()) {
            throw new ConfigurationException("Cannot find Kafka configuration: " + CONFIG_NAME);
        }
        XMap xmap = new XMap();
        xmap.register(KafkaConfigDescriptor.class);
        try (InputStream inStream = new FileInputStream(configFile)) {
            Object[] nodes = xmap.loadAll(inStream);
            for (Object node : nodes) {
                if (node != null) {
                    return (KafkaConfigDescriptor) node;
                }
            }
            throw new ConfigurationException("No KafkaConfigDescriptor found in " + configFile.getAbsolutePath());
        } catch (IOException e) {
            throw new ConfigurationException(
                    "Failed to load KafkaConfigDescriptor from " + configFile.getAbsolutePath(), e);
        }
    }
}
