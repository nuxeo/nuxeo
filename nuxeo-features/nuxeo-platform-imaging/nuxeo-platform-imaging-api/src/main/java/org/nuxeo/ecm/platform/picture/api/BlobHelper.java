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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.picture.api;

import java.io.File;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLBlob;
import org.nuxeo.runtime.services.streaming.FileSource;
import org.nuxeo.runtime.services.streaming.StreamSource;

/**
 * Helpers around Blob objects.
 *
 * @since 5.6
 */
public class BlobHelper {

    // utility class
    private BlobHelper() {
    }

    /**
     * If the blob is backed by an actual file, return it.
     * <p>
     * Note that the File may be short-lived (temporary file), so should be used
     * immediately.
     *
     * @return a File, or {@code null} if this blob doesn't have one
     */
    public static File getFileFromBlob(Blob blob) {
        if (blob instanceof FileBlob) {
            return ((FileBlob) blob).getFile();
        } else if (blob instanceof SQLBlob) {
            StreamSource source = ((SQLBlob) blob).getBinary().getStreamSource();
            return ((FileSource) source).getFile();
        }
        return null;
    }

}
