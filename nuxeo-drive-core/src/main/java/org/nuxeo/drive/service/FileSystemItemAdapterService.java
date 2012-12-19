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

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.service.impl.FileSystemItemAdapterServiceImpl;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Service for creating the right {@link FileSystemItem} adapter depending on
 * the {@link DocumentModel} type or facet.
 * <p>
 * Factories can be contributed to implement a specific behavior for the
 * {@link FileSystemItem} adapter creation.
 *
 * @author Antoine Taillefer
 * @see FileSystemItemAdapterServiceImpl
 */
public interface FileSystemItemAdapterService {

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel}.
     *
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel}
     *         is not adaptable as a {@link FileSystemItem}
     * @see FileSystemItemFactory#getFileSystemItem(DocumentModel)
     */
    FileSystemItem getFileSystemItem(DocumentModel doc) throws ClientException;

    /**
     * Gets the {@link FileSystemItem} for the given {@link DocumentModel} and
     * parent {@link FileSystemItem} id.
     *
     * @return the {@link FileSystemItem} or null if the {@link DocumentModel}
     *         is not adaptable as a {@link FileSystemItem}
     * @see FileSystemItemFactory#getFileSystemItem(DocumentModel, String)
     */
    FileSystemItem getFileSystemItem(DocumentModel doc, String parentId)
            throws ClientException;

    /**
     * Gets the {@link FileSystemItemFactory} that can handle the the given
     * {@link FileSystemItem} id.
     *
     * @throws ClientException if no {@link FileSystemItemFactory} can handle
     *             the given {@link FileSystemItem} id
     * @see FileSystemItemFactory#canHandleFileSystemItemId(String)
     */
    FileSystemItemFactory getFileSystemItemFactoryForId(String id)
            throws ClientException;

    /**
     * Gets the {@link TopLevelFolderItemFactory}.
     */
    TopLevelFolderItemFactory getTopLevelFolderItemFactory();

}
