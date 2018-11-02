/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.lib.stream.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.RoundRobinAssignor;
import org.apache.kafka.clients.consumer.internals.PartitionAssignor;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nuxeo.lib.stream.log.kafka.KafkaUtils;

/**
 * Unit test to learn Kafka lib.
 *
 * @since 9.3
 */
public class TestLibKafka {

    final static short DEFAULT_REPLICATION = 1;

    @BeforeClass
    public static void assumeKafkaEnabled() {
        TestKafkaUtils.assumeKafkaEnabled();
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    @Test
    public void testSendMessage() {
        String topic = "test-message-topic";
        int partitions = 5;
        createTopic(topic, partitions);
        Properties props = getProducerProperties();
        Producer<String, String> producer = new KafkaProducer<>(props);
        assertNotNull(producer);
        assertEquals(partitions, producer.partitionsFor(topic).size());
        TestCallback callback = new TestCallback();
        for (long i = 0; i < 100; i++) {
            ProducerRecord<String, String> data = new ProducerRecord<>(topic, "key-" + i, "message-" + i);
            producer.send(data, callback);
        }
        producer.close();

    }

    @Test
    public void testSendMessageInBatch() {
        String topic = "test-message-topic";
        createTopic(topic, 5);
        Properties props = getProducerProperties();
        Producer<String, String> producer = new KafkaProducer<>(props);
        List<Future<RecordMetadata>> results = new ArrayList<>();
        for (long i = 0; i < 100; i++) {
            ProducerRecord<String, String> data = new ProducerRecord<>(topic, "key-" + i, "message-" + i);
            results.add(producer.send(data));
        }
        // there is no flush api see https://cwiki.apache.org/confluence/display/KAFKA/KIP-8
        for (Future<RecordMetadata> result : results) {
            try {
                result.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail(e.getMessage());
            } catch (ExecutionException e) {
                fail(e.getMessage());
            }
        }
        producer.close();
    }

    @Test
    public void testConsumer() {
        String topic = "test-message-topic";
        int partitions = 5;
        createTopic(topic, partitions);
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(getConsumerProperties());

        consumer.assign(IntStream.range(0, partitions)
                                 .boxed()
                                 .map(partition -> new TopicPartition(topic, partition))
                                 .collect(Collectors.toList()));
        consumer.seekToEnd(consumer.assignment());
        consumer.seekToBeginning(consumer.assignment());

        testSendMessageInBatch();

        assertEquals(5, consumer.assignment().size());
        int count = 0;
        for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofSeconds(2))) {
            // System.out.println("receive: " + record.key() + " " + record.value());
            count += 1;
        }
        consumer.close();
        assertTrue(count > 100);
    }

    @Test
    public void testAssignator() {
        final List<PartitionInfo> parts = new ArrayList<>();
        parts.addAll(getPartsFor("t0", 4));
        parts.addAll(getPartsFor("t1", 5));
        Map<String, PartitionAssignor.Subscription> subscriptions = new HashMap<>();
        subscriptions.put("C1.1", new PartitionAssignor.Subscription(Arrays.asList("t0", "t1")));
        subscriptions.put("C1.2", new PartitionAssignor.Subscription(Arrays.asList("t0", "t1")));
        subscriptions.put("C1.3", new PartitionAssignor.Subscription(Arrays.asList("t0", "t1")));
        Cluster cluster = new Cluster("kafka-cluster", Collections.emptyList(), parts, Collections.emptySet(),
                Collections.emptySet());
        PartitionAssignor assignor = new RoundRobinAssignor();
        // PartitionAssignor assignor2 = new RangeAssignor();
        Map<String, PartitionAssignor.Assignment> assignment = assignor.assign(cluster, subscriptions);
        // assertEquals(null, assignment);
        assertEquals(3, assignment.get("C1.1").partitions().size());
        assertEquals(3, assignment.get("C1.2").partitions().size());
        assertEquals(3, assignment.get("C1.3").partitions().size());
        TopicPartition c1tp0 = assignment.get("C1.1").partitions().get(0);
        assertEquals(new TopicPartition("t0", 0), c1tp0);
        // assertEquals(new HashMap<>(), assignor.assign(cluster, subscriptions));
    }

    protected Collection<PartitionInfo> getPartsFor(String topic, int partitions) {
        Collection<PartitionInfo> ret = new ArrayList<>();
        for (int i = 0; i < partitions; i++) {
            ret.add(new PartitionInfo(topic, i, null, null, null));
        }
        return ret;
    }

    protected Properties getAdminProperties() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaUtils.getBootstrapServers());
        return props;
    }

    protected Properties getProducerProperties() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaUtils.getBootstrapServers());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        // default
        props.put(ProducerConfig.RETRIES_CONFIG, 0); // default
        props.put(ProducerConfig.ACKS_CONFIG, "1"); // default
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // default
        return props;
    }

    protected Properties getConsumerProperties() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaUtils.getBootstrapServers());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "unit test");
        return props;
    }

    protected boolean topicExists(String topic) {
        try (AdminClient adminClient = AdminClient.create(getAdminProperties())) {
            adminClient.describeTopics(Collections.singletonList(topic)).values().get(topic).get();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof UnknownTopicOrPartitionException) {
                return false;
            }
            throw new RuntimeException(e);
        }
    }

    protected void createTopic(String topic, int partitions) {
        if (topicExists(topic)) {
            return;
        }
        try (AdminClient adminClient = AdminClient.create(getAdminProperties())) {
            CreateTopicsResult ret = adminClient.createTopics(
                    Collections.singletonList(new NewTopic(topic, partitions, DEFAULT_REPLICATION)));
            ret.all().get(2, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException("Unable to create topics " + topic + " within the timeout", e);
        }
    }

    protected static class TestCallback implements Callback {
        @Override
        public void onCompletion(RecordMetadata recordMetadata, Exception e) {
            if (e != null) {
                System.out.println("Error while producing message to topic :" + recordMetadata);
                e.printStackTrace();
            } else {
                String message = String.format("sent message to topic:%s partition:%s  offset:%s",
                        recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset());
                // System.out.println(message);
            }
        }
    }

}
