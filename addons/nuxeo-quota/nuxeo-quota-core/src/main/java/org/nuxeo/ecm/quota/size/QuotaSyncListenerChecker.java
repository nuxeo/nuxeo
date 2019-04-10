/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.utils.BlobsExtractor;
import org.nuxeo.ecm.quota.AbstractQuotaStatsUpdater;
import org.nuxeo.ecm.quota.QuotaStatsInitialWork;
import org.nuxeo.ecm.quota.QuotaUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * {@link org.nuxeo.ecm.quota.QuotaStatsUpdater} counting space used by Blobs in document. This default implementation
 * does not track the space used by versions, or the space used by non-Blob properties
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.6
 */
public class QuotaSyncListenerChecker extends AbstractQuotaStatsUpdater {

    public static final String DISABLE_QUOTA_CHECK_LISTENER = "disableQuotaListener";

    private static Log log = LogFactory.getLog(QuotaSyncListenerChecker.class);

    @Override
    public void computeInitialStatistics(CoreSession unrestrictedSession, QuotaStatsInitialWork currentWorker) {
        QuotaComputerProcessor processor = new QuotaComputerProcessor();
        String query = "SELECT ecm:uuid FROM Document where ecm:isCheckedInVersion=0 and ecm:isProxy=0 order by dc:created desc";
        IterableQueryResult res = unrestrictedSession.queryAndFetch(query, "NXQL");
        log.debug("Starting initial Quota computation");
        long total = res.size();
        log.debug("Start iteration on " + total + " items");
        try {
            for (Map<String, Serializable> r : res) {
                String uuid = (String) r.get("ecm:uuid");
                // this will force an update if the plugin was installed and
                // then removed
                removeFacet(unrestrictedSession, uuid);
            }
        } finally {
            res.close();
        }
        removeFacet(unrestrictedSession, unrestrictedSession.getRootDocument().getId());
        unrestrictedSession.save();
        try {
            long idx = 0;
            res = unrestrictedSession.queryAndFetch(query, "NXQL");
            for (Map<String, Serializable> r : res) {
                String uuid = (String) r.get("ecm:uuid");
                computeSizeOnDocument(unrestrictedSession, uuid, processor);
                currentWorker.notifyProgress(++idx, total);
            }
        } finally {
            res.close();
        }
    }

    private void removeFacet(CoreSession unrestrictedSession, String uuid) {
        DocumentModel target = unrestrictedSession.getDocument(new IdRef(uuid));
        if (target.hasFacet(QuotaAwareDocument.DOCUMENTS_SIZE_STATISTICS_FACET)) {
            if (log.isTraceEnabled()) {
                log.trace("doc with uuid " + uuid + " already up to date");
            }
            QuotaAware quotaDoc = target.getAdapter(QuotaAware.class);
            quotaDoc.resetInfos(true);
            if (log.isDebugEnabled()) {
                log.debug(target.getPathAsString() + " reset to " + quotaDoc.getQuotaInfo());
            }
            target.removeFacet(QuotaAwareDocument.DOCUMENTS_SIZE_STATISTICS_FACET);
            DocumentModel origTarget = target;
            QuotaUtils.disableListeners(target);
            target = unrestrictedSession.saveDocument(target);
            QuotaUtils.clearContextData(target);
            QuotaUtils.clearContextData(origTarget);
        }
    }

