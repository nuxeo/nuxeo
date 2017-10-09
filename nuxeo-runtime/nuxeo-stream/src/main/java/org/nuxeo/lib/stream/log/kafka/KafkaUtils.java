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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.I0Itec.zkclient.exception.ZkTimeoutException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.consumer.RangeAssignor;
import org.apache.kafka.clients.consumer.RoundRobinAssignor;
import org.apache.kafka.clients.consumer.internals.PartitionAssignor;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.requests.MetadataResponse;
import org.nuxeo.lib.stream.log.LogPartition;

import kafka.admin.AdminClient;
import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.cluster.Broker;
import kafka.cluster.EndPoint;
import kafka.coordinator.group.GroupOverview;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import scala.collection.Iterator;
import scala.collection.JavaConversions;
import scala.collection.Seq;

/**
 * Misc Kafka Utils
 *
 * @since 9.3
 */
public class KafkaUtils implements AutoCloseable {
    public static final String DEFAULT_ZK_SERVER = "localhost:2181";

    public static final int ZK_TIMEOUT_MS = 6000;

    public static final int ZK_CONNECTION_TIMEOUT_MS = 10000;

    private static final Log log = LogFactory.getLog(KafkaUtils.class);

    protected final ZkClient zkClient;

    protected final ZkUtils zkUtils;

    public KafkaUtils() {
        this(DEFAULT_ZK_SERVER);
    }

    public KafkaUtils(String zkServers) {
        log.debug("Init zkServers: " + zkServers);
        this.zkClient = createZkClient(zkServers);
        this.zkUtils = createZkUtils(zkServers, zkClient);
    }

    public static boolean kafkaDetected() {
        return kafkaDetected(DEFAULT_ZK_SERVER);
    }

    public static boolean kafkaDetected(String zkServers) {
        try {
            ZkClient tmp = new ZkClient(zkServers, 1000, 1000, ZKStringSerializer$.MODULE$);
            tmp.close();
        } catch (ZkTimeoutException e) {
            return false;
        }
        return true;
    }

    protected static ZkUtils createZkUtils(String zkServers, ZkClient zkClient) {
        return new ZkUtils(zkClient, new ZkConnection(zkServers), false);
    }

    protected static ZkClient createZkClient(String zkServers) {
        return new ZkClient(zkServers, ZK_TIMEOUT_MS, ZK_CONNECTION_TIMEOUT_MS, ZKStringSerializer$.MODULE$);
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
        createTopic(topic, partitions, 1);
    }

