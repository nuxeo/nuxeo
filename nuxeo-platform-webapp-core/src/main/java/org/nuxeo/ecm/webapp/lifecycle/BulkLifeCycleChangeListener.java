/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

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

public class BulkLifeCycleChangeListener implements PostCommitEventListener {

    private static final Log log = LogFactory.getLog(BulkLifeCycleChangeListener.class);

    protected static final String LIFECYCLE_TRANSITION_EVENT = "lifecycle_transition_event";
    protected static final String OPTION_NAME_FROM = "from";
    protected static final String OPTION_NAME_TO = "to";
    protected static final String OPTION_NAME_TRANSITION = "transition";
 
    public static final String DELETED_LIFECYCLE_STATE = "deleted";

    public void handleEvent(EventBundle events) throws ClientException {
        if (!events.containsEventName(LIFECYCLE_TRANSITION_EVENT)) {
            return;
        }
        for (Event event : events) {
            if (LIFECYCLE_TRANSITION_EVENT.equals(event.getName())) {
                processTransition(event);
            }
        }
    }

    protected void processTransition(Event event) {
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
                } else {
                    try {
                        DocumentModelList docModelList = session.getChildren(doc.getRef());
                        String transition = (String) docCtx.getProperty(OPTION_NAME_TRANSITION);
                        String targetState = (String) docCtx.getProperty(OPTION_NAME_TO);
                        changeDocumentsState(session, docModelList, transition, targetState);
                        session.save();
                    } catch (ClientException e) {
                        log.error("Unable to get children", e);
                        return;
                    }
                }
            }
        }
    }

    protected void changeDocumentsState(CoreSession documentManager, DocumentModelList docModelList,
            String transition, String targetState) throws ClientException {
        for (DocumentModel docMod : docModelList) {
            boolean removed = false;
            if (docMod.getCurrentLifeCycleState() == null) {
                if (DELETED_LIFECYCLE_STATE.equals(targetState)) {
                    log.debug("Doc has no lifecycle, deleting ...");
                    documentManager.removeDocument(docMod.getRef());
                    removed = true;
                }
            } else if (docMod.getAllowedStateTransitions().contains(transition)) {
                docMod.followTransition(transition);
            } else {
                if (targetState.equals(docMod.getCurrentLifeCycleState())) {
                    log.debug("Document" + docMod.getRef() + " is already in the target LifeCycle state");
                } else if (DELETED_LIFECYCLE_STATE.equals(targetState)) {
                    log.debug("Impossible to change state of " + docMod.getRef() + " :removing");
                    documentManager.removeDocument(docMod.getRef());
                    removed = true;
                } else {
                    log.debug("Document" + docMod.getRef() + " has no transition to the target LifeCycle state");
                }
            }
            if (docMod.isFolder() && !removed) {
                changeDocumentsState(documentManager,
                        documentManager.getChildren(docMod.getRef()),
                        transition, targetState);
            }
        }
    }

}
