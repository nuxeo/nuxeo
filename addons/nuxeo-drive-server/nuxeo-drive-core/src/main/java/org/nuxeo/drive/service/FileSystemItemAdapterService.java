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

import java.util.Set;
import java.util.concurrent.Semaphore;

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
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel}. If the document is in the trash it is not
     * considered as adaptable as a {@link FileSystemItem}, thus the method returns null.
     *
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel} is not adaptable as a
     *         {@link FileSystemItem}
     * @see FileSystemItemFactory#getFileSystemItem(DocumentModel)
     */
    FileSystemItem getFileSystemItem(DocumentModel doc);

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel}. If {@code includeDeleted} is true no filter
     * is applied on the "trashed" state, else if the document is in the trash it is not considered as
     * adaptable as a {@link FileSystemItem}, thus the method returns null.
     *
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel} is not adaptable as a
     *         {@link FileSystemItem}
     * @see FileSystemItemFactory#getFileSystemItem(DocumentModel, boolean)
     */
    FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted);

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel}.
     * <p>
     * If {@code includeDeleted} is true no filter is applied on the "trashed" state, else if the document is
     * in the trash it is not considered as adaptable as a {@link FileSystemItem}, thus the method returns null.
     * <p>
     * If {@code relaxSyncRootConstraint} is true no filter is applied on the synchronization root aspect for the
     * current user.
     *
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel} is not adaptable as a
     *         {@link FileSystemItem}
     * @see FileSystemItemFactory#getFileSystemItem(DocumentModel, boolean, boolean)
     */
    FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted, boolean relaxSyncRootConstraint);

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel}.
     * <p>
     * If {@code includeDeleted} is true no filter is applied on the "trashed" state, else if the document is
     * in the trash it is not considered as adaptable as a {@link FileSystemItem}, thus the method returns null.
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
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel} forcing its parent with the given
     * {@code parentItem}. If the document is in the trash it is not considered as adaptable as a
     * {@link FileSystemItem}, thus the method returns null.
     *
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel} is not adaptable as a
     *         {@link FileSystemItem}
     * @see FileSystemItemFactory#getFileSystemItem(DocumentModel, FolderItem)
     */
    FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem);

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel} forcing its parent with the given
     * {@code parentItem}. If {@code includeDeleted} is true no filter is applied on the "trashed" state,
     * else if the document is in the trash it is not considered as adaptable as a {@link FileSystemItem}, thus the
     * method returns null.
     *
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel} is not adaptable as a
     *         {@link FileSystemItem}
     * @see FileSystemItemFactory#getFileSystemItem(DocumentModel, FolderItem, boolean)
     */
    FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem, boolean includeDeleted);

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel} forcing its parent with the given
     * {@code parentItem}.
     * <p>
     * If {@code includeDeleted} is true no filter is applied on the "trashed" state, else if the document is
     * in the trash it is not considered as adaptable as a {@link FileSystemItem}, thus the method returns null.
     * <p>
     * If {@code relaxSyncRootConstraint} is true no filter is applied on the synchronization root aspect for the
     * current user.
     *
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel} is not adaptable as a
     *         {@link FileSystemItem}
     * @see FileSystemItemFactory#getFileSystemItem(DocumentModel, FolderItem, boolean, boolean)
     */
    FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem, boolean includeDeleted,
            boolean relaxSyncRootConstraint);

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel} forcing its parent with the given
     * {@code parentItem}.
     * <p>
     * If {@code includeDeleted} is true no filter is applied on the "trashed" state, else if the document is
     * in the trash it is not considered as adaptable as a {@link FileSystemItem}, thus the method returns null.
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

    /**
     * Allows to limit the number of concurrent scrolls run by {@link FolderItem#scrollDescendants(String, int, long)}.
     *
     * @since 8.3
     */
    Semaphore getScrollBatchSemaphore();

}
