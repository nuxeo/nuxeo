package org.nuxeo.ecm.platform.importer.queue.tests;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.queue.consumer.AbstractConsumer;
import org.nuxeo.ecm.platform.importer.queue.consumer.Consumer;
import org.nuxeo.ecm.platform.importer.queue.consumer.ConsumerFactory;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

public class SlowConsumerFactory implements ConsumerFactory {

    private int waitTimeInSeconds;

    class SlowConsumer extends AbstractConsumer {
        int docs = 0;

        private int waitTimeInSeconds;

        public SlowConsumer(ImporterLogger log, DocumentModel root, int batchSize, BlockingQueue<SourceNode> queue,
                int waitTimeInSeconds) {
            super(log, root, batchSize, queue);
            this.waitTimeInSeconds = waitTimeInSeconds;

        }

        @Override
        public double getNbDocsCreated() {
            return docs;
        }

        @Override
        protected void process(CoreSession session, SourceNode sn) throws Exception {
            DocumentModel doc = session.createDocumentModel("/", sn.getName(), "File");
            doc = session.createDocument(doc);
            docs++;
            log.error("Consuming " + sn.getName());
            if(waitTimeInSeconds >0) {
                Thread.sleep(TimeUnit.SECONDS.toMillis(waitTimeInSeconds));
            }

        }

    }

    public SlowConsumerFactory(int waitTimeInSeconds) {
        this.waitTimeInSeconds = waitTimeInSeconds;
    }

    @Override
    public Consumer createConsumer(ImporterLogger log, DocumentModel root, int batchSize,
            BlockingQueue<SourceNode> queue) {
        return new SlowConsumer(log, root, batchSize, queue, waitTimeInSeconds);
    }

}
