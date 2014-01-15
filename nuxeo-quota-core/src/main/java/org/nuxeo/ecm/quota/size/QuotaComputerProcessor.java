/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */

package org.nuxeo.ecm.quota.size;

import static org.nuxeo.ecm.core.api.LifeCycleConstants.DELETE_TRANSITION;
import static org.nuxeo.ecm.core.api.LifeCycleConstants.UNDELETE_TRANSITION;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_REMOVE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_REMOVE_VERSION;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CHECKEDIN;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CHECKEDOUT;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED_BY_COPY;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_MOVED;
import static org.nuxeo.ecm.quota.size.QuotaAwareDocument.DOCUMENTS_SIZE_STATISTICS_FACET;
import static org.nuxeo.ecm.quota.size.SizeUpdateEventContext.DOCUMENT_UPDATE_INITIAL_STATISTICS;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.ShallowDocumentModel;

/**
 * Asynchronous listener triggered by the {@link QuotaSyncListenerChecker} when
 * Quota needs to be recomputed
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.6
 */
public class QuotaComputerProcessor implements PostCommitEventListener {

    protected static final Log log = LogFactory.getLog(QuotaComputerProcessor.class);

    @Override
    public void handleEvent(EventBundle eventBundle) throws ClientException {

        if (eventBundle.containsEventName(SizeUpdateEventContext.QUOTA_UPDATE_NEEDED)) {

            for (Event event : eventBundle) {
                if (event.getName().equals(
                        SizeUpdateEventContext.QUOTA_UPDATE_NEEDED)) {
                    EventContext ctx = event.getContext();

                    if (ctx instanceof DocumentEventContext) {
                        SizeUpdateEventContext quotaCtx = SizeUpdateEventContext.unwrap((DocumentEventContext) ctx);
                        if (quotaCtx != null) {
                            processQuotaComputation(quotaCtx);
                            // double check
                            debugCheck(quotaCtx);
                        }
                    }
                }
            }
        }
    }

    protected void debugCheck(SizeUpdateEventContext quotaCtx)
            throws ClientException {
        String sourceEvent = quotaCtx.getSourceEvent();
        CoreSession session = quotaCtx.getCoreSession();
        DocumentModel sourceDocument = quotaCtx.getSourceDocument();

        if (session.exists(sourceDocument.getRef())) {
            DocumentModel doc = session.getDocument(sourceDocument.getRef());
            if (log.isTraceEnabled()) {
                if (doc.hasFacet(DOCUMENTS_SIZE_STATISTICS_FACET)) {
                    log.trace("Double Check Facet was added OK");
                } else {
                    log.trace("No facet !!!!");
                }
            }
        } else {
            log.debug("Document " + sourceDocument.getRef()
                    + " no longer exists (" + sourceEvent + ")");
        }

    }

