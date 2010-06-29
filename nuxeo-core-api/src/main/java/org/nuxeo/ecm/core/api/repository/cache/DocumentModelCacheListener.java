package org.nuxeo.ecm.core.api.repository.cache;

import org.nuxeo.ecm.core.api.DocumentModel;

public interface DocumentModelCacheListener {

    void documentsChanged(DocumentModel[] docs, boolean urgent);

    void subreeChanged(DocumentModel[] docs, boolean urgent);

}
