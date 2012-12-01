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
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Dummy folder implementation of a {@link FileSystemItemFactory} for test
 * purpose.
 *
 * @author Antoine Taillefer
 */
public class DummyFolderItemFactory implements FileSystemItemFactory {

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc) {
        return new DummyFolderItem(doc);
    }

}
