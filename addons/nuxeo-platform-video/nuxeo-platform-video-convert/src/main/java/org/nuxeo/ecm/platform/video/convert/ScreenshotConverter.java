/*
 * (C) Copyright 2010-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.video.convert;

import static org.nuxeo.ecm.platform.video.convert.Constants.POSITION_PARAMETER;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandException;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.runtime.api.Framework;

/**
 * Extract a JPEG screenshot of the video at a given time offset (position).
 *
 * @author ogrisel
 */
public class ScreenshotConverter implements Converter {

    public static final Log log = LogFactory.getLog(ScreenshotConverter.class);

    public static final String FFMPEG_SCREENSHOT_COMMAND = "ffmpeg-screenshot";

    @Override
    public void init(ConverterDescriptor descriptor) {
    }

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {
        Blob blob = blobHolder.getBlob();
        if (blob == null) {
            throw new ConversionException("conversion failed (null blob)");
        }
        try (CloseableFile source = blob.getCloseableFile("." + FilenameUtils.getExtension(blob.getFilename()))) {
            Blob outBlob = Blobs.createBlobWithExtension(".jpeg");

            CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
            CmdParameters params = cles.getDefaultCmdParameters();
            params.addNamedParameter("inFilePath", source.getFile().getAbsolutePath());
            params.addNamedParameter("outFilePath", outBlob.getFile().getAbsolutePath());
            Double position = 0.0;
            if (parameters != null) {
                position = (Double) parameters.get(POSITION_PARAMETER);
                if (position == null) {
                    position = 0.0;
                }
            }
            long positionParam = Math.round(position);
            params.addNamedParameter(POSITION_PARAMETER, String.valueOf(positionParam));
            ExecResult res = cles.execCommand(FFMPEG_SCREENSHOT_COMMAND, params);
            if (!res.isSuccessful()) {
                throw res.getError();
            }

            outBlob.setMimeType("image/jpeg");
            outBlob.setFilename(String.format("video-screenshot-%05d.000.jpeg", positionParam));
            return new SimpleCachableBlobHolder(outBlob);
        } catch (CommandNotAvailable | IOException | CommandException e) {
            throw new ConversionException("error extracting screenshot from '" + blob.getFilename() + "'", e);
        }
    }

}
