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
package org.nuxeo.ecm.platform.importer.queue.tests.features;

import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import kafka.utils.MockTime;
import kafka.utils.TestUtils;
import kafka.utils.Time;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import kafka.zk.EmbeddedZookeeper;
import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.SimpleFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

import static junit.framework.TestCase.assertEquals;

public class KafkaFeature extends SimpleFeature {

    public static final String TOPIC = "test";

    public static final String CLIENT = "127.0.0.1:9092";

    private static final String ZK_HOST = "127.0.0.1";

    private KafkaServer kafkaServer;

    private ZkClient zkClient;

    private EmbeddedZookeeper zkServer;

    private static final Log log = LogFactory.getLog(KafkaFeature.class);


    @Override
    public void beforeRun(FeaturesRunner runner) throws Exception {

        log.debug("**** Starting Kafka test environment");

        // setup ZooKeeper
        zkServer = new EmbeddedZookeeper();

        String zkConnect = ZK_HOST + ":" + zkServer.port();
        zkClient = new ZkClient(zkConnect, 30000, 30000, ZKStringSerializer$.MODULE$);
        ZkUtils zkUtils = ZkUtils.apply(zkClient, false);

        // setup Broker
        Properties brokerProps = setupProperties(zkConnect);

        KafkaConfig config = new KafkaConfig(brokerProps);
        Time mock = new MockTime();
        kafkaServer = TestUtils.createServer(config, mock);
        kafkaServer.startup();

        if (zkUtils.getAllBrokersInCluster().size() == 0) {
            throw new RuntimeException("Cluster not started");
        }

        assertEquals(1, zkUtils.getAllBrokersInCluster().size());

//        propagateTopics(zkUtils, servers, topicReplicationFactor, topicPartition);

        log.debug("**** Kafka test environment Started");
    }

//    public void propagateTopics(ZkUtils utils, List<KafkaServer> servers, Integer replications, Integer partitions) {
//        AdminUtils.createTopic(utils, TOPIC, partitions, replications, new Properties(),
//                RackAwareMode.Disabled$.MODULE$);
//        TestUtils.waitUntilMetadataIsPropagated(scala.collection.JavaConversions.asScalaBuffer(servers), TOPIC, 4, 10000);
//    }

    @Override
    public void afterRun(FeaturesRunner runner) throws Exception {
        log.debug("**** Shutting down Kafka test environment");
        kafkaServer.shutdown();
        zkClient.close();
        zkServer.shutdown();
        log.debug("**** Kafka test environment Stopped");
    }

    private Properties setupProperties(String zk) throws IOException {
        Properties props = new Properties();
        props.put("broker.id", 0);
        props.put("host.name", ZK_HOST);
        props.put("port", 9092);
        props.put("num.partitions", 4);
        props.put("default.replication.factor", 1);
        props.put("replica.fetch.max.bytes", 4194304);
        props.put("message.max.bytes", 30100);
        props.put("zookeeper.connect", zk);
        props.put("zookeeper.connection.timeout.ms", 3000);
        props.setProperty("log.dirs", Files.createTempDirectory("kafka-").toAbsolutePath().toString());

        return props;
    }
}