/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Laurent Doguin
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.picture.core.im;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.picture.api.BlobHelper;
import org.nuxeo.ecm.platform.picture.core.ImageUtils;
import org.nuxeo.ecm.platform.picture.magick.utils.ImageCropper;
import org.nuxeo.ecm.platform.picture.magick.utils.ImageIdentifier;
import org.nuxeo.ecm.platform.picture.magick.utils.ImageResizer;
import org.nuxeo.ecm.platform.picture.magick.utils.ImageRotater;
import org.nuxeo.runtime.api.Framework;

public class IMImageUtils implements ImageUtils {

    private static final Log log = LogFactory.getLog(IMImageUtils.class);

    public static abstract class ImageMagickCaller {

        protected File sourceFile;

        // a tmp file is needed if the blob doesn't have a file, or
        // if it has one but with an incorrect extension
        protected File tmpFile;

        protected File targetFile;

        public Blob call(Blob blob, String targetExt, String commandName) {
            CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
            CommandAvailability availability = cles.getCommandAvailability(commandName);
            if (!availability.isAvailable()) {
                return null;
            }

            try {
                makeFiles(blob, targetExt);

                callImageMagick();

                Blob targetBlob = new FileBlob(targetFile);
                Framework.trackFile(targetFile, targetBlob);
                return targetBlob;
            } catch (Exception e) {
                log.error("ImageMagick failed on command: " + commandName, e);
                return null;
            } finally {
                if (tmpFile != null) {
                    tmpFile.delete();
                }
            }
        }

        protected void makeFiles(Blob blob, String targetExt) throws Exception {
            sourceFile = BlobHelper.getFileFromBlob(blob);

            // check extension
            String ext = FilenameUtils.getExtension(blob.getFilename());
            if (ext == null || "".equals(ext)) {
                // no known extension
                if (sourceFile == null) {
                    sourceFile = createTempSource(blob, "tmp");
                }
                // detect extension
                ext = ImageIdentifier.getInfo(sourceFile.getPath()).getFormat();
                if (tmpFile == null) {
                    // copy source with proper name
                    sourceFile = createTempSource(blob, ext);
                } else {
                    // rename tmp file
                    File newTmpFile = new File(
                            FilenameUtils.removeExtension(tmpFile.getPath())
                                    + "." + ext);
                    tmpFile.renameTo(newTmpFile);
                    tmpFile = newTmpFile;
                    sourceFile = newTmpFile;
                }
            } else {
                // check that extension on source is correct
                if (sourceFile != null
                        && !ext.equals(FilenameUtils.getExtension(sourceFile.getName()))) {
                    sourceFile = null;
                }
            }

            if (sourceFile == null) {
                sourceFile = createTempSource(blob, ext);
            }

            if (targetExt == null) {
                targetExt = ext;
            }
            targetFile = File.createTempFile("nuxeoImageTarget", "."
                    + targetExt);
        }

        protected File createTempSource(Blob blob, String ext)
                throws IOException {
            tmpFile = File.createTempFile("nuxeoImageSource", "." + ext);
            blob.transferTo(tmpFile);
            return tmpFile;
        }

        public abstract void callImageMagick() throws Exception;
    }

    @Override
    public Blob crop(Blob blob, final int x, final int y, final int width,
            final int height) {
        return new ImageMagickCaller() {
            @Override
            public void callImageMagick() throws Exception {
                ImageCropper.crop(sourceFile.getAbsolutePath(),
                        targetFile.getAbsolutePath(), width, height, x, y);
            }
        }.call(blob, null, "resizer");
    }

    @Override
    public Blob resize(Blob blob, String finalFormat, final int width,
            final int height, final int depth) {
        return new ImageMagickCaller() {
            @Override
            public void callImageMagick() throws Exception {
                ImageResizer.resize(sourceFile.getAbsolutePath(),
                        targetFile.getAbsolutePath(), width, height, depth);
            }
        }.call(blob, finalFormat, "resizer");
    }

    @Override
    public Blob rotate(Blob blob, final int angle) {
        return new ImageMagickCaller() {
            @Override
            public void callImageMagick() throws Exception {
                ImageRotater.rotate(sourceFile.getAbsolutePath(),
                        targetFile.getAbsolutePath(), angle);
            }
        }.call(blob, null, "rotate");
    }

    @Override
    public boolean isAvailable() {
        CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
        CommandAvailability commandAvailability = cles.getCommandAvailability("identify");
        return commandAvailability.isAvailable();
    }

}
