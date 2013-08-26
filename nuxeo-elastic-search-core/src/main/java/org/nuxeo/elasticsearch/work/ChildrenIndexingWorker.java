package org.nuxeo.elasticsearch.work;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelIterator;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.ElasticSearchService;
import org.nuxeo.runtime.api.Framework;

public class ChildrenIndexingWorker extends AbstractIndexingWorker implements
        Work {

    public ChildrenIndexingWorker(DocumentModel doc) {
        super(doc);
    }

    @Override
    public String getTitle() {
        String title = " Elasticsearch indexing children for doc " + docRef
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
        DocumentModelIterator iter = session.getChildrenIterator(docRef);
        while (iter.hasNext()) {
            DocumentModel child = iter.next();
            if (!isAlreadyScheduledForIndexing(child)) {
                ess.indexNow(child);
            }
            if (child.isFolder()) {
                ChildrenIndexingWorker subWorker = new ChildrenIndexingWorker(
                        child);
                WorkManager wm = Framework.getLocalService(WorkManager.class);
                wm.schedule(subWorker);
            }
        }

    }

}
