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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.servlet.http.Part;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.platform.mimetype.MimetypeDetectionException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;

public class FileUtils {

    private static final Log log = LogFactory.getLog(FileUtils.class);

    private FileUtils() {
    }

    /**
     * Creates a serializable blob from a stream, with filename and mimetype detection.
     * <p>
     * Creates a serializable FileBlob which stores data in a temporary file on the hard disk.
     *
     * @param in the input stream holding data
     * @param filename the file name. Will be set on the blob and will used for mimetype detection.
     * @param mimeType the detected mimetype at upload. Can be null. Will be verified by the mimetype service.
     */
    public static Blob createSerializableBlob(InputStream in, String filename, String mimeType) {
        Blob blob = null;
        try {
            // persisting the blob makes it possible to read the binary content
            // of the request stream several times (mimetype sniffing, digest
            // computation, core binary storage)
            blob = Blobs.createBlob(in, mimeType);
            // filename
            if (filename != null) {
                filename = getCleanFileName(filename);
            }
            blob.setFilename(filename);
            // mimetype detection
            MimetypeRegistry mimeService = Framework.getService(MimetypeRegistry.class);
            String detectedMimeType = mimeService.getMimetypeFromFilenameAndBlobWithDefault(filename, blob, null);
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
            log.error("Could not fetch mimetype for file " + filename, e);
        } catch (IOException e) {
            log.error(e);
        }
        return blob;
    }

    /**
     * Creates a Blob from a {@link Part}.
     * <p>
     * Attempts to capture the underlying temporary file, if one exists. This needs to use reflection to avoid having
     * dependencies on the application server.
     *
     * @param part the servlet part
     * @return the blob
     * @since 7.2
     */
    public static Blob createBlob(Part part) throws IOException {
        Blob blob = null;
        try {
            // part : org.apache.catalina.core.ApplicationPart
            // part.fileItem : org.apache.tomcat.util.http.fileupload.disk.DiskFileItem
            // part.fileItem.isInMemory() : false if on disk
            // part.fileItem.getStoreLocation() : java.io.File
            Field fileItemField = part.getClass().getDeclaredField("fileItem");
            fileItemField.setAccessible(true);
            Object fileItem = fileItemField.get(part);
            if (fileItem != null) {
                Method isInMemoryMethod = fileItem.getClass().getDeclaredMethod("isInMemory");
                boolean inMemory = ((Boolean) isInMemoryMethod.invoke(fileItem)).booleanValue();
                if (!inMemory) {
                    Method getStoreLocationMethod = fileItem.getClass().getDeclaredMethod("getStoreLocation");
                    File file = (File) getStoreLocationMethod.invoke(fileItem);
                    if (file != null) {
                        // move the file to a temporary blob we own
                        blob = Blobs.createBlobWithExtension(null);
                        Files.move(file.toPath(), blob.getFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } catch (ReflectiveOperationException | SecurityException | IllegalArgumentException e) {
            // unknown Part implementation
        }
        if (blob == null) {
            // if we couldn't get to the file, use the InputStream
            blob = Blobs.createBlob(part.getInputStream());
        }
        blob.setMimeType(part.getContentType());
        blob.setFilename(retrieveFilename(part));
        return blob;
    }

    /**
     * Helper method to retrieve filename from part, waiting for servlet-api related improvements.
     * <p>
     * Filename is cleaned before being returned.
     *
     * @see #getCleanFileName(String)
     * @since 7.1
     */
    public static String retrieveFilename(Part part) {
        for (String cd : part.getHeader("content-disposition").split(";")) {
            if (cd.trim().startsWith("filename")) {
                String filename = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                return getCleanFileName(filename);
            }
        }
        return null;
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

    public static void configureFileBlob(Blob blob) {
        // mimetype detection
        MimetypeRegistry mimeService = Framework.getService(MimetypeRegistry.class);
        String detectedMimeType;
        try {
            detectedMimeType = mimeService.getMimetypeFromFilenameAndBlobWithDefault(blob.getFilename(), blob, null);
        } catch (MimetypeDetectionException e) {
            log.error("could not fetch mimetype for file " + blob.getFilename(), e);
            return;
        }
        if (detectedMimeType == null) {
            if (blob.getMimeType() != null) {
                return;
            }
            // default
            detectedMimeType = "application/octet-stream";
        }
        blob.setMimeType(detectedMimeType);
        return;
    }

}
