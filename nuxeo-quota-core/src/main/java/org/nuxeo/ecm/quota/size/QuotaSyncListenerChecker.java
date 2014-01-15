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

import static org.nuxeo.ecm.core.api.LifeCycleConstants.DELETED_STATE;
import static org.nuxeo.ecm.core.api.LifeCycleConstants.DELETE_TRANSITION;
import static org.nuxeo.ecm.core.api.LifeCycleConstants.TRANSTION_EVENT_OPTION_TRANSITION;
import static org.nuxeo.ecm.core.api.LifeCycleConstants.UNDELETE_TRANSITION;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_REMOVE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_REMOVE_VERSION;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CHECKEDIN;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CHECKEDOUT;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED_BY_COPY;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_MOVED;
import static org.nuxeo.ecm.quota.size.SizeUpdateEventContext.DOCUMENT_UPDATE_INITIAL_STATISTICS;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.nuxeo.ecm.quota.QuotaStatsUpdater} counting space used by Blobs in
 * document. This default implementation does not track the space used by
 * versions, or the space used by non-Blob properties
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.6
 */
public class QuotaSyncListenerChecker extends AbstractQuotaStatsUpdater {

    public static final String DISABLE_QUOTA_CHECK_LISTENER = "disableQuotaListener";

    private static Logger LOG = LoggerFactory.getLogger(QuotaSyncListenerChecker.class);

