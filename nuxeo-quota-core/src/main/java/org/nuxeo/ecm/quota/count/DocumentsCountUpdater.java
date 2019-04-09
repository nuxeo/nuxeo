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
 *     Thomas Roger
 *     Florent Guillaume
 */
package org.nuxeo.ecm.quota.count;

import static org.nuxeo.ecm.core.schema.FacetNames.FOLDERISH;
import static org.nuxeo.ecm.quota.count.Constants.DOCUMENTS_COUNT_STATISTICS_CHILDREN_COUNT_PROPERTY;
import static org.nuxeo.ecm.quota.count.Constants.DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY;
import static org.nuxeo.ecm.quota.count.Constants.DOCUMENTS_COUNT_STATISTICS_FACET;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.model.DeltaLong;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.quota.AbstractQuotaStatsUpdater;
import org.nuxeo.ecm.quota.QuotaStatsInitialWork;
import org.nuxeo.ecm.quota.QuotaUtils;
import org.nuxeo.ecm.quota.size.QuotaExceededException;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * {@link org.nuxeo.ecm.quota.QuotaStatsUpdater} counting the non folderish documents.
 * <p>
 * Store the descendant and children count on {@code Folderish} documents.
 *
 * @since 5.5
 */
public class DocumentsCountUpdater extends AbstractQuotaStatsUpdater {

    private static final Log log = LogFactory.getLog(DocumentsCountUpdater.class);

    public static final int BATCH_SIZE = 50;

    @Override
    protected void processDocumentCreated(CoreSession session, DocumentModel doc) {
        if (doc.isVersion()) {
            return;
        }
        List<DocumentModel> ancestors = getAncestors(session, doc);
        long docCount = getCount(doc);
        updateCountStatistics(session, doc, ancestors, docCount);
    }

    @Override
    protected void processDocumentCopied(CoreSession session, DocumentModel doc) {
        List<DocumentModel> ancestors = getAncestors(session, doc);
        long docCount = getCount(doc);
        updateCountStatistics(session, doc, ancestors, docCount);
    }

    @Override
    protected void processDocumentCheckedIn(CoreSession session, DocumentModel doc) {
        // NOP
    }

    @Override
    protected void processDocumentBeforeCheckedIn(CoreSession session, DocumentModel doc) {
        // NOP
    }

    @Override
    protected void processDocumentCheckedOut(CoreSession session, DocumentModel doc) {
        // NOP
    }

    @Override
    protected void processDocumentBeforeCheckedOut(CoreSession session, DocumentModel doc) {
        // NOP
    }

    @Override
    protected void processDocumentUpdated(CoreSession session, DocumentModel doc) {
    }

    @Override
    protected void processDocumentMoved(CoreSession session, DocumentModel doc, DocumentModel sourceParent) {
        List<DocumentModel> ancestors = getAncestors(session, doc);
        List<DocumentModel> sourceAncestors = getAncestors(session, sourceParent);
        sourceAncestors.add(0, sourceParent);
        long docCount = getCount(doc);
        updateCountStatistics(session, doc, ancestors, docCount);
        updateCountStatistics(session, doc, sourceAncestors, -docCount);
    }

    @Override
    protected void processDocumentAboutToBeRemoved(CoreSession session, DocumentModel doc) {
        List<DocumentModel> ancestors = getAncestors(session, doc);
        long docCount = getCount(doc);
        updateCountStatistics(session, doc, ancestors, -docCount);
    }

    @Override
    protected void handleQuotaExceeded(QuotaExceededException e, Event event) {
        // never rollback on Exceptions
    }

    @Override
    protected boolean needToProcessEventOnDocument(Event event, DocumentModel doc) {
        return true;
    }

    @Override
    protected void processDocumentBeforeUpdate(CoreSession session, DocumentModel doc) {
        // NOP
    }

    protected void updateCountStatistics(CoreSession session, DocumentModel doc, List<DocumentModel> ancestors,
            long count) {
        if (ancestors == null || ancestors.isEmpty()) {
            return;
        }
        if (count == 0) {
            return;
        }

        if (!doc.hasFacet(FOLDERISH)) {
            DocumentModel parent = ancestors.get(0);
            updateCount(session, parent, DOCUMENTS_COUNT_STATISTICS_CHILDREN_COUNT_PROPERTY, count);
        }

        ancestors.forEach(ancestor -> updateCount(session, ancestor,
                DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY, count));
        session.save();
    }

