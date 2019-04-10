package org.nuxeo.ecm.platform.importer.queue.tests;

import org.junit.Test;
import org.nuxeo.ecm.platform.importer.log.BufferredLogger;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.queue.manager.CQManager;
import org.nuxeo.ecm.platform.importer.queue.manager.QueuesManager;
import org.nuxeo.ecm.platform.importer.queue.producer.RandomNodeProducer;
import org.nuxeo.ecm.platform.importer.source.ImmutableNode;

import static org.junit.Assert.*;
import static org.nuxeo.ecm.platform.query.api.AbstractPageProvider.log;
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
 * Contributors:
 *     bdelbosc
 */

/**
 * @since 9.1
 */
public class TestRandomNodeProducer {

    @Test
    public void testProducer() {
        // ImporterLogger logger = mock(ImporterLogger.class);
        // To get logs
        ImporterLogger logger = new BufferredLogger(log);

        // When using big number make sure java assert is disable or CQ GC will knock at the door
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        loader.setDefaultAssertionStatus(false);

        // should be around 100k/s/CPU without blob (355 bytes/doc)
        // 25k/s with 1k blob 3.8KB/doc
        int nbConsumers = 7;
        int nbDocuments = 10 * 1000 + 51;
        RandomNodeProducer producer = new RandomNodeProducer(logger, nbDocuments, nbConsumers)
                .countFolderAsDocument(true)
                .setMaxDocumentsPerFolder(2*1000)
                .setMaxFoldersPerFolder(30)
                .withBlob(1, true)
                .setLang("en_US");

        QueuesManager<ImmutableNode> qm = new CQManager<>(logger, nbConsumers);
        producer.init(qm);
        producer.run();
        assertEquals(nbDocuments, producer.getNbProcessed());
    }
}