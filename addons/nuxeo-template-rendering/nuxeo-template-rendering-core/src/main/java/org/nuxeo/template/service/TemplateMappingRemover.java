package org.nuxeo.template.service;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

public class TemplateMappingRemover extends UnrestrictedSessionRunner {

    protected DocumentRef targetRef;

    protected String type2Remove;

    protected TemplateMappingRemover(CoreSession session, DocumentModel doc, String type2Remove) {
        super(session);
        targetRef = doc.getRef();
        this.type2Remove = type2Remove;
    }

    protected TemplateMappingRemover(CoreSession session, String uid, String type2Remove) {
        super(session);
        targetRef = new IdRef(uid);
        this.type2Remove = type2Remove;
    }

    @Override
    public void run() throws ClientException {
        DocumentModel doc = session.getDocument(targetRef);
        TemplateSourceDocument source = doc.getAdapter(TemplateSourceDocument.class);
        source.removeForcedType(type2Remove, true);
    }

}
