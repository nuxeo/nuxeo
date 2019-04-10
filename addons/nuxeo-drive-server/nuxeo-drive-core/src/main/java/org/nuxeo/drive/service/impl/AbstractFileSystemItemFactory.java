/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.impl;

import java.security.Principal;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.AbstractFileSystemItem;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.FileSystemItemManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.model.NoSuchDocumentException;
import org.nuxeo.runtime.api.Framework;

/**
 * Base class for {@link FileSystemItemFactory} implementers. It is
 * {@link DocumentModel} backed.
 *
 * @author Antoine Taillefer
 * @see DefaultFileSystemItemFactory
 */
public abstract class AbstractFileSystemItemFactory implements
        FileSystemItemFactory {

    private static final Log log = LogFactory.getLog(AbstractFileSystemItemFactory.class);

    protected String name;

    /*--------------------------- FileSystemItemFactory ---------------------*/
    @Override
    public abstract void handleParameters(Map<String, String> parameters)
            throws ClientException;

    @Override
    public abstract boolean isFileSystemItem(DocumentModel doc,
            boolean includeDeleted, boolean relaxSyncRootConstraint)
            throws ClientException;

    /**
     * Adapts the given {@link DocumentModel} to a {@link FileSystemItem}.
     *
     * @see #getFileSystemItem(DocumentModel, boolean, String, boolean)
     */
    protected abstract FileSystemItem adaptDocument(DocumentModel doc,
            boolean forceParentItem, FolderItem parentItem,
            boolean relaxSyncRootConstraint) throws ClientException;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isFileSystemItem(DocumentModel doc) throws ClientException {
        return isFileSystemItem(doc, false);
    }

    @Override
    public boolean isFileSystemItem(DocumentModel doc, boolean includeDeleted)
            throws ClientException {
        return isFileSystemItem(doc, includeDeleted, false);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc)
            throws ClientException {
        return getFileSystemItem(doc, false);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc,
            boolean includeDeleted) throws ClientException {
        return getFileSystemItem(doc, false, null, includeDeleted, false);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc,
            boolean includeDeleted, boolean relaxSyncRootConstraint)
            throws ClientException {
        return getFileSystemItem(doc, false, null, includeDeleted,
                relaxSyncRootConstraint);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc,
            FolderItem parentItem) throws ClientException {
        return getFileSystemItem(doc, parentItem, false);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc,
            FolderItem parentItem, boolean includeDeleted)
            throws ClientException {
        return getFileSystemItem(doc, true, parentItem, includeDeleted, false);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc,
            FolderItem parentItem, boolean includeDeleted,
            boolean relaxSyncRootConstraint) throws ClientException {
        return getFileSystemItem(doc, true, parentItem, includeDeleted,
                relaxSyncRootConstraint);
    }

    @Override
    public boolean canHandleFileSystemItemId(String id) {
        try {
            parseFileSystemId(id);
        } catch (ClientException e) {
            log.trace(e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * The default factory considers that a {@link FileSystemItem} with the
     * given id exists if the backing {@link DocumentModel} can be fetched and
     * {@link #isFileSystemItem(DocumentModel)} returns true.
     *
     * @see #isFileSystemItem(DocumentModel)
     */
    @Override
    public boolean exists(String id, Principal principal)
            throws ClientException {
        try {
            DocumentModel doc = getDocumentByFileSystemId(id, principal);
            return isFileSystemItem(doc);
        } catch (ClientException e) {
            if (e.getCause() instanceof NoSuchDocumentException) {
                log.debug(String.format(
                        "No doc related to id %s, returning false.", id));
                return false;
            } else {
                throw e;
            }
        }
    }

    @Override
    public FileSystemItem getFileSystemItemById(String id, Principal principal)
            throws ClientException {
        try {
            DocumentModel doc = getDocumentByFileSystemId(id, principal);
            return getFileSystemItem(doc);
        } catch (ClientException e) {
            if (e.getCause() instanceof NoSuchDocumentException) {
                log.debug(String.format(
                        "No doc related to id %s, returning null.", id));
                return null;
            } else {
                throw e;
            }
        }

    }

    @Override
    public FileSystemItem getFileSystemItemById(String id, String parentId,
            Principal principal) throws ClientException {
        try {
            FileSystemItem parentItem = Framework.getService(
                    FileSystemItemAdapterService.class).getFileSystemItemFactoryForId(
                    parentId).getFileSystemItemById(parentId, principal);
            if (!(parentItem instanceof FolderItem)) {
                throw new ClientException(String.format(
                        "FileSystemItem with id %s should be a FolderItem",
                        parentId));
            }
            DocumentModel doc = getDocumentByFileSystemId(id, principal);
            return getFileSystemItem(doc, (FolderItem) parentItem);
        } catch (ClientException e) {
            if (e.getCause() instanceof NoSuchDocumentException) {
                log.debug(String.format(
                        "No doc related to id %s, returning null.", id));
                return null;
            } else {
                throw e;
            }
        }

    }

    @Override
    public DocumentModel getDocumentByFileSystemId(String id,
            Principal principal) throws ClientException {
        // Parse id, expecting
        // pattern:fileSystemItemFactoryName#repositoryName#docId
        String[] idFragments = parseFileSystemId(id);
        String repositoryName = idFragments[1];
        String docId = idFragments[2];
        CoreSession session = Framework.getLocalService(
                FileSystemItemManager.class).getSession(repositoryName,
                principal);
        return getDocumentById(docId, session);
    }

    /*--------------------------- Protected ---------------------------------*/
    protected FileSystemItem adaptDocument(DocumentModel doc,
            boolean forceParentItem, FolderItem parentItem)
            throws ClientException {
        return adaptDocument(doc, forceParentItem, parentItem, false);
    }

    protected FileSystemItem getFileSystemItem(DocumentModel doc,
            boolean forceParentItem, FolderItem parentItem,
            boolean includeDeleted, boolean relaxSyncRootConstraint)
            throws ClientException {

        // If the doc is not adaptable as a FileSystemItem return null
        if (!isFileSystemItem(doc, includeDeleted, relaxSyncRootConstraint)) {
            log.trace(String.format(
                    "Document %s cannot be adapted as a FileSystemItem => returning null.",
                    doc.getId()));
            return null;
        }
        return adaptDocument(doc, forceParentItem, parentItem,
                relaxSyncRootConstraint);
    }

    protected String[] parseFileSystemId(String id) throws ClientException {

        // Parse id, expecting pattern:
        // fileSystemItemFactoryName#repositoryName#docId
        String[] idFragments = id.split(AbstractFileSystemItem.FILE_SYSTEM_ITEM_ID_SEPARATOR);
        if (idFragments.length != 3) {
            throw new ClientException(
                    String.format(
                            "FileSystemItem id %s cannot be handled by factory named %s. Should match the 'fileSystemItemFactoryName#repositoryName#docId' pattern.",
                            id, name));
        }

        // Check if factory name matches
        String factoryName = idFragments[0];
        if (!name.equals(factoryName)) {
            throw new ClientException(
                    String.format(
                            "Factoy name [%s] parsed from id %s does not match the actual factory name [%s].",
                            factoryName, id, name));
        }
        return idFragments;
    }

    protected DocumentModel getDocumentById(String docId, CoreSession session)
            throws ClientException {
        return session.getDocument(new IdRef(docId));
    }

}
