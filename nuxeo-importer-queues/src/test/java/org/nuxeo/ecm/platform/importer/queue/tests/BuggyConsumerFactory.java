package org.nuxeo.ecm.platform.importer.queue.tests;

import java.util.Random;
import java.util.concurrent.BlockingQueue;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.queue.consumer.AbstractConsumer;
import org.nuxeo.ecm.platform.importer.queue.consumer.Consumer;
import org.nuxeo.ecm.platform.importer.queue.consumer.ConsumerFactory;
import org.nuxeo.ecm.platform.importer.queue.manager.QueuesManager;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class BuggyConsumerFactory implements ConsumerFactory {

    private final int consumerDelayMs;

    public BuggyConsumerFactory() {
        this(0);
    }

    public BuggyConsumerFactory(int consumerDelayMs) {
        this.consumerDelayMs = consumerDelayMs;
    }

    class BuggyConsumer extends AbstractConsumer {
        private final int delayMs;
        int docs = 0;

        public BuggyConsumer(ImporterLogger log, DocumentModel root, int batchSize, QueuesManager queuesManager, int queue,
                             int delayMs) {
            super(log, root, batchSize, queuesManager, queue);
            this.delayMs = delayMs;
        }

        @Override
        public double getNbDocsCreated() {
            return docs;
        }

        @Override
        protected void process(CoreSession session, SourceNode sn) throws Exception {
            if (sn instanceof BuggySourceNode) {
                BuggySourceNode bsn = (BuggySourceNode) sn;
                DocumentModel doc = session.createDocumentModel("/", bsn.getName(), "File");
                doc = session.createDocument(doc);
                if (delayMs > 0) {
                    Thread.sleep((new Random()).nextInt(delayMs));
                }
                if (bsn.isTransactionBuggy()) {
                    TransactionHelper.setTransactionRollbackOnly();
                    // Thread.sleep(500);
                } else {
                    docs++;
                }
                if (bsn.isExceptionBuggy()) {
                    // Thread.sleep(1000);
                    throw new Exception("This is a buggy exception during consumer processing !");
                }

            }
        }

    }

    @Override
    public Consumer createConsumer(ImporterLogger log, DocumentModel root, int batchSize,
                                   QueuesManager queuesManager, int queue) {
        return new BuggyConsumer(log, root, batchSize, queuesManager, queue, consumerDelayMs);
    }

}
