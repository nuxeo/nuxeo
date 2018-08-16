/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandException;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.picture.core.ImageUtils;
import org.nuxeo.ecm.platform.picture.magick.utils.ImageConverter;
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
            CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
            CommandAvailability availability = cles.getCommandAvailability(commandName);
            if (!availability.isAvailable()) {
                return null;
            }

            try {
                makeFiles(blob, targetExt);

                callImageMagick();

                Blob targetBlob = Blobs.createBlob(targetFile);
                targetBlob.setFilename(getFilename(blob, targetExt));
                Framework.trackFile(targetFile, targetBlob);
                return targetBlob;
            } catch (CommandNotAvailable | CommandException | IOException e) {
                log.error("ImageMagick failed on command: " + commandName, e);
                return null;
            } finally {
                if (tmpFile != null) {
                    tmpFile.delete();
                }
            }
        }

        protected void makeFiles(Blob blob, String targetExt) throws CommandNotAvailable, CommandException, IOException {
            sourceFile = blob.getFile();

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
                    File newTmpFile = new File(FilenameUtils.removeExtension(tmpFile.getPath()) + "." + ext);
                    tmpFile.renameTo(newTmpFile);
                    tmpFile = newTmpFile;
                    sourceFile = newTmpFile;
                }
            } else {
                // check that extension on source is correct
                if (sourceFile != null && !ext.equals(FilenameUtils.getExtension(sourceFile.getName()))) {
                    sourceFile = null;
                }
            }

            if (sourceFile == null) {
                sourceFile = createTempSource(blob, ext);
            }

            if (targetExt == null) {
                targetExt = ext;
            }
            targetFile = Framework.createTempFile("nuxeoImageTarget", "." + targetExt);
        }

        protected File createTempSource(Blob blob, String ext) throws IOException {
            tmpFile = Framework.createTempFile("nuxeoImageSource", "." + ext);
            blob.transferTo(tmpFile);
            return tmpFile;
        }

        protected String getFilename(Blob blob, String targetExt) {
            String baseName = FilenameUtils.getBaseName(blob.getFilename());
            return baseName + "." + targetExt;
        }

        public abstract void callImageMagick() throws CommandNotAvailable, CommandException;
    }

    @Override
    public Blob crop(Blob blob, final int x, final int y, final int width, final int height) {
        return new ImageMagickCaller() {
            @Override
            public void callImageMagick() throws CommandNotAvailable, CommandException {
                ImageCropper.crop(sourceFile.getAbsolutePath(), targetFile.getAbsolutePath(), width, height, x, y);
            }
        }.call(blob, null, "resizer");
    }

    @Override
    public Blob resize(Blob blob, String finalFormat, final int width, final int height, final int depth) {
        return new ImageMagickCaller() {
            @Override
            public void callImageMagick() throws CommandNotAvailable, CommandException {
                ImageResizer.resize(sourceFile.getAbsolutePath(), targetFile.getAbsolutePath(), width, height, depth);
            }
        }.call(blob, finalFormat, "resizer");
    }

    @Override
    public Blob rotate(Blob blob, final int angle) {
        return new ImageMagickCaller() {
            @Override
            public void callImageMagick() throws CommandNotAvailable, CommandException {
                ImageRotater.rotate(sourceFile.getAbsolutePath(), targetFile.getAbsolutePath(), angle);
            }
        }.call(blob, null, "rotate");
    }

    @Override
    public Blob convertToPDF(Blob blob) {
        return new ImageMagickCaller() {
            @Override
            public void callImageMagick() throws CommandNotAvailable, CommandException {
                ImageConverter.convert(sourceFile.getAbsolutePath(), targetFile.getAbsolutePath());
            }
        }.call(blob, "pdf", "converter");
    }

    @Override
    public boolean isAvailable() {
        CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
        CommandAvailability commandAvailability = cles.getCommandAvailability("identify");
        return commandAvailability.isAvailable();
    }

}
