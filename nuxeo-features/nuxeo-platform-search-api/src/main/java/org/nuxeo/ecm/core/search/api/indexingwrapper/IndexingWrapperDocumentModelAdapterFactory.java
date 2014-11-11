package org.nuxeo.ecm.core.search.api.indexingwrapper;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.runtime.api.Framework;

public class IndexingWrapperDocumentModelAdapterFactory implements DocumentAdapterFactory {

    protected static IndexingWrapperManagerService wrapperService;

    protected IndexingWrapperManagerService getWrapperService() {
        if (wrapperService == null) {
            wrapperService = Framework
                    .getLocalService(IndexingWrapperManagerService.class);
        }
        return wrapperService;
    }

    public Object getAdapter(DocumentModel doc, Class itf) {
        return getWrapperService().getIndexingWrapper(doc);
    }

}
