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

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_REMOVE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CHECKEDIN;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED_BY_COPY;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_MOVED;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.utils.BlobsExtractor;
import org.nuxeo.ecm.quota.AbstractQuotaStatsUpdater;
import org.nuxeo.ecm.quota.QuotaStatsInitialWork;
import org.nuxeo.runtime.api.Framework;

/**
 * {@link org.nuxeo.ecm.quota.QuotaStatsUpdater} counting space used by Blobs in
 * document. This default implementation does not track the space used by
 * versions, or the space used by non-Blob properties
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.6
 */
public class QuotaSyncListenerChecker extends AbstractQuotaStatsUpdater {

    public static final List<String> EVENTS_TO_HANDLE = Arrays.asList(
            ABOUT_TO_REMOVE, DOCUMENT_CREATED_BY_COPY, DOCUMENT_MOVED,
            BEFORE_DOC_UPDATE, DOCUMENT_CREATED);

    public static final String DISABLE_QUOTA_CHECK_LISTENER = "disableQuotaListener";

    @Override
    public void computeInitialStatistics(CoreSession unrestrictedSession,
            QuotaStatsInitialWork currentWorker) {

        QuotaComputerProcessor processor = new QuotaComputerProcessor();
        try {
            IterableQueryResult res = unrestrictedSession.queryAndFetch(
                    "SELECT ecm:uuid FROM Document where ecm:isCheckedInVersion=0 and ecm:isProxy=0 order by dc:created desc",
                    "NXQL");

            log.debug("Starting initial Quota computation");
            long total = res.size();
            log.debug("Start iteration on " + total + " items");
            try {
                long idx = 0;
                for (Map<String, Serializable> r : res) {
                    String uuid = (String) r.get("ecm:uuid");
                    computeSizeOnDocument(unrestrictedSession, uuid, processor);
                    idx++;
                    currentWorker.notifyProgress(idx++, total);
                }
            } finally {
                res.close();
            }

        } catch (Exception e) {
            log.error("Error during initial Quota Size computation", e);
        }

    }

    protected void computeSizeOnDocument(CoreSession unrestrictedSession,
            String uuid, QuotaComputerProcessor processor)
            throws ClientException {

        IdRef ref = new IdRef(uuid);
        if (log.isTraceEnabled()) {
            log.trace("process Quota initial computation on uuid " + uuid);
        }
        if (unrestrictedSession.exists(ref)) {
            DocumentModel target = unrestrictedSession.getDocument(ref);
            if (target.hasFacet(QuotaAwareDocument.DOCUMENTS_SIZE_STATISTICS_FACET)
                    || target.getPathAsString().equals("/")) {
                if (log.isTraceEnabled()) {
                    log.trace("doc with uuid " + uuid + " already up to date");
                }
                return;
            }
            BlobSizeInfo bsi = computeSizeImpact(target, false);
            SizeUpdateEventContext quotaCtx = new SizeUpdateEventContext(
                    unrestrictedSession, bsi, DOCUMENT_CREATED, target);
            if (log.isTraceEnabled()) {
                log.trace("doc with uuid " + uuid + " started update");
            }
            processor.processQuotaComputation(quotaCtx);
            if (log.isTraceEnabled()) {
                log.trace("doc with uuid " + uuid + " update completed");
            }

        } else {
            if (log.isTraceEnabled()) {
                log.trace("doc with uuid " + uuid + " does not exist");
            }
        }
    }

    @Override
    protected ClientException handleException(ClientException e, Event event) {
        if (e instanceof QuotaExceededException) {
            log.info("Current event " + event.getName()
                    + " would break Quota restriction, rolling back");
            event.markRollBack("Quota Exceeded", e);
        }
        return e;
    }

    @Override
    protected void processDocumentCreated(CoreSession session,
            DocumentModel targetDoc, DocumentEventContext docCtx)
            throws ClientException {
        BlobSizeInfo bsi = computeSizeImpact(targetDoc, false);

        DocumentModel targetDocumentToCheck = targetDoc;
        DocumentEventContext targetDocCtx = docCtx;
        String sourceEvent = DOCUMENT_CREATED;

        if (targetDoc.isVersion()) {
            targetDocumentToCheck = session.getDocument(new IdRef(
                    targetDoc.getSourceId()));
            targetDocCtx = new DocumentEventContext(docCtx.getCoreSession(),
                    docCtx.getPrincipal(), targetDocumentToCheck);
            targetDocCtx.setCategory(docCtx.getCategory());
            targetDocCtx.setProperties(docCtx.getProperties());
            sourceEvent = DOCUMENT_CHECKEDIN;
        }

        // only process if Blobs where added or removed
        if (bsi.getBlobSizeDelta() != 0 || targetDoc.isVersion()) {
            checkConstraints(session, targetDocumentToCheck,
                    targetDocumentToCheck.getParentRef(), bsi,
                    targetDoc.isVersion());
            SizeUpdateEventContext asyncEventCtx = new SizeUpdateEventContext(
                    session, targetDocCtx, bsi, sourceEvent);
            sendUpdateEvents(asyncEventCtx);
        }
    }

