/**
 *
 */
package org.nuxeo.ecm.core.search.threading.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 * 
 */
class ReindexingAllTask extends IndexingBrowseTask {

    private static final Log log = LogFactory.getLog(ReindexingAllTask.class);

    private static int DEFAULT_DOC_BATCH_SIZE = 50;

    public ReindexingAllTask(DocumentRef docRef, String repositoryName) {
        super(docRef, repositoryName);
    }

    public void run() {

        final String errorMsg = "Reindexing all documents failed...";

        final int current_batch_size = searchService.getIndexingDocBatchSize();

        try {
            log.debug("Reindexing all task started for document: " + docRef);

            // Increase the batch size for performance sake.
            searchService.setIndexingDocBatchSize(DEFAULT_DOC_BATCH_SIZE);

            final long initialIndexingThreadNumber = searchService.getTotalCompletedIndexingTasks();

            super.run();

            while (searchService.getActiveIndexingTasks() > 0) {

                Thread.sleep(5000);

                long nbIndexedDocs = searchService.getTotalCompletedIndexingTasks()
                        - initialIndexingThreadNumber;

                log.info("indexed docs : " + nbIndexedDocs);
            }

            // Flush remaining indexing sessions below doc
            // batch size.

            // FIXME
            // log.info("Flush remaining sessions...");
            // searchService.saveAllSessions();
            log.debug("Reindexing all task done for document: " + docRef);

        } catch (InterruptedException e) {
            log.error(errorMsg, e);
            // }
            // catch (IndexingException e) {
            // r log.error(errorMsg, e);
        } finally {
            searchService.setIndexingDocBatchSize(current_batch_size);
        }

    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ReindexingAllTask) {
            ReindexingAllTask task = (ReindexingAllTask) obj;
            return docRef.equals(task.docRef)
                    && repositoryName.equals(task.repositoryName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + (docRef == null ? 0 : docRef.hashCode());
        result = 37 * result
                + (repositoryName == null ? 0 : repositoryName.hashCode());
        return result;
    }
}
