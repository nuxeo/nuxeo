package org.nuxeo.ecm.platform.ec.notification.service;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;

public class UnrestrictedDocFetcher extends UnrestrictedSessionRunner {

    private String docId;

    private DocumentModel doc;

    public UnrestrictedDocFetcher(String docId) {
        super("default");
        this.docId = docId;
    }

    @Override
    public void run() throws ClientException {
        doc = session.getDocument(new IdRef(docId));
    }

    public DocumentModel getDocument() {
        return doc;
    }

    public static DocumentModel fetch(String docId) {
        UnrestrictedDocFetcher fetcher = new UnrestrictedDocFetcher(docId);
        fetcher.runUnrestricted();
        return fetcher.getDocument();
    }

}
