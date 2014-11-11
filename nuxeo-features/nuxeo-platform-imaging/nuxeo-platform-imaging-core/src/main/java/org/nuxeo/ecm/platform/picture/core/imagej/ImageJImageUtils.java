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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.picture.core.ImageUtils;
import org.nuxeo.runtime.api.Framework;

import ij.ImagePlus;
import ij.io.FileInfo;
import ij.io.FileSaver;
import ij.process.ImageProcessor;

public class ImageJImageUtils implements ImageUtils {

    private static final Log log = LogFactory.getLog(ImageJImageUtils.class);

    @Deprecated
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
                FileInputStream fis = new FileInputStream(resultFile);
                Framework.trackFile(resultFile, fis);
                return fis;
            }
        } catch (IOException e) {
            log.error("Cannot save the file", e);
        }
        return null;
    }

    @Deprecated
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
                FileInputStream fis = new FileInputStream(resultFile);
                Framework.trackFile(resultFile, fis);
                return fis;
            }
        } catch (IOException e) {
            log.error("Cannot save the file", e);
        }
        return null;
    }

    @Deprecated
    public InputStream rotate(InputStream in, int angle) {
        try {
            FileBlob fb = new FileBlob(in);
            String path = fb.getFile().getPath();
            ImagePlus f = new ImagePlus(path);
            String fileName = f.getFileInfo().fileName;
            ImageProcessor im = f.getProcessor();
            im.setInterpolate(true);
            ImageProcessor rotatedImage;
            if (angle < 0) {
                rotatedImage = im.rotateLeft();
            } else {
                rotatedImage = im.rotateRight();
            }
            ImagePlus newImage = new ImagePlus("small", rotatedImage);
            File resultFile = save(newImage, fileName.split("\\.")[0], "tmp",
                    f.getOriginalFileInfo().fileFormat);
            if (resultFile != null) {
                FileInputStream fis = new FileInputStream(resultFile);
                Framework.trackFile(resultFile, fis);
                return fis;
            }
        } catch (IOException e) {
            log.error("Cannot save the file", e);
        }
        return null;
    }

    public Blob crop(Blob blob, int x, int y, int width, int height) {
        File sourceFile = null;
        try {
            sourceFile = File.createTempFile("source", blob.getFilename());
            blob.transferTo(sourceFile);
            String path = sourceFile.getAbsolutePath();
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
                Blob resultBlob = new FileBlob(resultFile);
                Framework.trackFile(resultFile, resultBlob);
                return resultBlob;
            }
        } catch (IOException e) {
            log.error("Cannot save the file", e);
        } finally {
            if (sourceFile != null) {
                sourceFile.delete();
            }
        }
        return null;
    }

    public Blob resize(Blob blob, String finalFormat, int width, int height,
            int depth) {
        File sourceFile = null;
        try {
            sourceFile = File.createTempFile("source", blob.getFilename());
            blob.transferTo(sourceFile);
            String path = sourceFile.getAbsolutePath();
            ImagePlus f = new ImagePlus(path);
            String fileName = f.getFileInfo().fileName;
            ImageProcessor im = f.getProcessor();
            im.setInterpolate(true);
            ImageProcessor ip_small = im.resize(width, height);
            ImagePlus small = new ImagePlus("small", ip_small);
            File resultFile = save(small, fileName.split("\\.")[0],
                    finalFormat != null ? finalFormat : "tmp",
                    finalFormat != null ? FileInfo.UNKNOWN
                            : f.getOriginalFileInfo().fileFormat);
            if (resultFile != null) {
                Blob resultBlob = new FileBlob(resultFile);
                Framework.trackFile(resultFile, resultBlob);
                return resultBlob;
            }
        } catch (IOException e) {
            log.error("Cannot save the file", e);
        } finally {
            if (sourceFile != null) {
                sourceFile.delete();
            }
        }
        return null;
    }

    public Blob rotate(Blob blob, int angle) {
        File sourceFile = null;
        try {
            sourceFile = File.createTempFile("source", blob.getFilename());
            blob.transferTo(sourceFile);
            String path = sourceFile.getAbsolutePath();
            ImagePlus f = new ImagePlus(path);
            String fileName = f.getFileInfo().fileName;
            ImageProcessor im = f.getProcessor();
            im.setInterpolate(true);
            ImageProcessor rotatedImage;
            if (angle < 0) {
                rotatedImage = im.rotateLeft();
            } else {
                rotatedImage = im.rotateRight();
            }
            ImagePlus newImage = new ImagePlus("small", rotatedImage);
            File resultFile = save(newImage, fileName.split("\\.")[0], "tmp",
                    f.getOriginalFileInfo().fileFormat);
            if (resultFile != null) {
                Blob resultBlob = new FileBlob(resultFile);
                Framework.trackFile(resultFile, resultBlob);
                return resultBlob;
            }
        } catch (IOException e) {
            log.error("Cannot save the file", e);
        } finally {
            if (sourceFile != null) {
                sourceFile.delete();
            }
        }
        return null;
    }

    private File save(ImagePlus imp, String name, String ext, int fileFormat) {
        String path = name;
        FileSaver fs = new FileSaver(imp);
        try {
            if (fileFormat == FileInfo.TIFF) {
                File resultFile = File.createTempFile(path, "tiff");
                fs.saveAsTiff(resultFile.getPath());
                return resultFile;
            } else if (fileFormat == FileInfo.RAW) {
                File resultFile = File.createTempFile(path, "raw");
                fs.saveAsRaw(resultFile.getPath());
                return resultFile;
            } else if (fileFormat == FileInfo.ZIP_ARCHIVE) {
                File resultFile = File.createTempFile(path, "zip");
                fs.saveAsZip(resultFile.getPath());
                return resultFile;
            } else if (fileFormat == FileInfo.BMP) {
                File resultFile = File.createTempFile(path, "bmp");
                fs.saveAsBmp(resultFile.getPath());
                return resultFile;
            } else if (ext.equals("tiff") || ext.equals("TIFF")) {
                File resultFile = File.createTempFile(path, "tiff");
                fs.saveAsTiff(resultFile.getPath());
                return resultFile;
            } else if (ext.equals("gif") || ext.equals("GIF")) {
                File resultFile = File.createTempFile(path, "gif");
                fs.saveAsGif(resultFile.getPath());
                return resultFile;
            } else if (ext.equals("jpg") || ext.equals("JPG")
                    || ext.equals("jpeg") || ext.equals("JPEG")) {
                File resultFile = File.createTempFile(path, "jpg");
                fs.saveAsJpeg(resultFile.getPath());
                return resultFile;
            } else if (ext.equals("raw") || ext.equals("RAW")) {
                File resultFile = File.createTempFile(path, "raw");
                fs.saveAsRaw(resultFile.getPath());
                return resultFile;
            } else if (ext.equals("zip") || ext.equals("ZIP")) {
                File resultFile = File.createTempFile(path, "zip");
                fs.saveAsZip(resultFile.getPath());
                return resultFile;
            } else if (ext.equals("bmp") || ext.equals("BMP")) {
                File resultFile = File.createTempFile(path, "bmp");
                fs.saveAsBmp(resultFile.getPath());
                return resultFile;
            } else if (ext.equals("png")) {
                File resultFile = File.createTempFile(path, "PNG");
                fs.saveAsPng(resultFile.getPath());
                return resultFile;
            } else if (ext.equals("fits")) {
                File resultFile = File.createTempFile(path, "fits");
                fs.saveAsFits(resultFile.getPath());
                return resultFile;
            } else {
                File resultFile = File.createTempFile(path, "jpg");
                fs.saveAsJpeg(resultFile.getPath());
                return resultFile;
            }
        } catch (IOException e) {
            log.error("Cannot save the file", e);
        }

        return null;
    }

    public boolean isAvailable() {
        return true; // we only need the imagej jars in the classpath
    }
}
