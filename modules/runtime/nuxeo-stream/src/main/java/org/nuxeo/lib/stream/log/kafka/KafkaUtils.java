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
package org.nuxeo.lib.stream.log.kafka;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteRecordsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.RecordsToDelete;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.ConsumerPartitionAssignor;
import org.apache.kafka.clients.consumer.RangeAssignor;
import org.apache.kafka.clients.consumer.RoundRobinAssignor;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.common.errors.UnsupportedVersionException;
import org.nuxeo.lib.stream.StreamRuntimeException;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.Name;

/**
 * Misc Kafka Utils
 *
 * @since 9.3
 */
public class KafkaUtils implements AutoCloseable {
    private static final Log log = LogFactory.getLog(KafkaUtils.class);

    public static final String BOOTSTRAP_SERVERS_PROP = "kafka.bootstrap.servers";

    public static final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";

    protected final AdminClient adminClient;

    protected volatile List<String> allConsumers;

    protected volatile long allConsumersTime;

    protected static final long ALL_CONSUMERS_CACHE_TIMEOUT_MS = 2000;

    protected static final long ADMIN_CLIENT_CLOSE_TIMEOUT_S = 5;

    public KafkaUtils() {
        this(getDefaultAdminProperties());
    }

    public KafkaUtils(Properties adminProperties) {
        this.adminClient = AdminClient.create(adminProperties);
    }

