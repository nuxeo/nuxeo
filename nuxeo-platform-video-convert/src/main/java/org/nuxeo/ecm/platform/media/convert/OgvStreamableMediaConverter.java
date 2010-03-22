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
package org.nuxeo.ecm.platform.media.convert;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.video.convert.BaseVideoConverter;
import org.nuxeo.runtime.api.Framework;

/**
 * Generate a hinted video for streaming. Hinted track added are used by the
 * streaming server to optimize the flow.
 *
 * @author bjalon
 */
public class OgvStreamableMediaConverter extends BaseVideoConverter implements
        Converter {

    public static final Log log = LogFactory.getLog(OgvStreamableMediaConverter.class);

    protected CommandLineExecutorService cleService;

    public void init(ConverterDescriptor descriptor) {
        try {
            cleService = Framework.getService(CommandLineExecutorService.class);
        } catch (Exception e) {
            log.error(e, e);
            return;
        }
    }

    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        File outFile = null;
        File pivotFile = null;
        Blob blob = null;
        InputFile inputFile = null;
        try {
            blob = blobHolder.getBlob();
            inputFile = new InputFile(blob);
            pivotFile = File.createTempFile("StreamableMediaConverter-pivot-",
                    ".tmp.ogv");

            // To ogv that we use as pivot format
            CmdParameters paramsForPivot = new CmdParameters();
            paramsForPivot.addNamedParameter("inFilePath",
                    inputFile.file.getAbsolutePath());
            paramsForPivot.addNamedParameter("outFilePath",
                    pivotFile.getAbsolutePath());
            ExecResult resultPivot = cleService.execCommand(
                    ConverterConstants.FFMPEG_CONVERT, paramsForPivot);
            if (!resultPivot.isSuccessful()) {
                log.debug(resultPivot.getOutput());
                throw new ConversionException(
                        resultPivot.getOutput().toString());
            }
            if (log.isDebugEnabled()) {
                log.debug("Command line for ogv conversion time execution : "
                        + resultPivot.getExecTime());
                log.debug(resultPivot.getOutput());
            }

            // To mp4 H264 + AAC
            outFile = File.createTempFile("StreamableMediaConverter-out-",
                    ".tmp.mp4");
            CmdParameters paramsForStreamable = new CmdParameters();
            paramsForStreamable.addNamedParameter("inFilePath",
                    pivotFile.getAbsolutePath());
            paramsForStreamable.addNamedParameter("outFilePath",
                    outFile.getAbsolutePath());
            ExecResult resultMp4 = cleService.execCommand(
                    ConverterConstants.HANDBRAKE_CONVERT_MP4,
                    paramsForStreamable);
            if (!resultMp4.isSuccessful()) {
                log.debug(resultMp4.getOutput());
                throw new ConversionException(resultMp4.getOutput().toString());
            }
            if (log.isDebugEnabled()) {
                log.debug("Command line for mp4 conversion time execution : "
                        + resultMp4.getExecTime());
                log.debug(resultMp4.getOutput());
            }

            // Hint mp4 file
            CmdParameters paramsForHint = new CmdParameters();
            paramsForHint.addNamedParameter("filePath",
                    outFile.getAbsolutePath());
            ExecResult resultHint = cleService.execCommand(
                    ConverterConstants.MP4BOX_HINT_MEDIA, paramsForHint);
            if (!resultHint.isSuccessful()) {
                log.debug(resultHint.getOutput());
                throw new ConversionException(resultMp4.getOutput().toString());
            }
            if (log.isDebugEnabled()) {
                log.debug("Command line for hint time execution : "
                        + resultHint.getExecTime());
                log.debug(resultHint.getOutput());
            }

            Blob outBlob = StreamingBlob.createFromStream(
                    new FileInputStream(outFile), "video/mp4").persist();
            outBlob.setFilename(String.format("streamable-media.mp4"));

            if (outBlob == null) {
                throw new ConversionException("Conversion returned a blob null");
            }

            return new SimpleBlobHolder(outBlob);
        } catch (Exception e) {
            if (blob != null) {
                throw new ConversionException(
                        "error streamable media genereation from '"
                                + blob.getFilename() + "': " + e.getMessage(),
                        e);
            } else {
                throw new ConversionException(e.getMessage(), e);
            }
        } finally {
            FileUtils.deleteQuietly(outFile);
            FileUtils.deleteQuietly(pivotFile);
            if (inputFile != null && inputFile.isTempFile) {
                FileUtils.deleteQuietly(inputFile.file);
            }
        }
    }
}
