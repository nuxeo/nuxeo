/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.adapter.impl;

import static org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.RootlessItemException;
import org.nuxeo.drive.adapter.ScrollFileSystemItemList;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.cache.Cache;
import org.nuxeo.ecm.core.cache.CacheService;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * {@link DocumentModel} backed implementation of a {@link FolderItem}.
 *
 * @author Antoine Taillefer
 */
public class DocumentBackedFolderItem extends AbstractDocumentBackedFileSystemItem implements FolderItem {

    private static final Logger log = LogManager.getLogger(DocumentBackedFolderItem.class);

    private static final long serialVersionUID = 1L;

    private static final String FOLDER_ITEM_CHILDREN_PAGE_PROVIDER = "FOLDER_ITEM_CHILDREN";

    protected static final String DESCENDANTS_SCROLL_CACHE = "driveDescendantsScroll";

    protected static final String MAX_DESCENDANTS_BATCH_SIZE_PROPERTY = "org.nuxeo.drive.maxDescendantsBatchSize";

    protected static final String MAX_DESCENDANTS_BATCH_SIZE_DEFAULT = "1000";

    protected static final int VCS_CHUNK_SIZE = 100;

    protected boolean canCreateChild;

    protected boolean canScrollDescendants;

    public DocumentBackedFolderItem(String factoryName, DocumentModel doc) {
        this(factoryName, doc, false);
    }

    public DocumentBackedFolderItem(String factoryName, DocumentModel doc, boolean relaxSyncRootConstraint) {
        this(factoryName, doc, relaxSyncRootConstraint, true);
    }

    public DocumentBackedFolderItem(String factoryName, DocumentModel doc, boolean relaxSyncRootConstraint,
            boolean getLockInfo) {
        super(factoryName, doc, relaxSyncRootConstraint, getLockInfo);
        initialize(doc);
    }

    public DocumentBackedFolderItem(String factoryName, FolderItem parentItem, DocumentModel doc) {
        this(factoryName, parentItem, doc, false);
    }

    public DocumentBackedFolderItem(String factoryName, FolderItem parentItem, DocumentModel doc,
            boolean relaxSyncRootConstraint) {
        this(factoryName, parentItem, doc, relaxSyncRootConstraint, true);
    }

    public DocumentBackedFolderItem(String factoryName, FolderItem parentItem, DocumentModel doc,
            boolean relaxSyncRootConstraint, boolean getLockInfo) {
        super(factoryName, parentItem, doc, relaxSyncRootConstraint, getLockInfo);
        initialize(doc);
    }

    protected DocumentBackedFolderItem() {
        // Needed for JSON deserialization
    }

    /*--------------------- FileSystemItem ---------------------*/
    @Override
    public void rename(String name) {
        try (CloseableCoreSession session = CoreInstance.openCoreSession(repositoryName, principal)) {
            // Update doc properties
            DocumentModel doc = getDocument(session);
            doc.setPropertyValue("dc:title", name);
            doc.putContextData(CoreSession.SOURCE, "drive");
            doc = session.saveDocument(doc);
            session.save();
            // Update FileSystemItem attributes
            this.docTitle = name;
            this.name = name;
            updateLastModificationDate(doc);
        }
    }

    /*--------------------- FolderItem -----------------*/
    @Override
    @SuppressWarnings("unchecked")
    public List<FileSystemItem> getChildren() {
        try (CloseableCoreSession session = CoreInstance.openCoreSession(repositoryName, principal)) {
            PageProviderService pageProviderService = Framework.getService(PageProviderService.class);
            Map<String, Serializable> props = new HashMap<>();
            props.put(CORE_SESSION_PROPERTY, (Serializable) session);
            PageProvider<DocumentModel> childrenPageProvider = (PageProvider<DocumentModel>) pageProviderService.getPageProvider(
                    FOLDER_ITEM_CHILDREN_PAGE_PROVIDER, null, null, 0L, props, docId);
            long pageSize = childrenPageProvider.getPageSize();

            List<FileSystemItem> children = new ArrayList<>();
            int nbChildren = 0;
            boolean reachedPageSize = false;
            boolean hasNextPage = true;
            // Since query results are filtered, make sure we iterate on PageProvider to get at most its page size
            // number of
            // FileSystemItems
            while (nbChildren < pageSize && hasNextPage) {
                List<DocumentModel> dmChildren = childrenPageProvider.getCurrentPage();
                for (DocumentModel dmChild : dmChildren) {
                    // NXP-19442: Avoid useless and costly call to DocumentModel#getLockInfo
                    FileSystemItem child = getFileSystemItemAdapterService().getFileSystemItem(dmChild, this, false,
                            false, false);
                    if (child != null) {
                        children.add(child);
                        nbChildren++;
                        if (nbChildren == pageSize) {
                            reachedPageSize = true;
                            break;
                        }
                    }
                }
                if (!reachedPageSize) {
                    hasNextPage = childrenPageProvider.isNextPageAvailable();
                    if (hasNextPage) {
                        childrenPageProvider.nextPage();
                    }
                }
            }

            return children;
        }
    }