    public void processQuotaComputation(SizeUpdateEventContext quotaCtx)
            throws ClientException {
        String sourceEvent = quotaCtx.getSourceEvent();
        CoreSession session = quotaCtx.getCoreSession();
        DocumentModel sourceDocument = quotaCtx.getSourceDocument();

        if (sourceDocument instanceof ShallowDocumentModel) {
            if (!(ABOUT_TO_REMOVE.equals(sourceEvent) || ABOUT_TO_REMOVE_VERSION.equals(sourceEvent))) {
                log.error("Unable to reconnect Document "
                        + sourceDocument.getPathAsString() + " on event "
                        + sourceEvent);
                return;
            }
        }
        List<DocumentModel> parents = new ArrayList<DocumentModel>();

        log.debug("compute Quota on " + sourceDocument.getPathAsString()
                + " and parents");

        if (ABOUT_TO_REMOVE.equals(sourceEvent)
                || ABOUT_TO_REMOVE_VERSION.equals(sourceEvent)) {
            // use the store list of parentIds
            for (String id : quotaCtx.getParentUUIds()) {
                if (session.exists(new IdRef(id))) {
                    parents.add(session.getDocument(new IdRef(id)));
                }
            }
        } else if (DOCUMENT_MOVED.equals(sourceEvent)) {

            if (quotaCtx.getParentUUIds() != null
                    && quotaCtx.getParentUUIds().size() > 0) {
                // use the store list of parentIds
                for (String id : quotaCtx.getParentUUIds()) {
                    if (session.exists(new IdRef(id))) {
                        parents.add(session.getDocument(new IdRef(id)));
                    }
                }
            } else {
                parents.addAll(getParents(sourceDocument, session));
            }
        } else {
            // DELETE_TRANSITION
            // UNDELETE_TRANSITION
            // BEFORE_DOC_UPDATE
            // DOCUMENT_CREATED
            // DOCUMENT_CREATED_BY_COPY
            // DOCUMENT_CHECKEDIN
            // DOCUMENT_CHECKEDOUT

            // several events in the bundle may impact the same doc,
            // so it may have already been modified
            sourceDocument = session.getDocument(sourceDocument.getRef());
            // TODO fix DocumentModel.refresh() to correctly take into account
            // dynamic facets, then use this instead:
            // sourceDocument.refresh();

            if (sourceDocument.getRef() == null) {
                log.error("SourceDocument has no ref");
            } else {
                try {
                parents.addAll(getParents(sourceDocument, session));
                } catch (ClientException e) {
                    log.trace("Could get parent : " + e.getMessage());
                }
            }

            QuotaAware quotaDoc = sourceDocument.getAdapter(QuotaAware.class);
            // process Quota on target Document
            if (!DOCUMENT_CREATED_BY_COPY.equals(sourceEvent)) {
                if (quotaDoc == null) {
                    log.debug("  add Quota Facet on "
                            + sourceDocument.getPathAsString());
                    quotaDoc = QuotaAwareDocumentFactory.make(sourceDocument,
                            false);

                } else {
                    log.debug("  update Quota Facet on "
                            + sourceDocument.getPathAsString());
                }
                if (DOCUMENT_CHECKEDIN.equals(sourceEvent)) {
                    long versionSize = getVersionSizeFromCtx(quotaCtx);
                    quotaDoc.addVersionsSize(versionSize, false);
                    quotaDoc.addTotalSize(versionSize, true);

                } else if (DOCUMENT_CHECKEDOUT.equals(sourceEvent)) {
                    // All quota computation are now handled on Checkin
                } else if (DELETE_TRANSITION.equals(sourceEvent)
                        || UNDELETE_TRANSITION.equals(sourceEvent)) {
                    quotaDoc.addTrashSize(quotaCtx.getBlobSize(), true);
                } else if (DOCUMENT_UPDATE_INITIAL_STATISTICS.equals(sourceEvent)) {
                    quotaDoc.addInnerSize(quotaCtx.getBlobSize(), false);
                    quotaDoc.addTotalSize(quotaCtx.getVersionsSizeOnTotal(),
                            false);
                    quotaDoc.addTrashSize(quotaCtx.getTrashSize(), false);
                    quotaDoc.addVersionsSize(quotaCtx.getVersionsSize(), true);
                } else {
                    // BEFORE_DOC_UPDATE
                    // DOCUMENT_CREATED
                    quotaDoc.addInnerSize(quotaCtx.getBlobDelta(), true);
                }
            } else {
                //When we copy some doc that are not folderish, we don't
                //copy the versions so we can't rely on the copied quotaDocInfo
                if (!sourceDocument.isFolder()) {
                    quotaDoc.resetInfos(false);
                    quotaDoc.addInnerSize(quotaCtx.getBlobSize(), true);
                }
            }

        }
        if (parents.size() > 0) {
            if (DOCUMENT_CHECKEDIN.equals(sourceEvent)) {
                long versionSize = getVersionSizeFromCtx(quotaCtx);

                processOnParents(parents, versionSize, 0L, versionSize, true,
                        false, true);
            } else if (DOCUMENT_CHECKEDOUT.equals(sourceEvent)) {
                // All quota computation are now handled on Checkin
            } else if (DELETE_TRANSITION.equals(sourceEvent)
                    || UNDELETE_TRANSITION.equals(sourceEvent)) {
                processOnParents(parents, quotaCtx.getBlobSize(),
                        quotaCtx.getTrashSize(), false, true);
            } else if (ABOUT_TO_REMOVE_VERSION.equals(sourceEvent)) {
                processOnParents(parents, quotaCtx.getBlobDelta(), 0L,
                        quotaCtx.getBlobDelta(), true, false, true);
            } else if (ABOUT_TO_REMOVE.equals(sourceEvent)) {
                // when permanently deleting the doc clean the trash if the doc
                // is in trash and all
                // archived versions size
                processOnParents(
                        parents,
                        quotaCtx.getBlobDelta(),
                        quotaCtx.getTrashSize(),
                        quotaCtx.getVersionsSize(),
                        true,
                        quotaCtx.getProperties().get(
                                SizeUpdateEventContext._UPDATE_TRASH_SIZE) != null
                                && (Boolean) quotaCtx.getProperties().get(
                                        SizeUpdateEventContext._UPDATE_TRASH_SIZE),
                        true);
            } else if (DOCUMENT_MOVED.equals(sourceEvent)) {
                // update versionsSize on source parents since all archived
                // versions
                // are also moved
                processOnParents(parents, quotaCtx.getBlobDelta(), 0L,
                        quotaCtx.getVersionsSize(), true, false, true);
            } else if (DOCUMENT_UPDATE_INITIAL_STATISTICS.equals(sourceEvent)) {
                processOnParents(
                        parents,
                        quotaCtx.getBlobSize()
                                + quotaCtx.getVersionsSizeOnTotal(),
                        quotaCtx.getTrashSize(),
                        quotaCtx.getVersionsSize(),
                        true,
                        quotaCtx.getProperties().get(
                                SizeUpdateEventContext._UPDATE_TRASH_SIZE) != null
                                && (Boolean) quotaCtx.getProperties().get(
                                        SizeUpdateEventContext._UPDATE_TRASH_SIZE),
                        true);
            } else if (DOCUMENT_CREATED_BY_COPY.equals(sourceEvent)) {
                processOnParents(parents, quotaCtx.getBlobSize(), 0, true,
                        false);
            } else {
                processOnParents(parents, quotaCtx.getBlobDelta(),
                        quotaCtx.getBlobDelta(), true, false);
            }
        }
    }

