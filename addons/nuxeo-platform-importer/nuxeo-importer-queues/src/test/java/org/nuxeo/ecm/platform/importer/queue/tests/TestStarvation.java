package org.nuxeo.ecm.platform.importer.queue.tests;

import static org.junit.Assert.assertEquals;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.importer.filter.EventServiceConfiguratorFilter;
import org.nuxeo.ecm.platform.importer.filter.ImporterFilter;
import org.nuxeo.ecm.platform.importer.log.BufferredLogger;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.queue.QueueImporter;
import org.nuxeo.ecm.platform.importer.queue.consumer.ConsumerFactory;
import org.nuxeo.ecm.platform.importer.queue.manager.RandomQueuesManager;
import org.nuxeo.ecm.platform.importer.queue.producer.Producer;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestStarvation {

    protected static final Log log = LogFactory.getLog(TestImporter.class);

    @Inject
    CoreSession session;

    private ImporterLogger logger;

    private QueueImporter importer;

    private RandomQueuesManager qm;

    @Before
    public void doBefore() {
        logger = new BufferredLogger(log);
        importer = new QueueImporter(logger);
        ImporterFilter filter = new EventServiceConfiguratorFilter(true, false, true, false, true);
        importer.addFilter(filter);
        qm = new RandomQueuesManager(logger, 3, 2);
    }

    @Test
    public void slowProducerTest() throws InterruptedException {



        // Given a producer that generate some buggy nodes.
        // index-0, index-30, index-50, index-60 and index-90 should not be created
        Producer producer = new CustomSpeedProducer(logger, 20, 10);
        ConsumerFactory fact = new BuggyConsumerFactory();

        // When the importer launches the import
        importer.importDocuments(producer, qm, "/", session.getRepositoryName(), 5, fact);

        // Then all nodes should be imported
        DocumentModelList docs = session.query("SELECT * FROM File");
        assertEquals("Count of documents that should have been created after import", 20, docs.size());


    }

    @Test
    public void slowConsumerTest() throws Exception {
        int nbNode = 20;
        int speed = 0;
        Producer producer = new CustomSpeedProducer(logger, nbNode, speed);
        int waitTimeInSeconds = 10;
        ConsumerFactory fact = new SlowConsumerFactory(waitTimeInSeconds);

        // When the importer launches the import
        int batchSize = 3;
        importer.importDocuments(producer, qm, "/", session.getRepositoryName(), batchSize, fact);


        DocumentModelList docs = session.query("SELECT * FROM File");
        assertEquals("Count of documents that should have been created after import", nbNode, docs.size());
    }

}