    protected void computeSizeOnDocument(CoreSession unrestrictedSession, String uuid, QuotaComputerProcessor processor)
            {
        IdRef ref = new IdRef(uuid);
        DocumentModel target = unrestrictedSession.getDocument(ref);
        if (log.isTraceEnabled()) {
            log.trace("process Quota initial computation on uuid " + uuid);
        }
        if (unrestrictedSession.exists(ref)) {
            if (log.isTraceEnabled()) {
                log.trace("doc with uuid " + uuid + " started update");
            }
            SizeUpdateEventContext quotaCtx = updateEventToProcessNewDocument(unrestrictedSession, target);
            quotaCtx.setProperty(SizeUpdateEventContext.SOURCE_EVENT_PROPERTY_KEY, DOCUMENT_UPDATE_INITIAL_STATISTICS);
            quotaCtx.getProperties().put(SizeUpdateEventContext._UPDATE_TRASH_SIZE,
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
    protected void handleQuotaExceeded(QuotaExceededException e, Event event) {
        String msg = "Current event " + event.getName() + " would break Quota restriction, rolling back";
        log.info(msg);
        e.addInfo(msg);
        event.markRollBack("Quota Exceeded", e);
    }

    @Override
    protected void processDocumentCreated(CoreSession session, DocumentModel targetDoc, DocumentEventContext docCtx)
            {

        if (targetDoc.isVersion()) {
            // version taken into account by checkout
            // TODO 5.7 version accounting should be different
            return;
        }
        BlobSizeInfo bsi = computeSizeImpact(targetDoc, false);

        // always add the quota facet
        QuotaAware quotaDoc = targetDoc.getAdapter(QuotaAware.class);
        if (quotaDoc == null) {
            log.trace("  add Quota Facet on " + targetDoc.getPathAsString());
            QuotaAwareDocumentFactory.make(targetDoc, true);
        }
        // only process if blobs are present
        if (bsi.getBlobSizeDelta() != 0) {
            checkConstraints(session, targetDoc, targetDoc.getParentRef(), bsi);
            SizeUpdateEventContext asyncEventCtx = new SizeUpdateEventContext(session, docCtx, bsi, DOCUMENT_CREATED);
            sendUpdateEvents(asyncEventCtx);
        }
    }

    @Override
    protected void processDocumentCheckedIn(CoreSession session, DocumentModel doc, DocumentEventContext docCtx)
            {
        // on checkin the versions size is incremented (and also the total)

        BlobSizeInfo bsi = computeSizeImpact(doc, false);
        // only process if blobs are present
        if (bsi.getBlobSize() != 0) {
            // no checkConstraints as total size not impacted
            SizeUpdateEventContext asyncEventCtx = new SizeUpdateEventContext(session, docCtx, bsi, DOCUMENT_CHECKEDIN);
            sendUpdateEvents(asyncEventCtx);
        }
    }

    @Override
    protected void processDocumentCheckedOut(CoreSession session, DocumentModel doc, DocumentEventContext docCtx)
            {
        // on checkout we account in the total for the last version size
        BlobSizeInfo bsi = computeSizeImpact(doc, false);
        // only process if blobs are present
        if (bsi.getBlobSize() != 0) {
            checkConstraints(session, doc, doc.getParentRef(), bsi, true);
            SizeUpdateEventContext asyncEventCtx = new SizeUpdateEventContext(session, docCtx, bsi, DOCUMENT_CHECKEDOUT);
            sendUpdateEvents(asyncEventCtx);
        }
    }

    @Override
    protected void processDocumentUpdated(CoreSession session, DocumentModel doc, DocumentEventContext docCtx)
            {
        // Nothing to do !
    }

    @Override
    protected void processDocumentBeforeUpdate(CoreSession session, DocumentModel targetDoc, DocumentEventContext docCtx)
            {

        BlobSizeInfo bsi = computeSizeImpact(targetDoc, true);
        log.debug("calling processDocumentBeforeUpdate, bsi=" + bsi.toString());
        // only process if Blobs where added or removed
        if (bsi.getBlobSizeDelta() != 0) {
            checkConstraints(session, targetDoc, targetDoc.getParentRef(), bsi);
            SizeUpdateEventContext asyncEventCtx = new SizeUpdateEventContext(session, docCtx, bsi, BEFORE_DOC_UPDATE);
            sendUpdateEvents(asyncEventCtx);
        }
    }

    @Override
    protected void processDocumentCopied(CoreSession session, DocumentModel targetDoc, DocumentEventContext docCtx)
            {
        QuotaAware quotaDoc = targetDoc.getAdapter(QuotaAware.class);
        if (quotaDoc != null) {
            long total = quotaDoc.getTotalSize() - quotaDoc.getVersionsSize() - quotaDoc.getTrashSize();
            BlobSizeInfo bsi = new BlobSizeInfo();
            bsi.blobSize = total;
            bsi.blobSizeDelta = total;
            if (total > 0) {
                // check on parent since Session is not committed for now
                checkConstraints(session, targetDoc, targetDoc.getParentRef(), bsi);
                SizeUpdateEventContext asyncEventCtx = new SizeUpdateEventContext(session, docCtx, bsi,
                        DOCUMENT_CREATED_BY_COPY);
                sendUpdateEvents(asyncEventCtx);
            }
        }
    }

    @Override
    protected void processDocumentMoved(CoreSession session, DocumentModel targetDoc, DocumentModel sourceParent,
            DocumentEventContext docCtx) {

        if (docCtx.getProperties().get(CoreEventConstants.DESTINATION_REF)
                .equals(sourceParent.getRef())) {
            log.debug(targetDoc.getPathAsString() + "(" + targetDoc.getId() + ") - document is just being renamed, skipping");
            return;
        }
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
            SizeUpdateEventContext asyncEventCtx = new SizeUpdateEventContext(session, docCtx, bsi, DOCUMENT_MOVED);
            long versSize = quotaDoc.getVersionsSize();
            asyncEventCtx.setVersionsSize(versSize);
            sendUpdateEvents(asyncEventCtx);

            // also need to trigger update on source tree
            BlobSizeInfo bsiRemove = new BlobSizeInfo();
            bsiRemove.blobSize = total;
            bsiRemove.blobSizeDelta = -total;

            asyncEventCtx = new SizeUpdateEventContext(session, docCtx, sourceParent, bsiRemove, DOCUMENT_MOVED);
            versSize = -quotaDoc.getVersionsSize();
            asyncEventCtx.setVersionsSize(versSize);
            List<String> sourceParentUUIDs = getParentUUIDS(session, sourceParent);
            sourceParentUUIDs.add(0, sourceParent.getId());
            asyncEventCtx.setParentUUIds(sourceParentUUIDs);
            sendUpdateEvents(asyncEventCtx);
        }

    }

    @Override
    protected void processDocumentAboutToBeRemoved(CoreSession session, DocumentModel targetDoc,
            DocumentEventContext docCtx) {

        if (targetDoc.isVersion()) {
            // for versions we need to decrement the live doc + it's parents
            List<String> parentUUIDs = new ArrayList<String>();
            parentUUIDs.add(targetDoc.getSourceId());
            parentUUIDs.addAll(getParentUUIDS(session, new IdRef(targetDoc.getSourceId())));

            // We only have to decrement the inner size of this doc
            QuotaAware quotaDoc = targetDoc.getAdapter(QuotaAware.class);
            // we do not write the right quota on the version, so we need to recompute it instead of quotaDoc#getInnerSize
            BlobSizeInfo bsi = computeSizeImpact(targetDoc, false);
            SizeUpdateEventContext asyncEventCtx = new SizeUpdateEventContext(session, docCtx, bsi.getBlobSize(),
                    ABOUT_TO_REMOVE_VERSION);
            asyncEventCtx.setParentUUIds(parentUUIDs);
            sendUpdateEvents(asyncEventCtx);
            return;
        }

        QuotaAware quotaDoc = targetDoc.getAdapter(QuotaAware.class);
        long total = 0;
        long versSize = 0;
        if (quotaDoc == null) {
            // the document could have been just created and the previous
            // computation
            // hasn't finished yet, see NXP-13665
            BlobSizeInfo bsi = computeSizeImpact(targetDoc, false);
            total = bsi.getBlobSize();
            log.debug("Document " + targetDoc.getId() + " doesn't have the facet quotaDoc. Compute impacted size:"
                    + total);
        }
        if (quotaDoc != null) {
            total = quotaDoc.getTotalSize();
            versSize = -quotaDoc.getVersionsSize();
            log.debug("Found facet quotaDoc on document  " + targetDoc.getId()
                    + ".Notifying QuotaComputerProcessor with total size: " + total + " and versions size: " + versSize);
        }
        if (total > 0) {
            List<String> parentUUIDs = getParentUUIDS(session, targetDoc);
            SizeUpdateEventContext asyncEventCtx = new SizeUpdateEventContext(session, docCtx, total, ABOUT_TO_REMOVE);
            // remove size for all its versions from sizeVersions on parents
            if (versSize != 0) {
                asyncEventCtx.setVersionsSize(versSize);
            }
            asyncEventCtx.setParentUUIds(parentUUIDs);
            asyncEventCtx.getProperties().put(SizeUpdateEventContext._UPDATE_TRASH_SIZE,
                    DELETED_STATE.equals(targetDoc.getCurrentLifeCycleState()));
            sendUpdateEvents(asyncEventCtx);
        }
    }

    @Override
    protected boolean needToProcessEventOnDocument(Event event, DocumentModel targetDoc) {

        if (targetDoc == null) {
            return false;
        }
        if (targetDoc.isProxy()) {
            log.debug("Escape from listener: not precessing proxies");
            return false;
        }

        Boolean block = (Boolean) targetDoc.getContextData(DISABLE_QUOTA_CHECK_LISTENER);
        if (Boolean.TRUE.equals(block)) {
            log.debug("Escape from listener to avoid reentrancy");
            // ignore the event - we are blocked by the caller
            // used to avoid reentrancy when the async event handler
            // do update the docs to set the new size !
            return false;
        }
        return true;
    }

    protected void sendUpdateEvents(SizeUpdateEventContext eventCtx) {

        Event quotaUpdateEvent = eventCtx.newQuotaUpdateEvent();
        log.debug("prepared event on target tree with context " + eventCtx.toString());
        EventService es = Framework.getLocalService(EventService.class);
        es.fireEvent(quotaUpdateEvent);
    }

    protected List<String> getParentUUIDS(CoreSession unrestrictedSession, final DocumentRef docRef)
            {

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

    protected List<String> getParentUUIDS(CoreSession unrestrictedSession, final DocumentModel doc)
            {
        return getParentUUIDS(unrestrictedSession, doc.getRef());
    }

    protected void checkConstraints(CoreSession unrestrictedSession, final DocumentModel doc,
            final DocumentRef parentRef, final BlobSizeInfo bsi) {
        checkConstraints(unrestrictedSession, doc, parentRef, bsi, false);
    }

    protected void checkConstraints(CoreSession unrestrictedSession, final DocumentModel doc,
            final DocumentRef parentRef, final BlobSizeInfo bsi, final boolean checkWithTotalSize)
            {
        if (parentRef == null) {
            return;
        }

        long addition = bsi.blobSizeDelta;
        if (checkWithTotalSize) {
            addition = bsi.getBlobSize();
        }

        if (addition <= 0) {
            return;
        }
        List<DocumentModel> parents = unrestrictedSession.getParentDocuments(parentRef);
        DocumentModel parentDoc = unrestrictedSession.getDocument(parentRef);
        if (!parents.contains(parentDoc)) {
            parents.add(parentDoc);
        }
        for (DocumentModel parent : parents) {
            log.debug("processing " + parent.getId() + " " + parent.getPathAsString());
            QuotaAware qap = parent.getAdapter(QuotaAware.class);
            // when enabling quota on user workspaces, the max size set on
            // the
            // UserWorkspacesRoot is the max size set on every user workspace
            if (qap != null && !"UserWorkspacesRoot".equals(parent.getType()) && qap.getMaxQuota() > 0) {
                Long newTotalSize = new Long(qap.getTotalSize() + addition);
                try {
                    if (qap.totalSizeCacheExists()) {
                        Long oldTotalSize = qap.getTotalSizeCache();
                        if (oldTotalSize == null) {
                            newTotalSize = new Long(qap.getTotalSize() + addition);
                            log.debug("to create cache entry to create: " + qap.getDoc().getId() + " total: " + newTotalSize);
                        } else {
                            newTotalSize = new Long(oldTotalSize + addition);
                            log.debug("cache entry to update: " + qap.getDoc().getId() + " total: " + newTotalSize);
                        }
                        qap.putTotalSizeCache(newTotalSize);
                        if (log.isDebugEnabled()) {
                            log.debug("cache vs. DB: " + newTotalSize + " # " + (qap.getTotalSize() + addition));
                        }
                    }
                } catch (IOException e) {
                    log.error(e.getMessage() + ": unable to use cache " + QuotaAware.QUOTA_TOTALSIZE_CACHE_NAME + ", fallback to basic mechanism");
                    newTotalSize = new Long(qap.getTotalSize() + addition);
                }
                if (newTotalSize > qap.getMaxQuota()) {
                    log.info("Raising Quota Exception on " + doc.getPathAsString());
                    try {
                        qap.invalidateTotalSizeCache();
                    } catch (IOException e) {
                        log.error(e.getMessage() + ": unable to invalidate cache " + QuotaAware.QUOTA_TOTALSIZE_CACHE_NAME + " for " + qap.getDoc().getId());
                    }
                    throw new QuotaExceededException(parent, doc, qap.getMaxQuota());
                }
            }
        }
    }

    protected BlobSizeInfo computeSizeImpact(DocumentModel doc, boolean onlyIfBlobHasChanged) {

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
     */
    protected List<Blob> getBlobs(DocumentModel doc, boolean onlyIfBlobHasChanged) {
        QuotaSizeService sizeService = Framework.getService(QuotaSizeService.class);
        Set<String> excludedPathSet = new HashSet<String>(sizeService.getExcludedPathList());

        BlobsExtractor extractor = new BlobsExtractor();
        extractor.setExtractorProperties(null, new HashSet<String>(excludedPathSet), true);

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
                String schema = blobProperty.getParent().getSchema().getName();
                String propName = blobProperty.getName();

                log.debug(String.format("Using [%s:%s] for quota blob computation (size : %d)", schema, propName,
                        blob.getLength()));
                result.add(blob);
            }
        }
        return result;
    }

    @Override
    protected void processDocumentTrashOp(CoreSession session, DocumentModel doc, DocumentEventContext docCtx)
            {
        String transition = (String) docCtx.getProperties().get(TRANSTION_EVENT_OPTION_TRANSITION);
        if (transition != null && (!(DELETE_TRANSITION.equals(transition) || UNDELETE_TRANSITION.equals(transition)))) {
            return;
        }

        QuotaAware quotaDoc = doc.getAdapter(QuotaAware.class);
        if (quotaDoc != null) {
            long absSize = quotaDoc.getInnerSize();
            if (log.isDebugEnabled()) {
                if (quotaDoc.getDoc().isFolder()) {
                    log.debug(quotaDoc.getDoc().getPathAsString() + " is a folder, just inner size (" + absSize + ") taken into account for trash size");
                }
            }
            long total = (DELETE_TRANSITION.equals(transition) == true ? absSize : -absSize);
            BlobSizeInfo bsi = new BlobSizeInfo();
            bsi.blobSize = total;
            bsi.blobSizeDelta = total;
            if (absSize > 0) {
                // check constrains not needed, since the documents stays in
                // the same folder
                // TODO move this check to QuotaSyncListenerChecker

                SizeUpdateEventContext asyncEventCtx = new SizeUpdateEventContext(session, docCtx, bsi, transition);
                sendUpdateEvents(asyncEventCtx);
            }
        }
    }

    @Override
    protected void processDocumentBeforeRestore(CoreSession session, DocumentModel targetDoc,
            DocumentEventContext docCtx) {
        QuotaAware quotaDoc = targetDoc.getAdapter(QuotaAware.class);
        if (quotaDoc != null) {
            long total = quotaDoc.getTotalSize();
            if (total > 0) {
                List<String> parentUUIDs = getParentUUIDS(session, targetDoc);
                SizeUpdateEventContext asyncEventCtx = new SizeUpdateEventContext(session, docCtx, total,
                        ABOUT_TO_REMOVE);
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
    protected void processDocumentRestored(CoreSession session, DocumentModel targetDoc, DocumentEventContext docCtx)
            {
        QuotaAware quotaDoc = targetDoc.getAdapter(QuotaAware.class);
        if (quotaDoc == null) {
            log.debug("  add Quota Facet on " + targetDoc.getPathAsString());
            quotaDoc = QuotaAwareDocumentFactory.make(targetDoc, true);
        }
        quotaDoc.resetInfos(true);
        sendUpdateEvents(updateEventToProcessNewDocument(session, targetDoc));
    }

    private SizeUpdateEventContext updateEventToProcessNewDocument(CoreSession unrestrictedSession, DocumentModel target)
            {
        BlobSizeInfo bsi = computeSizeImpact(target, false);
        SizeUpdateEventContext quotaCtx = null;

        // process versions if any ; document is not in trash
        List<DocumentModel> versions = unrestrictedSession.getVersions(target.getRef());
        if (versions.isEmpty() && !DELETED_STATE.equals(target.getCurrentLifeCycleState())) {
            quotaCtx = new SizeUpdateEventContext(unrestrictedSession, bsi, DOCUMENT_CREATED, target);

        } else {
            long versionsSize = 0;
            for (DocumentModel documentModel : versions) {
                versionsSize += computeSizeImpact(documentModel, false).blobSize;;
            }

            quotaCtx = new SizeUpdateEventContext(unrestrictedSession, bsi, DOCUMENT_UPDATE_INITIAL_STATISTICS, target);
            quotaCtx.setVersionsSize(versionsSize);
        }
        return quotaCtx;
    }
}
