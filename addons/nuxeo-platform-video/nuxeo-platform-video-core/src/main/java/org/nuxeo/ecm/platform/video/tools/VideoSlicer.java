/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Ricardo Dias
 */
package org.nuxeo.ecm.platform.video.tools;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.runtime.api.Framework;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * The {@link VideoTool} for slicing video blobs.
 *
 * @since 8.4
 */
public class VideoSlicer extends VideoTool {

    public final static String NAME = "slicerTool";

    public final static String START_AT_PARAM = "startAt";

    public final static String DURATION_PARAM = "duration";

    public final static String ENCODE_PARAM = "encode";

    protected final static String SEGMENTS_PATH = "segmentsPath";

    protected static final String COMMAND_SLICER_DEFAULT = "videoSlicer";

    protected static final String COMMAND_SLICER_BY_COPY = "videoSlicerByCopy";

    protected static final String COMMAND_SLICER_SEGMENTS = "videoSlicerSegments";

    protected static final String COMMAND_SLICER_START_AT = "videoSlicerStartAt";

    public VideoSlicer() {
        super(NAME, COMMAND_SLICER_DEFAULT);
    }

    @Override
    public Map<String, String> setupParameters(BlobHolder blobHolder, Map<String, Object> parameters) {
        Map<String, String> cmdParameters = super.setupParameters(blobHolder, parameters);
        Blob video = blobHolder.getBlob();
        String startAt = (String) parameters.get(START_AT_PARAM);
        String duration = (String) parameters.get(DURATION_PARAM);
        boolean encode = (boolean) parameters.get(ENCODE_PARAM);

        boolean useStart = !StringUtils.isEmpty(startAt);
        boolean useDuration = !StringUtils.isEmpty(duration);
        if (useStart) {
            cmdParameters.put(START_AT_PARAM, startAt);

            String finalFilename = setupFilename(video.getFilename(), startAt, duration);
            File outputFile;
            try {
                outputFile = Framework.createTempFile(FilenameUtils.removeExtension(finalFilename),
                        "sliced." + FilenameUtils.getExtension(finalFilename));
            } catch (IOException e) {
                throw new NuxeoException("VideoSlicer could not set up temporary output file.", e);
            }
            Framework.trackFile(outputFile, this);
            cmdParameters.put(OUTPUT_FILE_PATH_PARAM, outputFile.getAbsolutePath());

            // check if the slicer should use the duration
            if (useDuration) {
                cmdParameters.put(DURATION_PARAM, duration);
                commandLineName = encode ? COMMAND_SLICER_DEFAULT : COMMAND_SLICER_BY_COPY;
            } else {
                commandLineName = COMMAND_SLICER_START_AT;
            }
        } else if (useDuration) {
            Path segmentsDirectory;
            try {
                segmentsDirectory = Framework.createTempDirectory("Segments");
            } catch (IOException e) {
                throw new NuxeoException("VideoSlicer could not create temporary directory for video segments", e);
            }
            File segments = segmentsDirectory.toFile();
            File dir = new File(segments.getAbsolutePath(), addSuffixToFileName(video.getFilename(), "-%03d"));
            String outFilePattern = dir.getAbsolutePath();

            cmdParameters.put(OUTPUT_FILE_PATH_PARAM, outFilePattern);
            cmdParameters.put(DURATION_PARAM, duration);
            cmdParameters.put(SEGMENTS_PATH, segments.getAbsolutePath());
            commandLineName = COMMAND_SLICER_SEGMENTS;
        }
        return cmdParameters;
    }

    @Override
    public BlobHolder buildResult(BlobHolder input, Map<String, String> cmdParams) {
        if (commandLineName.equals(COMMAND_SLICER_SEGMENTS)) {
            BlobList parts = new BlobList();
            File segments = new File(cmdParams.get(SEGMENTS_PATH));
            for (File segmentFile : segments.listFiles()) {
                FileBlob fb = new FileBlob(segmentFile);
                fb.setFilename(segmentFile.getName());
                fb.setMimeType(input.getBlob().getMimeType());
                Framework.trackFile(segmentFile, parts);
                parts.add(fb);
            }
            return new SimpleBlobHolder(parts);
        } else {
            Blob result = new FileBlob(new File(cmdParams.get(OUTPUT_FILE_PATH_PARAM)));
            result.setMimeType(input.getBlob().getMimeType());
            result.setFilename(cmdParams.get(OUTPUT_FILE_PATH_PARAM));
            return new SimpleBlobHolder(result);
        }
    }

    private String setupFilename(String filename, String startAt, String duration) {
        String suffix = StringUtils.isEmpty(startAt) ? "" : "-" + startAt.replaceAll(":", "");
        suffix += StringUtils.isEmpty(duration) ? "" : "-" + duration.replaceAll(":", "");
        return addSuffixToFileName(filename, suffix);
    }

    private String addSuffixToFileName(String filename, String suffix) {
        if (StringUtils.isEmpty(filename) || StringUtils.isEmpty(suffix)) {
            return filename;
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) {
            return filename + suffix;
        }
        return filename.substring(0, dotIndex) + suffix + filename.substring(dotIndex);
    }
}
