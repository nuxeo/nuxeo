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
    public void work() throws Exception {
        CoreSession session = initSession(repositoryName);
        ElasticSearchService ess = Framework.getLocalService(ElasticSearchService.class);

        DocumentModel doc = session.getDocument(docRef);
        ess.indexNow(doc);
        if (recurse) {
            ChildrenIndexingWorker subWorker = new ChildrenIndexingWorker(doc);
            WorkManager wm = Framework.getLocalService(WorkManager.class);
            wm.schedule(subWorker);
        }
    }

}
