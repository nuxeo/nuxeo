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
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.quota.size;

import static org.nuxeo.ecm.core.api.versioning.VersioningService.VERSIONING_OPTION;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.utils.BlobsExtractor;
import org.nuxeo.ecm.quota.AbstractQuotaStatsUpdater;
import org.nuxeo.ecm.quota.QuotaStatsInitialWork;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * {@link org.nuxeo.ecm.quota.QuotaStatsUpdater} counting space used by Blobs in document. This implementation does not
 * track the space used by non-Blob properties.
 *
 * @since 8.3
 */
public class DocumentsSizeUpdater extends AbstractQuotaStatsUpdater {
    private static Logger log = LogManager.getLogger(DocumentsSizeUpdater.class);

    public static final String DISABLE_QUOTA_CHECK_LISTENER = "disableQuotaListener";

    public static final String USER_WORKSPACES_ROOT = "UserWorkspacesRoot";

    /** @since 11.1 */
    public static final String CLEAR_SCROLL_SIZE_PROP = "nuxeo.quota.clear.scroll.size";

    /** @since 11.1 */
    public static final int DEFAULT_CLEAR_SCROLL_SIZE = 500;

    /** @since 11.1 */
    public static final String CLEAR_SCROLL_KEEP_ALIVE_PROP = "nuxeo.quota.clear.scroll.keepAliveSeconds";

    /** @since 11.1 */
    public static final int DEFAULT_CLEAR_SCROLL_KEEP_ALIVE = 60;

    /** @since 11.1 */
    public static final String INIT_SCROLL_SIZE_PROP = "nuxeo.quota.init.scroll.size";

    /** @since 11.1 */
    public static final int DEFAULT_INIT_SCROLL_SIZE = 250;

    /** @since 11.1 */
    public static final String INIT_SCROLL_KEEP_ALIVE_PROP = "nuxeo.quota.init.scroll.keepAliveSeconds";

    /** @since 11.1 */
    public static final int DEFAULT_INIT_SCROLL_KEEP_ALIVE = 120;

    @Override
    public void computeInitialStatistics(CoreSession session, QuotaStatsInitialWork currentWorker, String path) {
        log.debug("Starting initial Quota computation for path: {}", path);
        String query = "SELECT ecm:uuid FROM Document WHERE ecm:isVersion = 0 AND ecm:isProxy = 0";
        DocumentModel root;
        if (path == null) {
            root = session.getRootDocument();
        } else {
            root = session.getDocument(new PathRef(path));
            query += " AND ecm:path STARTSWITH " + NXQL.escapeString(path);
        }
        // get scroll configuration parameters
        ConfigurationService confService = Framework.getService(ConfigurationService.class);
        int clearScrollSize = confService.getInteger(CLEAR_SCROLL_SIZE_PROP, DEFAULT_CLEAR_SCROLL_SIZE);
        int clearScrollKeepAlive = confService.getInteger(CLEAR_SCROLL_KEEP_ALIVE_PROP,
                DEFAULT_CLEAR_SCROLL_KEEP_ALIVE);
        int initScrollSize = confService.getInteger(INIT_SCROLL_SIZE_PROP, DEFAULT_INIT_SCROLL_SIZE);
        int initScrollKeepAlive = confService.getInteger(INIT_SCROLL_KEEP_ALIVE_PROP, DEFAULT_INIT_SCROLL_KEEP_ALIVE);

        // reset on all documents
        // this will force an update if the quota addon was installed and then removed
        log.debug("Start scrolling to clear quotas");
        long clearCount = scrollAndDo(session, query, clearScrollSize, clearScrollKeepAlive,
                (uuid, idx) -> clearQuotas(session, uuid));
        log.debug("End scrolling to clear quotas, documentCount={}", clearCount);
        clearQuotas(session, root.getId());
        session.save();

        // recompute quota on each doc
        log.debug("Start scrolling to init quotas");
        long initCount = scrollAndDo(session, query, initScrollSize, initScrollKeepAlive, (uuid, idx) -> {
            DocumentModel doc = session.getDocument(new IdRef(uuid));
            log.trace("process Quota initial computation on uuid={}", doc::getId);
            log.trace("doc with uuid {} started update", doc::getId);
            initDocument(session, doc);
            log.trace("doc with uuid {} update completed", doc::getId);
            currentWorker.notifyProgress(idx, clearCount);
        });
        log.debug("End scrolling to init quotas, documentCount={}", initCount);

        // if recomputing only for descendants of a given path, recompute ancestors from their direct children
        if (path != null) {
            DocumentModel doc = root;
            do {
                doc = session.getDocument(doc.getParentRef());
                initDocumentFromChildren(doc);
            } while (!doc.getPathAsString().equals("/"));
        }
    }

