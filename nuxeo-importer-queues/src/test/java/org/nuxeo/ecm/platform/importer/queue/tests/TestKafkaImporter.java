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
package org.nuxeo.ecm.platform.importer.queue.tests;

import com.google.inject.Inject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.importer.filter.EventServiceConfiguratorFilter;
import org.nuxeo.ecm.platform.importer.filter.ImporterFilter;
import org.nuxeo.ecm.platform.importer.log.BufferredLogger;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.queue.QueueImporter;
import org.nuxeo.ecm.platform.importer.queue.consumer.ConsumerFactory;
import org.nuxeo.ecm.platform.importer.queue.consumer.SourceNodeConsumerFactory;
import org.nuxeo.ecm.platform.importer.queue.manager.KQManager;
import org.nuxeo.ecm.platform.importer.queue.manager.QueuesManager;
import org.nuxeo.ecm.platform.importer.queue.producer.Producer;
import org.nuxeo.ecm.platform.importer.queue.producer.SourceNodeProducer;
import org.nuxeo.ecm.platform.importer.queue.tests.features.KafkaFeature;
import org.nuxeo.ecm.platform.importer.source.RandomTextSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import java.io.IOException;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, KafkaFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({
        "org.nuxeo.ecm.platform.importer.core", //
})
@LocalDeploy("org.nuxeo.ecm.platform.importer.queue.test:test-kafka-service-contrib.xml")
public class TestKafkaImporter {

    protected static final Log log = LogFactory.getLog(TestKafkaImporter.class);

    @Inject
    private CoreSession session;

    @Test
    public void shouldImport() throws IOException {

        ImporterLogger logger = new BufferredLogger(log);
        QueueImporter<SourceNode> importer = new QueueImporter<>(logger);

        ImporterFilter filter = new EventServiceConfiguratorFilter(true, false, true, false, true);
        importer.addFilter(filter);

        QueuesManager<SourceNode> qm = new KQManager<>(logger, 8);

        RandomTextSourceNode root = RandomTextSourceNode.init(1000, 1, true);

        Producer<SourceNode> producer = new SourceNodeProducer(root, logger);

        ConsumerFactory fact = new SourceNodeConsumerFactory();
        importer.importDocuments(producer, qm, "/", session.getRepositoryName(), 5, fact);
        assertTrue(importer.getCreatedDocsCounter() > 1000);

    }

    @Test
    public void shouldHandleTopicDuplication() throws IOException {
        final int EXPECTED_TOPIC_SIZE = 1;
        ImporterLogger logger = new BufferredLogger(log);

        QueuesManager<SourceNode> qm = new KQManager<>(logger, 2);
        List<String> topics = ((KQManager)qm).allTopics();

        assertEquals(EXPECTED_TOPIC_SIZE, topics.size());

        QueuesManager<SourceNode> qm2 = new KQManager<>(logger, 2);
        List<String> duplicated = ((KQManager)qm2).allTopics();
        assertEquals(EXPECTED_TOPIC_SIZE, duplicated.size());
    }
}
