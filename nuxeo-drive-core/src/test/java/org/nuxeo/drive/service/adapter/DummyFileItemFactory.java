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
package org.nuxeo.drive.service.adapter;

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.impl.DefaultFileSystemItemFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * Dummy file implementation of a {@link FileSystemItemFactory} for test purpose.
 *
 * @author Antoine Taillefer
 */
public class DummyFileItemFactory extends DefaultFileSystemItemFactory {

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc) {
        return new DummyFileItem(this, doc);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted) {
        return getFileSystemItem(doc);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted,
            boolean relaxSyncRootConstraint) {
        return getFileSystemItem(doc);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted, boolean relaxSyncRootConstraint,
            boolean getLockInfo) {
        return getFileSystemItem(doc);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem) {
        return new DummyFileItem(this, parentItem, doc);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem, boolean includeDeleted) {
        return getFileSystemItem(doc, parentItem);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem, boolean includeDeleted,
            boolean relaxSyncRootConstraint) {
        return getFileSystemItem(doc, parentItem);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem, boolean includeDeleted,
            boolean relaxSyncRootConstraint, boolean getLockInfo) {
        return getFileSystemItem(doc, parentItem);
    }

    @Override
    public boolean canHandleFileSystemItemId(String id) {
        return false;
    }

    @Override
    public FileSystemItem getFileSystemItemById(String id, NuxeoPrincipal principal) {
        return null;
    }

}
