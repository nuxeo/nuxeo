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
import org.apache.commons.lang.StringUtils;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * Helper for {@link FileSystemItem} manipulation.
 *
 * @author Antoine Taillefer
 */
public final class FileSystemItemHelper {

    public static final String MD5_DIGEST_ALGORITHM = "md5";

    private FileSystemItemHelper() {
        // Helper class
    }

    /**
     * Gets the digest of the given blob. If null, computes it using the given
     * digest algorithm. For now only md5 is supported.
     *
     * @throws UnsupportedOperationException if the digest algorithm is not
     *             supported
     * @throws ClientException if the digest computation fails with an
     *             {@link IOException}
     */
    public static String getDigest(Blob blob, String digestAlgorithm)
            throws ClientException {
        String digest = blob.getDigest();
        if (StringUtils.isEmpty(digest)) {
            if (MD5_DIGEST_ALGORITHM.equals(digestAlgorithm)) {
                try {
                    digest = DigestUtils.md5Hex(blob.getStream());
                } catch (IOException e) {
                    throw new ClientException(String.format(
                            "Error while computing digest for blob %s.",
                            blob.getFilename()), e);
                }
            } else {
                throw new UnsupportedOperationException(String.format(
                        "Unsupported digest algorithm %s.", digestAlgorithm));
            }
        }
        return digest;
    }

}
