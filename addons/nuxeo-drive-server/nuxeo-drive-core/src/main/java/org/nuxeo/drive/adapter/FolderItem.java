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
package org.nuxeo.drive.adapter;

import java.util.List;
import java.util.concurrent.Semaphore;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.nuxeo.drive.adapter.impl.DocumentBackedFolderItem;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Representation of a folder.
 * <p>
 * In the case of a {@link DocumentModel} backed implementation, the backing document is Folderish. Typically a Folder
 * or a Workspace.
 *
 * @author Antoine Taillefer
 * @see DocumentBackedFolderItem
 */
public interface FolderItem extends FileSystemItem {

    @JsonIgnore
    List<FileSystemItem> getChildren();

    /**
     * Returns {@code true} if the {@link #scrollDescendants(String, int, long)} API can be used.
     *
     * @since 8.3
     */
    boolean getCanScrollDescendants();

    /**
     * Retrieves at most {@code batchSize} {@link FileSystemItem} descendants for the given {@code scrollId}.
     * <p>
     * When passing a null {@code scrollId} the initial search request is executed and the first batch of results is
     * returned along with a {@code scrollId} which should be passed to the next call in order to retrieve the next
     * batch of results.
     * <p>
     * Ideally, the search context made available by the initial search request is kept alive during {@code keepAlive}
     * milliseconds if {@code keepAlive} is positive.
     * <p>
     * Results are not necessarily sorted.
     * <p>
     * This method is protected by a {@link Semaphore}, made available by
     * {@link FileSystemItemAdapterService#getScrollBatchSemaphore()}, to limit the number of concurrent executions and
     * avoid too much memory pressure.
     *
     * @throws UnsupportedOperationException if {@link #getCanScrollDescendants()} returns {@code false}.
     * @since 8.3
     */
    @JsonIgnore
    ScrollFileSystemItemList scrollDescendants(String scrollId, int batchSize, long keepAlive);

    boolean getCanCreateChild();

    /**
     * @deprecated since 9.1, use {@link #createFile(String, boolean)} instead
     */
    @Deprecated
    default FileItem createFile(Blob blob) {
        return createFile(blob, false);
    }

    /**
     * @param overwrite allows to overwrite an existing file with the same title
     * @since 9.1
     */
    FileItem createFile(Blob blob, boolean overwrite);

    /**
     * @deprecated since 9.1, use {@link #createFolder(String, boolean)} instead
     */
    @Deprecated
    default FolderItem createFolder(String name) {
        return createFolder(name, false);
    }

    /**
     * @param overwrite allows to overwrite an existing folder with the same title
     * @since 9.1
     */
    FolderItem createFolder(String name, boolean overwrite);

}
