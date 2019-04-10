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

import org.codehaus.jackson.annotate.JsonIgnore;
import org.nuxeo.drive.adapter.impl.DocumentBackedFolderItem;
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

    boolean getCanCreateChild();

    FileItem createFile(Blob blob);

    FolderItem createFolder(String name);

}
