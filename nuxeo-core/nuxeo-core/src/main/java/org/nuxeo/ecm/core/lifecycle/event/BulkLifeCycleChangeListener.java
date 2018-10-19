/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.lifecycle.event;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.nuxeo.ecm.core.lifecycle.LifeCycleService;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;
import org.nuxeo.runtime.transaction.TransactionHelper;

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
            changeChildrenState(session, transition, targetState, doc);
        }
    }

    protected void reinitDocumentsLifeCyle(CoreSession documentManager, DocumentModelList docs) {
        for (DocumentModel docMod : docs) {
            documentManager.reinitLifeCycleState(docMod.getRef());
            if (docMod.isFolder()) {
                DocumentModelList children = documentManager.query(String.format(
                        "SELECT * FROM Document WHERE ecm:isTrashed = 0 AND ecm:parentId = '%s'", docMod.getRef()));
                reinitDocumentsLifeCyle(documentManager, children);
            }
        }
    }

    protected boolean isNonRecursiveTransition(String transition, String type) {
        List<String> nonRecursiveTransitions = Framework.getService(LifeCycleService.class)
                                                        .getNonRecursiveTransitionForDocType(type);
        return nonRecursiveTransitions.contains(transition);
    }

    /**
     * @since 9.2
     */
    protected void changeChildrenState(CoreSession session, String transition, String targetState, DocumentModel doc) {
        // Check if we need to paginate children fetch
        ConfigurationService confService = Framework.getService(ConfigurationService.class);
        boolean paginate = confService.isBooleanPropertyTrue(PAGINATE_GET_CHILDREN_PROPERTY);
        if (paginate) {
            long pageSize = Long.parseLong(confService.getProperty(GET_CHILDREN_PAGE_SIZE_PROPERTY, "500"));
            // execute a first query to know total size
            String query = String.format("SELECT * FROM Document where ecm:parentId ='%s'", doc.getId());
            DocumentModelList documents = session.query(query, null, pageSize, 0, true);
            changeDocumentsState(session, transition, targetState, documents);
            session.save();
            // commit the first page
            TransactionHelper.commitOrRollbackTransaction();

            // loop on other children
            long nbChildren = documents.totalSize();
            for (long offset = pageSize; offset < nbChildren; offset += pageSize) {
                long i = offset;
                // start a new transaction
                TransactionHelper.runInTransaction(() -> {
                    DocumentModelList docs = session.query(query, null, pageSize, i, false);
                    changeDocumentsState(session, transition, targetState, docs);
                    session.save();
                });
            }

            // start a new transaction for following
            TransactionHelper.startTransaction();
        } else {
            DocumentModelList documents = session.getChildren(doc.getRef());
            changeDocumentsState(session, transition, targetState, documents);
            session.save();
        }
    }

    /**
     * Change doc state. Don't recurse on children as following transition trigger an event which will be handled by
     * this listener.
     *
     * @since 9.2
     */
    protected void changeDocumentsState(CoreSession session, String transition, String targetState,
            DocumentModelList docs) {
        for (DocumentModel doc : docs) {
            if (doc.getCurrentLifeCycleState() == null) {
                if (LifeCycleConstants.DELETED_STATE.equals(targetState)) {
                    log.debug("Doc has no lifecycle, deleting ...");
                    session.removeDocument(doc.getRef());
                }
            } else if (doc.getAllowedStateTransitions().contains(transition) && !doc.isProxy()) {
                if (LifeCycleConstants.DELETE_TRANSITION.equals(transition)
                        || LifeCycleConstants.UNDELETE_TRANSITION.equals(transition)) {
                    // just skip renaming for trash mechanism
                    // here we leverage backward compatibility mechanism in AbstractSession#followTransition
                    doc.putContextData(TrashService.DISABLE_TRASH_RENAMING, Boolean.TRUE);
                }
                doc.followTransition(transition);
            } else {
                if (targetState.equals(doc.getCurrentLifeCycleState())) {
                    log.debug("Document" + doc.getRef() + " is already in the target LifeCycle state");
                } else if (LifeCycleConstants.DELETED_STATE.equals(targetState)) {
                    log.debug("Impossible to change state of " + doc.getRef() + " :removing");
                    session.removeDocument(doc.getRef());
                } else {
                    log.debug("Document" + doc.getRef() + " has no transition to the target LifeCycle state");
                }
            }
        }
    }

}
