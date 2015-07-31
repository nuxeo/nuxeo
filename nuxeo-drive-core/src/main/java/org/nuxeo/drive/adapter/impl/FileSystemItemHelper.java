/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.adapter.impl;

import java.io.IOException;

import org.apache.commons.codec.digest.DigestUtils;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.service.VersioningFileSystemItemFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.versioning.VersioningService;

/**
 * Helper for {@link FileSystemItem} manipulation.
 *
 * @author Antoine Taillefer
 */
public final class FileSystemItemHelper {

    public static final String MD5_DIGEST_ALGORITHM = "MD5";

    private FileSystemItemHelper() {
        // Helper class
    }

    /**
     * @since 7.4
     */
    public static void versionIfNeeded(VersioningFileSystemItemFactory factory, DocumentModel doc, CoreSession session)
            throws ClientException {
        if (factory.needsVersioning(doc)) {
            doc.putContextData(VersioningService.VERSIONING_OPTION, factory.getVersioningOption());
            session.saveDocument(doc);
        }
    }

    /**
     * Gets the md5 digest of the given blob.
     */
    public static String getMD5Digest(Blob blob) throws ClientException {
        try {
            return DigestUtils.md5Hex(blob.getStream());
        } catch (IOException e) {
            throw new ClientException(String.format("Error while computing digest for blob %s.", blob.getFilename()), e);
        }
    }

}
