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

import org.nuxeo.drive.adapter.AbstractDocumentBackedFileSystemItem;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Dummy file implementation of a {@link FileSystemItem} for test purpose.
 *
 * @author Antoine Taillefer
 */
public class DummyFileItem extends AbstractDocumentBackedFileSystemItem {

    public DummyFileItem(DocumentModel doc) {
        super(doc);
    }

    @Override
    public String getName() throws ClientException {
        return "Dummy file with id " + getDocument().getId();
    }

    @Override
    public boolean isFolder() {
        return false;
    }

}
