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
import org.apache.kafka.clients.admin.AdminClientConfig;
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
import org.nuxeo.lib.stream.log.LogPartition;

import kafka.admin.AdminClient;
import kafka.coordinator.group.GroupOverview;
import scala.collection.Iterator;
import scala.collection.JavaConversions;

/**
 * Misc Kafka Utils
 *
 * @since 9.3
 */
public class KafkaUtils implements AutoCloseable {
    private static final Log log = LogFactory.getLog(KafkaUtils.class);

    public static final String BOOTSTRAP_SERVERS_PROP = "kafka.bootstrap.servers";

    public static final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";

    protected final Properties adminProperties;

    protected AdminClient adminClient;

    protected org.apache.kafka.clients.admin.AdminClient newAdminClient;

    protected List<String> allConsumers;

    protected long allConsumersTime;

    protected static final long ALL_CONSUMERS_CACHE_TIMEOUT_MS = 2000;

    public KafkaUtils() {
        this(getDefaultAdminProperties());
    }

    public KafkaUtils(Properties adminProperties) {
        this.adminProperties = adminProperties;
    }

    public static Properties getDefaultAdminProperties() {
        Properties ret = new Properties();
        ret.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers());
        return ret;
    }

    public static String getBootstrapServers() {
        return System.getProperty(BOOTSTRAP_SERVERS_PROP, DEFAULT_BOOTSTRAP_SERVERS);
    }

    public static boolean kafkaDetected() {
        Properties p = getDefaultAdminProperties();
        log.debug("KafkaUtils.kafkaDetected() " + p);
        AdminClient client = AdminClient.create(p);
        try {
            client.findAllBrokers();
            return true;
        } catch (RuntimeException e) {
            log.error(e);
            return false;
        } finally {
            if (client != null) {
                client.close();
            }
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
        CreateTopicsResult ret = getNewAdminClient().createTopics(
                Collections.singletonList(new NewTopic(topic, partitions, replicationFactor)));
        try {
            ret.all().get(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException("Unable to create topics " + topic + " within the timeout", e);
        }
    }

    public boolean topicExists(String topic) {
        try {
            TopicDescription desc = getNewAdminClient().describeTopics(Collections.singletonList(topic))
                                                       .values()
                                                       .get(topic)
                                                       .get();
            if (log.isDebugEnabled()) {
                log.debug(String.format("Topic %s exists: %s", topic, desc));
            }
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

    public Set<String> listTopics() {
        try {
            return getNewAdminClient().listTopics().names().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> listConsumers(String topic) {
        return listAllConsumers().stream().filter(consumer -> getConsumerTopics(consumer).contains(topic)).collect(
                Collectors.toList());
    }

    protected List<String> getConsumerTopics(String group) {
        return JavaConversions.mapAsJavaMap(getAdminClient().listGroupOffsets(group))
                              .keySet()
                              .stream()
                              .map(TopicPartition::topic)
                              .collect(Collectors.toList());
    }

    protected org.apache.kafka.clients.admin.AdminClient getNewAdminClient() {
        if (newAdminClient == null) {
            newAdminClient = org.apache.kafka.clients.admin.AdminClient.create(adminProperties);
        }
        return newAdminClient;
    }

    protected AdminClient getAdminClient() {
        if (adminClient == null) {
            adminClient = AdminClient.create(adminProperties);
        }
        return adminClient;
    }

    public List<String> listAllConsumers() {
        long now = System.currentTimeMillis();
        if (allConsumers == null || now - allConsumersTime > ALL_CONSUMERS_CACHE_TIMEOUT_MS) {
            allConsumers = new ArrayList<>();
            // this returns only consumer group that use the subscribe API (known by coordinator)
            scala.collection.immutable.List<GroupOverview> groups = getAdminClient().listAllConsumerGroupsFlattened();
            Iterator<GroupOverview> iterator = groups.iterator();
            GroupOverview group;
            while (iterator.hasNext()) {
                group = iterator.next();
                if (group != null && !allConsumers.contains(group.groupId())) {
                    allConsumers.add(group.groupId());
                }
            }
            if (!allConsumers.isEmpty()) {
                allConsumersTime = now;
            }
        }
        return allConsumers;
    }

    /**
     * Work only if delete.topic.enable is true which is not the default
     */
    public void markTopicForDeletion(String topic) {
        log.debug("mark topic for deletion: " + topic);
        try {
            getNewAdminClient().deleteTopics(Collections.singleton(topic)).all().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public int getNumberOfPartitions(String topic) {
        DescribeTopicsResult descriptions = getNewAdminClient().describeTopics(Collections.singletonList(topic));
        try {
            return descriptions.values().get(topic).get().partitions().size();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        if (adminClient != null) {
            adminClient.close();
            adminClient = null;
        }
        if (newAdminClient != null) {
            newAdminClient.close();
            newAdminClient = null;
        }
        log.debug("Closed.");
    }

}
