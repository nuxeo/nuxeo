/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.lifecycle.event;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Listener for life cycle change events.
 * <p>
 * If event occurs on a folder, it will recurse on children to perform the same transition if possible.
 * <p>
 * If the transition event is about marking documents as "deleted", and a child cannot perform the transition, it will
 * be removed.
 * <p>
 * Undelete transitions are not processed, but this listener instead looks for a specific documentUndeleted event. This
 * is because we want to undelete documents (parents) under which we don't want to recurse.
 * <p>
 * Reinit document copy lifeCycle (BulkLifeCycleChangeListener is bound to the event documentCreatedByCopy)
 */
public class BulkLifeCycleChangeListener implements PostCommitEventListener {

    /**
     * @since 8.10-HF05 9.2
     */
    public static final String PAGINATE_GET_CHILDREN_PROPERTY = "nuxeo.bulkLifeCycleChangeListener.paginate-get-children";

    /**
     * @since 8.10-HF05 9.2
     */
    public static final String GET_CHILDREN_PAGE_SIZE_PROPERTY = "nuxeo.bulkLifeCycleChangeListener.get-children-page-size";

    /**
     * @since 8.10-HF05 9.2
     */
    public static final long GET_CHILDREN_PAGINATION_DISABLED_FLAG = -1;

    private static final Log log = LogFactory.getLog(BulkLifeCycleChangeListener.class);

    @Override
    public void handleEvent(EventBundle events) {
        if (!events.containsEventName(LifeCycleConstants.TRANSITION_EVENT)
                && !events.containsEventName(LifeCycleConstants.DOCUMENT_UNDELETED)
                && !events.containsEventName(DocumentEventTypes.DOCUMENT_CREATED_BY_COPY)) {
            return;
        }
        for (Event event : events) {
            String name = event.getName();
            if (LifeCycleConstants.TRANSITION_EVENT.equals(name) || LifeCycleConstants.DOCUMENT_UNDELETED.equals(name)
                    || DocumentEventTypes.DOCUMENT_CREATED_BY_COPY.equals(name)) {
                processTransition(event);
            }
        }
    }

