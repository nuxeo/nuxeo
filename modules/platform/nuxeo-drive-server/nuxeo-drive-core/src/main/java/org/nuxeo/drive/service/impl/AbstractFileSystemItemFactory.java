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
package org.nuxeo.drive.service.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.AbstractFileSystemItem;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.runtime.api.Framework;

/**
 * Base class for {@link FileSystemItemFactory} implementers. It is {@link DocumentModel} backed.
 *
 * @author Antoine Taillefer
 * @see DefaultFileSystemItemFactory
 */
public abstract class AbstractFileSystemItemFactory implements FileSystemItemFactory {

    private static final Logger log = LogManager.getLogger(AbstractFileSystemItemFactory.class);

    protected String name;

    /**
     * Adapts the given {@link DocumentModel} to a {@link FileSystemItem}.
     *
     * @see #getFileSystemItem(DocumentModel, boolean, FolderItem, boolean, boolean, boolean)
     */
    protected abstract FileSystemItem adaptDocument(DocumentModel doc, boolean forceParentItem, FolderItem parentItem,
            boolean relaxSyncRootConstraint, boolean getLockInfo);

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isFileSystemItem(DocumentModel doc) {
        return isFileSystemItem(doc, false);
    }

    @Override
    public boolean isFileSystemItem(DocumentModel doc, boolean includeDeleted) {
        return isFileSystemItem(doc, includeDeleted, false);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc) {
        return getFileSystemItem(doc, false);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted) {
        return getFileSystemItem(doc, false, null, includeDeleted, false, true);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted,
            boolean relaxSyncRootConstraint) {
        return getFileSystemItem(doc, false, null, includeDeleted, relaxSyncRootConstraint, true);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted, boolean relaxSyncRootConstraint,
            boolean getLockInfo) {
        return getFileSystemItem(doc, false, null, includeDeleted, relaxSyncRootConstraint, getLockInfo);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem) {
        return getFileSystemItem(doc, parentItem, false);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem, boolean includeDeleted) {
        return getFileSystemItem(doc, true, parentItem, includeDeleted, false, true);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem, boolean includeDeleted,
            boolean relaxSyncRootConstraint) {
        return getFileSystemItem(doc, true, parentItem, includeDeleted, relaxSyncRootConstraint, true);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem, boolean includeDeleted,
            boolean relaxSyncRootConstraint, boolean getLockInfo) {
        return getFileSystemItem(doc, true, parentItem, includeDeleted, relaxSyncRootConstraint, getLockInfo);
    }

    @Override
    public boolean canHandleFileSystemItemId(String id) {
        try {
            parseFileSystemId(id);
        } catch (IllegalArgumentException e) {
            log.trace(e::getMessage);
            return false;
        }
        return true;
    }

    /**
     * The default factory considers that a {@link FileSystemItem} with the given id exists if the backing
     * {@link DocumentModel} can be fetched and {@link #isFileSystemItem(DocumentModel)} returns true.
     *
     * @see #isFileSystemItem(DocumentModel)
     */
    @Override
    public boolean exists(String id, NuxeoPrincipal principal) {
        String[] idFragments = parseFileSystemId(id);
        String repositoryName = idFragments[1];
        String docId = idFragments[2];
        try {
            CoreSession session = CoreInstance.getCoreSession(repositoryName, principal);
            DocumentModel doc = getDocumentById(docId, session);
            return isFileSystemItem(doc);
        } catch (DocumentNotFoundException e) {
            log.debug("No doc related to id {}, returning false.", docId);
            return false;
        } catch (DocumentSecurityException e) {
            log.debug("User {} cannot access doc {}, returning false.", principal, docId);
            return false;
        }
    }

    @Override
    public FileSystemItem getFileSystemItemById(String id, NuxeoPrincipal principal) {
        String[] idFragments = parseFileSystemId(id);
        String repositoryName = idFragments[1];
        String docId = idFragments[2];
        try {
            CoreSession session = CoreInstance.getCoreSession(repositoryName, principal);
            DocumentModel doc = getDocumentById(docId, session);
            return getFileSystemItem(doc);
        } catch (DocumentNotFoundException e) {

            log.debug("No doc related to id {}, returning null.", docId);
            return null;
        } catch (DocumentSecurityException e) {
            log.debug("User {} cannot access doc {}, returning null.", principal, docId);
            return null;
        }
    }

    @Override
    public FileSystemItem getFileSystemItemById(String id, String parentId, NuxeoPrincipal principal) {
        String[] idFragments = parseFileSystemId(id);
        String repositoryName = idFragments[1];
        String docId = idFragments[2];
        try {
            CoreSession session = CoreInstance.getCoreSession(repositoryName, principal);
            FileSystemItem parentItem = Framework.getService(FileSystemItemAdapterService.class)
                                                 .getFileSystemItemFactoryForId(parentId)
                                                 .getFileSystemItemById(parentId, principal);
            if (!(parentItem instanceof FolderItem)) {
                throw new NuxeoException(String.format("FileSystemItem with id %s should be a FolderItem", parentId));
            }
            DocumentModel doc = getDocumentById(docId, session);
            return getFileSystemItem(doc, (FolderItem) parentItem);
        } catch (DocumentNotFoundException e) {
            log.debug("No doc related to id {}, returning null.", docId);
            return null;
        } catch (DocumentSecurityException e) {
            log.debug("User {} cannot access doc {}, returning null.", principal, docId);
            return null;
        }

    }

    /*--------------------------- Protected ---------------------------------*/
    protected FileSystemItem adaptDocument(DocumentModel doc, boolean forceParentItem, FolderItem parentItem) {
        return adaptDocument(doc, forceParentItem, parentItem, false, true);
    }

    protected FileSystemItem getFileSystemItem(DocumentModel doc, boolean forceParentItem, FolderItem parentItem,
            boolean includeDeleted, boolean relaxSyncRootConstraint, boolean getLockInfo) {

        // If the doc is not adaptable as a FileSystemItem return null
        if (!isFileSystemItem(doc, includeDeleted, relaxSyncRootConstraint)) {
            log.trace("Document {} cannot be adapted as a FileSystemItem => returning null.", doc::getId);
            return null;
        }
        return adaptDocument(doc, forceParentItem, parentItem, relaxSyncRootConstraint, getLockInfo);
    }

    protected String[] parseFileSystemId(String id) {

        // Parse id, expecting pattern:
        // fileSystemItemFactoryName#repositoryName#docId
        String[] idFragments = id.split(AbstractFileSystemItem.FILE_SYSTEM_ITEM_ID_SEPARATOR);
        if (idFragments.length != 3) {
            throw new IllegalArgumentException(String.format(
                    "FileSystemItem id %s cannot be handled by factory named %s. Should match the 'fileSystemItemFactoryName#repositoryName#docId' pattern.",
                    id, name));
        }

        // Check if factory name matches
        String factoryName = idFragments[0];
        if (!name.equals(factoryName)) {
            throw new IllegalArgumentException(
                    String.format("Factoy name [%s] parsed from id %s does not match the actual factory name [%s].",
                            factoryName, id, name));
        }
        return idFragments;
    }

    protected DocumentModel getDocumentById(String docId, CoreSession session) {
        return session.getDocument(new IdRef(docId));
    }

}
