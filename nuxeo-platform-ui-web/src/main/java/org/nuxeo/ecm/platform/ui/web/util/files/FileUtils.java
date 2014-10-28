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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.platform.mimetype.MimetypeDetectionException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.streaming.FileSource;

public class FileUtils {

    private static final Log log = LogFactory.getLog(FileUtils.class);

    private FileUtils() {
    }

    /**
     * Creates a serializable blob from a stream, with filename and mimetype
     * detection.
     * <p>
     * Creates an in-memory blob if data is under 64K, otherwise constructs a
     * serializable FileBlob which stores data in a temporary file on the hard
     * disk.
     *
     * @param file the input stream holding data
     * @param filename the file name. Will be set on the blob and will used for
     *            mimetype detection.
     * @param mimeType the detected mimetype at upload. Can be null. Will be
     *            verified by the mimetype service.
     */
    public static Blob createSerializableBlob(InputStream file,
            String filename, String mimeType) {
        Blob blob = null;
        try {
            // persisting the blob makes it possible to read the binary content
            // of the request stream several times (mimetype sniffing, digest
            // computation, core binary storage)
            blob = StreamingBlob.createFromStream(file, mimeType).persist();
            // filename
            if (filename != null) {
                filename = getCleanFileName(filename);
            }
            blob.setFilename(filename);
            // mimetype detection
            MimetypeRegistry mimeService = Framework.getService(MimetypeRegistry.class);
            String detectedMimeType = mimeService.getMimetypeFromFilenameAndBlobWithDefault(
                    filename, blob, null);
            if (detectedMimeType == null) {
                if (mimeType != null) {
                    detectedMimeType = mimeType;
                } else {
                    // default
                    detectedMimeType = "application/octet-stream";
                }
            }
            blob.setMimeType(detectedMimeType);
        } catch (MimetypeDetectionException e) {
            log.error(String.format("could not fetch mimetype for file %s",
                    filename), e);
        } catch (IOException e) {
            log.error(e);
        } catch (Exception e) {
            log.error(e);
        }
        return blob;
    }

    /**
     * Returns a clean filename, stripping upload path on client side.
     * <p>
     * Fixes NXP-544
     */
    public static String getCleanFileName(String filename) {
        String res = null;
        int lastWinSeparator = filename.lastIndexOf('\\');
        int lastUnixSeparator = filename.lastIndexOf('/');
        int lastSeparator = Math.max(lastWinSeparator, lastUnixSeparator);
        if (lastSeparator != -1) {
            res = filename.substring(lastSeparator + 1, filename.length());
        } else {
            res = filename;
        }
        return res;
    }

    /**
     * A Blob based on a File but whose contract says that the file is allowed
     * to be moved to another filesystem location if needed.
     * <p>
     * The move is done by getting the StreamSource from the Blob, casting to
     * FileSource.
     *
     * @since 5.7.2
     */
    public static class TemporaryFileBlob extends StreamingBlob {

        private static final long serialVersionUID = 1L;

        public TemporaryFileBlob(File file, String mimeType, String encoding,
                String filename, String digest) {
            super(new FileSource(file), mimeType, encoding, filename, digest);
        }

        @Override
        public boolean isTemporary() {
            return true; // for SQLSession#getBinary
        }

        @Override
        public FileSource getStreamSource() {
            return (FileSource) src;
        }
    }

    /**
     * Creates a TemporaryFileBlob. Similar to
     * FileUtils.createSerializableBlob.
     *
     * @since 5.7.2
     */
    public static Blob createTemporaryFileBlob(File file, String filename,
            String mimeType) {
        if (filename != null) {
            filename = FileUtils.getCleanFileName(filename);
        }
        Blob blob = new TemporaryFileBlob(file, mimeType, null, filename, null);
        return configureFileBlob(blob, filename, mimeType);
    }

    protected static Blob configureFileBlob(Blob blob, String filename,
            String mimeType) {
        try {
            // mimetype detection
            MimetypeRegistry mimeService = Framework.getLocalService(MimetypeRegistry.class);
            String detectedMimeType = mimeService.getMimetypeFromFilenameAndBlobWithDefault(
                    filename, blob, null);
            if (detectedMimeType == null) {
                if (mimeType != null) {
                    detectedMimeType = mimeType;
                } else {
                    // default
                    detectedMimeType = "application/octet-stream";
                }
            }
            blob.setMimeType(detectedMimeType);
        } catch (MimetypeDetectionException e) {
            log.error(String.format("could not fetch mimetype for file %s",
                    filename), e);
        }
        return blob;
    }

}