    public static Properties getDefaultAdminProperties() {
        Properties ret = new Properties();
        ret.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers());
        ret.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 10_000);
        return ret;
    }

    public static String getBootstrapServers() {
        String bootstrapServers = System.getProperty(BOOTSTRAP_SERVERS_PROP, DEFAULT_BOOTSTRAP_SERVERS);
        if (bootstrapServers == null || bootstrapServers.isEmpty()) {
            bootstrapServers = DEFAULT_BOOTSTRAP_SERVERS;
        }
        return bootstrapServers;
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public static boolean kafkaDetected() {
        AdminClient client = AdminClient.create(getDefaultAdminProperties());
        try {
            client.describeCluster().nodes().get(5, TimeUnit.SECONDS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StreamRuntimeException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof org.apache.kafka.common.errors.TimeoutException) {
                return false;
            } else {
                throw new StreamRuntimeException(e);
            }
        } catch (TimeoutException e) {
            return false;
        } finally {
            // cannot use try with resource because of timeout
            client.close(Duration.ofSeconds(1));
        }
    }

    public static List<List<LogPartition>> rangeAssignments(int threads, Map<String, Integer> streams) {
        RangeAssignor assignor = new RangeAssignor();
        return assignments(assignor, threads, streams);
    }

    public static List<List<LogPartition>> roundRobinAssignments(int threads, Map<String, Integer> streams) {
        RoundRobinAssignor assignor = new RoundRobinAssignor();
        return assignments(assignor, threads, streams);
    }

    protected static List<List<LogPartition>> assignments(ConsumerPartitionAssignor assignor, int threads,
            Map<String, Integer> streams) {
        final List<PartitionInfo> parts = new ArrayList<>();
        streams.forEach((streamName, size) -> parts.addAll(getPartsFor(streamName, size)));
        Map<String, ConsumerPartitionAssignor.Subscription> subscriptions = new HashMap<>();
        List<String> streamNames = streams.keySet().stream().sorted().collect(Collectors.toList());
        for (int i = 0; i < threads; i++) {
            subscriptions.put(String.valueOf(i), new ConsumerPartitionAssignor.Subscription(streamNames));
        }
        Cluster cluster = new Cluster("kafka-cluster", Collections.emptyList(), parts, Collections.emptySet(),
                Collections.emptySet());
        Map<String, ConsumerPartitionAssignor.Assignment> assignments = assignor.assign(cluster,
                new ConsumerPartitionAssignor.GroupSubscription(subscriptions)).groupAssignment();
        List<List<LogPartition>> ret = new ArrayList<>(threads);
        for (int i = 0; i < threads; i++) {
            ret.add(assignments.get(String.valueOf(i))
                               .partitions()
                               .stream()
                               .map(part -> new LogPartition(Name.ofUrn(part.topic()), part.partition()))
                               .collect(Collectors.toList()));
        }
        return ret;
    }

    protected static Collection<PartitionInfo> getPartsFor(String topic, int partitions) {
        Collection<PartitionInfo> ret = new ArrayList<>();
        for (int i = 0; i < partitions; i++) {
            ret.add(new PartitionInfo(topic, i, null, null, null));
        }
        return ret;
    }

    /**
     * Creates a topic with partitions and replications that default to broker configuration.
     *
     * @since 2021.33
     */
    public void createTopic(String topic) {
        createTopic(topic, -1, (short) -1);
    }

    /**
     * Creates a topic with replication factor that defaults to broker configuration.
     *
     * @since 2021.33
     */
    public void createTopic(String topic, int partitions) {
        createTopic(topic, partitions, (short) -1);
    }

    public void createTopicWithoutReplication(String topic, int partitions) {
        createTopic(topic, partitions, (short) 1);
    }

    /**
     * Creates a topic with the given partitions and replication.
     * Since 2021.33 partitions (or replications) below 1 defaults to broker configuration.
     */
    public void createTopic(String topic, int partitions, short replicationFactor) {
        Optional<Integer> parts = (partitions < 1) ? Optional.empty() : Optional.of(partitions);
        Optional<Short> factor = (replicationFactor < 1) ? Optional.empty() : Optional.of(replicationFactor);
        log.info("Creating topic: " + topic + ", partitions: " + parts + ", replications: " + factor);
        CreateTopicsResult ret = adminClient.createTopics(Collections.singletonList(new NewTopic(topic, parts, factor)));
        try {
            ret.all().get(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StreamRuntimeException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TopicExistsException) {
                log.warn("Cannot create topic, it already exists: " + topic);
            } else if (e.getCause() instanceof org.apache.kafka.common.errors.TimeoutException) {
                throw new StreamRuntimeException("Unable to create topic " + topic + " within the request timeout", e);
            } else if (e.getCause() instanceof UnsupportedVersionException) {
                throw new StreamRuntimeException(
                        "Using default replication factor (or partitions) is only supported with Kafka broker >= 2.4. "
                                + "Update your Kafka cluster or use kafka.default.replication.factor >= 1",
                        e);
            } else {
                throw new StreamRuntimeException(e);
            }
        } catch (TimeoutException e) {
            throw new StreamRuntimeException("Unable to create topic " + topic + " within the 5m code timeout", e);
        }
        if (!topicReady(topic)) {
            waitForTopicCreation(topic, Duration.ofMinutes(3));
        }
        if (partitions(topic) != partitions) {
            log.warn("Topic: " + topic + " created with different partitioning, expected: " + partitions + ", actual: "
                    + partitions(topic));
        }
    }

    protected void waitForTopicCreation(String topic, Duration timeout) {
        log.warn("Waiting for brokers to become aware that the topic " + topic + " has been created.");
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (System.currentTimeMillis() > deadline) {
                throw new StreamRuntimeException(new TimeoutException(
                        "Timeout while waiting for topic " + topic + " metadata propagation in the cluster"));
            }
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new StreamRuntimeException("Interrupted while waiting for topic creation " + topic, e);
            }
        } while (!topicReady(topic));
        log.debug("Topic is now available");
    }

    public boolean topicExists(String topic) {
        return partitions(topic) > 0;
    }

    public boolean topicReady(String topic) {
        try {
            TopicDescription desc = adminClient.describeTopics(Collections.singletonList(topic))
                                               .values()
                                               .get(topic)
                                               .get();
            if (desc.partitions().size() < 1) {
                log.warn("Topic: " + topic + ", without partition");
                return false;
            }
            for (TopicPartitionInfo info : desc.partitions()) {
                if (info.leader().isEmpty()) {
                    log.warn("Topic: " + topic + " not ready, no leader for partition: " + info);
                    return false;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StreamRuntimeException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof UnknownTopicOrPartitionException) {
                log.info("Topic: " + topic + " unknown");
                return false;
            }
            throw new StreamRuntimeException(e);
        }
        return true;
    }

    public int partitions(String topic) {
        try {
            TopicDescription desc = adminClient.describeTopics(Collections.singletonList(topic))
                                               .values()
                                               .get(topic)
                                               .get();
            if (log.isDebugEnabled()) {
                log.debug(String.format("Topic %s exists: %s", topic, desc));
            }
            return desc.partitions().size();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StreamRuntimeException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof UnknownTopicOrPartitionException) {
                return -1;
            }
            throw new StreamRuntimeException(e);
        }
    }

    public Set<String> listTopics() {
        try {
            return adminClient.listTopics().names().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StreamRuntimeException(e);
        } catch (ExecutionException e) {
            throw new StreamRuntimeException(e);
        }
    }

    public List<String> listConsumers(String topic) {
        return listAllConsumers().stream()
                                 .filter(consumer -> getConsumerTopics(consumer).contains(topic))
                                 .collect(Collectors.toList());
    }

    protected List<String> getConsumerTopics(String group) {
        try {
            return adminClient.listConsumerGroupOffsets(group)
                              .partitionsToOffsetAndMetadata()
                              .get()
                              .keySet()
                              .stream()
                              .map(TopicPartition::topic)
                              .collect(Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StreamRuntimeException(e);
        } catch (ExecutionException e) {
            throw new StreamRuntimeException(e);
        }
    }

    public synchronized List<String> listAllConsumers() {
        long now = System.currentTimeMillis();
        if (allConsumers == null || (now - allConsumersTime) > ALL_CONSUMERS_CACHE_TIMEOUT_MS) {
            try {
                allConsumers = adminClient.listConsumerGroups()
                                          .all()
                                          .get()
                                          .stream()
                                          .map(ConsumerGroupListing::groupId)
                                          .collect(Collectors.toList());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new StreamRuntimeException(e);
            } catch (ExecutionException e) {
                throw new StreamRuntimeException(e);
            }
            if (!allConsumers.isEmpty()) {
                allConsumersTime = now;
            }
        }
        return allConsumers;
    }

    public int getNumberOfPartitions(String topic) {
        DescribeTopicsResult descriptions = adminClient.describeTopics(Collections.singletonList(topic));
        try {
            return descriptions.values().get(topic).get().partitions().size();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StreamRuntimeException(e);
        } catch (ExecutionException e) {
            throw new StreamRuntimeException(e);
        }
    }

    @Override
    public void close() {
        adminClient.close(Duration.ofSeconds(ADMIN_CLIENT_CLOSE_TIMEOUT_S));
        log.debug("Closed.");
    }

    public boolean delete(String topic) {
        log.info("Deleting topic: " + topic);
        DeleteTopicsResult result = adminClient.deleteTopics(Collections.singleton(topic));
        return result.values().get(topic).isDone();
    }

    /**
     * Delete all records of a topic by moving the first offsets to end of each partition.
     *
     * @since 2021.43
     */
    public void deleteRecords(String topic) {
        log.info("Deleting records of topic: " + topic);
        int partitions;
        try {
            partitions = getNumberOfPartitions(topic);
        } catch (StreamRuntimeException e) {
            log.warn("Fail to get number of partition " + e.getMessage());
            return;
        }
        Map<TopicPartition, RecordsToDelete> recordsToDelete = new HashMap<>(partitions);
        RecordsToDelete all = RecordsToDelete.beforeOffset(-1);
        for (int partition = 0; partition < partitions; partition++) {
            recordsToDelete.put(new TopicPartition(topic, partition), all);
        }
        DeleteRecordsResult ret = adminClient.deleteRecords(recordsToDelete);
        try {
            ret.all().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Remove all existing consumers and their committed positions.
     * For testing purpose.
     *
     * @since 2021.43
     */
    public void deleteConsumers() {
        List<String> consumers = listAllConsumers();
        log.info("Deleting all consumers: " + consumers);
        try {
            adminClient.deleteConsumerGroups(consumers).all().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
