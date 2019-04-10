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
package org.nuxeo.drive.service.adapter;

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.DocumentBackedFileItem;
import org.nuxeo.drive.service.VersioningFileSystemItemFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Dummy file implementation of a {@link FileSystemItem} for test purpose.
 * 
 * @author Antoine Taillefer
 */
public class DummyFileItem extends DocumentBackedFileItem {

    private static final long serialVersionUID = 1L;

    public DummyFileItem(VersioningFileSystemItemFactory factory, DocumentModel doc) throws ClientException {
        super(factory, doc);
    }

    public DummyFileItem(VersioningFileSystemItemFactory factory, FolderItem parentItem, DocumentModel doc)
            throws ClientException {
        super(factory, parentItem, doc);
    }

    @Override
    public String getName() {
        return "Dummy file with id " + docId;
    }

}