    protected long scrollAndDo(CoreSession session, String query, int scrollSize, int scrollKeepAlive,
            BiConsumer<String, Long> consumer) {
        long count = 0;
        ScrollResult<String> scroll = session.scroll(query, scrollSize, scrollKeepAlive);
        while (scroll.hasResults()) {
            for (String uuid : scroll.getResults()) {
                consumer.accept(uuid, ++count);
            }
            // commit current scroll
            session.save();
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
            // next scroll
            scroll = session.scroll(scroll.getScrollId());
        }
        return count;
    }

    protected void clearQuotas(CoreSession session, String docID) {
        DocumentModel doc = session.getDocument(new IdRef(docID));
        QuotaAware quotaDoc = doc.getAdapter(QuotaAware.class);
        if (quotaDoc != null) {
            quotaDoc.clearInfos();
            quotaDoc.save();
        }
    }

    protected void initDocument(CoreSession session, DocumentModel doc) {
        boolean isDeleted = doc.isTrashed();
        long size = getBlobsSize(doc);
        long versionsSize = getVersionsSize(session, doc);
        updateDocumentAndAncestors(session, doc, size, size + versionsSize, isDeleted ? size : 0, versionsSize);
    }

    protected void initDocumentFromChildren(DocumentModel doc) {
        CoreSession session = doc.getCoreSession();
        boolean isDeleted = doc.isTrashed();
        long innerSize = getBlobsSize(doc);
        long versionsSize = getVersionsSize(session, doc);
        long totalSize = innerSize + versionsSize;
        long trashSize = isDeleted ? innerSize : 0;
        for (DocumentModel child : session.getChildren(doc.getRef())) {
            QuotaAware quotaDoc = child.getAdapter(QuotaAware.class);
            if (quotaDoc == null) {
                continue;
            }
            totalSize += quotaDoc.getTotalSize();
            trashSize += quotaDoc.getTrashSize();
            versionsSize += quotaDoc.getVersionsSize();
        }
        QuotaAware quotaDoc = doc.getAdapter(QuotaAware.class);
        if (quotaDoc == null) {
            quotaDoc = QuotaAwareDocumentFactory.make(doc);
        }
        quotaDoc.setAll(innerSize, totalSize, trashSize, versionsSize);
        quotaDoc.save();
    }

    @Override
    protected void handleQuotaExceeded(QuotaExceededException e, Event event) {
        String msg = "Current event " + event.getName() + " would break Quota restriction, rolling back";
        log.info(msg);
        e.addInfo(msg);
        event.markRollBack("Quota Exceeded", e);
    }

    @Override
    protected void processDocumentCreated(CoreSession session, DocumentModel doc) {
        if (doc.isVersion()) {
            // version taken into account by checkout
            return;
        }
        QuotaAware quotaDoc = doc.getAdapter(QuotaAware.class);
        if (quotaDoc == null) {
            // always add the quota facet
            quotaDoc = QuotaAwareDocumentFactory.make(doc);
            quotaDoc.save();
        }
        long size = getBlobsSize(doc);
        checkQuota(session, doc, size);
        updateDocumentAndAncestors(session, doc, size, size, 0, 0);
    }

