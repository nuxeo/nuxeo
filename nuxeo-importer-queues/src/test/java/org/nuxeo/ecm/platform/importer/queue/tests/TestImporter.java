/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.platform.importer.queue.consumer.ImmutableNodeConsumerFactory;
import org.nuxeo.ecm.platform.importer.queue.consumer.SourceNodeConsumerFactory;
import org.nuxeo.ecm.platform.importer.queue.manager.BQManager;
import org.nuxeo.ecm.platform.importer.queue.manager.CQManager;
import org.nuxeo.ecm.platform.importer.queue.producer.Producer;
import org.nuxeo.ecm.platform.importer.queue.producer.RandomNodeProducer;
import org.nuxeo.ecm.platform.importer.queue.producer.SourceNodeProducer;
import org.nuxeo.ecm.platform.importer.source.ImmutableNode;
import org.nuxeo.ecm.platform.importer.source.RandomTextSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestImporter {

    protected static final Log log = LogFactory.getLog(TestImporter.class);

    @Inject
    CoreSession session;

    @Test
    public void shouldImportSourceNode() {

        ImporterLogger logger = new BufferredLogger(log);
        QueueImporter<SourceNode> importer = new QueueImporter<>(logger);

        ImporterFilter filter = new EventServiceConfiguratorFilter(true, false, true, false, true);
        importer.addFilter(filter);

        BQManager<SourceNode> qm = new BQManager<>(logger, 2, 100);

        RandomTextSourceNode root = RandomTextSourceNode.init(1000, 1, true);

        Producer<SourceNode> producer = new SourceNodeProducer(root, logger);

        ConsumerFactory<SourceNode> fact = new SourceNodeConsumerFactory();
        importer.importDocuments(producer, qm, "/", session.getRepositoryName(), 5, fact);
        assertTrue(importer.getCreatedDocsCounter() > 1000);

    }

    @Test
    public void shouldImportImmutableNode() {
        int nbConsumers = 7;
        int nbDocuments = 683;
        int batchSize = 100;

        ImporterLogger logger = new BufferredLogger(log);
        QueueImporter<ImmutableNode> importer = new QueueImporter<>(logger);

        ImporterFilter filter = new EventServiceConfiguratorFilter(true, false, true, false, true);
        importer.addFilter(filter);

        // BQManager<ImmutableNode> qm = new BQManager<>(logger, nbConsumers, 10);
        CQManager<ImmutableNode> qm = new CQManager<>(logger, nbConsumers);
        Producer<ImmutableNode> producer = new RandomNodeProducer(logger, nbDocuments, nbConsumers); //.withBlob(1, true);

        ConsumerFactory<ImmutableNode> fact = new ImmutableNodeConsumerFactory();
        importer.importDocuments(producer, qm, "/", session.getRepositoryName(), batchSize, fact);
        assertEquals(nbDocuments, importer.getCreatedDocsCounter());

    }


}
