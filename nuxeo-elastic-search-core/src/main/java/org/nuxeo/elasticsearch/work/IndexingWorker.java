package org.nuxeo.elasticsearch.work;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.ElasticSearchService;
import org.nuxeo.runtime.api.Framework;

public class IndexingWorker extends AbstractIndexingWorker implements Work {

    protected final boolean recurse;

    public IndexingWorker(DocumentModel doc, boolean recurse) {
        super(doc);
        this.recurse = recurse;
    }

    @Override
    public String getTitle() {
        String title = " Elasticsearch indexing for doc " + docRef
                + " in repository " + repositoryName;
        if (path != null) {
            title = title + " (" + path + ")";
        }
        return title;
    }

    @Override
    protected void doIndexingWork(CoreSession session,
            ElasticSearchService ess, DocumentModel doc) throws Exception {
        ess.indexNow(doc);
        if (recurse) {
            ChildrenIndexingWorker subWorker = new ChildrenIndexingWorker(doc);
            WorkManager wm = Framework.getLocalService(WorkManager.class);
            wm.schedule(subWorker);
        }
    }

    /*
    @Override
    protected Work clone(DocumentModel doc) {
        return new IndexingWorker(doc, recurse);
    }*/

}
