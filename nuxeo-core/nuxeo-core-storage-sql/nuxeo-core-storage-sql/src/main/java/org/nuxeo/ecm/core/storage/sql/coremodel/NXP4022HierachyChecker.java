package org.nuxeo.ecm.core.storage.sql.coremodel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

public class NXP4022HierachyChecker {
    
    protected static final Log log = LogFactory.getLog(NXP4022HierachyChecker.class);
    
    public static boolean exists(CoreSession session, DocumentRef rootRef, DocumentRef docRef) throws ClientException {
        if (rootRef.equals(docRef)) {
            return true;
        }
        if (!session.exists(docRef)) {
            log.error("document " + docRef + " is gone");
            return false;
        }
        DocumentModel document = session.getDocument(docRef);
        if (exists(session, rootRef, document.getParentRef()) == false) {
            log.error("document " + document.getId() + "(" + document.getTitle() + ") is an orphan");
            return false;
        }
        return true;
    }

}