    protected void processTransition(Event event) {
        log.debug("Processing lifecycle change in async listener");
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }
        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        DocumentModel doc = docCtx.getSourceDocument();
        if (!doc.isFolder() && !DocumentEventTypes.DOCUMENT_CREATED_BY_COPY.equals(event.getName())) {
            return;
        }
        CoreSession session = docCtx.getCoreSession();
        if (session == null) {
            log.error("Can not process lifeCycle change since session is null");
            return;
        }
        String transition;
        String targetState;
        if (DocumentEventTypes.DOCUMENT_CREATED_BY_COPY.equals(event.getName())) {
            if (!Boolean.TRUE.equals(event.getContext().getProperties().get(CoreEventConstants.RESET_LIFECYCLE))) {
                return;
            }
            DocumentModelList docs = new DocumentModelListImpl();
            docs.add(doc);
            if (session.exists(doc.getRef())) {
                reinitDocumentsLifeCyle(session, docs);
                session.save();
            }
        } else {
            if (LifeCycleConstants.TRANSITION_EVENT.equals(event.getName())) {
                transition = (String) docCtx.getProperty(LifeCycleConstants.TRANSTION_EVENT_OPTION_TRANSITION);
                if (isNonRecursiveTransition(transition, doc.getType())) {
                    // transition should not recurse into children
                    return;
                }
                if (LifeCycleConstants.UNDELETE_TRANSITION.equals(transition)) {
                    // not processed (as we can undelete also parents)
                    // a specific event documentUndeleted will be used instead
                    return;
                }
                targetState = (String) docCtx.getProperty(LifeCycleConstants.TRANSTION_EVENT_OPTION_TO);
            } else { // LifeCycleConstants.DOCUMENT_UNDELETED
                transition = LifeCycleConstants.UNDELETE_TRANSITION;
                targetState = ""; // unused
            }
            ConfigurationService confService = Framework.getService(ConfigurationService.class);
            boolean paginate = confService.isBooleanPropertyTrue(PAGINATE_GET_CHILDREN_PROPERTY);
            long pageSize = paginate ? Long.parseLong(confService.getProperty(GET_CHILDREN_PAGE_SIZE_PROPERTY, "500"))
                    : GET_CHILDREN_PAGINATION_DISABLED_FLAG;
            changeChildrenState(session, pageSize, transition, targetState, doc);
            session.save();
        }
    }

    protected void reinitDocumentsLifeCyle(CoreSession documentManager, DocumentModelList docs) {
        for (DocumentModel docMod : docs) {
            documentManager.reinitLifeCycleState(docMod.getRef());
            if (docMod.isFolder()) {
                DocumentModelList children = documentManager.query(String.format(
                        "SELECT * FROM Document WHERE ecm:currentLifeCycleState != 'deleted' AND ecm:parentId = '%s'",
                        docMod.getRef()));
                reinitDocumentsLifeCyle(documentManager, children);
            }
        }
    }

    protected boolean isNonRecursiveTransition(String transition, String type) {
        List<String> nonRecursiveTransitions = NXCore.getLifeCycleService().getNonRecursiveTransitionForDocType(type);
        return nonRecursiveTransitions.contains(transition);
    }

    /**
     * @since 8.10-HF05 9.2
     */
    protected void changeChildrenState(CoreSession session, long pageSize, String transition, String targetState,
            DocumentModel doc) {
        if (pageSize == GET_CHILDREN_PAGINATION_DISABLED_FLAG) {
            DocumentModelList docs = session.getChildren(doc.getRef());
            changeDocumentsState(session, pageSize, transition, targetState, docs);
        } else {
            // execute a first query to know total size
            String query = String.format("SELECT * FROM Document where parentId ='%s'", doc.getId());
            DocumentModelList docs = session.query(query, null, pageSize, 0, true);
            changeDocumentsState(session, pageSize, transition, targetState, docs);
            // loop on other children
            long nbChildren = docs.totalSize();
            for (long i = 1; i < nbChildren / pageSize; i++) {
                docs = session.query(query, null, pageSize, pageSize * i, false);
                changeDocumentsState(session, pageSize, transition, targetState, docs);
            }
        }
    }

    // change doc state and recurse in children
    /**
     * @since 8.10-HF05 9.2
     */
    protected void changeDocumentsState(CoreSession session, long pageSize, String transition, String targetState,
            DocumentModelList docs) {
        for (DocumentModel doc : docs) {
            boolean removed = false;
            if (doc.getCurrentLifeCycleState() == null) {
                if (LifeCycleConstants.DELETED_STATE.equals(targetState)) {
                    log.debug("Doc has no lifecycle, deleting ...");
                    session.removeDocument(doc.getRef());
                    removed = true;
                }
            } else if (doc.getAllowedStateTransitions().contains(transition) && !doc.isProxy()) {
                doc.followTransition(transition);
            } else {
                if (targetState.equals(doc.getCurrentLifeCycleState())) {
                    log.debug("Document" + doc.getRef() + " is already in the target LifeCycle state");
                } else if (LifeCycleConstants.DELETED_STATE.equals(targetState)) {
                    log.debug("Impossible to change state of " + doc.getRef() + " :removing");
                    session.removeDocument(doc.getRef());
                    removed = true;
                } else {
                    log.debug("Document" + doc.getRef() + " has no transition to the target LifeCycle state");
                }
            }
            if (doc.isFolder() && !removed) {
                changeChildrenState(session, pageSize, transition, targetState, doc);
            }
        }
    }

    // change doc state and recurse in children
    /**
     * @deprecated since 9.2 use {@link #changeDocumentsState(CoreSession, long, String, String, DocumentModelList)}
     *             instead to allow paginating children fetch (depending on configuration).
     */
    @Deprecated
    protected void changeDocumentsState(CoreSession session, DocumentModelList docs, String transition,
            String targetState) {
        ConfigurationService confService = Framework.getService(ConfigurationService.class);
        boolean paginate = confService.isBooleanPropertyTrue(PAGINATE_GET_CHILDREN_PROPERTY);
        long pageSize = paginate ? Long.parseLong(confService.getProperty(GET_CHILDREN_PAGE_SIZE_PROPERTY, "500"))
                : GET_CHILDREN_PAGINATION_DISABLED_FLAG;
        changeDocumentsState(session, pageSize, transition, targetState, docs);
    }

}
