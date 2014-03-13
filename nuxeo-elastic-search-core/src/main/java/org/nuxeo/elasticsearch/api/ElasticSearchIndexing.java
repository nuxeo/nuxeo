package org.nuxeo.elasticsearch.api;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

public interface ElasticSearchIndexing {

    /**
     * Starts an async indexing task
     *
     * @param doc
     * @param recurse
     */
    void index(DocumentModel doc, boolean recurse);

    /**
     * Index synchronously a single {@link DocumentModel}
     *
     * @param doc
     * @return
     * @throws ClientException
     */
    String indexNow(DocumentModel doc) throws ClientException;

}
