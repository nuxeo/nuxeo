package org.nuxeo.elasticsearch;

import org.elasticsearch.client.Client;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

public interface ElasticSearchService {

    void index(DocumentLocation docLocation, boolean children);

    String indexNow(DocumentModel doc) throws ClientException ;

    Client getClient();

    DocumentModelList query(CoreSession session, String query, int pageSize);

}
