package org.nuxeo.ecm.webapp.lifecycle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

public class MassLifeCycleChangeListener implements PostCommitEventListener {

    private static final Log log = LogFactory.getLog(MassLifeCycleChangeListener.class);

    protected static final String LIFECYCLE_TRANSITION_EVENT = "lifecycle_transition_event";

    protected static final String OPTION_NAME_FROM = "from";

    protected static final String OPTION_NAME_TO = "to";

    protected static final String OPTION_NAME_TRANSITION = "transition";

    public void handleEvent(EventBundle events) throws ClientException {

        if (!events.containsEventName(LIFECYCLE_TRANSITION_EVENT)) {
            return;
        }
        for (Event event : events) {
            if (LIFECYCLE_TRANSITION_EVENT.equals(event.getName())) {
                processTransation(event);
            }
        }

    }

    protected void processTransation(Event event) {
        log.debug("Processing lifecycle change in async listener");
        EventContext ctx = event.getContext();
        if (ctx instanceof DocumentEventContext) {
            DocumentEventContext docCtx = (DocumentEventContext) ctx;

            DocumentModel doc = docCtx.getSourceDocument();
            if (doc.isFolder()) {
                CoreSession session = docCtx.getCoreSession();
                if (session == null) {
                    log.error("Can not process lifeCycle change since session is null");
                    return;
                }
                else {
                    DocumentModelList docModelList=null;
                    try {
                        docModelList = session.getChildren(doc.getRef());
                        String transition = (String)docCtx.getProperty(OPTION_NAME_TRANSITION);
                        changeDocumentsState(session, docModelList, transition);
                    } catch (ClientException e) {
                        log.error("Unable to get children", e);
                        return;
                    }
                }
            }
        }
    }

    protected void changeDocumentsState(CoreSession documentManager, DocumentModelList docModelList,
            String transition) throws ClientException {
        for (DocumentModel docMod : docModelList) {
            if (docMod.getAllowedStateTransitions().contains(transition)) {
                docMod.followTransition(transition);
            } else {
                log.warn("Impossible to change state of " + docMod.getRef());
            }

            if (docMod.isFolder()) {
                changeDocumentsState(documentManager, documentManager.getChildren(docMod
                        .getRef()), transition);
            }
        }
    }

}
