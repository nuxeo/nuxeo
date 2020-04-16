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
package org.nuxeo.lib.stream.log.kafka;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.nuxeo.lib.stream.log.AbstractLogConfig;
import org.nuxeo.lib.stream.log.NameResolver;

/**
 * @since 11.1
 */
public class KafkaLogConfig extends AbstractLogConfig {

    public static final String DISABLE_SUBSCRIBE_PROP = "subscribe.disable";

    public static final String DEFAULT_REPLICATION_FACTOR_PROP = "default.replication.factor";

    protected final Properties adminProperties;

    protected final Properties producerProperties;

    protected final Properties consumerProperties;

    protected final short defaultReplicationFactor;

    protected final Boolean disableSubscribe;

    protected final NameResolver resolver;

    protected final String name;

    public KafkaLogConfig(String name, boolean defaultConfig, List<String> patterns, String prefix,
            Properties adminProperties,
            Properties producerProperties, Properties consumerProperties) {
        super(defaultConfig, patterns);
        this.name = name;
        resolver = new NameResolver(prefix);
        this.producerProperties = normalizeProducerProperties(producerProperties);
        this.consumerProperties = normalizeConsumerProperties(consumerProperties);
        if (adminProperties == null || adminProperties.isEmpty()) {
            this.adminProperties = createAdminProperties(this.producerProperties);
        } else {
            this.adminProperties = normalizeAdminProperties(adminProperties);
        }
        disableSubscribe = Boolean.valueOf(consumerProperties.getProperty(DISABLE_SUBSCRIBE_PROP, "false"));
        defaultReplicationFactor = Short.parseShort(
                producerProperties.getProperty(DEFAULT_REPLICATION_FACTOR_PROP, "1"));
    }

    public short getReplicatorFactor() {
        return defaultReplicationFactor;
    }

    public Boolean getDisableSubscribe() {
        return disableSubscribe;
    }

    public NameResolver getResolver() {
        return resolver;
    }

    public Properties getAdminProperties() {
        return adminProperties;
    }

    public Properties getProducerProperties() {
        return producerProperties;
    }

    public Properties getConsumerProperties() {
        return consumerProperties;
    }

    protected Properties normalizeAdminProperties(Properties adminProperties) {
        // anything to remove?
        return (Properties) adminProperties.clone();
    }

    protected Properties normalizeProducerProperties(Properties producerProperties) {
        Properties ret;
        if (producerProperties != null) {
            ret = (Properties) producerProperties.clone();
        } else {
            ret = new Properties();
        }
        ret.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        ret.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.BytesSerializer");
        ret.remove(DEFAULT_REPLICATION_FACTOR_PROP);
        return ret;
    }

    protected Properties normalizeConsumerProperties(Properties consumerProperties) {
        Properties ret;
        if (consumerProperties != null) {
            ret = (Properties) consumerProperties.clone();
        } else {
            ret = new Properties();
        }
        ret.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        ret.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.BytesDeserializer");
        ret.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        ret.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        ret.remove(DISABLE_SUBSCRIBE_PROP);
        return ret;
    }

    protected Properties createAdminProperties(Properties producerProperties) {
        Properties ret = new Properties();
        for (Map.Entry<Object, Object> prop : producerProperties.entrySet()) {
            switch (prop.getKey().toString()) {
            case ProducerConfig.ACKS_CONFIG:
            case ProducerConfig.BATCH_SIZE_CONFIG:
            case ProducerConfig.BUFFER_MEMORY_CONFIG:
            case ProducerConfig.COMPRESSION_TYPE_CONFIG:
            case ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG:
            case ProducerConfig.LINGER_MS_CONFIG:
            case ProducerConfig.MAX_BLOCK_MS_CONFIG:
            case ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG:
            case ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG:
            case DEFAULT_REPLICATION_FACTOR_PROP:
                // Skip non admin config properties to avoid warning on unused properties
                break;
            default:
                ret.put(prop.getKey(), prop.getValue());
            }
        }
        return ret;
    }

    @Override
    public String toString() {
        return "KafkaLogConfig{name='" + name + "', resolver='" + resolver + '\'' + ", adminProperties="
                + filterDisplayedProperties(adminProperties) + ", producerProperties="
                + filterDisplayedProperties(producerProperties) + ", consumerProperties="
                + filterDisplayedProperties(consumerProperties) + ", defaultReplicationFactor="
                + defaultReplicationFactor + ", disableSubscribe=" + disableSubscribe + '}';
    }

    protected String filterDisplayedProperties(Properties properties) {
        String ret = properties.toString();
        if (!ret.contains("password")) {
            return ret;
        }
        return ret.replaceAll("password=.[^\\\"\\;\\,\\ ]*", "password=****");
    }
}