    @Override
    protected void processDocumentCheckedIn(CoreSession session, DocumentModel doc) {
        // nothing to do, we do things at aboutToCheckIn time
    }

    @Override
    protected void processDocumentBeforeCheckedIn(CoreSession session, DocumentModel doc) {
        // on checkin the versions size is incremented (and also the total)
        long size = getBlobsSize(doc);
        checkQuota(session, doc, size);
        // detect if we're currently saving the document or just checking it in
        boolean allowSave = doc.getContextData().containsKey(VERSIONING_OPTION);
        updateDocument(doc, 0, size, 0, size, allowSave);
        updateAncestors(session, doc, size, 0, size);
    }

    @Override
    protected void processDocumentCheckedOut(CoreSession session, DocumentModel doc) {
        // nothing to do, checking out the document doesn't change size
    }

    @Override
    protected void processDocumentBeforeCheckedOut(CoreSession session, DocumentModel doc) {
        // nothing to do, checking out the document doesn't change size
    }

    @Override
    protected void processDocumentUpdated(CoreSession session, DocumentModel doc) {
        // nothing to do, we do things at beforeDocumentModification time
    }

    @Override
    protected void processDocumentBeforeUpdate(CoreSession session, DocumentModel doc) {
        QuotaAware quotaDoc = doc.getAdapter(QuotaAware.class);
        long oldSize = quotaDoc == null ? 0 : quotaDoc.getInnerSize();
        long delta = getBlobsSize(doc) - oldSize;
        checkQuota(session, doc, delta);
        updateDocument(doc, delta, delta, 0, 0, false); // DO NOT SAVE as this is a "before" event
        updateAncestors(session, doc, delta, 0, 0);
    }

    @Override
    protected void processDocumentCopied(CoreSession session, DocumentModel doc) {
        QuotaAware quotaDoc = doc.getAdapter(QuotaAware.class);
        if (quotaDoc == null) {
            return;
        }
        long size = quotaDoc.getTotalSize() - quotaDoc.getVersionsSize() - quotaDoc.getTrashSize();
        checkQuota(session, doc, size);
        if (!doc.isFolder() && size > 0) {
            // when we copy some doc that is not folderish, we don't
            // copy the versions so we can't rely on the copied quotaDocInfo
            quotaDoc.resetInfos();
            quotaDoc.save();
            updateDocument(doc, size, size, 0, 0);
        }
        updateAncestors(session, doc, size, 0, 0);
    }

    @Override
    protected void processDocumentMoved(CoreSession session, DocumentModel doc, DocumentModel sourceParent) {
        QuotaAware quotaDoc = doc.getAdapter(QuotaAware.class);
        long size = quotaDoc == null ? 0 : quotaDoc.getTotalSize();
        checkQuota(session, doc, size);
        long versionsSize = quotaDoc == null ? 0 : quotaDoc.getVersionsSize();
        // add on new ancestors
        updateAncestors(session, doc, size, 0, versionsSize);
        // remove from old ancestors
        if (sourceParent != null) {
            updateDocumentAndAncestors(session, sourceParent, 0, -size, 0, -versionsSize);
        }
    }

