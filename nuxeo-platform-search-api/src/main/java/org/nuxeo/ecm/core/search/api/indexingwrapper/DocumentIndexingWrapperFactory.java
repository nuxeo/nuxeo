package org.nuxeo.ecm.core.search.api.indexingwrapper;

import org.nuxeo.ecm.core.api.DocumentModel;

public interface DocumentIndexingWrapperFactory {

	DocumentModel getIndexingWrapper(DocumentModel doc);
}
