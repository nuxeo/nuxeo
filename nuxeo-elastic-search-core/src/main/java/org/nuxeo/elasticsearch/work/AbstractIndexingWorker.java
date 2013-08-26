package org.nuxeo.elasticsearch.work;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.elasticsearch.ElasticSearchAdmin;
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

}