    @Override
    protected void processDocumentAboutToBeRemoved(CoreSession session, DocumentModel doc) {
        if (doc.isVersion()) {
            // for versions we need to decrement the live doc + its parents
            // We only have to decrement the inner size of this doc
            // we do not write the right quota on the version, so we need to recompute it instead of
            // quotaDoc#getInnerSize
            long size = getBlobsSize(doc);
            String sourceId = doc.getSourceId();
            if (size > 0 && sourceId != null) {
                DocumentModel liveDoc = session.getDocument(new IdRef(sourceId));
                updateDocumentAndAncestors(session, liveDoc, 0, -size, 0, -size);
            }
            return;
        }

        QuotaAware quotaDoc = doc.getAdapter(QuotaAware.class);
        long size;
        long versionsSize;
        if (quotaDoc == null) {
            // the document could have been just created and the previous computation
            // hasn't finished yet, see NXP-13665
            size = getBlobsSize(doc);
            versionsSize = 0;
            log.trace("Document {} doesn't have the facet quotaDoc. Compute impacted size: {}", doc.getId(), size);
        } else {
            size = quotaDoc.getTotalSize();
            versionsSize = quotaDoc.getVersionsSize();
            log.trace("Found facet quotaDoc on document  {}. Total size: {} and versions size: {}", doc.getId(), size,
                    versionsSize);
        }
        // remove size for all its versions from sizeVersions on parents
        boolean isDeleted = doc.isTrashed();
        // when permanently deleting the doc clean the trash if the doc is in the trash
        // and all archived versions size
        log.trace("Processing document about to be removed on parents. Total: {}, trash size: {}, versions size: ",
                size, isDeleted ? size : 0, versionsSize);
        long deltaTrash = isDeleted ? versionsSize - size : 0;
        updateAncestors(session, doc, -size, deltaTrash, -versionsSize);
    }

    @Override
    protected void processDocumentTrashOp(CoreSession session, DocumentModel doc, boolean isTrashed) {
        QuotaAware quotaDoc = doc.getAdapter(QuotaAware.class);
        if (quotaDoc == null) {
            return;
        }
        long size = quotaDoc.getInnerSize();
        if (log.isTraceEnabled()) {
            if (quotaDoc.getDoc().isFolder()) {
                log.trace(quotaDoc.getDoc().getPathAsString() + " is a folder, just inner size (" + size
                        + ") taken into account for trash size");
            }
        }
        long delta = isTrashed ? size : -size;
        // constraints check not needed, since the documents stays in the same folder
        updateDocumentAndAncestors(session, doc, 0, 0, delta, 0);
    }

    @Override
    protected void processDocumentBeforeRestore(CoreSession session, DocumentModel doc) {
        QuotaAware quotaDoc = doc.getAdapter(QuotaAware.class);
        if (quotaDoc == null) {
            return;
        }
        // remove versions size on parents since they will be recalculated on restore
        long size = quotaDoc.getTotalSize();
        long versionsSize = quotaDoc.getVersionsSize();
        updateAncestors(session, doc, -size, 0, -versionsSize);
    }

    @Override
    protected void processDocumentRestored(CoreSession session, DocumentModel doc) {
        QuotaAware quotaDoc = QuotaAwareDocumentFactory.make(doc);
        quotaDoc.resetInfos();
        quotaDoc.save();
        long size = getBlobsSize(doc);
        long versionsSize = getVersionsSize(session, doc);
        updateDocumentAndAncestors(session, doc, size, size + versionsSize, 0, versionsSize);
    }

    @Override
    protected boolean needToProcessEventOnDocument(Event event, DocumentModel doc) {
        if (doc == null) {
            return false;
        }
        if (doc.isProxy()) {
            return false;
        }
        // avoid reentrancy
        return !Boolean.TRUE.equals(doc.getContextData(DISABLE_QUOTA_CHECK_LISTENER));
    }

    /** Checks the size delta against the maximum quota specified for this document or an ancestor. */
    protected void checkQuota(CoreSession session, DocumentModel doc, long delta) {
        if (delta <= 0) {
            return;
        }
        for (DocumentModel parent : getAncestors(session, doc)) {
            log.trace("processing {} {}", parent::getId, parent::getPathAsString);
            QuotaAware quotaDoc = parent.getAdapter(QuotaAware.class);
            // when enabling quota on user workspaces, the max size set on the
            // UserWorkspacesRoot is the max size set on every user workspace
            if (quotaDoc == null || quotaDoc.getMaxQuota() <= 0 || USER_WORKSPACES_ROOT.equals(parent.getType())) {
                continue;
            }
            if (quotaDoc.getTotalSize() + delta > quotaDoc.getMaxQuota()) {
                log.info("Raising Quota Exception on {} ({})", doc::getId, doc::getPathAsString);
                throw new QuotaExceededException(parent, doc, quotaDoc.getMaxQuota());
            }
        }
    }

