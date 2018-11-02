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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.RangeAssignor;
import org.apache.kafka.clients.consumer.RoundRobinAssignor;
import org.apache.kafka.clients.consumer.internals.PartitionAssignor;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.nuxeo.lib.stream.StreamRuntimeException;
import org.nuxeo.lib.stream.log.LogPartition;

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
            throw new StreamRuntimeException(e);
        } catch (TimeoutException e) {
            return false;
        } finally {
            // cannot use try with resource because of timeout
            client.close(0, TimeUnit.SECONDS);
        }
    }

    public static List<List<LogPartition>> rangeAssignments(int threads, Map<String, Integer> streams) {
        PartitionAssignor assignor = new RangeAssignor();
        return assignments(assignor, threads, streams);
    }

    public static List<List<LogPartition>> roundRobinAssignments(int threads, Map<String, Integer> streams) {
        PartitionAssignor assignor = new RoundRobinAssignor();
        return assignments(assignor, threads, streams);
    }

    protected static List<List<LogPartition>> assignments(PartitionAssignor assignor, int threads,
            Map<String, Integer> streams) {
        final List<PartitionInfo> parts = new ArrayList<>();
        streams.forEach((streamName, size) -> parts.addAll(getPartsFor(streamName, size)));
        Map<String, PartitionAssignor.Subscription> subscriptions = new HashMap<>();
        List<String> streamNames = streams.keySet().stream().sorted().collect(Collectors.toList());
        for (int i = 0; i < threads; i++) {
            subscriptions.put(String.valueOf(i), new PartitionAssignor.Subscription(streamNames));
        }
        Cluster cluster = new Cluster("kafka-cluster", Collections.emptyList(), parts, Collections.emptySet(),
                Collections.emptySet());
        Map<String, PartitionAssignor.Assignment> assignments = assignor.assign(cluster, subscriptions);
        List<List<LogPartition>> ret = new ArrayList<>(threads);
        for (int i = 0; i < threads; i++) {
            ret.add(assignments.get(String.valueOf(i))
                               .partitions()
                               .stream()
                               .map(part -> new LogPartition(part.topic(), part.partition()))
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

    public void createTopicWithoutReplication(String topic, int partitions) {
        createTopic(topic, partitions, (short) 1);
    }

    public void createTopic(String topic, int partitions, short replicationFactor) {
        log.info("Creating topic: " + topic + ", partitions: " + partitions + ", replications: " + replicationFactor);
        if (topicExists(topic)) {
            throw new IllegalArgumentException("Cannot create Topic already exists: " + topic);
        }
        CreateTopicsResult ret = adminClient.createTopics(
                Collections.singletonList(new NewTopic(topic, partitions, replicationFactor)));
        try {
            ret.all().get(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StreamRuntimeException(e);
        } catch (ExecutionException e) {
            throw new StreamRuntimeException(e);
        } catch (TimeoutException e) {
            throw new StreamRuntimeException("Unable to create topics " + topic + " within the timeout", e);
        }
    }

    public boolean topicExists(String topic) {
        return partitions(topic) > 0;
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
        adminClient.close(ADMIN_CLIENT_CLOSE_TIMEOUT_S, TimeUnit.SECONDS);
        log.debug("Closed.");
    }

}