    @Override
    public boolean getCanScrollDescendants() {
        return canScrollDescendants;
    }

    @Override
    public ScrollFileSystemItemList scrollDescendants(String scrollId, int batchSize, long keepAlive) {
        Semaphore semaphore = Framework.getService(FileSystemItemAdapterService.class).getScrollBatchSemaphore();
        try {
            log.trace("Thread [{}] acquiring scroll batch semaphore", Thread::currentThread);
            semaphore.acquire();
            try {
                log.trace("Thread [{}] acquired scroll batch semaphore, available permits reduced to {}",
                        Thread::currentThread, semaphore::availablePermits);
                return doScrollDescendants(scrollId, batchSize, keepAlive);
            } finally {
                semaphore.release();
                log.trace("Thread [{}] released scroll batch semaphore, available permits increased to {}",
                        Thread::currentThread, semaphore::availablePermits);
            }
        } catch (InterruptedException cause) {
            Thread.currentThread().interrupt();
            throw new NuxeoException("Scroll batch interrupted", cause);
        }
    }

    protected ScrollFileSystemItemList doScrollDescendants(String scrollId, int batchSize, long keepAlive) {
        try (CloseableCoreSession session = CoreInstance.openCoreSession(repositoryName, principal)) {

            // Limit batch size sent by the client
            checkBatchSize(batchSize);

            // Scroll through a batch of documents
            ScrollDocumentModelList descendantDocsBatch = getScrollBatch(scrollId, batchSize, session, keepAlive);
            String newScrollId = descendantDocsBatch.getScrollId();
            if (descendantDocsBatch.isEmpty()) {
                // No more descendants left to return
                return new ScrollFileSystemItemListImpl(newScrollId, 0);
            }

            // Adapt documents as FileSystemItems
            List<FileSystemItem> descendants = adaptDocuments(descendantDocsBatch, session);
            log.debug("Retrieved {} descendants of FolderItem {} (batchSize = {})", descendants::size, () -> docPath,
                    () -> batchSize);
            return new ScrollFileSystemItemListImpl(newScrollId, descendants);
        }
    }

    protected void checkBatchSize(int batchSize) {
        int maxDescendantsBatchSize = Integer.parseInt(Framework.getService(ConfigurationService.class).getProperty(
                MAX_DESCENDANTS_BATCH_SIZE_PROPERTY, MAX_DESCENDANTS_BATCH_SIZE_DEFAULT));
        if (batchSize > maxDescendantsBatchSize) {
            throw new NuxeoException(String.format(
                    "Batch size %d is greater than the maximum batch size allowed %d. If you need to increase this limit you can set the %s configuration property but this is not recommended for performance reasons.",
                    batchSize, maxDescendantsBatchSize, MAX_DESCENDANTS_BATCH_SIZE_PROPERTY));
        }
    }

    @SuppressWarnings("unchecked")
    protected ScrollDocumentModelList getScrollBatch(String scrollId, int batchSize, CoreSession session,
            long keepAlive) {
        Cache scrollingCache = Framework.getService(CacheService.class).getCache(DESCENDANTS_SCROLL_CACHE);
        if (scrollingCache == null) {
            throw new NuxeoException("Cache not found: " + DESCENDANTS_SCROLL_CACHE);
        }
        String newScrollId;
        List<String> descendantIds;
        if (StringUtils.isEmpty(scrollId)) {
            // Perform initial query to fetch ids of all the descendant documents and put the result list in a
            // cache, aka "search context"
            descendantIds = new ArrayList<>();
            StringBuilder sb = new StringBuilder(
                    String.format("SELECT ecm:uuid FROM Document WHERE ecm:ancestorId = '%s'", docId));
            sb.append(" AND ecm:isTrashed = 0");
            sb.append(" AND ecm:mixinType != 'HiddenInNavigation'");
            // Don't need to add ecm:isVersion = 0 because versions are already excluded by the
            // ecm:ancestorId clause since they have no path
            String query = sb.toString();
            log.debug("Executing initial query to scroll through the descendants of {}: {}", docPath, query);
            try (IterableQueryResult res = session.queryAndFetch(sb.toString(), NXQL.NXQL)) {
                Iterator<Map<String, Serializable>> it = res.iterator();
                while (it.hasNext()) {
                    descendantIds.add((String) it.next().get(NXQL.ECM_UUID));
                }
            }
            // Generate a scroll id
            newScrollId = UUID.randomUUID().toString();
            log.debug("Put initial query result list (search context) in the {} cache at key (scrollId) {}",
                    DESCENDANTS_SCROLL_CACHE, newScrollId);
            scrollingCache.put(newScrollId, (Serializable) descendantIds);
        } else {
            // Get the descendant ids from the cache
            descendantIds = (List<String>) scrollingCache.get(scrollId);
            if (descendantIds == null) {
                throw new NuxeoException(String.format("No search context found in the %s cache for scrollId [%s]",
                        DESCENDANTS_SCROLL_CACHE, scrollId));
            }
            newScrollId = scrollId;
        }

        if (descendantIds.isEmpty()) {
            return new ScrollDocumentModelList(newScrollId, 0);
        }

        // Extract a batch of descendant ids
        List<String> descendantIdsBatch = getBatch(descendantIds, batchSize);
        // Update descendant ids in the cache
        scrollingCache.put(newScrollId, (Serializable) descendantIds);
        // Fetch documents from VCS
        DocumentModelList descendantDocsBatch = fetchFromVCS(descendantIdsBatch, session);
        return new ScrollDocumentModelList(newScrollId, descendantDocsBatch);
    }

