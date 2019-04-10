/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.quota;

import static org.nuxeo.ecm.core.api.LifeCycleConstants.DELETE_TRANSITION;
import static org.nuxeo.ecm.core.api.LifeCycleConstants.TRANSITION_EVENT;
import static org.nuxeo.ecm.core.api.LifeCycleConstants.TRANSTION_EVENT_OPTION_TRANSITION;
import static org.nuxeo.ecm.core.api.LifeCycleConstants.UNDELETE_TRANSITION;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_CHECKIN;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_CHECKOUT;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_REMOVE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_REMOVE_VERSION;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_RESTORE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CHECKEDIN;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CHECKEDOUT;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED_BY_COPY;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_MOVED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_RESTORED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;
import static org.nuxeo.ecm.core.api.trash.TrashService.DOCUMENT_TRASHED;
import static org.nuxeo.ecm.core.api.trash.TrashService.DOCUMENT_UNTRASHED;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.quota.size.QuotaExceededException;

/**
 * Abstract class implementing {@code QuotaStatsUpdater} to handle common cases.
 * <p>
 * Provides abstract methods to override for common events.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public abstract class AbstractQuotaStatsUpdater implements QuotaStatsUpdater {

    protected String name;

    protected String label;

    protected String descriptionLabel;

    protected static Log log = LogFactory.getLog(AbstractQuotaStatsUpdater.class);

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public void setDescriptionLabel(String descriptionLabel) {
        this.descriptionLabel = descriptionLabel;
    }

    @Override
    public String getDescriptionLabel() {
        return descriptionLabel;
    }

    @Override
    public void updateStatistics(CoreSession session, DocumentEventContext docCtx, Event event) {
        DocumentModel doc = docCtx.getSourceDocument();
        if (!needToProcessEventOnDocument(event, doc)) {
            log.debug("Exit Listener !!!!");
            return;
        }
        try {
            switch (event.getName()) {
            case DOCUMENT_CREATED:
                processDocumentCreated(session, doc);
                break;
            case ABOUT_TO_REMOVE:
            case ABOUT_TO_REMOVE_VERSION:
                processDocumentAboutToBeRemoved(session, doc);
                break;
            case DOCUMENT_CREATED_BY_COPY:
                processDocumentCopied(session, doc);
                break;
            case DOCUMENT_MOVED:
                DocumentRef sourceParentRef = (DocumentRef) docCtx.getProperty(CoreEventConstants.PARENT_PATH);
                DocumentRef destinationRef = (DocumentRef) docCtx.getProperty(CoreEventConstants.DESTINATION_REF);
                DocumentModel sourceParent = sourceParentRef == null ? null : session.getDocument(sourceParentRef);
                DocumentModel parent = destinationRef == null ? null : session.getDocument(destinationRef);
                if (sourceParent == null && parent == null
                        || sourceParent != null && parent != null && sourceParent.getId().equals(parent.getId())) {
                    // rename
                    break;
                }
                processDocumentMoved(session, doc, sourceParent);
                break;
            case DOCUMENT_UPDATED:
                processDocumentUpdated(session, doc);
                break;
            case BEFORE_DOC_UPDATE:
                processDocumentBeforeUpdate(session, doc);
                break;
            case TRANSITION_EVENT:
                String transition = (String) docCtx.getProperty(TRANSTION_EVENT_OPTION_TRANSITION);
                if (!DELETE_TRANSITION.equals(transition) && !UNDELETE_TRANSITION.equals(transition)) {
                    break;
                }
                processDocumentTrashOp(session, doc, DELETE_TRANSITION.equals(transition));
                break;
            case DOCUMENT_CHECKEDIN:
                processDocumentCheckedIn(session, doc);
                break;
            case ABOUT_TO_CHECKIN:
                processDocumentBeforeCheckedIn(session, doc);
                break;
            case DOCUMENT_CHECKEDOUT:
                processDocumentCheckedOut(session, doc);
                break;
            case ABOUT_TO_CHECKOUT:
                processDocumentBeforeCheckedOut(session, doc);
                break;
            case BEFORE_DOC_RESTORE:
                processDocumentBeforeRestore(session, doc);
                break;
            case DOCUMENT_RESTORED:
                processDocumentRestored(session, doc);
                break;
            case DOCUMENT_TRASHED:
                processDocumentTrashOp(session, doc, true);
                break;
            case DOCUMENT_UNTRASHED:
                processDocumentTrashOp(session, doc, false);
                break;
            }
        } catch (QuotaExceededException e) {
            handleQuotaExceeded(e, event);
            throw e;
        }
    }

    /** Gets all the ancestors of the document, including the root. */
    protected List<DocumentModel> getAncestors(CoreSession session, DocumentModel doc) {
        List<DocumentModel> ancestors = new ArrayList<>();
        for (DocumentRef documentRef : session.getParentDocumentRefs(doc.getRef())) {
            ancestors.add(session.getDocument(documentRef));
        }
        return ancestors;
    }

    protected abstract void handleQuotaExceeded(QuotaExceededException e, Event event);

    protected abstract boolean needToProcessEventOnDocument(Event event, DocumentModel doc);

    protected abstract void processDocumentCreated(CoreSession session, DocumentModel doc);

    protected abstract void processDocumentCopied(CoreSession session, DocumentModel doc);

    protected abstract void processDocumentCheckedIn(CoreSession session, DocumentModel doc);

    /**
     * @since 11.1
     */
    protected abstract void processDocumentBeforeCheckedIn(CoreSession session, DocumentModel doc);

    protected abstract void processDocumentCheckedOut(CoreSession session, DocumentModel doc);

    /**
     * @since 11.1
     */
    protected abstract void processDocumentBeforeCheckedOut(CoreSession session, DocumentModel doc);

    protected abstract void processDocumentUpdated(CoreSession session, DocumentModel doc);

    protected abstract void processDocumentMoved(CoreSession session, DocumentModel doc, DocumentModel sourceParent);

    protected abstract void processDocumentAboutToBeRemoved(CoreSession session, DocumentModel doc);

    protected abstract void processDocumentBeforeUpdate(CoreSession session, DocumentModel doc);

    protected abstract void processDocumentTrashOp(CoreSession session, DocumentModel doc, boolean isTrashed);

    protected abstract void processDocumentRestored(CoreSession session, DocumentModel doc);

    protected abstract void processDocumentBeforeRestore(CoreSession session, DocumentModel doc);

}
