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
 *     Thomas Roger <troger@nuxeo.com>
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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.model.DeltaLong;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.quota.AbstractQuotaStatsUpdater;
import org.nuxeo.ecm.quota.QuotaStatsInitialWork;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * {@link org.nuxeo.ecm.quota.QuotaStatsUpdater} counting the non folderish
 * documents.
 * <p>
 * Store the descendant and children count on {@code Folderish} documents.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class DocumentsCountUpdater extends AbstractQuotaStatsUpdater {

    private static final Log log = LogFactory.getLog(DocumentsCountUpdater.class);

    public static final int BATCH_SIZE = 50;

    @Override
    protected void processDocumentCreated(CoreSession session,
            DocumentModel doc, DocumentEventContext docCtx)
            throws ClientException {
        if (doc.isVersion()) {
            return;
        }
        List<DocumentModel> ancestors = getAncestors(session, doc);
        long docCount = getCount(doc);
        updateCountStatistics(session, doc, ancestors, docCount);
    }

    @Override
    protected void processDocumentCopied(CoreSession session,
            DocumentModel doc, DocumentEventContext docCtx)
            throws ClientException {
        List<DocumentModel> ancestors = getAncestors(session, doc);
        long docCount = getCount(doc);
        updateCountStatistics(session, doc, ancestors, docCount);
    }

    @Override
    protected void processDocumentCheckedIn(CoreSession session,
            DocumentModel doc, DocumentEventContext docCtx)
            throws ClientException {
        // NOP
    }

    @Override
    protected void processDocumentCheckedOut(CoreSession session,
            DocumentModel doc, DocumentEventContext docCtx)
            throws ClientException {
        // NOP
    }

    @Override
    protected void processDocumentUpdated(CoreSession session,
            DocumentModel doc, DocumentEventContext docCtx)
            throws ClientException {
    }

    @Override
    protected void processDocumentMoved(CoreSession session, DocumentModel doc,
            DocumentModel sourceParent, DocumentEventContext docCtx)
            throws ClientException {
        List<DocumentModel> ancestors = getAncestors(session, doc);
        List<DocumentModel> sourceAncestors = getAncestors(session,
                sourceParent);
        sourceAncestors.add(0, sourceParent);
        long docCount = getCount(doc);
        updateCountStatistics(session, doc, ancestors, docCount);
        updateCountStatistics(session, doc, sourceAncestors, -docCount);
    }

    @Override
    protected void processDocumentAboutToBeRemoved(CoreSession session,
            DocumentModel doc, DocumentEventContext docCtx)
            throws ClientException {
        List<DocumentModel> ancestors = getAncestors(session, doc);
        long docCount = getCount(doc);
        updateCountStatistics(session, doc, ancestors, -docCount);
    }

    @Override
    protected ClientException handleException(ClientException e, Event event) {
        // never rollback on Exceptions
        return e;
    }

    @Override
    protected boolean needToProcessEventOnDocument(Event event,
            DocumentModel targetDoc) {
        return true;
    }

    @Override
    protected void processDocumentBeforeUpdate(CoreSession session,
            DocumentModel targetDoc, DocumentEventContext docCtx) {
        // NOP
    }

    protected void updateCountStatistics(CoreSession session,
            DocumentModel doc, List<DocumentModel> ancestors, long count)
            throws ClientException {
        if (ancestors == null || ancestors.isEmpty()) {
            return;
        }
        if (count == 0) {
            return;
        }

        if (!doc.hasFacet(FOLDERISH)) {
            DocumentModel parent = ancestors.get(0);
            updateParentChildrenCount(session, parent, count);
        }

        for (DocumentModel ancestor : ancestors) {
            Number previous;
            if (ancestor.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET)) {
                previous = (Number) ancestor.getPropertyValue(DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY);
            } else {
                ancestor.addFacet(DOCUMENTS_COUNT_STATISTICS_FACET);
                previous = null;
            }
            Number descendantsCount = DeltaLong.deltaOrLong(previous, count);
            ancestor.setPropertyValue(
                    DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY,
                    descendantsCount);
            setSystemContextData(ancestor);
            session.saveDocument(ancestor);
        }

        session.save();
    }

    protected void updateParentChildrenCount(CoreSession session,
            DocumentModel parent, long count) throws ClientException {
        Number previous;
        if (parent.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET)) {
            previous = (Number) parent.getPropertyValue(DOCUMENTS_COUNT_STATISTICS_CHILDREN_COUNT_PROPERTY);
        } else {
            parent.addFacet(DOCUMENTS_COUNT_STATISTICS_FACET);
            previous = null;
        }
        Number childrenCount = DeltaLong.deltaOrLong(previous, count);
        parent.setPropertyValue(
                DOCUMENTS_COUNT_STATISTICS_CHILDREN_COUNT_PROPERTY,
                childrenCount);
        setSystemContextData(parent);
        session.saveDocument(parent);
    }

    protected long getCount(DocumentModel doc) throws ClientException {
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
    public void computeInitialStatistics(CoreSession session,
            QuotaStatsInitialWork currentWorker) {
        try {
            Map<String, String> folders = getFolders(session);
            Map<String, Count> documentsCountByFolder = computeDocumentsCountByFolder(
                    session, folders);
            saveDocumentsCount(session, documentsCountByFolder);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected Map<String, String> getFolders(CoreSession session)
            throws ClientException {
        IterableQueryResult res = session.queryAndFetch(
                "SELECT ecm:uuid, ecm:parentId FROM Document WHERE ecm:mixinType = 'Folderish'",
                "NXQL");
        try {
            Map<String, String> folders = new HashMap<String, String>();

            for (Map<String, Serializable> r : res) {
                folders.put((String) r.get("ecm:uuid"),
                        (String) r.get("ecm:parentId"));
            }
            return folders;
        } finally {
            if (res != null) {
                res.close();
            }
        }
    }

    protected Map<String, Count> computeDocumentsCountByFolder(
            CoreSession session, Map<String, String> folders)
            throws ClientException {
        IterableQueryResult res = session.queryAndFetch(
                "SELECT ecm:uuid, ecm:parentId FROM Document", "NXQL");
        try {
            Map<String, Count> foldersCount = new HashMap<String, Count>();
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
        } finally {
            if (res != null) {
                res.close();
            }
        }
    }

    protected void updateParentsDocumentsCount(Map<String, String> folders,
            Map<String, Count> foldersCount, String folderId) {
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

    protected void saveDocumentsCount(CoreSession session,
            Map<String, Count> foldersCount) throws ClientException {
        long docsCount = 0;
        for (Map.Entry<String, Count> entry : foldersCount.entrySet()) {
            String folderId = entry.getKey();
            if (folderId == null) {
                continue;
            }

            try {
                DocumentModel folder = session.getDocument(new IdRef(folderId));
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
            } catch (ClientException e) {
                log.warn(e);
                log.debug(e, e);
            }
        }
        session.save();
    }

    protected void saveDocumentsCount(CoreSession session,
            DocumentModel folder, Count count) throws ClientException {
        if (!folder.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET)) {
            folder.addFacet(DOCUMENTS_COUNT_STATISTICS_FACET);
        }
        folder.setPropertyValue(
                DOCUMENTS_COUNT_STATISTICS_CHILDREN_COUNT_PROPERTY,
                Long.valueOf(count.childrenCount));
        folder.setPropertyValue(
                DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY,
                Long.valueOf(count.descendantsCount));
        setSystemContextData(folder);
        session.saveDocument(folder);
    }

    /**
     * Object to store documents count for a folder
     */
    private static class Count {

        public long childrenCount = 0;

        public long descendantsCount = 0;
    }

    @Override
    protected void processDocumentTrashOp(CoreSession session,
            DocumentModel doc, DocumentEventContext docCtx) {
        // do nothing for count
    }

    @Override
    protected void processDocumentRestored(CoreSession session,
            DocumentModel doc, DocumentEventContext docCtx)
            throws ClientException {
        // do nothing
    }

    @Override
    protected void processDocumentBeforeRestore(CoreSession session,
            DocumentModel doc, DocumentEventContext docCtx)
            throws ClientException {
        // do nothing
    }
}
