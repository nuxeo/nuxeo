/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.video.convert;

import static org.nuxeo.ecm.platform.video.convert.Constants.INPUT_FILE_PATH_PARAMETER;
import static org.nuxeo.ecm.platform.video.convert.Constants.OUTPUT_FILE_NAME_PARAMETER;
import static org.nuxeo.ecm.platform.video.convert.Constants.OUTPUT_FILE_PATH_PARAMETER;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolderWithProperties;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.convert.plugins.CommandLineBasedConverter;
import org.nuxeo.ecm.platform.video.VideoInfo;
import org.nuxeo.runtime.api.Framework;

/**
 * Base class for converters doing video conversions.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public abstract class BaseVideoConversionConverter extends
        CommandLineBasedConverter {

    @Override
    protected Map<String, Blob> getCmdBlobParameters(BlobHolder blobHolder,
            Map<String, Serializable> stringSerializableMap)
            throws ConversionException {
        Map<String, Blob> cmdBlobParams = new HashMap<String, Blob>();
        try {
            cmdBlobParams.put(INPUT_FILE_PATH_PARAMETER, blobHolder.getBlob());
        } catch (ClientException e) {
            throw new ConversionException("Unable to get Blob for holder", e);
        }
        return cmdBlobParams;
    }

    @Override
    protected Map<String, String> getCmdStringParameters(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {
        Map<String, String> cmdStringParams = new HashMap<String, String>();

        String baseDir = getTmpDirectory(parameters);
        Path tmpPath = new Path(baseDir).append(getTmpDirectoryPrefix() + "_"
                + UUID.randomUUID());

        File outDir = new File(tmpPath.toString());
        boolean dirCreated = outDir.mkdir();
        if (!dirCreated) {
            throw new ConversionException(
                    "Unable to create tmp dir for transformer output: "
                            + outDir);
        }

        try {
            File outFile = File.createTempFile("videoConversion", getVideoExtension(), outDir);
            // delete the file as we need only the path for ffmpeg
            outFile.delete();
            Framework.trackFile(outFile, this);
            cmdStringParams.put(OUTPUT_FILE_PATH_PARAMETER,
                outFile.getAbsolutePath());
            String baseName = FilenameUtils.getBaseName(blobHolder.getBlob().getFilename());
            cmdStringParams.put(OUTPUT_FILE_NAME_PARAMETER, baseName + getVideoExtension());

            VideoInfo videoInfo = (VideoInfo) parameters.get("videoInfo");
            if (videoInfo == null) {
                return cmdStringParams;
            }

            long width = videoInfo.getWidth();
            long height = videoInfo.getHeight();
            long newHeight = (Long) parameters.get("height");

            long newWidth = width * newHeight / height;
            if (newWidth % 2 != 0) {
                newWidth += 1;
            }

            cmdStringParams.put("width", String.valueOf(newWidth));
            cmdStringParams.put("height", String.valueOf(newHeight));
            return cmdStringParams;
        } catch (Exception e) {
            throw new ConversionException("Unable to get Blob for holder", e);
        }
    }

    @Override
    protected BlobHolder buildResult(List<String> cmdOutput,
            CmdParameters cmdParameters) throws ConversionException {
        String outputPath = cmdParameters.getParameters().get(
                OUTPUT_FILE_PATH_PARAMETER);
        File outputFile = new File(outputPath);
        List<Blob> blobs = new ArrayList<Blob>();
        String outFileName = cmdParameters.getParameters().get(OUTPUT_FILE_NAME_PARAMETER);
        if (outFileName == null) {
            outFileName = outputFile.getName();
        } else {
            outFileName = unquoteValue(outFileName);
        }

        Blob blob = new FileBlob(outputFile);
        blob.setFilename(outFileName);
        blob.setMimeType(getVideoMimeType());
        blobs.add(blob);

        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put("cmdOutput", (Serializable) cmdOutput);
        return new SimpleBlobHolderWithProperties(blobs, properties);
    }

    /**
     * @since 5.6
     */
    protected String unquoteValue(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    protected abstract String getVideoMimeType();

    protected abstract String getVideoExtension();

    protected abstract String getTmpDirectoryPrefix();

}
