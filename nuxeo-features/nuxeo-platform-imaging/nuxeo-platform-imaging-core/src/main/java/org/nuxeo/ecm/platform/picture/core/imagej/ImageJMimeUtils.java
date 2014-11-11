/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.picture.core.imagej;

import ij.ImagePlus;
import ij.io.Opener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.picture.core.MimeUtils;

public class ImageJMimeUtils implements MimeUtils {

    private static final Log log = LogFactory.getLog(ImageJMimeUtils.class);

    public String getImageMimeType(File file) {
        Opener op = new Opener();
        ImagePlus im = op.openImage(file.getPath());
        if (im == null){
            return null;
        }
        int fileType = im.getOriginalFileInfo().fileFormat;
        return getInternalMimeType(fileType);
    }

    public String getImageMimeType(InputStream in) {
        FileBlob fb;
        try {
            fb = new FileBlob(in);
        } catch (IOException e) {
            log.error("Can't find the file", e);
            return "";
        }
        String path = fb.getFile().getPath();
        Opener op = new Opener();
        ImagePlus im = op.openImage(path);
        if (im == null){
            return null;
        }
        int fileType = im.getOriginalFileInfo().fileFormat;
        return getInternalMimeType(fileType);
    }

    private String getInternalMimeType(int fileType) {
        if (fileType == Opener.JPEG) {
            return "image/jpeg";
        } else if (fileType == Opener.BMP) {
            return "image/bmp";
        } else if (fileType == Opener.PNG) {
            return "image/png";
        } else if (fileType == Opener.GIF) {
            return "image/gif";
        } else if (fileType == Opener.TIFF) {
            return "image/tiff";
        } else if (fileType == Opener.TIFF_AND_DICOM) {
            return "image/tiff";
        } else if (fileType == Opener.FITS) {
            return "image/fits";
        } else if (fileType == Opener.PGM) {
            return "image/x-portable-graymap";
        } else if (fileType == Opener.ZIP) {
            return "application/zip";
        } else {
            return "application/octet-stream";
        }
    }

}