    /** Gets the sum of all blobs sizes for all the document's versions. */
    protected long getVersionsSize(CoreSession session, DocumentModel doc) {
        long versionsSize = 0;
        for (DocumentModel version : session.getVersions(doc.getRef())) {
            versionsSize += getBlobsSize(version);
        }
        return versionsSize;
    }

    /** Gets the sum of all blobs sizes for this document. */
    protected long getBlobsSize(DocumentModel doc) {
        long size = 0;
        for (Blob blob : getAllBlobs(doc)) {
            size += blob.getLength();
        }
        return size;
    }

    /** Returns the list of blobs for this document. */
    protected List<Blob> getAllBlobs(DocumentModel doc) {
        QuotaSizeService sizeService = Framework.getService(QuotaSizeService.class);
        Collection<String> excludedPaths = sizeService.getExcludedPathList();
        BlobsExtractor extractor = new BlobsExtractor();
        extractor.setExtractorProperties(null, new HashSet<>(excludedPaths), true);
        return extractor.getBlobs(doc);
    }

    protected void updateDocument(DocumentModel doc, long deltaInner, long deltaTotal, long deltaTrash,
            long deltaVersions) {
        updateDocument(doc, deltaInner, deltaTotal, deltaTrash, deltaVersions, true);
    }

    protected void updateDocument(DocumentModel doc, long deltaInner, long deltaTotal, long deltaTrash,
            long deltaVersions, boolean allowSave) {
        QuotaAware quotaDoc = doc.getAdapter(QuotaAware.class);
        boolean save = false;
        if (quotaDoc == null) {
            log.trace("   add quota on: {} ({})", doc::getId, doc::getPathAsString);
            quotaDoc = QuotaAwareDocumentFactory.make(doc);
            save = true;
        } else {
            log.trace("   update quota on: {} ({}) ({})", doc::getId, doc::getPathAsString, quotaDoc::getQuotaInfo);
        }
        if (deltaInner != 0) {
            quotaDoc.addInnerSize(deltaInner);
            save = true;
        }
        if (deltaTotal != 0) {
            quotaDoc.addTotalSize(deltaTotal);
            save = true;
        }
        if (deltaTrash != 0) {
            quotaDoc.addTrashSize(deltaTrash);
            save = true;
        }
        if (deltaVersions != 0) {
            quotaDoc.addVersionsSize(deltaVersions);
            save = true;
        }
        if (save && allowSave) {
            quotaDoc.save();
        }
        log.trace("     ==> {} ({}) ({})", doc::getId, doc::getPathAsString, quotaDoc::getQuotaInfo);
    }

    protected void updateAncestors(CoreSession session, DocumentModel doc, long deltaTotal, long deltaTrash,
            long deltaVersions) {
        if (deltaTotal == 0 && deltaTrash == 0 && deltaVersions == 0) {
            // avoids computing ancestors if there's no update to do
            return;
        }
        List<DocumentModel> ancestors = getAncestors(session, doc);
        for (DocumentModel ancestor : ancestors) {
            updateDocument(ancestor, 0, deltaTotal, deltaTrash, deltaVersions);
        }
    }

    protected void updateDocumentAndAncestors(CoreSession session, DocumentModel doc, long deltaInner, long deltaTotal,
            long deltaTrash, long deltaVersions) {
        updateDocument(doc, deltaInner, deltaTotal, deltaTrash, deltaVersions);
        updateAncestors(session, doc, deltaTotal, deltaTrash, deltaVersions);
    }

}
