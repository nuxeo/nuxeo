package org.nuxeo.ecm.platform.audit;

import javax.persistence.EntityManager;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.runtime.api.Framework;

/**
 * Should implement the future interface needed for handling locally routed
 * message. The MessageHandler should be replaced by the final one at this time.
 * Ask me, alex russel or thierry delprat for more information about that work.
 *
 * @author Stephane Lacoin (Nuxeo EP software engineer)
 */
public class AuditMessageHandler {

    public void onDocumentMessage(EntityManager em, CoreSession session, DocumentMessage message) throws AuditException {
        NXAuditEventsService service =
                (NXAuditEventsService) Framework.getRuntime().getComponent(NXAuditEventsService.NAME);
        service.logMessage(em, session, message);
    }

}
