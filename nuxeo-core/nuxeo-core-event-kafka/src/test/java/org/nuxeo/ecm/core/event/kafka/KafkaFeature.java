/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     tiry
 */
package org.nuxeo.ecm.core.event.kafka;

import static org.junit.Assert.assertEquals;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
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
/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     tiry
 */
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.SimpleFeature;

//@Features({ RuntimeFeature.class })
public class KafkaFeature extends SimpleFeature {

    public static final String ZKHOST = "127.0.0.1";

    public static final String BROKERHOST = "127.0.0.1";

    public static final String BROKERPORT = "9093";

    public static final String TOPIC = KafkaPipe.TOPIC;

    protected KafkaServer kafkaServer;

    protected ZkClient zkClient;

    protected EmbeddedZookeeper zkServer;

    protected static final Log log = LogFactory.getLog(KafkaFeature.class);



    @Override
    public void beforeRun(FeaturesRunner runner) throws Exception {

        log.debug("**** Starting Kafka test envrionment");

        // setup ZooKeeper
        zkServer = new EmbeddedZookeeper();
        String zkConnect = ZKHOST + ":" + zkServer.port();
        zkClient = new ZkClient(zkConnect, 30000, 30000, ZKStringSerializer$.MODULE$);
        ZkUtils zkUtils = ZkUtils.apply(zkClient, false);

        // setup Broker
        Properties brokerProps = new Properties();
        brokerProps.setProperty("zookeeper.connect", zkConnect);
        brokerProps.setProperty("broker.id", "0");
        brokerProps.setProperty("log.dirs", Files.createTempDirectory("kafka-").toAbsolutePath().toString());
        brokerProps.setProperty("host.name", BROKERHOST);
        brokerProps.setProperty("port", BROKERPORT);

        KafkaConfig config = new KafkaConfig(brokerProps);
        Time mock = new MockTime();
        kafkaServer = TestUtils.createServer(config, mock);

        // XXX should not be needed !?
        kafkaServer.startup();

        if (zkUtils.getAllBrokersInCluster().size() == 0) {
            throw new RuntimeException("Cluster not started");
        }

        assertEquals(1, zkUtils.getAllBrokersInCluster().size());

        int topicPartition = 1;
        int topicReplicationFactor = 1;

        AdminUtils.createTopic(zkUtils, TOPIC, topicPartition, topicReplicationFactor, new Properties(),
                RackAwareMode.Disabled$.MODULE$);

        List<KafkaServer> servers = new ArrayList<KafkaServer>();
        servers.add(kafkaServer);
        TestUtils.waitUntilMetadataIsPropagated(scala.collection.JavaConversions.asScalaBuffer(servers), TOPIC, 0, 5000);

        log.debug("**** Kafka test envrionment Started");

    }

    @Override
    public void afterRun(FeaturesRunner runner) throws Exception {
        log.debug("**** Shuting down Kafka test envrionment");
        kafkaServer.shutdown();
        zkClient.close();
        zkServer.shutdown();
        log.debug("**** Kafka test envrionment Stoped");

    }

}
