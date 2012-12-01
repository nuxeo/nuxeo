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
package org.nuxeo.drive.service.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.impl.DocumentBackedFileItem;
import org.nuxeo.drive.adapter.impl.DocumentBackedFolderItem;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

/**
 * Default implementation of a {@link FileSystemItemFactory}. It is
 * {@link DocumentModel} backed and is the one used by Nuxeo Drive.
 *
 * @author Antoine Taillefer
 */
public class DefaultFileSystemItemFactory implements FileSystemItemFactory {

    private static final Log log = LogFactory.getLog(DefaultFileSystemItemFactory.class);

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc)
            throws ClientException {
        if (doc.isFolder()) {
            return new DocumentBackedFolderItem(doc);
        }
        if (hasBlob(doc)) {
            return new DocumentBackedFileItem(doc);
        }
        log.debug(String.format(
                "Document %s is not Folderish nor a BlobHolder with a blob, it cannot be adapted as a FileSystemItem => returning null.",
                doc.getId()));
        return null;
    }

    protected boolean hasBlob(DocumentModel doc) throws ClientException {
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh == null) {
            log.debug(String.format("Document %s is not a BlobHolder.",
                    doc.getId()));
            return false;
        }
        Blob blob = bh.getBlob();
        if (blob == null) {
            log.debug(String.format(
                    "Document %s is a BlobHolder without a blob.", doc.getId()));
            return false;
        }
        return true;
    }

}
