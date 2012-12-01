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

import java.security.Principal;

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.service.impl.FileSystemItemAdapterServiceImpl;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Service for creating the right {@link FileSystemItem} adapter depending on
 * the {@link DocumentModel} type or facet.
 * <p>
 * Factories can be contributed to implement a specific behavior for the
 * {@link FileSystemItem} retrieval.
 *
 * @author Antoine Taillefer
 * @see FileSystemItemAdapterServiceImpl
 */
public interface FileSystemItemAdapterService {

    /**
     * Gets the {@link FileSystemItem} adapter for the given
     * {@link DocumentModel}.
     */
    FileSystemItem getFileSystemItemAdapter(DocumentModel doc)
            throws ClientException;

    /**
     * Gets the {@link FileSystemItem} with the given id for the given user.
     */
    FileSystemItem getFileSystemItemById(String id, Principal principal)
            throws ClientException;

}
