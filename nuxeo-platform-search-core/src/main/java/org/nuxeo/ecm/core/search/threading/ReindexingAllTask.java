/**
 *
 */
package org.nuxeo.ecm.core.search.threading;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;

/**
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class ReindexingAllTask extends IndexingTask {

    private static final Log log = LogFactory.getLog(ReindexingAllTask.class);

    private static int DEFAULT_DOC_BATCH_SIZE = 50;

    public ReindexingAllTask(DocumentModel dm, Boolean recursive) {
        super(dm, recursive);
    }

    public ReindexingAllTask(DocumentModel dm, Boolean recursive,
            boolean fulltext) {
        super(dm, recursive, fulltext);
    }

    // XXX deal with that.
    public ReindexingAllTask(ResolvedResources resources) {
        super(resources);
    }

    public void run() {

        final String errorMsg = "Reindexing all documents failed...";

        // Init search service.
        getSearchService();

        final int current_batch_size = searchService.getIndexingDocBatchSize();

        try {

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
            //log.info("Flush remaining sessions...");
            //searchService.saveAllSessions();

        } catch (InterruptedException e) {
            log.error(errorMsg, e);
        //}
        //catch (IndexingException e) {
        //r    log.error(errorMsg, e);
        } finally {
            searchService.setIndexingDocBatchSize(current_batch_size);
        }

    }
}