    @Override
    public void computeInitialStatistics(CoreSession unrestrictedSession,
            QuotaStatsInitialWork currentWorker) {

        QuotaComputerProcessor processor = new QuotaComputerProcessor();
        try {
            String query = "SELECT ecm:uuid FROM Document where ecm:isCheckedInVersion=0 and ecm:isProxy=0 order by dc:created desc";
            IterableQueryResult res = unrestrictedSession.queryAndFetch(query,
                    "NXQL");
            log.debug("Starting initial Quota computation");
            long total = res.size();
            log.debug("Start iteration on " + total + " items");
            try {
                for (Map<String, Serializable> r : res) {
                    String uuid = (String) r.get("ecm:uuid");
                    // this will force an update if the plugin was installed and
                    // then removed
                    try {
                        removeFacet(unrestrictedSession, uuid);
                    } catch (ClientException e) {
                        log.warn("Could not remove facet: " + e.getMessage());
                    }
                }
            } finally {
                res.close();
            }
            try {
                long idx = 0;
                res = unrestrictedSession.queryAndFetch(query, "NXQL");
                for (Map<String, Serializable> r : res) {
                    String uuid = (String) r.get("ecm:uuid");
                    try {
                        computeSizeOnDocument(unrestrictedSession, uuid, processor);
                    } catch (ClientException e) {
                        log.warn("Could not computeSizeOnDocument : " + e.getMessage());
                    }
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

    private void removeFacet(CoreSession unrestrictedSession, String uuid)
            throws ClientException {
        DocumentModel target = unrestrictedSession.getDocument(new IdRef(uuid));
        if (target.hasFacet(QuotaAwareDocument.DOCUMENTS_SIZE_STATISTICS_FACET)) {
            if (log.isTraceEnabled()) {
                log.trace("doc with uuid " + uuid + " already up to date");
            }
            target.removeFacet(QuotaAwareDocument.DOCUMENTS_SIZE_STATISTICS_FACET);
            target = unrestrictedSession.saveDocument(target);
        }
    }

    protected void computeSizeOnDocument(CoreSession unrestrictedSession,
            String uuid, QuotaComputerProcessor processor)
            throws ClientException {
        DocumentModel target = unrestrictedSession.getDocument(new IdRef(uuid));
        IdRef ref = new IdRef(uuid);
        if (log.isTraceEnabled()) {
            log.trace("process Quota initial computation on uuid " + uuid);
        }
        if (unrestrictedSession.exists(ref)) {
            if (log.isTraceEnabled()) {
                log.trace("doc with uuid " + uuid + " started update");
            }
            SizeUpdateEventContext quotaCtx = updateEventToProcessNewDocument(
                    unrestrictedSession, target);
            quotaCtx.getProperties().put(
                    SizeUpdateEventContext._UPDATE_TRASH_SIZE,
                    DELETED_STATE.equals(target.getCurrentLifeCycleState()));
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

        if (targetDoc.isVersion()) {
            // version taken into account by checkout
            // TODO 5.7 version accounting should be different
            return;
        }
        BlobSizeInfo bsi = computeSizeImpact(targetDoc, false);

        // only process if blobs are present
        if (bsi.getBlobSizeDelta() != 0) {
            checkConstraints(session, targetDoc, targetDoc.getParentRef(), bsi);
            SizeUpdateEventContext asyncEventCtx = new SizeUpdateEventContext(
                    session, docCtx, bsi, DOCUMENT_CREATED);
            sendUpdateEvents(asyncEventCtx);
        } else {
            // make the doc quota aware even if the impact size is 0, see
            // NXP-10718
            QuotaAware quotaDoc = targetDoc.getAdapter(QuotaAware.class);
            if (quotaDoc == null) {
                log.debug("  add Quota Facet on " + targetDoc.getPathAsString());
                quotaDoc = QuotaAwareDocumentFactory.make(targetDoc, true);
            }
        }
    }

    @Override
    protected void processDocumentCheckedIn(CoreSession session,
            DocumentModel doc, DocumentEventContext docCtx)
            throws ClientException {
        // on checkin the versions size is incremented (and also the total)

        BlobSizeInfo bsi = computeSizeImpact(doc, false);
        // only process if blobs are present
        if (bsi.getBlobSize() != 0) {
            // no checkConstraints as total size not impacted
            SizeUpdateEventContext asyncEventCtx = new SizeUpdateEventContext(
                    session, docCtx, bsi, DOCUMENT_CHECKEDIN);
            sendUpdateEvents(asyncEventCtx);
        }
    }

    @Override
    protected void processDocumentCheckedOut(CoreSession session,
            DocumentModel doc, DocumentEventContext docCtx)
            throws ClientException {
        // on checkout we account in the total for the last version size
        BlobSizeInfo bsi = computeSizeImpact(doc, false);
        // only process if blobs are present
        if (bsi.getBlobSize() != 0) {
            checkConstraints(session, doc, doc.getParentRef(), bsi, true);
            SizeUpdateEventContext asyncEventCtx = new SizeUpdateEventContext(
                    session, docCtx, bsi, DOCUMENT_CHECKEDOUT);
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
            long total = quotaDoc.getTotalSize() - quotaDoc.getVersionsSize()
                    - quotaDoc.getTrashSize();
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
            long versSize = quotaDoc.getVersionsSize();
            asyncEventCtx.setVersionsSize(versSize);
            sendUpdateEvents(asyncEventCtx);

            // also need to trigger update on source tree
            BlobSizeInfo bsiRemove = new BlobSizeInfo();
            bsiRemove.blobSize = total;
            bsiRemove.blobSizeDelta = -total;

            asyncEventCtx = new SizeUpdateEventContext(session, docCtx,
                    sourceParent, bsiRemove, DOCUMENT_MOVED);
            versSize = -quotaDoc.getVersionsSize();
            asyncEventCtx.setVersionsSize(versSize);
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

        if (targetDoc.isVersion()) {
            // for versions we need to decrement the live doc + it's parents
            List<String> parentUUIDs = new ArrayList<String>();
            parentUUIDs.add(targetDoc.getSourceId());
            parentUUIDs.addAll(getParentUUIDS(session,
                    new IdRef(targetDoc.getSourceId())));

            // We only have to decrement the inner size of this doc
            QuotaAware quotaDoc = targetDoc.getAdapter(QuotaAware.class);
            SizeUpdateEventContext asyncEventCtx = new SizeUpdateEventContext(
                    session, docCtx, quotaDoc.getInnerSize(),
                    ABOUT_TO_REMOVE_VERSION);
            asyncEventCtx.setParentUUIds(parentUUIDs);
            sendUpdateEvents(asyncEventCtx);
            return;
        }

        QuotaAware quotaDoc = targetDoc.getAdapter(QuotaAware.class);
        if (quotaDoc != null) {
            long total = quotaDoc.getTotalSize();
            if (total > 0) {
                List<String> parentUUIDs = getParentUUIDS(session, targetDoc);
                SizeUpdateEventContext asyncEventCtx = new SizeUpdateEventContext(
                        session, docCtx, total, ABOUT_TO_REMOVE);
                // remove size for all its versions from sizeVersions on parents
                long versSize = -quotaDoc.getVersionsSize();
                asyncEventCtx.setVersionsSize(versSize);
                asyncEventCtx.setParentUUIds(parentUUIDs);
                asyncEventCtx.getProperties().put(
                        SizeUpdateEventContext._UPDATE_TRASH_SIZE,
                        DELETED_STATE.equals(targetDoc.getCurrentLifeCycleState()));
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
            final DocumentRef docRef) throws ClientException {

        final List<String> result = new ArrayList<String>();
        if (docRef == null || docRef.toString() == null) {
            return result;
        }
        DocumentRef[] parentRefs = unrestrictedSession.getParentDocumentRefs(docRef);
        for (DocumentRef parentRef : parentRefs) {
            result.add(parentRef.toString());
        }
        return result;
    }

    protected List<String> getParentUUIDS(CoreSession unrestrictedSession,
            final DocumentModel doc) throws ClientException {
        return getParentUUIDS(unrestrictedSession, doc.getRef());
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
            boolean onlyIfBlobHasChanged) throws ClientException {

        BlobSizeInfo result = new BlobSizeInfo();

        QuotaAware quotaDoc = doc.getAdapter(QuotaAware.class);
        if (quotaDoc != null) {
            result.blobSize = quotaDoc.getInnerSize();
        } else {
            result.blobSize = 0;
        }

        List<Blob> blobs = getBlobs(doc, onlyIfBlobHasChanged);

        // If we have no blobs, it can mean
        if (blobs.size() == 0) {
            if (onlyIfBlobHasChanged) {
                // Nothing has changed
                result.blobSizeDelta = 0;
            } else {
                // Or the blob have been removed
                result.blobSizeDelta = -result.blobSize;
                result.blobSize = 0;
            }
        } else {
            // When we have blobs
            long size = 0;
            for (Blob blob : blobs) {
                if (blob != null) {
                    size += blob.getLength();
                }
            }
            result.blobSizeDelta = size - result.blobSize;
            result.blobSize = size;
        }

        return result;
    }

    /**
     * Return the list of changed blob
     *
     * @param doc
     * @param onlyIfBlobHasChanged
     * @return
     * @throws ClientException
     */
    protected List<Blob> getBlobs(DocumentModel doc,
            boolean onlyIfBlobHasChanged) throws ClientException {

        try {
            QuotaSizeService sizeService = Framework.getLocalService(QuotaSizeService.class);
            Set<String> excludedPathSet = new HashSet<String>(
                    sizeService.getExcludedPathList());

            BlobsExtractor extractor = new BlobsExtractor();
            extractor.setExtractorProperties(null, new HashSet<String>(
                    excludedPathSet), true);

            Collection<Property> blobProperties = extractor.getBlobsProperties(doc);

            boolean needRecompute = !onlyIfBlobHasChanged;

            if (onlyIfBlobHasChanged) {
                for (Property blobProperty : blobProperties) {
                    if (blobProperty.isDirty()) {
                        needRecompute = true;
                        break;
                    }
                }
            }

            List<Blob> result = new ArrayList<Blob>();
            if (needRecompute) {
                for (Property blobProperty : blobProperties) {
                    Blob blob = (Blob) blobProperty.getValue();
                    if (blob != null && blob.getLength() < 0) {
                        blob.persist();
                    }
                    String schema = blobProperty.getParent().getSchema().getName();
                    String propName = blobProperty.getName();

                    LOG.debug(String.format(
                            "Using [%s:%s] for quota blob computation (size : %d)",
                            schema, propName, blob.getLength()));
                    result.add(blob);
                }
            }
            return result;
        } catch (Exception e) {
            throw new ClientException("Unable to extract Blob size", e);
        }
    }

    @Override
    protected void processDocumentTrashOp(CoreSession session,
            DocumentModel doc, DocumentEventContext docCtx)
            throws ClientException {
        String transition = (String) docCtx.getProperties().get(
                TRANSTION_EVENT_OPTION_TRANSITION);
        if (transition != null
                && (!(DELETE_TRANSITION.equals(transition) || UNDELETE_TRANSITION.equals(transition)))) {
            return;
        }

        QuotaAware quotaDoc = doc.getAdapter(QuotaAware.class);
        if (quotaDoc != null) {
            long absSize = quotaDoc.getTotalSize();
            long total = (DELETE_TRANSITION.equals(transition) == true ? absSize
                    : -absSize);
            BlobSizeInfo bsi = new BlobSizeInfo();
            bsi.blobSize = total;
            bsi.blobSizeDelta = total;
            if (absSize > 0) {
                // check constrains not needed, since the documents stays in
                // the same folder
                // TODO move this check to QuotaSyncListenerChecker

                SizeUpdateEventContext asyncEventCtx = new SizeUpdateEventContext(
                        session, docCtx, bsi, transition);
                sendUpdateEvents(asyncEventCtx);
            }
        }
    }

    @Override
    protected void processDocumentBeforeRestore(CoreSession session,
            DocumentModel targetDoc, DocumentEventContext docCtx)
            throws ClientException {
        QuotaAware quotaDoc = targetDoc.getAdapter(QuotaAware.class);
        if (quotaDoc != null) {
            long total = quotaDoc.getTotalSize();
            if (total > 0) {
                List<String> parentUUIDs = getParentUUIDS(session, targetDoc);
                SizeUpdateEventContext asyncEventCtx = new SizeUpdateEventContext(
                        session, docCtx, total, ABOUT_TO_REMOVE);
                // remove size for all its versions from sizeVersions on parents
                // since they will be recalculated on restore
                long versSize = -quotaDoc.getVersionsSize();
                asyncEventCtx.setVersionsSize(versSize);
                asyncEventCtx.setParentUUIds(parentUUIDs);
                sendUpdateEvents(asyncEventCtx);
            }
        }
    }

    @Override
    protected void processDocumentRestored(CoreSession session,
            DocumentModel targetDoc, DocumentEventContext docCtx)
            throws ClientException {
        QuotaAware quotaDoc = targetDoc.getAdapter(QuotaAware.class);
        if (quotaDoc == null) {
            log.debug("  add Quota Facet on " + targetDoc.getPathAsString());
            quotaDoc = QuotaAwareDocumentFactory.make(targetDoc, true);
        }
        quotaDoc.resetInfos(true);
        sendUpdateEvents(updateEventToProcessNewDocument(session, targetDoc));
    }

    private SizeUpdateEventContext updateEventToProcessNewDocument(
            CoreSession unrestrictedSession, DocumentModel target)
            throws ClientException {
        BlobSizeInfo bsi = computeSizeImpact(target, false);
        SizeUpdateEventContext quotaCtx = null;

        // process versions if any ; document is not in trash
        List<DocumentModel> versions = unrestrictedSession.getVersions(target.getRef());
        if (versions.size() == 0
                && !DELETED_STATE.equals(target.getCurrentLifeCycleState())) {
            quotaCtx = new SizeUpdateEventContext(unrestrictedSession, bsi,
                    DOCUMENT_CREATED, target);

        } else {
            long lastVersionSize = 0;
            long versionsSize = 0;
            for (DocumentModel documentModel : versions) {
                long s = computeSizeImpact(documentModel, false).blobSize;
                if (documentModel.isLatestVersion()) {
                    lastVersionSize = s;
                } else {
                    versionsSize = versionsSize + s;
                }
            }
            quotaCtx = new SizeUpdateEventContext(unrestrictedSession, bsi,
                    DOCUMENT_UPDATE_INITIAL_STATISTICS, target);
            quotaCtx.setVersionsSizeOnTotal(lastVersionSize);
            quotaCtx.setVersionsSize(versionsSize + lastVersionSize);

        }
        return quotaCtx;
    }
}