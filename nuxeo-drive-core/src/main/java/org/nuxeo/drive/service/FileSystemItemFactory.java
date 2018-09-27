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
package org.nuxeo.drive.service;

import java.security.Principal;
import java.util.Map;

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.service.impl.AbstractFileSystemItemFactory;
import org.nuxeo.drive.service.impl.DefaultFileSystemItemFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Lock;

/**
 * Interface for the classes contributed to the {@code fileSystemItemFactory} extension point of the
 * {@link FileSystemItemAdapterService}.
 * <p>
 * Allows to get a {@link FileSystemItem} for a given {@link DocumentModel} or a given {@link FileSystemItem} id.
 *
 * @author Antoine Taillefer
 * @see AbstractFileSystemItemFactory
 * @see DefaultFileSystemItemFactory
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
     * Handles the factory parameters contributed through the {@code fileSystemItemFactory} contribution.
     */
    void handleParameters(Map<String, String> parameters);

    /**
     * Returns true if the given {@link DocumentModel} is adaptable as a {@link FileSystemItem}.
     *
     * @see #isFileSystemItem(DocumentModel, boolean)
     */
    boolean isFileSystemItem(DocumentModel doc);

    /**
     * Returns true if the given {@link DocumentModel} is adaptable as a {@link FileSystemItem}. If
     * {@code includeDeleted} is true no filter is applied on the "trashed" state, else if the document is in this state
     * it is not considered as adaptable as a {@link FileSystemItem}, thus the method returns false.
     */
    boolean isFileSystemItem(DocumentModel doc, boolean includeDeleted);

    /**
     * Returns true if the given {@link DocumentModel} is adaptable as a {@link FileSystemItem}.
     * <p>
     * If {@code includeDeleted} is true no filter is applied on the "trashed" state, else if the document is in this
     * state it is not considered as adaptable as a {@link FileSystemItem}, thus the method returns false.
     * <p>
     * If {@code relaxSyncRootConstraint} is true no filter is applied on the synchronization root aspect for the
     * current user.
     */
    boolean isFileSystemItem(DocumentModel doc, boolean includeDeleted, boolean relaxSyncRootConstraint);

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel}.
     *
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel} is not adaptable as a
     *         {@link FileSystemItem}
     * @see #isFileSystemItem(DocumentModel)
     * @see #getFileSystemItem(DocumentModel, boolean)
     */
    FileSystemItem getFileSystemItem(DocumentModel doc);

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel}. If {@code includeDeleted} is true no filter
     * is applied on the "trashed" state, else if the document is in the trash it is not considered as adaptable as a
     * {@link FileSystemItem}, thus the method returns null.
     *
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel} is not adaptable as a
     *         {@link FileSystemItem}
     * @see #isFileSystemItem(DocumentModel, boolean)
     */
    FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted);

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel}.
     * <p>
     * If {@code includeDeleted} is true no filter is applied on the "trashed" state, else if the document is in this
     * state it is not considered as adaptable as a {@link FileSystemItem}, thus the method returns null.
     * <p>
     * If {@code relaxSyncRootConstraint} is true no filter is applied on the synchronization root aspect for the
     * current user.
     *
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel} is not adaptable as a
     *         {@link FileSystemItem}
     * @see #isFileSystemItem(DocumentModel, boolean, boolean)
     */
    FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted, boolean relaxSyncRootConstraint);

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel}.
     * <p>
     * If {@code includeDeleted} is true no filter is applied on the "trashed" state, else if the document is in this
     * state it is not considered as adaptable as a {@link FileSystemItem}, thus the method returns null.
     * <p>
     * If {@code relaxSyncRootConstraint} is true no filter is applied on the synchronization root aspect for the
     * current user.
     * <p>
     * If {@code getLockInfo} is true the {@link Lock} is fetched from the {@link DocumentModel} and set on the returned
     * {@link FileSystemItem}.
     *
     * @since 8.3
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel} is not adaptable as a
     *         {@link FileSystemItem}
     * @see #isFileSystemItem(DocumentModel, boolean, boolean)
     */
    FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted, boolean relaxSyncRootConstraint,
            boolean getLockInfo);

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel} forcing its parent with the given
     * {@code parentItem}.
     *
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel} is not adaptable as a
     *         {@link FileSystemItem}
     * @see #getFileSystemItem(DocumentModel, FolderItem, boolean)
     */
    FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem);

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel} forcing its parent with the given
     * {@code parentItem}. If {@code includeDeleted} is true no filter is applied on the "trashed" state, else if the
     * document is in the trash it is not considered as adaptable as a {@link FileSystemItem}, thus the method returns
     * null.
     *
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel} is not adaptable as a
     *         {@link FileSystemItem}
     */
    FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem, boolean includeDeleted);

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel} forcing its parent with the given
     * {@code parentItem}.
     * <p>
     * If {@code includeDeleted} is true no filter is applied on the "trashed" state, else if the document is in this
     * state it is not considered as adaptable as a {@link FileSystemItem}, thus the method returns null.
     * <p>
     * If {@code relaxSyncRootConstraint} is true no filter is applied on the synchronization root aspect for the
     * current user.
     *
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel} is not adaptable as a
     *         {@link FileSystemItem}
     */
    FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem, boolean includeDeleted,
            boolean relaxSyncRootConstraint);

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel} forcing its parent with the given
     * {@code parentItem}.
     * <p>
     * If {@code includeDeleted} is true no filter is applied on the "trashed" state, else if the document is in this
     * state it is not considered as adaptable as a {@link FileSystemItem}, thus the method returns null.
     * <p>
     * If {@code relaxSyncRootConstraint} is true no filter is applied on the synchronization root aspect for the
     * current user.
     * <p>
     * If {@code getLockInfo} is true the {@link Lock} is fetched from the {@link DocumentModel} and set on the returned
     * {@link FileSystemItem}.
     *
     * @since 8.3
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel} is not adaptable as a
     *         {@link FileSystemItem}
     */
    FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem, boolean includeDeleted,
            boolean relaxSyncRootConstraint, boolean getLockInfo);

    /**
     * Returns true if the given {@link FileSystemItem} id can be handled by this factory. It is typically the case when
     * the factory has been responsible for generating the {@link FileSystemItem}.
     */
    boolean canHandleFileSystemItemId(String id);

    /**
     * Returns true if a {@link FileSystemItem} with the given id exists for the given principal.
     */
    boolean exists(String id, Principal principal);

    /**
     * Gets the {@link FileSystemItem} with the given id using a core session fetched with the given principal.
     *
     * @return the {@link FileSystemItem} or null if none matches the given id
     */
    FileSystemItem getFileSystemItemById(String id, Principal principal);

    /**
     * Gets the {@link FileSystemItem} with the given id and parent id using a core session fetched with the given
     * principal.
     *
     * @return the {@link FileSystemItem} or null if none matches the given id and parent id
     * @since 6.0
     */
    FileSystemItem getFileSystemItemById(String id, String parentId, Principal principal);

    /**
     * Gets the {@link DocumentModel} bound to the given {@link FileSystemItem} id using a core session fetched with the
     * given principal.
     *
     * @deprecated since 7.2
     * @return the {@link DocumentModel}
     * @since 6.0
     */
    @Deprecated
    DocumentModel getDocumentByFileSystemId(String id, Principal principal);

}
