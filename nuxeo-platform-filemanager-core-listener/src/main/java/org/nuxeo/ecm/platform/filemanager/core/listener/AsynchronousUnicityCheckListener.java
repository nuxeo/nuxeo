package org.nuxeo.ecm.platform.filemanager.core.listener;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

public class AsynchronousUnicityCheckListener extends AbstractUnicityChecker implements PostCommitEventListener  {

    private static final Log log = LogFactory.getLog(AsynchronousUnicityCheckListener.class);


    public void handleEvent(EventBundle events) throws ClientException {

        if (!isUnicityCheckEnabled()) {
            return;
        }

        if (events.containsEventName(DocumentEventTypes.DOCUMENT_CREATED) || events.containsEventName(DocumentEventTypes.DOCUMENT_UPDATED)) {

            List<String> uuids = new ArrayList<String>();
            for (Event event : events) {
                if (DocumentEventTypes.DOCUMENT_CREATED.equals(event.getName()) || DocumentEventTypes.DOCUMENT_UPDATED.equals(event.getName())) {
                    EventContext ctx = event.getContext();
                    if (ctx instanceof DocumentEventContext) {
                        DocumentEventContext docCtx = (DocumentEventContext) ctx;

                        DocumentModel doc2Check = docCtx.getSourceDocument();
                        if (doc2Check.isProxy()) {
                            continue;
                        }
                        if (!uuids.contains(doc2Check.getId())) {
                            uuids.add(doc2Check.getId());
                            doUnicityCheck(doc2Check, docCtx.getCoreSession(), event);
                        }
                    }
                }
            }

        }

    }


    @Override
    protected void onDuplicatedDoc(CoreSession session, Principal principal,
            DocumentModel newDoc, List<DocumentLocation> existingDocs, Event event) {
        // simply send a message
        log.info("Duplicated file detected");
        raiseDuplicatedFileEvent(session, principal, newDoc, existingDocs);
    }



}
