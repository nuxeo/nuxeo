package org.nuxeo.ecm.platform.importer.queue.tests;

import java.util.concurrent.BlockingQueue;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.queue.consumer.AbstractConsumer;
import org.nuxeo.ecm.platform.importer.queue.consumer.Consumer;
import org.nuxeo.ecm.platform.importer.queue.consumer.ConsumerFactory;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class BuggyConsumerFactory implements ConsumerFactory {

    class BuggyConsumer extends AbstractConsumer {
        int docs = 0;

        public BuggyConsumer(ImporterLogger log, DocumentModel root, int batchSize, BlockingQueue<SourceNode> queue) {
            super(log, root, batchSize, queue);

        }

        @Override
        public double getNbDocsCreated() {
            return docs;
        }


        @Override
        protected void process(CoreSession session, SourceNode sn) throws Exception {
            if (sn instanceof BuggySourceNode) {
                BuggySourceNode bsn = (BuggySourceNode) sn;
                if (bsn.isTransactionBuggy()) {
                    TransactionHelper.setTransactionRollbackOnly();

                } else {
                    DocumentModel doc = session.createDocumentModel("/", bsn.getName(), "File");
                    doc = session.createDocument(doc);
                    docs++;
                }
                if (bsn.isExceptionBuggy()) {
                    throw new Exception("This is a buggy exception !");
                }

            }
        }

    }

    @Override
    public Consumer createConsumer(ImporterLogger log, DocumentModel root, int batchSize,
            BlockingQueue<SourceNode> queue) {
        return new BuggyConsumer(log, root, batchSize, queue);
    }

}
