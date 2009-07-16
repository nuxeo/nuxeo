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

package org.nuxeo.ecm.platform.picture.core.mistral;

import it.tidalwave.image.EditableImage;
import it.tidalwave.image.metadata.EXIFDirectory;
import it.tidalwave.image.op.ReadOp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.ecm.platform.picture.core.MimeUtils;

/**
 * @author Max Stepanov
 */
public class MistralMimeUtils implements MimeUtils {

    private static final Log log = LogFactory.getLog(MistralMimeUtils.class);

    private static final int BUFFER_LIMIT = 32000000;

    public static final String MIME_IMAGE_JPEG = "image/jpeg";

    public static final String MIME_IMAGE_GIF = "image/gif";

    public static final String MIME_IMAGE_PNG = "image/png";

    public static final String MIME_IMAGE_TIFF = "image/tiff";

    public static final String MIME_IMAGE_BMP = "image/x-ms-bmp";

    public static final String MIME_UNKNOWN = "application/octet-stream";

    private static final Map<String, String> mimeTypes = new HashMap<String, String>();

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

    public String getImageMimeType(InputStream in) {
        return getInternalImageMimeType(in);
    }

    public String getImageMimeType(File file) {
        return getInternalImageMimeType(file);
    }

    public String getInternalImageMimeType(Object source) {
        String type = null;

        try {
            if (source instanceof InputStream) {
                BufferedInputStream bin;
                if (source instanceof BufferedInputStream) {
                    bin = (BufferedInputStream) source;
                } else {
                    source = bin = new BufferedInputStream((InputStream) source);
                }
                bin.mark(BUFFER_LIMIT);
            }
            EditableImage image = EditableImage.create(new ReadOp(source,
                    ReadOp.Type.METADATA));
            EXIFDirectory exif = image.getEXIFDirectory();
            if (exif.isCompressionAvailable()) {
                String compression = exif.getCompression().toString().toUpperCase();
                if (compression.contains("JPEG")) {
                    type = MIME_IMAGE_JPEG;
                }
            }
        } catch (IOException e) {
            log.debug("Can't instanciate file", e);
            return null;
        }
        //
        // if (type == null) {
        // String ext = source.getName().toLowerCase();
        // int index = ext.lastIndexOf('.');
        // if (index >= 0) {
        // ext = ext.substring(index + 1);
        // }
        // type = mimeTypes.get(ext);
        // }
        if (type == null) {
            type = MIME_UNKNOWN;
        }
        return type;
    }
}