    @Override
    protected void processDocumentUpdated(CoreSession session,
            DocumentModel doc, DocumentEventContext docCtx)
            throws ClientException {
        // Nothing to do !
    }

    @Override
    protected void processDocumentBeforeUpdate(CoreSession session,
            DocumentModel targetDoc, DocumentEventContext docCtx)
            throws ClientException {
        BlobSizeInfo bsi = computeSizeImpact(targetDoc, true);
        log.debug("calling processDocumentBeforeUpdate, bsi=" + bsi.toString());
        // only process if Blobs where added or removed
        if (bsi.getBlobSizeDelta() != 0) {
            checkConstraints(session, targetDoc, targetDoc.getParentRef(), bsi);
            SizeUpdateEventContext asyncEventCtx = new SizeUpdateEventContext(
                    session, docCtx, bsi, BEFORE_DOC_UPDATE);
            sendUpdateEvents(asyncEventCtx);
        }
    }

    @Override
    protected void processDocumentCopied(CoreSession session,
            DocumentModel targetDoc, DocumentEventContext docCtx)
            throws ClientException {
        QuotaAware quotaDoc = targetDoc.getAdapter(QuotaAware.class);
        if (quotaDoc != null) {
            long total = quotaDoc.getTotalSize();
            BlobSizeInfo bsi = new BlobSizeInfo();
            bsi.blobSize = total;
            bsi.blobSizeDelta = total;
            if (total > 0) {
                // check on parent since Session is not committed for now
                checkConstraints(session, targetDoc, targetDoc.getParentRef(),
                        bsi);
                SizeUpdateEventContext asyncEventCtx = new SizeUpdateEventContext(
                        session, docCtx, bsi, DOCUMENT_CREATED_BY_COPY);
                sendUpdateEvents(asyncEventCtx);
            }
        }
    }

    @Override
    protected void processDocumentMoved(CoreSession session,
            DocumentModel targetDoc, DocumentModel sourceParent,
            DocumentEventContext docCtx) throws ClientException {

        QuotaAware quotaDoc = targetDoc.getAdapter(QuotaAware.class);
        long total = 0;
        if (quotaDoc != null) {
            total = quotaDoc.getTotalSize();
        }
        BlobSizeInfo bsi = new BlobSizeInfo();
        bsi.blobSize = total;
        bsi.blobSizeDelta = total;
        if (total > 0) {
            // check on destination parent since Session is not committed for
            // now
            checkConstraints(session, targetDoc, targetDoc.getParentRef(), bsi);
            SizeUpdateEventContext asyncEventCtx = new SizeUpdateEventContext(
                    session, docCtx, bsi, DOCUMENT_MOVED);
            sendUpdateEvents(asyncEventCtx);

            // also need to trigger update on source tree
            BlobSizeInfo bsiRemove = new BlobSizeInfo();
            bsiRemove.blobSize = total;
            bsiRemove.blobSizeDelta = -total;

            asyncEventCtx = new SizeUpdateEventContext(session, docCtx,
                    sourceParent, bsiRemove, DOCUMENT_MOVED);
            List<String> sourceParentUUIDs = getParentUUIDS(session,
                    sourceParent);
            sourceParentUUIDs.add(0, sourceParent.getId());
            asyncEventCtx.setParentUUIds(sourceParentUUIDs);
            sendUpdateEvents(asyncEventCtx);
        }

    }

    @Override
    protected void processDocumentAboutToBeRemoved(CoreSession session,
            DocumentModel targetDoc, DocumentEventContext docCtx)
            throws ClientException {

        QuotaAware quotaDoc = targetDoc.getAdapter(QuotaAware.class);
        if (quotaDoc != null) {
            long total = quotaDoc.getTotalSize();
            if (total > 0) {
                List<String> parentUUIDs = getParentUUIDS(session, targetDoc);
                SizeUpdateEventContext asyncEventCtx = new SizeUpdateEventContext(
                        session, docCtx, total, ABOUT_TO_REMOVE);
                asyncEventCtx.setParentUUIds(parentUUIDs);
                sendUpdateEvents(asyncEventCtx);
            }
        }
    }