    /**
     * Extracts batchSize elements from the input list.
     */
    protected List<String> getBatch(List<String> ids, int batchSize) {
        List<String> batch = new ArrayList<>(batchSize);
        int idCount = 0;
        Iterator<String> it = ids.iterator();
        while (it.hasNext() && idCount < batchSize) {
            batch.add(it.next());
            it.remove();
            idCount++;
        }
        return batch;
    }

    protected DocumentModelList fetchFromVCS(List<String> ids, CoreSession session) {
        DocumentModelList res = null;
        int size = ids.size();
        int start = 0;
        int end = Math.min(VCS_CHUNK_SIZE, size);
        boolean done = false;
        while (!done) {
            DocumentModelList docs = fetchFromVcsChunk(ids.subList(start, end), session);
            if (res == null) {
                res = docs;
            } else {
                res.addAll(docs);
            }
            if (end >= ids.size()) {
                done = true;
            } else {
                start = end;
                end = Math.min(start + VCS_CHUNK_SIZE, size);
            }
        }
        return res;
    }

    protected DocumentModelList fetchFromVcsChunk(final List<String> ids, CoreSession session) {
        int docCount = ids.size();
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM Document WHERE ecm:uuid IN (");
        for (int i = 0; i < docCount; i++) {
            sb.append(NXQL.escapeString(ids.get(i)));
            if (i < docCount - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        String query = sb.toString();
        log.debug("Fetching {} documents from VCS: {}", docCount, query);
        return session.query(query);
    }

    /**
     * Adapts the given {@link DocumentModelList} as {@link FileSystemItem}s using a cache for the {@link FolderItem}
     * ancestors.
     */
    protected List<FileSystemItem> adaptDocuments(DocumentModelList docs, CoreSession session) {
        Map<DocumentRef, FolderItem> ancestorCache = new HashMap<>();
        log.trace("Caching current FolderItem for doc {}: {}", () -> docPath, this::getPath);
        ancestorCache.put(new IdRef(docId), this);
        List<FileSystemItem> descendants = new ArrayList<>(docs.size());
        for (DocumentModel doc : docs) {
            FolderItem parent = populateAncestorCache(ancestorCache, doc, session, false);
            if (parent == null) {
                log.debug("Cannot adapt parent document of {} as a FileSystemItem, skipping descendant document",
                        doc::getPathAsString);
                continue;
            }
            // NXP-19442: Avoid useless and costly call to DocumentModel#getLockInfo
            FileSystemItem descendant = getFileSystemItemAdapterService().getFileSystemItem(doc, parent, false, false,
                    false);
            if (descendant != null) {
                if (descendant.isFolder()) {
                    log.trace("Caching descendant FolderItem for doc {}: {}", doc::getPathAsString,
                            descendant::getPath);
                    ancestorCache.put(doc.getRef(), (FolderItem) descendant);
                }
                descendants.add(descendant);
            }
        }
        return descendants;
    }

    protected FolderItem populateAncestorCache(Map<DocumentRef, FolderItem> cache, DocumentModel doc,
            CoreSession session, boolean cacheItem) {
        DocumentRef parentDocRef = session.getParentDocumentRef(doc.getRef());
        if (parentDocRef == null) {
            throw new RootlessItemException("Reached repository root");
        }

        FolderItem parentItem = cache.get(parentDocRef);
        if (parentItem != null) {
            log.trace("Found parent FolderItem in cache for doc {}: {}", doc::getPathAsString, parentItem::getPath);
            return getFolderItem(cache, doc, parentItem, cacheItem);
        }

        log.trace("No parent FolderItem found in cache for doc {}, computing ancestor cache", doc::getPathAsString);
        DocumentModel parentDoc = null;
        try {
            parentDoc = session.getDocument(parentDocRef);
        } catch (DocumentSecurityException e) {
            log.debug("User {} has no READ access on parent of document {} ({}).", principal::getName,
                    doc::getPathAsString, doc::getId, () -> e);
            return null;
        }
        parentItem = populateAncestorCache(cache, parentDoc, session, true);
        if (parentItem == null) {
            return null;
        }
        return getFolderItem(cache, doc, parentItem, cacheItem);
    }

    protected FolderItem getFolderItem(Map<DocumentRef, FolderItem> cache, DocumentModel doc, FolderItem parentItem,
            boolean cacheItem) {
        if (cacheItem) {
            // NXP-19442: Avoid useless and costly call to DocumentModel#getLockInfo
            FileSystemItem fsItem = getFileSystemItemAdapterService().getFileSystemItem(doc, parentItem, true, false,
                    false);
            if (fsItem == null) {
                log.debug(
                        "Reached document {} that cannot be  adapted as a (possibly virtual) descendant of the top level folder item.",
                        doc::getPathAsString);
                return null;
            }
            FolderItem folderItem = (FolderItem) fsItem;
            log.trace("Caching FolderItem for doc {}: {}", doc::getPathAsString, folderItem::getPath);
            cache.put(doc.getRef(), folderItem);
            return folderItem;
        } else {
            return parentItem;
        }
    }

    @Override
    public boolean getCanCreateChild() {
        return canCreateChild;
    }

    @Override
    public FolderItem createFolder(String name, boolean overwrite) {
        try (CloseableCoreSession session = CoreInstance.openCoreSession(repositoryName, principal)) {
            DocumentModel folder = getFileManager().createFolder(session, name, docPath, overwrite);
            if (folder == null) {
                throw new NuxeoException(String.format(
                        "Cannot create folder named '%s' as a child of doc %s. Probably because of the allowed sub-types for this doc type, please check them.",
                        name, docPath));
            }
            return (FolderItem) getFileSystemItemAdapterService().getFileSystemItem(folder, this);
        } catch (NuxeoException e) {
            e.addInfo(String.format("Error while trying to create folder %s as a child of doc %s", name, docPath));
            throw e;
        } catch (IOException e) {
            throw new NuxeoException(
                    String.format("Error while trying to create folder %s as a child of doc %s", name, docPath), e);
        }
    }

    @Override
    public FileItem createFile(Blob blob, boolean overwrite) {
        String fileName = blob.getFilename();
        try (CloseableCoreSession session = CoreInstance.openCoreSession(repositoryName, principal)) {
            DocumentModel file = getFileManager().createDocumentFromBlob(session, blob, docPath, overwrite, fileName,
                    false, true);
            if (file == null) {
                throw new NuxeoException(String.format(
                        "Cannot create file '%s' as a child of doc %s. Probably because there are no file importers registered, please check the contributions to the <extension target=\"org.nuxeo.ecm.platform.filemanager.service.FileManagerService\" point=\"plugins\"> extension point.",
                        fileName, docPath));
            }
            return (FileItem) getFileSystemItemAdapterService().getFileSystemItem(file, this);
        } catch (NuxeoException e) {
            e.addInfo(String.format("Error while trying to create file %s as a child of doc %s", fileName, docPath));
            throw e;
        } catch (IOException e) {
            throw new NuxeoException(
                    String.format("Error while trying to create file %s as a child of doc %s", fileName, docPath), e);
        }
    }

    /*--------------------- Protected -----------------*/
    protected void initialize(DocumentModel doc) {
        this.name = docTitle;
        this.folder = true;
        this.canCreateChild = !doc.hasFacet(FacetNames.PUBLISH_SPACE);
        if (canCreateChild) {
            if (Framework.getService(ConfigurationService.class)
                         .isBooleanPropertyTrue(PERMISSION_CHECK_OPTIMIZED_PROPERTY)) {
                // In optimized mode consider that canCreateChild <=> canRename because canRename <=> WriteProperties
                // and by default WriteProperties <=> Write <=> AddChildren
                this.canCreateChild = canRename;
            } else {
                // In non optimized mode check AddChildren
                this.canCreateChild = doc.getCoreSession().hasPermission(doc.getRef(), SecurityConstants.ADD_CHILDREN);
            }
        }
        this.canScrollDescendants = true;
    }

    protected FileManager getFileManager() {
        return Framework.getService(FileManager.class);
    }

    /*---------- Needed for JSON deserialization ----------*/
    protected void setCanCreateChild(boolean canCreateChild) {
        this.canCreateChild = canCreateChild;
    }

    protected void setCanScrollDescendants(boolean canScrollDescendants) {
        this.canScrollDescendants = canScrollDescendants;
    }

}
