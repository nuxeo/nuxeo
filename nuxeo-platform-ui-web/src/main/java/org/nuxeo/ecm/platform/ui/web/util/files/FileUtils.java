/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.util.files;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.platform.mimetype.MimetypeDetectionException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.ui.web.file.UploadedFile;
import org.nuxeo.runtime.api.Framework;

public class FileUtils {

    private static final Log log = LogFactory.getLog(FileUtils.class);

    private FileUtils() {
    }

    /**
     * Creates a Blob with content and mime/type.
     *
     * @param file
     * @return the content in byte[]
     * @throws MimetypeDetectionException
     */
    public static Blob fetchContent(UploadedFile file) {

        Blob blob = createSerializableBlob(file, null);
        try {
            MimetypeRegistry mimeService = Framework.getService(MimetypeRegistry.class);
            String mimeType = mimeService.getMimetypeFromFilenameAndBlobWithDefault(
                    file.getFilename(), blob, file.getContentType());
            blob.setMimeType(mimeType);
        } catch (MimetypeDetectionException e) {
            log.error(String.format("could not fetch mimetype for %s: %s",
                    file.getFilename(), e.getMessage()));
        } catch (Exception e) {
            log.error("Error whil accessible mimetype service "
                    + e.getMessage());
            }
        return blob;
    }

    /**
     * To be used to create a serializable blob from a UploadedFile creates a
     * in-memory blob if data is under 64K otherwise constructs a serializable
     * FileBlob which stores data in a temporary file on the hard disk.
     *
     * @param uf
     * @return
     */
    public static Blob createSerializableBlob(UploadedFile uf) {
        return createSerializableBlob(uf, null);
    }

    /**
     * To be used to create a serializable blob from a UploadedFile creates a
     * in-memory blob if data is under 64K otherwise constructs a serializable
     * FileBlob which stores data in a temporary file on the hard disk.
     * <p>
     * If mime type is null or too generic, get it from the uploaded file.
     *
     * @param file
     * @param mimeType
     * @return
     */
    public static Blob createSerializableBlob(UploadedFile file, String mimeType) {
        if (mimeType == null || mimeType.equals("application/octet-stream")) {
            mimeType = file.getContentType();
        }
        UploadedFileStreamSource src = new UploadedFileStreamSource(file);
        return new StreamingBlob(src, mimeType);
    }

}