    @Override
    protected boolean needToProcessEventOnDocument(Event event,
            DocumentModel targetDoc) {

        if (targetDoc == null) {
            return false;
        }
        if (targetDoc.isProxy()) {
            log.debug("Escape from listener: not precessing proxies");
            return false;
        }

        Boolean block = (Boolean) targetDoc.getContextData().getScopedValue(
                ScopeType.REQUEST, DISABLE_QUOTA_CHECK_LISTENER);
        if (block != null && block) {
            log.debug("Escape from listener to avoid reentrancy");
            // ignore the event - we are blocked by the caller
            // used to avoid reentrancy when the async event handler
            // do update the docs to set the new size !
            return false;
        }
        return true;
    }

    protected void sendUpdateEvents(SizeUpdateEventContext eventCtx)
            throws ClientException {

        Event quotaUpdateEvent = eventCtx.newQuotaUpdateEvent();
        log.debug("prepared event on target tree with context "
                + eventCtx.toString());
        EventService es = Framework.getLocalService(EventService.class);
        es.fireEvent(quotaUpdateEvent);
    }

    protected List<String> getParentUUIDS(CoreSession unrestrictedSession,
            final DocumentModel doc) throws ClientException {

        final List<String> result = new ArrayList<String>();
        DocumentRef[] parentRefs = unrestrictedSession.getParentDocumentRefs(doc.getRef());
        for (DocumentRef parentRef : parentRefs) {
            result.add(parentRef.toString());
        }
        return result;
    }

    protected void checkConstraints(CoreSession unrestrictedSession,
            final DocumentModel doc, final DocumentRef parentRef,
            final BlobSizeInfo bsi) throws ClientException {
        checkConstraints(unrestrictedSession, doc, parentRef, bsi, false);
    }

    protected void checkConstraints(CoreSession unrestrictedSession,
            final DocumentModel doc, final DocumentRef parentRef,
            final BlobSizeInfo bsi, final boolean checkWithTotalSize)
            throws ClientException {

        long addition = bsi.blobSizeDelta;
        if (checkWithTotalSize) {
            addition = bsi.getBlobSize();
        }

        if (addition <= 0) {
            return;
        }
        List<DocumentModel> parents = unrestrictedSession.getParentDocuments(parentRef);
        parents.add(unrestrictedSession.getDocument(parentRef));
        for (DocumentModel parent : parents) {
            QuotaAware qap = parent.getAdapter(QuotaAware.class);
            if (qap != null && qap.getMaxQuota() > 0) {
                if (qap.getTotalSize() + addition > qap.getMaxQuota()) {
                    log.info("Raising Quota Exception on "
                            + doc.getPathAsString());
                    throw new QuotaExceededException(parent, doc,
                            qap.getMaxQuota());
                }
            }
        }
    }

    protected BlobSizeInfo computeSizeImpact(DocumentModel doc,
            boolean onlyChanges) throws ClientException {

        BlobSizeInfo result = new BlobSizeInfo();

        QuotaAware quotaDoc = doc.getAdapter(QuotaAware.class);
        if (quotaDoc != null) {
            result.blobSize = quotaDoc.getInnerSize();
        } else {
            result.blobSize = 0;
        }

        List<Blob> blobs = getBlobs(doc, onlyChanges);

        if (onlyChanges) {
            if (blobs.size() == 0) {
                result.blobSizeDelta = 0;
            } else {
                long size = 0;
                for (Blob blob : blobs) {
                    if (blob != null) {
                        size += blob.getLength();
                    }
                }
                result.blobSizeDelta = size - result.blobSize;
                result.blobSize = size;
            }
        } else {
            if (blobs.size() == 0) {
                result.blobSizeDelta = -result.blobSize;
                result.blobSize = 0;
            } else {
                long size = 0;
                for (Blob blob : blobs) {
                    if (blob != null) {
                        size += blob.getLength();
                    }
                }
                result.blobSizeDelta = size - result.blobSize;
                result.blobSize = size;
            }
        }
        return result;
    }

    protected List<Blob> getBlobs(DocumentModel doc, boolean onlyChangedBlob)
            throws ClientException {

        try {
            BlobsExtractor extractor = new BlobsExtractor();
            List<Property> blobProperties = extractor.getBlobsProperties(doc);

            boolean needRecompute = !onlyChangedBlob;
            if (!needRecompute) {
                if (blobProperties.size() > 0) {
                    for (Property blobProperty : blobProperties) {
                        if (blobProperty.isDirty()) {
                            needRecompute = true;
                            break;
                        }
                    }
                }
            }
            List<Blob> result = new ArrayList<Blob>();
            if (needRecompute) {
                for (Property blobProperty : blobProperties) {
                    Blob blob = (Blob) blobProperty.getValue();
                    result.add(blob);
                }
            }
            return result;
        } catch (Exception e) {
            throw new ClientException("Unable to extract Blob size", e);
        }
    }

}
