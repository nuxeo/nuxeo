package com.leroymerlin.corp.fr.nuxeo.portal.testing;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

public class SimpleRepoFactory implements RepoFactory {

    public void createRepo(CoreSession session) throws ClientException {
        DocumentModel doc = session.createDocumentModel("/", "test", "Workspace");
        doc.setPropertyValue("dublincore:title", "Mon titre");
        doc = session.createDocument(doc);
        session.saveDocument(doc);
    }

}
