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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.imaging.core;

import it.tidalwave.image.EditableImage;
import it.tidalwave.image.metadata.EXIFDirectory;
import it.tidalwave.image.op.ReadOp;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Max Stepanov
 */
public class MimeUtils {

    private static final String MIME_IMAGE_JPEG = "image/jpeg";
    private static final String MIME_IMAGE_GIF = "image/gif";
    private static final String MIME_IMAGE_PNG = "image/png";
    private static final String MIME_IMAGE_TIFF = "image/tiff";
    private static final String MIME_IMAGE_BMP = "image/x-ms-bmp";

    private final static Map<String, String> mimeTypes = new HashMap<String, String>();

    static {
        mimeTypes.put("jpg", MIME_IMAGE_JPEG);
        mimeTypes.put("jpeg", MIME_IMAGE_JPEG);
        mimeTypes.put("jpe", MIME_IMAGE_JPEG);
        mimeTypes.put("gif", MIME_IMAGE_GIF);
        mimeTypes.put("png", MIME_IMAGE_PNG);
        mimeTypes.put("tiff", MIME_IMAGE_TIFF);
        mimeTypes.put("tif", MIME_IMAGE_TIFF);
        mimeTypes.put("bmp", MIME_IMAGE_BMP);
    }

    // Utility class
    private MimeUtils() {
    }

    public static String getImageMimeType(File file) {
        String type = null;

        try {
            EditableImage image = EditableImage.create(new ReadOp(file, ReadOp.Type.METADATA));
            EXIFDirectory exif = image.getEXIFDirectory();
            if (exif.isCompressionAvailable()) {
                String compression = exif.getCompression().toString().toUpperCase();
                if (compression.contains("JPEG")) {
                    type = MIME_IMAGE_JPEG;
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (type == null) {
            String ext = file.getName().toLowerCase();
            int index = ext.lastIndexOf('.');
            if (index >= 0) {
                ext = ext.substring(index + 1);
            }
            type = mimeTypes.get(ext);
        }
        if (type == null) {
            type = "application/octet-stream";
        }
        return type;
    }

}
