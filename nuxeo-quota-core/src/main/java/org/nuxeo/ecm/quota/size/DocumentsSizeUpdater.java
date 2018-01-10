/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.core.api.LifeCycleConstants.DELETED_STATE;
import static org.nuxeo.ecm.core.api.LifeCycleConstants.DELETE_TRANSITION;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.utils.BlobsExtractor;
import org.nuxeo.ecm.quota.AbstractQuotaStatsUpdater;
import org.nuxeo.ecm.quota.QuotaStatsInitialWork;
import org.nuxeo.runtime.api.Framework;

/**
 * {@link org.nuxeo.ecm.quota.QuotaStatsUpdater} counting space used by Blobs in document. This implementation does not
 * track the space used by non-Blob properties.
 *
 * @since 8.3
 */
public class DocumentsSizeUpdater extends AbstractQuotaStatsUpdater {

    private static Log log = LogFactory.getLog(DocumentsSizeUpdater.class);

    public static final String DISABLE_QUOTA_CHECK_LISTENER = "disableQuotaListener";

    public static final String USER_WORKSPACES_ROOT = "UserWorkspacesRoot";

    @Override
    public void computeInitialStatistics(CoreSession session, QuotaStatsInitialWork currentWorker, String path) {
        log.debug("Starting initial Quota computation for path: " + path);
        String query = "SELECT ecm:uuid FROM Document WHERE ecm:isCheckedInVersion = 0 AND ecm:isProxy = 0";
        DocumentModel root;
        if (path == null) {
            root = session.getRootDocument();
        } else {
            root = session.getDocument(new PathRef(path));
            query += " AND ecm:path STARTSWITH " + NXQL.escapeString(path);
        }
        // reset on all documents
        // this will force an update if the quota addon was installed and then removed
        long count = 0;
        IterableQueryResult res = session.queryAndFetch(query, "NXQL");
        try {
            count = res.size();
            log.debug("Start iteration on " + count + " items");
            for (Map<String, Serializable> r : res) {
                String uuid = (String) r.get("ecm:uuid");
                clearQuotas(session, uuid);
            }
        } finally {
            res.close();
        }
        clearQuotas(session, root.getId());
        session.save();

        // recompute quota on each doc
        res = session.queryAndFetch(query, "NXQL");
        try {
            long idx = 0;
            for (Map<String, Serializable> r : res) {
                String uuid = (String) r.get("ecm:uuid");
                DocumentModel doc = session.getDocument(new IdRef(uuid));
                if (log.isTraceEnabled()) {
                    log.trace("process Quota initial computation on uuid " + doc.getId());
                    log.trace("doc with uuid " + doc.getId() + " started update");
                }
                initDocument(session, doc);
                if (log.isTraceEnabled()) {
                    log.trace("doc with uuid " + doc.getId() + " update completed");
                }
                currentWorker.notifyProgress(++idx, count);
            }
        } finally {
            res.close();
        }

        // if recomputing only for descendants of a given path, recompute ancestors from their direct children
        if (path != null) {
            DocumentModel doc = root;
            do {
                doc = session.getDocument(doc.getParentRef());
                initDocumentFromChildren(doc);
            } while (!doc.getPathAsString().equals("/"));
        }
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
        boolean isDeleted = DELETED_STATE.equals(doc.getCurrentLifeCycleState());
        long size = getBlobsSize(doc);
        long versionsSize = getVersionsSize(session, doc);
        updateDocumentAndAncestors(session, doc, size, size + versionsSize, isDeleted ? size : 0, versionsSize);
    }

    protected void initDocumentFromChildren(DocumentModel doc) {
        CoreSession session = doc.getCoreSession();
        boolean isDeleted = DELETED_STATE.equals(doc.getCurrentLifeCycleState());
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
        // on checkin the versions size is incremented (and also the total)
        long size = getBlobsSize(doc);
        // no constraints check as total size is not impacted
        updateDocumentAndAncestors(session, doc, 0, size, 0, size);
    }

    @Override
    protected void processDocumentCheckedOut(CoreSession session, DocumentModel doc) {
        // on checkout we account in the total for the last version size
        long size = getBlobsSize(doc);
        checkQuota(session, doc, size);
        // all quota computation handled on checkin
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
            if (log.isTraceEnabled()) {
                log.trace(
                        "Document " + doc.getId() + " doesn't have the facet quotaDoc. Compute impacted size:" + size);
            }
        } else {
            size = quotaDoc.getTotalSize();
            versionsSize = quotaDoc.getVersionsSize();
            if (log.isTraceEnabled()) {
                log.trace("Found facet quotaDoc on document  " + doc.getId() + ". Total size: " + size
                        + " and versions size: " + versionsSize);
            }
        }
        // remove size for all its versions from sizeVersions on parents
        boolean isDeleted = DELETED_STATE.equals(doc.getCurrentLifeCycleState());
        // when permanently deleting the doc clean the trash if the doc is in trash
        // and all archived versions size
        if (log.isTraceEnabled()) {
            log.trace("Processing document about to be removed on parents. Total: " + size + " , trash size: "
                    + (isDeleted ? size : 0) + " , versions size: " + versionsSize);
        }
        long deltaTrash = isDeleted ? versionsSize - size : 0;
        updateAncestors(session, doc, -size, deltaTrash, -versionsSize);
    }

    @Override
    protected void processDocumentTrashOp(CoreSession session, DocumentModel doc, String transition) {
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
        boolean isDelete = DELETE_TRANSITION.equals(transition); // otherwise undelete
        long delta = isDelete ? size : -size;
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
        if (Boolean.TRUE.equals(doc.getContextData(DISABLE_QUOTA_CHECK_LISTENER))) {
            // avoid reentrancy
            return false;
        }
        return true;
    }

    /** Checks the size delta against the maximum quota specified for this document or an ancestor. */
    protected void checkQuota(CoreSession session, DocumentModel doc, long delta) {
        if (delta <= 0) {
            return;
        }
        for (DocumentModel parent : getAncestors(session, doc)) {
            if (log.isTraceEnabled()) {
                log.trace("processing " + parent.getId() + " " + parent.getPathAsString());
            }
            QuotaAware quotaDoc = parent.getAdapter(QuotaAware.class);
            // when enabling quota on user workspaces, the max size set on the
            // UserWorkspacesRoot is the max size set on every user workspace
            if (quotaDoc == null || quotaDoc.getMaxQuota() <= 0 || USER_WORKSPACES_ROOT.equals(parent.getType())) {
                continue;
            }
            if (quotaDoc.getTotalSize() + delta > quotaDoc.getMaxQuota()) {
                log.info("Raising Quota Exception on " + doc.getId() + " (" + doc.getPathAsString() + ")");
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
            if (log.isTraceEnabled()) {
                log.trace("   add quota on: " + doc.getId() + " (" + doc.getPathAsString() + ")");
            }
            quotaDoc = QuotaAwareDocumentFactory.make(doc);
            save = true;
        } else {
            if (log.isTraceEnabled()) {
                log.trace("   update quota on: " + doc.getId() + " (" + doc.getPathAsString() + ") (" + quotaDoc.getQuotaInfo() + ")");
            }
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
        if (log.isTraceEnabled()) {
            log.trace("     ==> " + doc.getId() + " (" + doc.getPathAsString() + ") (" + quotaDoc.getQuotaInfo() + ")");
        }
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
