package org.nuxeo.ecm.platform.importer.queue.tests;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.queue.consumer.AbstractConsumer;
import org.nuxeo.ecm.platform.importer.queue.consumer.Consumer;
import org.nuxeo.ecm.platform.importer.queue.consumer.ConsumerFactory;
import org.nuxeo.ecm.platform.importer.queue.manager.QueuesManager;
import org.nuxeo.runtime.transaction.TransactionHelper;

import java.util.Random;

public class BuggyConsumerFactory implements ConsumerFactory {

    private final int consumerDelayMs;

    public BuggyConsumerFactory() {
        this(0);
    }

    public BuggyConsumerFactory(int consumerDelayMs) {
        this.consumerDelayMs = consumerDelayMs;
    }

    class BuggyConsumer extends AbstractConsumer<BuggySourceNode> {
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
        public void process(CoreSession session, BuggySourceNode sn) throws Exception {
            DocumentModel doc = session.createDocumentModel("/", sn.getName(), "File");
            doc = session.createDocument(doc);
            if (delayMs > 0) {
                Thread.sleep((new Random()).nextInt(delayMs));
            }
            if (sn.isTransactionBuggy()) {
                TransactionHelper.setTransactionRollbackOnly();
                // Thread.sleep(500);
            } else {
                docs++;
            }
            if (sn.isExceptionBuggy()) {
                // Thread.sleep(1000);
                throw new Exception("This is a buggy exception during consumer processing !");
            }
        }

    }

    @Override
    public Consumer createConsumer(ImporterLogger log, DocumentModel root, int batchSize,
                                   QueuesManager queuesManager, int queue) {
        return new BuggyConsumer(log, root, batchSize, queuesManager, queue, consumerDelayMs);
    }

}
