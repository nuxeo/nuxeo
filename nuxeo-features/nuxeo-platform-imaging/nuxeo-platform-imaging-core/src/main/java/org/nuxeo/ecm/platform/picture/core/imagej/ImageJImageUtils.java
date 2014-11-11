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
import ij.io.FileInfo;
import ij.io.FileSaver;
import ij.process.ImageProcessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.picture.core.ImageUtils;

public class ImageJImageUtils implements ImageUtils {

    private static final Log log = LogFactory.getLog(ImageJImageUtils.class);

    public InputStream resize(InputStream in, int width, int height) {
        try {
            FileBlob fb = new FileBlob(in);
            String path = fb.getFile().getPath();
            ImagePlus f = new ImagePlus(path);
            String fileName = f.getFileInfo().fileName;
            ImageProcessor im = f.getProcessor();
            im.setInterpolate(true);
            ImageProcessor ip_small = im.resize(width, height);
            ImagePlus small = new ImagePlus("small", ip_small);
            File resultFile = save(small, fileName.split("\\.")[0], "tmp",
                    f.getOriginalFileInfo().fileFormat);
            if (resultFile != null) {
                return new FileInputStream(resultFile);
            }
        } catch (IOException e) {
            log.error("Cannot save the file", e);
        }
        return null;
    }

    public InputStream crop(InputStream in, int x, int y, int width, int height) {
        try {
            FileBlob fb = new FileBlob(in);
            String path = fb.getFile().getPath();
            ImagePlus f = new ImagePlus(path);
            String fileName = f.getFileInfo().fileName;
            ImageProcessor im = f.getProcessor();
            im.setInterpolate(true);
            im.setRoi(x, y, width, height);
            ImageProcessor ip_crop = im.crop();
            ImagePlus cropImage = new ImagePlus("small", ip_crop);
            File resultFile = save(cropImage, fileName.split("\\.")[0], "tmp",
                    f.getOriginalFileInfo().fileFormat);
            if (resultFile != null) {
                return new FileInputStream(resultFile);
            }
        } catch (IOException e) {
            log.error("Cannot save the file", e);
        }
        return null;
    }

    public InputStream rotate(InputStream in, int angle) {
        try {
            FileBlob fb = new FileBlob(in);
            String path = fb.getFile().getPath();
            ImagePlus f = new ImagePlus(path);
            String fileName = f.getFileInfo().fileName;
            ImageProcessor im = f.getProcessor();
            im.setInterpolate(true);
            ImageProcessor rotatedImage;
            if (angle < 0){
                rotatedImage = im.rotateLeft();
            }else{
                rotatedImage  = im.rotateRight();
            }
            ImagePlus newImage = new ImagePlus("small", rotatedImage);
            File resultFile = save(newImage, fileName.split("\\.")[0], "tmp",
                    f.getOriginalFileInfo().fileFormat);
            if (resultFile != null) {
                return new FileInputStream(resultFile);
            }
        } catch (IOException e) {
            log.error("Cannot save the file", e);
        }
        return null;
    }

    private File save(ImagePlus imp, String name, String ext, int fileFormat) {
        String path = name;
        FileSaver fs = new FileSaver(imp);
        try {
            if (fileFormat == FileInfo.TIFF) {
                File resultFile = File.createTempFile(path, "tiff");
                resultFile.deleteOnExit();
                fs.saveAsTiff(resultFile.getPath());
                return resultFile;
            } else if (fileFormat == FileInfo.RAW) {
                File resultFile = File.createTempFile(path, "raw");
                resultFile.deleteOnExit();
                fs.saveAsRaw(resultFile.getPath());
                return resultFile;
            } else if (fileFormat == FileInfo.ZIP_ARCHIVE) {
                File resultFile = File.createTempFile(path, "zip");
                resultFile.deleteOnExit();
                fs.saveAsZip(resultFile.getPath());
                return resultFile;
            } else if (fileFormat == FileInfo.BMP) {
                File resultFile = File.createTempFile(path, "bmp");
                resultFile.deleteOnExit();
                fs.saveAsBmp(resultFile.getPath());
                return resultFile;
            } else if (ext.equals("tiff") || ext.equals("TIFF")) {
                File resultFile = File.createTempFile(path, "tiff");
                resultFile.deleteOnExit();
                fs.saveAsTiff(resultFile.getPath());
                return resultFile;
            } else if (ext.equals("gif") || ext.equals("GIF")) {
                File resultFile = File.createTempFile(path, "gif");
                resultFile.deleteOnExit();
                fs.saveAsGif(resultFile.getPath());
                return resultFile;
            } else if (ext.equals("jpg") || ext.equals("JPG")
                    || ext.equals("jpeg") || ext.equals("JPEG")) {
                File resultFile = File.createTempFile(path, "jpg");
                resultFile.deleteOnExit();
                fs.saveAsJpeg(resultFile.getPath());
                return resultFile;
            } else if (ext.equals("raw") || ext.equals("RAW")) {
                File resultFile = File.createTempFile(path, "raw");
                resultFile.deleteOnExit();
                fs.saveAsRaw(resultFile.getPath());
                return resultFile;
            } else if (ext.equals("zip") || ext.equals("ZIP")) {
                File resultFile = File.createTempFile(path, "zip");
                resultFile.deleteOnExit();
                fs.saveAsZip(resultFile.getPath());
                return resultFile;
            } else if (ext.equals("bmp") || ext.equals("BMP")) {
                File resultFile = File.createTempFile(path, "bmp");
                resultFile.deleteOnExit();
                fs.saveAsBmp(resultFile.getPath());
                return resultFile;
            } else if (ext.equals("png")) {
                File resultFile = File.createTempFile(path, "PNG");
                resultFile.deleteOnExit();
                fs.saveAsPng(resultFile.getPath());
                return resultFile;
            } else if (ext.equals("fits")) {
                File resultFile = File.createTempFile(path, "fits");
                resultFile.deleteOnExit();
                fs.saveAsFits(resultFile.getPath());
                return resultFile;
            } else {
                File resultFile = File.createTempFile(path, "jpg");
                resultFile.deleteOnExit();
                fs.saveAsJpeg(resultFile.getPath());
                return resultFile;
            }
        } catch (IOException e) {
            log.error("Cannot save the file", e);
        }

        return null;
    }

}