    /**
     * @param quotaCtx
     * @return
     */
    private long getVersionSizeFromCtx(SizeUpdateEventContext quotaCtx) {
        return quotaCtx.getBlobSize() - quotaCtx.getBlobDelta();
    }

    protected void processOnParents(List<DocumentModel> parents, long delta,
            long trash, boolean total, boolean trashOp) throws ClientException {
        processOnParents(parents, delta, trash, 0L, total, trashOp, false);
    }

    protected void processOnParents(List<DocumentModel> parents,
            long deltaTotal, long trashSize, long deltaVersions, boolean total,
            boolean trashOp, boolean versionsOp) throws ClientException {
        for (DocumentModel parent : parents) {
            // process Quota on target Document
            QuotaAware quotaDoc = parent.getAdapter(QuotaAware.class);
            if (quotaDoc == null) {
                log.debug("   add Quota Facet on parent "
                        + parent.getPathAsString());
                quotaDoc = QuotaAwareDocumentFactory.make(parent, false);
            } else {
                log.debug("   update Quota Facet on parent "
                        + parent.getPathAsString());
            }
            if (total) {
                quotaDoc.addTotalSize(deltaTotal, true);
            }
            if (trashOp) {
                quotaDoc.addTrashSize(deltaTotal, true);
            }
            if (versionsOp) {
                quotaDoc.addVersionsSize(deltaVersions, true);
            }
        }
    }

    protected List<DocumentModel> getParents(DocumentModel sourceDocument,
            CoreSession session) throws ClientException {
        List<DocumentModel> parents = new ArrayList<DocumentModel>();
        // use getParentDocumentRefs instead of getParentDocuments , beacuse
        // getParentDocuments doesn't fetch the root document
        DocumentRef[] parentRefs = session.getParentDocumentRefs(sourceDocument.getRef());
        for (DocumentRef documentRef : parentRefs) {
            parents.add(session.getDocument(documentRef));
        }
        return parents;
    }
}