    public void createTopic(String topic, int partitions, int replicationFactor) {
        log.info("Creating topic: " + topic + ", partitions: " + partitions + ", replications: " + replicationFactor);
        if (AdminUtils.topicExists(zkUtils, topic)) {
            String msg = "Cannot create Topic already exists: " + topic;
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        AdminUtils.createTopic(zkUtils, topic, partitions, replicationFactor, new Properties(),
                RackAwareMode.Safe$.MODULE$);
        try {
            // TODO: check if it could be replaced by
            // TestUtils.waitUntilMetadataIsPropagated(zkUtils.getAllBrokersInCluster(), topic, 0, 5000);
            waitForTopicCreation(topic, Duration.ofSeconds(5));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        AdminUtils.deleteAllConsumerGroupInfoForTopicInZK(zkUtils, topic);
    }

    @SuppressWarnings("UnusedReturnValue")
    protected boolean waitForTopicCreation(String topic, Duration timeout) throws InterruptedException {
        // if you don't wait for a topic to be ready, this raise LEADER_NOT_AVAILABLE warning
        // and you can expects lots of rebalancing
        final long timeoutMs = timeout.toMillis();
        final long deadline = System.currentTimeMillis() + timeoutMs;
        boolean ret = false;
        while (!ret && System.currentTimeMillis() < deadline) {
            ret = allPartitionsAssigned(topic);
            Thread.sleep(100);
        }
        if (!ret) {
            log.error("Topic: " + topic + " has some uninitialized partitions.");
        }
        return ret;
    }

    protected boolean allPartitionsAssigned(String topic) {
        if (!AdminUtils.topicExists(zkUtils, topic)) {
            log.debug("Topic " + topic + " does not exists yet");
            return false;
        }
        MetadataResponse.TopicMetadata meta = AdminUtils.fetchTopicMetadataFromZk(topic, zkUtils);
        if (meta.partitionMetadata().isEmpty()) {
            log.debug("Topic " + topic + " has no partition yet");
            return false;
        }
        long errors = meta.partitionMetadata().stream().filter(p -> p.error().code() > 0).count();
        // System.out.println(topic + ": "+ errors);
        if (errors != 0) {
            log.debug("Topic " + topic + " have some uninitialized partitions");
        }
        return errors == 0;
    }

    public boolean topicExists(String topic) {
        return AdminUtils.topicExists(zkUtils, topic);
    }

    public List<String> listTopics() {
        Seq<String> topics = zkUtils.getAllTopics();
        return JavaConversions.seqAsJavaList(topics);
    }

    public List<String> listConsumers(Properties props, String topic) {
        return listAllConsumers(props).stream()
                                      .filter(consumer -> getConsumerTopics(props, consumer).contains(topic))
                                      .collect(Collectors.toList());
    }

    private List<String> getConsumerTopics(Properties props, String group) {
        AdminClient client = AdminClient.create(props);
        return JavaConversions.mapAsJavaMap(client.listGroupOffsets(group))
                              .keySet()
                              .stream()
                              .map(TopicPartition::topic)
                              .collect(Collectors.toList());
    }

    public List<String> listAllConsumers(Properties props) {
        List<String> ret = new ArrayList<>();
        AdminClient client = AdminClient.create(props);
        // this returns only consumer group that use the subscribe API (known by coordinator)
        scala.collection.immutable.List<GroupOverview> groups = client.listAllConsumerGroupsFlattened();
        Iterator<GroupOverview> iter = groups.iterator();
        GroupOverview group;
        while (iter.hasNext()) {
            group = iter.next();
            if (group != null) {
                ret.add(group.groupId());
            }
        }
        return ret;
    }

    /**
     * Work only if delete.topic.enable is true which is not the default
     */
    public void markTopicForDeletion(String topic) {
        log.debug("mark topic for deletion: " + topic);
        AdminUtils.deleteTopic(zkUtils, topic);
    }

    public int getNumberOfPartitions(String topic) {
        MetadataResponse.TopicMetadata metadata = AdminUtils.fetchTopicMetadataFromZk(topic, zkUtils);
        return metadata.partitionMetadata().size();
    }

    public void resetConsumerStates(String topic) {
        log.debug("Resetting consumer states");
        AdminUtils.deleteAllConsumerGroupInfoForTopicInZK(zkUtils, topic);
    }

    public Set<String> getBrokerEndPoints() {
        Set<String> ret = new HashSet<>();
        Seq<Broker> brokers = zkUtils.getAllBrokersInCluster();
        Broker broker;
        Iterator<Broker> iter = brokers.iterator();
        while (iter.hasNext()) {
            broker = iter.next();
            if (broker != null) {
                Seq<EndPoint> endPoints = broker.endPoints();
                Iterator<EndPoint> iter2 = endPoints.iterator();
                while (iter2.hasNext()) {
                    EndPoint endPoint = iter2.next();
                    ret.add(endPoint.connectionString());
                }
            }
        }
        return ret;
    }

    public String getDefaultBootstrapServers() {
        return getBrokerEndPoints().stream().collect(Collectors.joining(","));
    }

    @Override
    public void close() {
        if (zkUtils != null) {
            zkUtils.close();
        }
        if (zkClient != null) {
            zkClient.close();
        }
        log.debug("Closed.");
    }

}
