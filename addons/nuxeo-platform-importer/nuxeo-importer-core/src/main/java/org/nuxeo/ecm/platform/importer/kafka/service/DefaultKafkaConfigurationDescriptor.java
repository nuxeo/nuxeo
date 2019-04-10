/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     anechaev
 */
package org.nuxeo.ecm.platform.importer.kafka.service;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@XObject("kafkaConfig")
public class DefaultKafkaConfigurationDescriptor {

    protected Properties producerProperties;

    protected Properties consumerProperties;

    @XNode("@bootstrapServer")
    protected String bootstrapServer;

    @XNode("producerConfigs")
    protected ProducerConfiguration producerConfiguration;

    @XNode("consumerConfigs")
    protected ConsumerConfiguration consumerConfiguration;

    @XNodeList(value = "topics/topic", type = ArrayList.class, componentType = String.class)
    protected List<String> topics = new ArrayList<>();

    public void setProducerProperties(Properties producerProperties) {
        this.producerProperties = producerProperties;
    }

    public void setConsumerProperties(Properties consumerProperties) {
        this.consumerProperties = consumerProperties;
    }

    public Properties getProducerProperties() {
        if (producerProperties == null) {
            producerProperties = new Properties();
            producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
            producerProperties.put(ProducerConfig.ACKS_CONFIG, producerConfiguration.acks);
            producerProperties.put(ProducerConfig.RETRIES_CONFIG, producerConfiguration.retries);
            producerProperties.put(ProducerConfig.BATCH_SIZE_CONFIG, producerConfiguration.batchSize);
            producerProperties.put(ProducerConfig.LINGER_MS_CONFIG, producerConfiguration.lingerMs);
            producerProperties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, producerConfiguration.maxBlocksMs);

            producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, producerConfiguration.keySerializerClass.getName());
            producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, producerConfiguration.valueSerializerClass.getName());

        }
        return producerProperties;
    }

    public Properties getConsumerProperties() {
        if (consumerProperties == null) {
            consumerProperties = new Properties();
            consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
            consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerConfiguration.groupId);
            consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, consumerConfiguration.autoCommit);
            consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerConfiguration.offsetConfig);
            consumerProperties.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, consumerConfiguration.heartbeat);
            // Should be very careful with the config. for instance 1000ms does not work, the consumer will be marked as dead
            consumerProperties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, consumerConfiguration.sessionTimeout);
            consumerProperties.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, consumerConfiguration.requestTimeout);
            consumerProperties.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, consumerConfiguration.maxPartitionFetch);
            consumerProperties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, consumerConfiguration.maxPollRecords);
            consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, consumerConfiguration.keyDeserializerClass.getName());
            consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, consumerConfiguration.valueDeserializerClass.getName());
        }
        return consumerProperties;
    }

    public List<String> getTopics() {
        return topics;
    }

    @XObject("producerConfigs")
    public static class ProducerConfiguration {
        @XNode("@acks")
        protected String acks;

        @XNode("@retries")
        protected Integer retries;

        @XNode("@batchSize")
        protected Integer batchSize;

        @XNode("@lingerMs")
        protected Integer lingerMs;

        @XNode("@maxBlocksMs")
        protected Integer maxBlocksMs;

        @XNode("@keySerializer")
        protected Class<? extends Serializer> keySerializerClass;

        @XNode("@valueSerializer")
        protected Class<? extends Serializer> valueSerializerClass;
    }

    @XObject("consumerConfigs")
    public static class ConsumerConfiguration {
        @XNode("@groupId")
        protected String groupId;

        @XNode("@autoCommit")
        protected Boolean autoCommit;

        @XNode("@offsetConfig")
        protected String offsetConfig;

        @XNode("@sessionTimeout")
        protected Integer sessionTimeout;

        @XNode("@maxPartitionFetch")
        protected Integer maxPartitionFetch;

        @XNode("@maxPollRecords")
        protected Integer maxPollRecords;

        @XNode("@keyDeserializer")
        protected Class<? extends Deserializer> keyDeserializerClass;

        @XNode("@valueDeserializer")
        protected Class<? extends Deserializer> valueDeserializerClass;

        @XNode("@heartbeat")
        protected Integer heartbeat;

        @XNode("@requestTimeout")
        protected Integer requestTimeout;
    }
}