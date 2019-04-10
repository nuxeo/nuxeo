/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
public class ScreenshotConverter extends BaseVideoConverter implements Converter {

    public static final Log log = LogFactory.getLog(ScreenshotConverter.class);

    public static final String FFMPEG_SCREENSHOT_COMMAND = "ffmpeg-screenshot";

    @Override
    public void init(ConverterDescriptor descriptor) {
    }

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {

        Blob blob = blobHolder.getBlob();
        try (CloseableFile source = blob.getCloseableFile("." + FilenameUtils.getExtension(blob.getFilename()))) {
            Blob outBlob = Blobs.createBlobWithExtension(".jpeg");

            CmdParameters params = new CmdParameters();
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
            CommandLineExecutorService cleService = Framework.getService(CommandLineExecutorService.class);
            ExecResult res = cleService.execCommand(FFMPEG_SCREENSHOT_COMMAND, params);
            if (!res.isSuccessful()) {
                throw res.getError();
            }

            outBlob.setMimeType("image/jpeg");
            outBlob.setFilename(String.format("video-screenshot-%05d.000.jpeg", positionParam));
            return new SimpleCachableBlobHolder(outBlob);
        } catch (CommandNotAvailable | IOException | CommandException e) {
            String msg;
            if (blob != null) {
                msg = "error extracting screenshot from '" + blob.getFilename() + "'";
            } else {
                msg = "conversion failed";
            }
            throw new ConversionException(msg, e);
        }
    }

}
