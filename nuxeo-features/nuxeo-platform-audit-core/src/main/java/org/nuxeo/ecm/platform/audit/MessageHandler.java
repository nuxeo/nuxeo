package org.nuxeo.ecm.platform.audit;

import javax.persistence.EntityManager;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;

public interface MessageHandler {

    void onDocumentMessage(EntityManager em, CoreSession session,
            DocumentMessage message) throws AuditException;

}