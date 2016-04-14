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

import java.util.Set;

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.service.impl.FileSystemItemAdapterServiceImpl;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Lock;

/**
 * Service for creating the right {@link FileSystemItem} adapter depending on the {@link DocumentModel} type or facet.
 * <p>
 * Factories can be contributed to implement a specific behavior for the {@link FileSystemItem} adapter creation.
 *
 * @author Antoine Taillefer
 * @see FileSystemItemAdapterServiceImpl
 */
public interface FileSystemItemAdapterService {

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel}. If the document is in the "deleted" life
     * cycle state it is not considered as adaptable as a {@link FileSystemItem}, thus the method returns null.
     *
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel} is not adaptable as a
     *         {@link FileSystemItem}
     * @see FileSystemItemFactory#getFileSystemItem(DocumentModel)
     */
    FileSystemItem getFileSystemItem(DocumentModel doc);

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel}. If {@code includeDeleted} is true no filter
     * is applied on the "deleted" life cycle state, else if the document is in this state it is not considered as
     * adaptable as a {@link FileSystemItem}, thus the method returns null.
     *
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel} is not adaptable as a
     *         {@link FileSystemItem}
     * @see FileSystemItemFactory#getFileSystemItem(DocumentModel)
     */
    FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted);

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel}.
     * <p>
     * If {@code includeDeleted} is true no filter is applied on the "deleted" life cycle state, else if the document is
     * in this state it is not considered as adaptable as a {@link FileSystemItem}, thus the method returns null.
     * <p>
     * If {@code relaxSyncRootConstraint} is true no filter is applied on the synchronization root aspect for the
     * current user.
     *
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel} is not adaptable as a
     *         {@link FileSystemItem}
     * @see FileSystemItemFactory#getFileSystemItem(DocumentModel)
     */
    FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted, boolean relaxSyncRootConstraint);

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel}.
     * <p>
     * If {@code includeDeleted} is true no filter is applied on the "deleted" life cycle state, else if the document is
     * in this state it is not considered as adaptable as a {@link FileSystemItem}, thus the method returns null.
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
     * @see FileSystemItemFactory#getFileSystemItem(DocumentModel, boolean, boolean, boolean)
     */
    FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted, boolean relaxSyncRootConstraint,
            boolean getLockInfo);

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel} forcing its parent id with the given id. If
     * the document is in the "deleted" life cycle state it is not considered as adaptable as a {@link FileSystemItem},
     * thus the method returns null.
     *
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel} is not adaptable as a
     *         {@link FileSystemItem}
     * @see FileSystemItemFactory#getFileSystemItem(DocumentModel, String)
     */
    FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem);

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel} forcing its parent id with the given id. If
     * {@code includeDeleted} is true no filter is applied on the "deleted" life cycle state, else if the document is in
     * this state it is not considered as adaptable as a {@link FileSystemItem}, thus the method returns null.
     *
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel} is not adaptable as a
     *         {@link FileSystemItem}
     * @see FileSystemItemFactory#getFileSystemItem(DocumentModel, String)
     */
    FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem, boolean includeDeleted);

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel} forcing its parent id with the given id.
     * <p>
     * If {@code includeDeleted} is true no filter is applied on the "deleted" life cycle state, else if the document is
     * in this state it is not considered as adaptable as a {@link FileSystemItem}, thus the method returns null.
     * <p>
     * If {@code relaxSyncRootConstraint} is true no filter is applied on the synchronization root aspect for the
     * current user.
     *
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel} is not adaptable as a
     *         {@link FileSystemItem}
     * @see FileSystemItemFactory#getFileSystemItem(DocumentModel, String)
     */
    FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem, boolean includeDeleted,
            boolean relaxSyncRootConstraint);

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel} forcing its parent with the given
     * {@code parentItem}.
     * <p>
     * If {@code includeDeleted} is true no filter is applied on the "deleted" life cycle state, else if the document is
     * in this state it is not considered as adaptable as a {@link FileSystemItem}, thus the method returns null.
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
     * @see FileSystemItemFactory#getFileSystemItem(DocumentModel, FolderItem, boolean, boolean, boolean)
     */
    FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem, boolean includeDeleted,
            boolean relaxSyncRootConstraint, boolean getLockInfo);

    /**
     * Gets the {@link FileSystemItemFactory} that can handle the the given {@link FileSystemItem} id.
     *
     * @see FileSystemItemFactory#canHandleFileSystemItemId(String)
     */
    FileSystemItemFactory getFileSystemItemFactoryForId(String id);

    /**
     * Gets the {@link TopLevelFolderItemFactory}.
     */
    TopLevelFolderItemFactory getTopLevelFolderItemFactory();

    /**
     * Gets the {@link VirtualFolderItemFactory} for the given factory name.
     */
    VirtualFolderItemFactory getVirtualFolderItemFactory(String factoryName);

    /**
     * Gets the active {@link FileSystemItem} factory names.
     */
    Set<String> getActiveFileSystemItemFactories();

}
