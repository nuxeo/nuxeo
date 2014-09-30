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
package org.nuxeo.drive.service;

import java.security.Principal;
import java.util.Map;

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.service.impl.AbstractFileSystemItemFactory;
import org.nuxeo.drive.service.impl.DefaultFileSystemItemFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Interface for the classes contributed to the {@code fileSystemItemFactory}
 * extension point of the {@link FileSystemItemAdapterService}.
 * <p>
 * Allows to get a {@link FileSystemItem} for a given {@link DocumentModel} or a
 * given {@link FileSystemItem} id.
 *
 * @author Antoine Taillefer
 * @see AbstractFileSystemItemFactory
 * @see DefaultFileSystemItemFactory
 * @see VersioningFileSystemItemFactory
 * @see TopLevelFolderItemFactory
 */
public interface FileSystemItemFactory {

    /**
     * Gets the factory unique name.
     */
    String getName();

    /**
     * Sets the factory unique name.
     */
    void setName(String name);

    /**
     * Handles the factory parameters contributed through the
     * {@code fileSystemItemFactory} contribution.
     */
    void handleParameters(Map<String, String> parameters)
            throws ClientException;

    /**
     * Returns true if the given {@link DocumentModel} is adaptable as a
     * {@link FileSystemItem}.
     *
     * @see #isFileSystemItem(DocumentModel, boolean)
     */
    boolean isFileSystemItem(DocumentModel doc) throws ClientException;

    /**
     * Returns true if the given {@link DocumentModel} is adaptable as a
     * {@link FileSystemItem}. If {@code includeDeleted} is true no filter is
     * applied on the "deleted" life cycle state, else if the document is in
     * this state it is not considered as adaptable as a {@link FileSystemItem},
     * thus the method returns false.
     */
    boolean isFileSystemItem(DocumentModel doc, boolean includeDeleted)
            throws ClientException;

    /**
     * Returns true if the given {@link DocumentModel} is adaptable as a
     * {@link FileSystemItem}.
     * <p>
     * If {@code includeDeleted} is true no filter is applied on the "deleted"
     * life cycle state, else if the document is in this state it is not
     * considered as adaptable as a {@link FileSystemItem}, thus the method
     * returns false.
     * <p>
     * If {@code relaxSyncRootConstraint} is true no filter is applied on the
     * synchronization root aspect for the current user.
     */
    boolean isFileSystemItem(DocumentModel doc, boolean includeDeleted,
            boolean relaxSyncRootConstraint) throws ClientException;

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel}.
     *
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel}
     *         is not adaptable as a {@link FileSystemItem}
     * @see #isFileSystemItem(DocumentModel)
     * @see #getFileSystemItem(DocumentModel, boolean)
     */
    FileSystemItem getFileSystemItem(DocumentModel doc) throws ClientException;

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel}. If
     * {@code includeDeleted} is true no filter is applied on the "deleted" life
     * cycle state, else if the document is in this state it is not considered
     * as adaptable as a {@link FileSystemItem}, thus the method returns null.
     *
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel}
     *         is not adaptable as a {@link FileSystemItem}
     * @see #isFileSystemItem(DocumentModel, boolean))
     */
    FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted)
            throws ClientException;

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel}.
     * <p>
     * If {@code includeDeleted} is true no filter is applied on the "deleted"
     * life cycle state, else if the document is in this state it is not
     * considered as adaptable as a {@link FileSystemItem}, thus the method
     * returns null.
     * <p>
     * If {@code relaxSyncRootConstraint} is true no filter is applied on the
     * synchronization root aspect for the current user.
     *
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel}
     *         is not adaptable as a {@link FileSystemItem}
     * @see #isFileSystemItem(DocumentModel, boolean, boolean))
     */
    FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted,
            boolean relaxSyncRootConstraint) throws ClientException;

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel}
     * forcing its parent id with the given id.
     *
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel}
     *         is not adaptable as a {@link FileSystemItem}
     * @see #isFileSystemItem(DocumentModel)
     * @see #getFileSystemItem(DocumentModel, String, boolean)
     */
    FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem)
            throws ClientException;

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel}
     * forcing its parent id with the given id. If {@code includeDeleted} is
     * true no filter is applied on the "deleted" life cycle state, else if the
     * document is in this state it is not considered as adaptable as a
     * {@link FileSystemItem}, thus the method returns null.
     *
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel}
     *         is not adaptable as a {@link FileSystemItem}
     * @see #isFileSystemItem(DocumentModel, boolean)
     */
    FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem,
            boolean includeDeleted) throws ClientException;

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel}
     * forcing its parent id with the given id.
     * <p>
     * If {@code includeDeleted} is true no filter is applied on the "deleted"
     * life cycle state, else if the document is in this state it is not
     * considered as adaptable as a {@link FileSystemItem}, thus the method
     * returns null.
     * <p>
     * If {@code relaxSyncRootConstraint} is true no filter is applied on the
     * synchronization root aspect for the current user.
     *
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel}
     *         is not adaptable as a {@link FileSystemItem}
     * @see #isFileSystemItem(DocumentModel, boolean, boolean)
     */
    FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem,
            boolean includeDeleted, boolean relaxSyncRootConstraint)
            throws ClientException;

    /**
     * Returns true if the given {@link FileSystemItem} id can be handled by
     * this factory. It is typically the case when the factory has been
     * responsible for generating the {@link FileSystemItem}.
     */
    boolean canHandleFileSystemItemId(String id);

    /**
     * Returns true if a {@link FileSystemItem} with the given id exists for the
     * given principal.
     */
    boolean exists(String id, Principal principal) throws ClientException;

    /**
     * Gets the {@link FileSystemItem} with the given id using a core session
     * fetched with the given principal.
     *
     * @return the {@link FileSystemItem} or null if none matches the given id
     */
    FileSystemItem getFileSystemItemById(String id, Principal principal)
            throws ClientException;

    /**
     * Gets the {@link FileSystemItem} with the given id and parent id using a
     * core session fetched with the given principal.
     *
     * @return the {@link FileSystemItem} or null if none matches the given id
     *         and parent id
     * @since 5.9.6
     */
    FileSystemItem getFileSystemItemById(String id, String parentId,
            Principal principal) throws ClientException;

    /**
     * Gets the {@link DocumentModel} bound to the given {@link FileSystemItem}
     * id using a core session fetched with the given principal.
     *
     * @return the {@link DocumentModel}
     * @since 5.9.6
     */
    DocumentModel getDocumentByFileSystemId(String id, Principal principal)
            throws ClientException;

}
