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

package org.nuxeo.ecm.platform.picture.core.im;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.core.ImageUtils;
import org.nuxeo.ecm.platform.picture.core.mistral.MistralImageUtils;
import org.nuxeo.ecm.platform.picture.magick.utils.ImageCropper;
import org.nuxeo.ecm.platform.picture.magick.utils.ImageIdentifier;
import org.nuxeo.ecm.platform.picture.magick.utils.ImageResizer;
import org.nuxeo.ecm.platform.picture.magick.utils.ImageRotater;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Laurent Doguin
 */
public class IMImageUtils implements ImageUtils {

    private static final Log log = LogFactory.getLog(IMImageUtils.class);

    @Deprecated
    public InputStream crop(InputStream in, int x, int y, int width, int height) {
        try {
            CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
            CommandAvailability commandAvailability = cles.getCommandAvailability("crop");
            if (commandAvailability.isAvailable()) {
                FileBlob fb = new FileBlob(in);
                String path = fb.getFile().getAbsolutePath();
                ImageInfo imageInfo = ImageIdentifier.getInfo(path);
                File img2 = File.createTempFile("target", "."
                        + imageInfo.getFormat());
                ImageCropper.crop(path, img2.getAbsolutePath(), width, height,
                        x, y);
                InputStream is = new FileInputStream(img2);
                img2.delete();
                return is;
            } else {
                MistralImageUtils miu = new MistralImageUtils();
                return miu.crop(in, x, y, width, height);
            }
        } catch (Exception e) {
            log.error("Crop with ImageMagick failed", e);
            return null;
        }
    }

    @Deprecated
    public InputStream resize(InputStream in, int width, int height) {
        try {
            CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
            CommandAvailability commandAvailability = cles.getCommandAvailability("resizer");
            if (commandAvailability.isAvailable()) {
                FileBlob fb = new FileBlob(in);
                String path = fb.getFile().getAbsolutePath();

                ImageInfo imageInfo = ImageIdentifier.getInfo(path);
                File img2 = File.createTempFile("target", "."
                        + imageInfo.getFormat());
                ImageResizer.resize(path, img2.getAbsolutePath(), width,
                        height, imageInfo.getDepth());

                InputStream is = new FileInputStream(img2);
                img2.delete();

                return is;
            } else {
                MistralImageUtils miu = new MistralImageUtils();
                return miu.resize(in, width, height);
            }
        } catch (Exception e) {
            log.error("Resizing with ImageMagick failed", e);
        }
        return null;
    }

    @Deprecated
    public InputStream rotate(InputStream in, int angle) {
        try {
            CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
            CommandAvailability commandAvailability = cles.getCommandAvailability("rotate");
            if (commandAvailability.isAvailable()) {
                FileBlob fb = new FileBlob(in);
                String path = fb.getFile().getAbsolutePath();
                ImageInfo imageInfo = ImageIdentifier.getInfo(path);
                File img2 = File.createTempFile("target", "."
                        + imageInfo.getFormat());
                ImageRotater.rotate(path, img2.getAbsolutePath(), angle);
                InputStream is = new FileInputStream(img2);
                img2.delete();
                return is;
            } else {
                MistralImageUtils miu = new MistralImageUtils();
                return miu.rotate(in, angle);
            }
        } catch (Exception e) {
            log.error("Rotation with ImageMagick failed", e);
        }
        return null;
    }

    public Blob crop(Blob blob, int x, int y, int width, int height) {
        try {
            CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
            CommandAvailability commandAvailability = cles.getCommandAvailability("resizer");
            if (commandAvailability.isAvailable()) {
                File sourceFile = File.createTempFile("source",
                        blob.getFilename());
                try {
                    blob.transferTo(sourceFile);
                    String suffix = getTempSuffix(blob, sourceFile);
                    File targetFile = File.createTempFile("target", suffix);
                    ImageCropper.crop(sourceFile.getAbsolutePath(),
                            targetFile.getAbsolutePath(), width, height, x, y);
                    Blob targetBlob = new FileBlob(targetFile);
                    Framework.trackFile(targetFile, targetBlob);
                    return targetBlob;
                } finally {
                    if (sourceFile != null) {
                        sourceFile.delete();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Resizing with ImageMagick failed", e);
        }
        return null;
    }

    public Blob resize(Blob blob, String finalFormat, int width, int height,
            int depth) {
        try {
            CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
            CommandAvailability commandAvailability = cles.getCommandAvailability("resizer");
            if (commandAvailability.isAvailable()) {
                File sourceFile = File.createTempFile("source",
                        blob.getFilename());
                try {
                    blob.transferTo(sourceFile);
                    String suffix;
                    if (finalFormat != null) {
                        suffix = "." + finalFormat;
                    } else {
                        suffix = getTempSuffix(blob, sourceFile);
                    }
                    File targetFile = File.createTempFile("target", suffix);
                    ImageResizer.resize(sourceFile.getAbsolutePath(),
                            targetFile.getAbsolutePath(), width, height, depth);
                    Blob targetBlob = new FileBlob(targetFile);
                    Framework.trackFile(targetFile, targetBlob);
                    return targetBlob;
                } finally {
                    if (sourceFile != null) {
                        sourceFile.delete();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Resizing with ImageMagick failed", e);
        }
        return null;
    }

    public Blob rotate(Blob blob, int angle) {
        try {
            CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
            CommandAvailability commandAvailability = cles.getCommandAvailability("rotate");
            if (commandAvailability.isAvailable()) {
                File sourceFile = File.createTempFile("source",
                        blob.getFilename());
                try {
                    blob.transferTo(sourceFile);
                    String suffix = getTempSuffix(blob, sourceFile);
                    File targetFile = File.createTempFile("target", suffix);
                    ImageRotater.rotate(sourceFile.getAbsolutePath(),
                            targetFile.getAbsolutePath(), angle);
                    Blob targetBlob = new FileBlob(targetFile);
                    Framework.trackFile(targetFile, targetBlob);
                    return targetBlob;
                } finally {
                    if (sourceFile != null) {
                        sourceFile.delete();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Rotation with ImageMagick failed", e);
        }
        return null;
    }

    protected String getTempSuffix(Blob blob, File file)
            throws CommandNotAvailable {
        String suffix = blob.getFilename();
        if (suffix == null) {
            ImageInfo imageInfo = ImageIdentifier.getInfo(file.getAbsolutePath());
            suffix = "." + imageInfo.getFormat();
        }
        return suffix;
    }

    public boolean isAvailable() {
        CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
        CommandAvailability commandAvailability = cles.getCommandAvailability("identify");
        return commandAvailability.isAvailable();
    }

}
