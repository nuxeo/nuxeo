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
import org.nuxeo.common.utils.StringUtils;
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
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Generate a hinted video for streaming. Hinted track added are used by the
 * streaming server to optimize the flow.
 *
 * @author bjalon
 */
public class StreamableMediaConverter extends BaseVideoConverter implements
        Converter {

    public static final Log log = LogFactory.getLog(StreamableMediaConverter.class);

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
        Blob blob = null;
        InputFile inputFile = null;

        boolean transactionWasActive = TransactionHelper.isTransactionActive();
        if (transactionWasActive) {
            log.debug("Close transaction during streaming media, because conversion can very be long");
            TransactionHelper.commitOrRollbackTransaction();
        }

        try {
            blob = blobHolder.getBlob();
            inputFile = new InputFile(blob);

            // Convert the source to mp4 (h264 + aac)
            outFile = File.createTempFile("StreamableMediaConverter-out-",
                    ".tmp.mp4");
            CmdParameters paramsForStreamable = new CmdParameters();
            paramsForStreamable.addNamedParameter("inFilePath",
                    inputFile.file.getAbsolutePath());
            paramsForStreamable.addNamedParameter("outFilePath",
                    outFile.getAbsolutePath());
            ExecResult resultMp4 = cleService.execCommand(
                    ConverterConstants.HANDBRAKE_CONVERT_MP4,
                    paramsForStreamable);
            if (!resultMp4.isSuccessful()) {
                throw new ConversionException("Failed to build mp4 version of "
                        + blob.getFilename() + ": "
                        + StringUtils.join(resultMp4.getOutput(), " "));
            }
            log.info(String.format(
                    "mp4 conversion of '%s' execution time: %ds",
                    blob.getFilename(), resultMp4.getExecTime() / 1000));
            if (log.isDebugEnabled()) {
                log.debug(StringUtils.join(resultMp4.getOutput(), " "));
            }

            // Hint the resulting mp4 file for streaming
            CmdParameters paramsForHint = new CmdParameters();
            paramsForHint.addNamedParameter("filePath",
                    outFile.getAbsolutePath());
            ExecResult resultHint = cleService.execCommand(
                    ConverterConstants.MP4BOX_HINT_MEDIA, paramsForHint);
            if (!resultHint.isSuccessful()) {
                throw new ConversionException("Failed to hint mp4 version of "
                        + blob.getFilename() + ": "
                        + StringUtils.join(resultHint.getOutput(), " "));
            }
            log.info(String.format(
                    "mp4 hinting of '%s' execution time: %ds",
                    blob.getFilename(), resultHint.getExecTime() / 1000));
            if (log.isDebugEnabled()) {
                log.debug(StringUtils.join(resultHint.getOutput(), " "));
            }

            Blob outBlob = StreamingBlob.createFromStream(
                    new FileInputStream(outFile), "video/mp4").persist();
            outBlob.setFilename(blob.getFilename() + "--streamable.mp4");
            return new SimpleBlobHolder(outBlob);
        } catch (Exception e) {
            if (e instanceof ConversionException) {
                throw (ConversionException) e;
            } else if (blob != null) {
                throw new ConversionException(
                        "error building streamable version for '"
                                + blob.getFilename() + "': " + e.getMessage(),
                        e);
            } else {
                throw new ConversionException(e.getMessage(), e);
            }
        } finally {
            FileUtils.deleteQuietly(outFile);
            if (inputFile != null && inputFile.isTempFile) {
                FileUtils.deleteQuietly(inputFile.file);
            }
            if (transactionWasActive) {
                log.debug("Start a new transaction after video conversion");
                TransactionHelper.startTransaction();
            }
        }
    }
}
