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

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.nuxeo.ecm.platform.video.convert.Constants.INPUT_FILE_PATH_PARAMETER;
import static org.nuxeo.ecm.platform.video.convert.Constants.OUTPUT_FILE_NAME_PARAMETER;
import static org.nuxeo.ecm.platform.video.convert.Constants.OUTPUT_FILE_PATH_PARAMETER;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.NuxeoException;
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
public abstract class BaseVideoConversionConverter extends CommandLineBasedConverter {

    protected final String OUTPUT_TMP_PATH = "converterTmpPath";

    @Override
    protected Map<String, Blob> getCmdBlobParameters(BlobHolder blobHolder,
            Map<String, Serializable> stringSerializableMap) throws ConversionException {
        Map<String, Blob> cmdBlobParams = new HashMap<String, Blob>();
        cmdBlobParams.put(INPUT_FILE_PATH_PARAMETER, blobHolder.getBlob());
        return cmdBlobParams;
    }

    @Override
    protected Map<String, String> getCmdStringParameters(BlobHolder blobHolder, Map<String, Serializable> parameters)
            throws ConversionException {
        Map<String, String> cmdStringParams = new HashMap<String, String>();

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
        outFile.delete();
        Framework.trackFile(outFile, this);
        cmdStringParams.put(OUTPUT_FILE_PATH_PARAMETER, outFile.getAbsolutePath());
        String baseName = FilenameUtils.getBaseName(blobHolder.getBlob().getFilename());
        cmdStringParams.put(OUTPUT_FILE_NAME_PARAMETER, baseName + getVideoExtension());
        cmdStringParams.put(OUTPUT_TMP_PATH, outDir.getAbsolutePath());

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
    }

    @Override
    protected BlobHolder buildResult(List<String> cmdOutput, CmdParameters cmdParameters) throws ConversionException {
        String outputPath = cmdParameters.getParameter(OUTPUT_FILE_PATH_PARAMETER);
        File outputFile = new File(outputPath);
        List<Blob> blobs = new ArrayList<Blob>();
        String outFileName = cmdParameters.getParameter(OUTPUT_FILE_NAME_PARAMETER);
        if (outFileName == null) {
            outFileName = outputFile.getName();
        } else {
            outFileName = unquoteValue(outFileName);
        }

        String ext = "." + FilenameUtils.getExtension(outputPath);
        Blob blob;
        try {
            blob = Blobs.createBlobWithExtension(ext); // automatically tracked for removal
            Files.move(Paths.get(outputPath), blob.getFile().toPath(), REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ConversionException("Cannot create blob", e);
        }
        blob.setMimeType(getVideoMimeType());
        blob.setFilename(outFileName);
        blobs.add(blob);

        Map<String, Serializable> properties = new HashMap<String, Serializable>();
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
