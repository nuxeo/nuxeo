/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.video.convert;

import static org.nuxeo.ecm.platform.video.convert.Constants.INPUT_FILE_PATH_PARAMETER;
import static org.nuxeo.ecm.platform.video.convert.Constants.OUTPUT_FILE_NAME_PARAMETER;
import static org.nuxeo.ecm.platform.video.convert.Constants.OUTPUT_FILE_PATH_PARAMETER;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolderWithProperties;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.convert.plugins.CommandLineBasedConverter;
import org.nuxeo.ecm.platform.video.VideoInfo;
import org.nuxeo.ecm.platform.video.tools.VideoTool;

/**
 * Base class for converters doing video conversions.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public abstract class BaseVideoConversionConverter extends CommandLineBasedConverter {

    protected static final String OUTPUT_TMP_PATH = "converterTmpPath";

    @Override
    protected Map<String, Blob> getCmdBlobParameters(BlobHolder blobHolder,
            Map<String, Serializable> stringSerializableMap) {
        Map<String, Blob> cmdBlobParams = new HashMap<>();
        cmdBlobParams.put(INPUT_FILE_PATH_PARAMETER, blobHolder.getBlob());
        return cmdBlobParams;
    }

    @Override
    protected Map<String, String> getCmdStringParameters(BlobHolder blobHolder, Map<String, Serializable> parameters) {
        Map<String, String> cmdStringParams = new HashMap<>();

        String baseDir = getTmpDirectory(parameters);
        Path tmpPath = new Path(baseDir).append(getTmpDirectoryPrefix() + "_" + UUID.randomUUID());

        File outDir = new File(tmpPath.toString());
        boolean dirCreated = outDir.mkdir();
        if (!dirCreated) {
            throw new ConversionException("Unable to create tmp dir for transformer output: " + outDir);
        }

        File outFile;
        try {
            outFile = File.createTempFile("videoConversion", getVideoExtension(), outDir);
        } catch (IOException e) {
            throw new ConversionException("Unable to get Blob for holder", e);
        }
        // delete the file as we need only the path for ffmpeg
        try {
            Files.delete(outFile.toPath());
        } catch (IOException e) {
            throw new ConversionException("Unable to delete the temporary video conversion file.", e);
        }
        cmdStringParams.put(OUTPUT_FILE_PATH_PARAMETER, outFile.getAbsolutePath());
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

        cmdStringParams.put(OUTPUT_TMP_PATH, outDir.getAbsolutePath());
        cmdStringParams.put("width", String.valueOf(newWidth));
        cmdStringParams.put("height", String.valueOf(newHeight));
        return cmdStringParams;
    }

    @Override
    protected BlobHolder buildResult(List<String> cmdOutput, CmdParameters cmdParameters) {
        String outputPath = cmdParameters.getParameter(OUTPUT_FILE_PATH_PARAMETER);
        Blob blob = VideoTool.getTemporaryBlob(outputPath, getVideoMimeType());
        String outFileName = cmdParameters.getParameter(OUTPUT_FILE_NAME_PARAMETER);
        if (outFileName != null) {
            blob.setFilename(unquoteValue(outFileName));
        }
        List<Blob> blobs = new ArrayList<>(Collections.singletonList(blob));
        Map<String, Serializable> properties = new HashMap<>();
        properties.put("cmdOutput", (Serializable) cmdOutput);

        // remove the temp output directory
        File outDir = new File(cmdParameters.getParameter(OUTPUT_TMP_PATH));
        FileUtils.deleteQuietly(outDir);

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

    /**
     * Returns the video mime type to use for this converter.
     */
    protected abstract String getVideoMimeType();

    /**
     * Returns the video extension, always prefixed by '.'.
     */
    protected abstract String getVideoExtension();

    /**
     * Returns the temporary directory prefix to use for this converter.
     */
    protected abstract String getTmpDirectoryPrefix();

}