    protected void updateCount(CoreSession session, DocumentModel parent, String xpath, long count) {
        Number previous;
        if (parent.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET)) {
            previous = (Number) parent.getPropertyValue(xpath);
        } else {
            parent.addFacet(DOCUMENTS_COUNT_STATISTICS_FACET);
            previous = null;
        }
        DeltaLong childrenCount = DeltaLong.valueOf(previous, count);
        parent.setPropertyValue(xpath, childrenCount);
        // do not send notifications
        QuotaUtils.disableListeners(parent);
        DocumentModel origParent = parent;
        parent = session.saveDocument(parent);
        QuotaUtils.clearContextData(origParent);
    }

    protected long getCount(DocumentModel doc) {
        if (doc.hasFacet(FOLDERISH)) {
            if (doc.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET)) {
                Number count = (Number) doc.getPropertyValue(DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY);
                return count == null ? 0 : count.longValue();
            } else {
                return 0;
            }
        } else {
            return 1;
        }
    }

    @Override
    public void computeInitialStatistics(CoreSession session, QuotaStatsInitialWork currentWorker, String path) {
        // TODO path != null
        Map<String, String> folders = getFolders(session);
        Map<String, Count> documentsCountByFolder = computeDocumentsCountByFolder(session, folders);
        saveDocumentsCount(session, documentsCountByFolder);
    }

    protected Map<String, String> getFolders(CoreSession session) {
        try (IterableQueryResult res = session.queryAndFetch(
                "SELECT ecm:uuid, ecm:parentId FROM Document WHERE ecm:mixinType = 'Folderish'", "NXQL")) {
            Map<String, String> folders = new HashMap<>();

            for (Map<String, Serializable> r : res) {
                folders.put((String) r.get("ecm:uuid"), (String) r.get("ecm:parentId"));
            }
            return folders;
        }
    }

    protected Map<String, Count> computeDocumentsCountByFolder(CoreSession session, Map<String, String> folders) {
        try (IterableQueryResult res = session.queryAndFetch("SELECT ecm:uuid, ecm:parentId FROM Document", "NXQL")) {
            Map<String, Count> foldersCount = new HashMap<>();
            for (Map<String, Serializable> r : res) {
                String uuid = (String) r.get("ecm:uuid");
                if (folders.containsKey(uuid)) {
                    // a folder
                    continue;
                }

                String folderId = (String) r.get("ecm:parentId");
                if (!foldersCount.containsKey(folderId)) {
                    foldersCount.put(folderId, new Count());
                }
                Count count = foldersCount.get(folderId);
                count.childrenCount++;
                count.descendantsCount++;

                updateParentsDocumentsCount(folders, foldersCount, folderId);
            }
            return foldersCount;
        }
    }

    protected void updateParentsDocumentsCount(Map<String, String> folders, Map<String, Count> foldersCount,
            String folderId) {
        String parent = folders.get(folderId);
        while (parent != null) {
            if (!foldersCount.containsKey(parent)) {
                foldersCount.put(parent, new Count());
            }
            Count c = foldersCount.get(parent);
            c.descendantsCount++;
            parent = folders.get(parent);
        }
    }

    protected void saveDocumentsCount(CoreSession session, Map<String, Count> foldersCount) {
        long docsCount = 0;
        for (Map.Entry<String, Count> entry : foldersCount.entrySet()) {
            String folderId = entry.getKey();
            if (folderId == null) {
                continue;
            }
            DocumentModel folder;
            try {
                folder = session.getDocument(new IdRef(folderId));
            } catch (DocumentNotFoundException e) {
                log.warn(e);
                log.debug(e, e);
                continue;
            }
            if (folder.getPath().isRoot()) {
                // Root document
                continue;
            }
            saveDocumentsCount(session, folder, entry.getValue());
            docsCount++;
            if (docsCount % BATCH_SIZE == 0) {
                session.save();
                if (TransactionHelper.isTransactionActive()) {
                    TransactionHelper.commitOrRollbackTransaction();
                    TransactionHelper.startTransaction();
                }
            }
        }
        session.save();
    }

    protected void saveDocumentsCount(CoreSession session, DocumentModel folder, Count count) {
        if (!folder.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET)) {
            folder.addFacet(DOCUMENTS_COUNT_STATISTICS_FACET);
        }
        folder.setPropertyValue(DOCUMENTS_COUNT_STATISTICS_CHILDREN_COUNT_PROPERTY, Long.valueOf(count.childrenCount));
        folder.setPropertyValue(DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY,
                Long.valueOf(count.descendantsCount));
        // do not send notifications
        QuotaUtils.disableListeners(folder);
        DocumentModel origFolder = folder;
        folder = session.saveDocument(folder);
        QuotaUtils.clearContextData(origFolder);
    }

    /**
     * Object to store documents count for a folder
     */
    private static class Count {

        public long childrenCount = 0;

        public long descendantsCount = 0;
    }

    @Override
    protected void processDocumentTrashOp(CoreSession session, DocumentModel doc, boolean isTrashed) {
        // do nothing for count
    }

    @Override
    protected void processDocumentRestored(CoreSession session, DocumentModel doc) {
        // do nothing
    }

    @Override
    protected void processDocumentBeforeRestore(CoreSession session, DocumentModel doc) {
        // do nothing
    }
}
