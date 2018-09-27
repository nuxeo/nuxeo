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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.RootlessItemException;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.impl.CollectionSyncRootFolderItemFactory;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.trash.TrashService;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * {@link DocumentModel} backed implementation of a {@link FileSystemItem}.
 *
 * @author Antoine Taillefer
 * @see DocumentBackedFileItem
 * @see DocumentBackedFolderItem
 */
public abstract class AbstractDocumentBackedFileSystemItem extends AbstractFileSystemItem {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(AbstractDocumentBackedFileSystemItem.class);

    protected static final String PERMISSION_CHECK_OPTIMIZED_PROPERTY = "org.nuxeo.drive.permissionCheckOptimized";

    /** Backing {@link DocumentModel} attributes */
    protected String repositoryName;

    protected String docId;

    protected String docPath;

    protected String docTitle;

    protected AbstractDocumentBackedFileSystemItem(String factoryName, DocumentModel doc) {
        this(factoryName, doc, false);
    }

    protected AbstractDocumentBackedFileSystemItem(String factoryName, DocumentModel doc,
            boolean relaxSyncRootConstraint) {
        this(factoryName, doc, relaxSyncRootConstraint, true);
    }

    protected AbstractDocumentBackedFileSystemItem(String factoryName, DocumentModel doc,
            boolean relaxSyncRootConstraint, boolean getLockInfo) {
        this(factoryName, null, doc, relaxSyncRootConstraint, getLockInfo);
        CoreSession docSession = doc.getCoreSession();
        DocumentModel parentDoc = null;
        try {
            DocumentRef parentDocRef = docSession.getParentDocumentRef(doc.getRef());
            if (parentDocRef != null) {
                parentDoc = docSession.getDocument(parentDocRef);
            }
        } catch (DocumentSecurityException e) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "User %s has no READ access on parent of document %s (%s), will throw RootlessItemException.",
                        principal.getName(), doc.getPathAsString(), doc.getId()));
            }
        }
        try {
            if (parentDoc == null) {
                log.trace(
                        "We either reached the root of the repository or a document for which the current user doesn't have read access to its parent,"
                                + " without being adapted to a (possibly virtual) descendant of the top level folder item."
                                + " Let's raise a marker exception and let the caller give more information on the source document.");
                throw new RootlessItemException();
            } else {
                FileSystemItem parent = getFileSystemItemAdapterService().getFileSystemItem(parentDoc, true,
                        relaxSyncRootConstraint, getLockInfo);
                if (parent == null) {
                    log.trace(
                            "We reached a document for which the parent document cannot be  adapted to a (possibly virtual) descendant of the top level folder item."
                                    + " Let's raise a marker exception and let the caller give more information on the source document.");
                    throw new RootlessItemException();
                }
                parentId = parent.getId();
                path = parent.getPath() + '/' + id;
            }
        } catch (RootlessItemException e) {
            log.trace(
                    "Let's try to adapt the document as a member of a collection sync root, if not the case let's raise a marker exception and let the caller give more information on the source document.");
            if (!handleCollectionMember(doc, docSession, relaxSyncRootConstraint, getLockInfo)) {
                throw new RootlessItemException();
            }
        }
    }

    protected boolean handleCollectionMember(DocumentModel doc, CoreSession session, boolean relaxSyncRootConstraint,
            boolean getLockInfo) {
        if (!doc.hasSchema(CollectionConstants.COLLECTION_MEMBER_SCHEMA_NAME)) {
            return false;
        }
        CollectionManager cm = Framework.getService(CollectionManager.class);
        List<DocumentModel> docCollections = cm.getVisibleCollection(doc, session);
        if (docCollections.isEmpty()) {
            if (log.isTraceEnabled()) {
                log.trace(String.format("Doc %s (%s) is not member of any collection", doc.getPathAsString(),
                        doc.getId()));
            }
            return false;
        } else {
            FileSystemItem parent = null;
            DocumentModel collection = null;
            Iterator<DocumentModel> it = docCollections.iterator();
            while (it.hasNext() && parent == null) {
                collection = it.next();
                // Prevent infinite loop in case the collection is a descendant of the document being currently adapted
                // as a FileSystemItem and this collection is not a synchronization root for the current user
                if (collection.getPathAsString().startsWith(doc.getPathAsString() + "/")
                        && !Framework.getService(NuxeoDriveManager.class).isSynchronizationRoot(session.getPrincipal(),
                                collection)) {
                    continue;
                }
                try {
                    parent = getFileSystemItemAdapterService().getFileSystemItem(collection, true,
                            relaxSyncRootConstraint, getLockInfo);
                } catch (RootlessItemException e) {
                    if (log.isTraceEnabled()) {
                        log.trace(String.format(
                                "The collection %s (%s) of which doc %s (%s) is a member cannot be adapted as a FileSystemItem.",
                                collection.getPathAsString(), collection.getId(), doc.getPathAsString(), doc.getId()));
                    }
                    continue;
                }
            }
            if (parent == null) {
                if (log.isTraceEnabled()) {
                    log.trace(String.format(
                            "None of the collections of which doc %s (%s) is a member can be adapted as a FileSystemItem.",
                            doc.getPathAsString(), doc.getId()));
                }
                return false;
            }
            if (log.isTraceEnabled()) {
                log.trace(String.format(
                        "Using first collection %s (%s) of which doc %s (%s) is a member and that is adaptable as a FileSystemItem as a parent FileSystemItem.",
                        collection.getPathAsString(), collection.getId(), doc.getPathAsString(), doc.getId()));
            }

            parentId = parent.getId();
            path = parent.getPath() + '/' + id;
            return true;
        }
    }

    protected AbstractDocumentBackedFileSystemItem(String factoryName, FolderItem parentItem, DocumentModel doc,
            boolean relaxSyncRootConstraint) {
        this(factoryName, parentItem, doc, relaxSyncRootConstraint, true);
    }

    protected AbstractDocumentBackedFileSystemItem(String factoryName, FolderItem parentItem, DocumentModel doc,
            boolean relaxSyncRootConstraint, boolean getLockInfo) {

        super(factoryName, doc.getCoreSession().getPrincipal(), relaxSyncRootConstraint);

        // Backing DocumentModel attributes
        repositoryName = doc.getRepositoryName();
        docId = doc.getId();
        docPath = doc.getPathAsString();
        docTitle = doc.getTitle();

        // FileSystemItem attributes
        id = computeId(docId);
        creator = (String) doc.getPropertyValue("dc:creator");
        lastContributor = (String) doc.getPropertyValue("dc:lastContributor");
        creationDate = (Calendar) doc.getPropertyValue("dc:created");
        lastModificationDate = (Calendar) doc.getPropertyValue("dc:modified");
        CoreSession docSession = doc.getCoreSession();
        canRename = !doc.hasFacet(FacetNames.PUBLISH_SPACE) && !doc.isProxy()
                && docSession.hasPermission(doc.getRef(), SecurityConstants.WRITE_PROPERTIES);
        DocumentRef parentRef = doc.getParentRef();
        canDelete = !doc.hasFacet(FacetNames.PUBLISH_SPACE) && !doc.isProxy()
                && docSession.hasPermission(doc.getRef(), SecurityConstants.REMOVE);
        if (canDelete && Framework.getService(ConfigurationService.class)
                                  .isBooleanPropertyFalse(PERMISSION_CHECK_OPTIMIZED_PROPERTY)) {
            // In non optimized mode check RemoveChildren on the parent
            canDelete = parentRef == null || docSession.hasPermission(parentRef, SecurityConstants.REMOVE_CHILDREN);
        }
        if (getLockInfo) {
            lockInfo = doc.getLockInfo();
        }

        String parentPath;
        if (parentItem != null) {
            parentId = parentItem.getId();
            parentPath = parentItem.getPath();
        } else {
            parentId = null;
            parentPath = "";
        }
        path = parentPath + '/' + id;
    }

    protected AbstractDocumentBackedFileSystemItem() {
        // Needed for JSON deserialization
    }

    /*--------------------- FileSystemItem ---------------------*/
    @Override
    public void delete() {
        try (CloseableCoreSession session = CoreInstance.openCoreSession(repositoryName, principal)) {
            DocumentModel doc = getDocument(session);
            FileSystemItemFactory parentFactory = getFileSystemItemAdapterService().getFileSystemItemFactoryForId(
                    parentId);
            // Handle removal from a collection sync root
            if (CollectionSyncRootFolderItemFactory.FACTORY_NAME.equals(parentFactory.getName())) {
                String[] idFragments = parseFileSystemId(parentId);
                String parentRepositoryName = idFragments[1];
                String parentDocId = idFragments[2];
                if (!parentRepositoryName.equals(repositoryName)) {
                    throw new UnsupportedOperationException(String.format(
                            "Found collection member: %s [repo=%s] in a different repository from the collection one: %s [repo=%s].",
                            doc, repositoryName, parentDocId, parentRepositoryName));
                }
                DocumentModel collection = getDocumentById(parentDocId, session);
                Framework.getService(CollectionManager.class).removeFromCollection(collection, doc, session);
            } else {
                List<DocumentModel> docs = new ArrayList<DocumentModel>();
                docs.add(doc);
                getTrashService().trashDocuments(docs);
            }
        }
    }

    @Override
    public boolean canMove(FolderItem dest) {
        // Check source doc deletion
        if (!canDelete) {
            return false;
        }
        // Check add children on destination doc
        AbstractDocumentBackedFileSystemItem docBackedDest = (AbstractDocumentBackedFileSystemItem) dest;
        String destRepoName = docBackedDest.getRepositoryName();
        DocumentRef destDocRef = new IdRef(docBackedDest.getDocId());
        String sessionRepo = repositoryName;
        // If source and destination repository are different, use a core
        // session bound to the destination repository
        if (!repositoryName.equals(destRepoName)) {
            sessionRepo = destRepoName;
        }
        try (CloseableCoreSession session = CoreInstance.openCoreSession(sessionRepo, principal)) {
            if (!session.hasPermission(destDocRef, SecurityConstants.ADD_CHILDREN)) {
                return false;
            }
            return true;
        }
    }

    @Override
    public FileSystemItem move(FolderItem dest) {
        DocumentRef sourceDocRef = new IdRef(docId);
        AbstractDocumentBackedFileSystemItem docBackedDest = (AbstractDocumentBackedFileSystemItem) dest;
        String destRepoName = docBackedDest.getRepositoryName();
        DocumentRef destDocRef = new IdRef(docBackedDest.getDocId());
        // If source and destination repository are different, delete source and
        // create doc in destination
        if (repositoryName.equals(destRepoName)) {
            try (CloseableCoreSession session = CoreInstance.openCoreSession(repositoryName, principal)) {
                DocumentModel movedDoc = session.move(sourceDocRef, destDocRef, null);
                session.save();
                return getFileSystemItemAdapterService().getFileSystemItem(movedDoc, dest);
            }
        } else {
            // TODO: implement move to another repository
            throw new UnsupportedOperationException("Multi repository move is not supported yet.");
        }
    }

    /*--------------------- Protected -------------------------*/
    protected final String computeId(String docId) {
        StringBuilder sb = new StringBuilder();
        sb.append(super.getId());
        sb.append(repositoryName);
        sb.append(FILE_SYSTEM_ITEM_ID_SEPARATOR);
        sb.append(docId);
        return sb.toString();
    }

    protected String getRepositoryName() {
        return repositoryName;
    }

    protected String getDocId() {
        return docId;
    }

    protected String getDocPath() {
        return docPath;
    }

    protected DocumentModel getDocument(CoreSession session) {
        return session.getDocument(new IdRef(docId));
    }

    protected DocumentModel getDocumentById(String docId, CoreSession session) {
        return session.getDocument(new IdRef(docId));
    }

    protected void updateLastModificationDate(DocumentModel doc) {
        lastModificationDate = (Calendar) doc.getPropertyValue("dc:modified");
    }

    protected TrashService getTrashService() {
        return Framework.getService(TrashService.class);
    }

    protected String[] parseFileSystemId(String id) {

        // Parse id, expecting pattern:
        // fileSystemItemFactoryName#repositoryName#docId
        String[] idFragments = id.split(FILE_SYSTEM_ITEM_ID_SEPARATOR);
        if (idFragments.length != 3) {
            throw new IllegalArgumentException(String.format(
                    "FileSystemItem id %s is not valid. Should match the 'fileSystemItemFactoryName#repositoryName#docId' pattern.",
                    id));
        }
        return idFragments;
    }

    /*---------- Needed for JSON deserialization ----------*/
    @Override
    protected void setId(String id) {
        super.setId(id);
        String[] idFragments = parseFileSystemId(id);
        this.factoryName = idFragments[0];
        this.repositoryName = idFragments[1];
        this.docId = idFragments[2];
    }

}
