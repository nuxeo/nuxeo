package org.nuxeo.elasticsearch.work;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.ElasticSearchService;
import org.nuxeo.runtime.api.Framework;

public abstract class AbstractIndexingWorker extends AbstractWork {

    protected final String repositoryName;

    protected final DocumentRef docRef;

    protected String path;

    public AbstractIndexingWorker(DocumentModel doc) {
        repositoryName = doc.getRepositoryName();
        docRef = doc.getRef();
        path = doc.getPathAsString();
    }

    public boolean isAlreadyScheduledForIndexing(DocumentModel doc) {
        return Framework.getLocalService(ElasticSearchAdmin.class).isAlreadyScheduledForIndexing(
                doc);
    }

    @Override
    public String getCategory() {
        return "elasticSearchIndexing";
    }

    @Override
    public void work() throws Exception {
        CoreSession session = initSession(repositoryName);
        ElasticSearchService ess = Framework.getLocalService(ElasticSearchService.class);

        if (!session.exists(docRef) && Framework.isTestModeSet()) {
            // temporary hack for Test setup not being transactional
            Thread.sleep(500);
            WorkManager wm = Framework.getLocalService(WorkManager.class);
//            wm.schedule(this.clone(doc));
            return;
        }

        DocumentModel doc = session.getDocument(docRef);
        doIndexingWork(session, ess, doc);
    }

    protected abstract void doIndexingWork(CoreSession session, ElasticSearchService ess, DocumentModel doc) throws Exception ;

    protected abstract Work clone(DocumentModel doc);

}
