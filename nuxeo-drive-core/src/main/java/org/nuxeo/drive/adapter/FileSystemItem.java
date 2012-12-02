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
package org.nuxeo.drive.adapter;

import java.util.Calendar;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Representation of a file system item, typically a file or a folder.
 *
 * @author Antoine Taillefer
 * @see FileItem
 * @see FolderItem
 * @see AbstractDocumentBackedFileSystemItem
 */
public interface FileSystemItem {

    /**
     * Gets a unique id generated server-side.
     */
    String getId();

    /**
     * Gets the name displayed in the file system.
     */
    String getName() throws ClientException;

    boolean isFolder();

    String getCreator() throws ClientException;

    Calendar getCreationDate() throws ClientException;

    Calendar getLastModificationDate() throws ClientException;

    /**
     * Gets the backing {@link DocumentModel} in the case of a
     * {@link DocumentModel} backed implementation.
     *
     * @throws UnsupportedOperationException if the implementation is not
     *             {@link DocumentModel} backed.
     */
    DocumentModel getDocument();

}
