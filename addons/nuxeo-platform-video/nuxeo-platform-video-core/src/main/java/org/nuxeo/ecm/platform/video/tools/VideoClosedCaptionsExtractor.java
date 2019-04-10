/*
 * (C) Copyright 2016-2018 Nuxeo (http://nuxeo.com/) and others.
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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.runtime.api.Framework;

/**
 * The {@link VideoTool} that extracts closed captions.
 *
 * @since 8.4
 */
public class VideoClosedCaptionsExtractor extends VideoTool {

    public final static String NAME = "ccExtractorTool";

    public final static String START_AT_PARAM = "startAt";

    public final static String END_AT_PARAM = "endAt";

    public final static String OUTPUT_FORMAT_PARAM = "outFormat";

    protected static final String COMMANDLINE_FULL_VIDEO = "videoClosedCaptionsExtractor";

    protected static final String COMMANDLINE_SLICED_VIDEO = "videoPartClosedCaptionsExtractor";

    protected static final String DEFAULT_OUTFORMAT = "ttxt";

    protected static final List<String> TEXT_OUTFORMATS = Arrays.asList("srt", "txt", "ttxt");

    public VideoClosedCaptionsExtractor() {
        super(NAME, COMMANDLINE_FULL_VIDEO);
    }

    public List<String> getSupportedFormats() {
        return TEXT_OUTFORMATS;
    }

    public boolean isFormatSupported(String format) {
        return TEXT_OUTFORMATS.contains(format);
    }

    @Override
    public Map<String, String> setupParameters(BlobHolder blobHolder, Map<String, Object> parameters) {
        Map<String, String> cmdParameters = super.setupParameters(blobHolder, parameters);

        Blob video = blobHolder.getBlob();
        String outputFormat = (String) parameters.get(OUTPUT_FORMAT_PARAM);
        String startAt = (String) parameters.get(START_AT_PARAM);
        String endAt = (String) parameters.get(END_AT_PARAM);

        if (StringUtils.isEmpty(outputFormat)) {
            outputFormat = DEFAULT_OUTFORMAT;
        }
        cmdParameters.put(OUTPUT_FORMAT_PARAM, outputFormat);
        cmdParameters.put(OUTPUT_MIMETYPE_PARAM, outputFormat.equals(DEFAULT_OUTFORMAT) ? "text/xml" : "text/plain");

        if (!StringUtils.isBlank(startAt) && !StringUtils.isBlank(endAt)) {
            cmdParameters.put(START_AT_PARAM, startAt);
            cmdParameters.put(END_AT_PARAM, endAt);
            commandLineName = COMMANDLINE_SLICED_VIDEO;
        }

        try {
            File outputFile = Framework.createTempFile(FilenameUtils.removeExtension(video.getFilename()),
                    "-CC." + outputFormat);

            cmdParameters.put(OUTPUT_FILE_PATH_PARAM, outputFile.getAbsolutePath());
        } catch (IOException e) {
            throw new NuxeoException("Cannot setup parameters for VideoClosedCaptionsExtractor", e);
        }
        return cmdParameters;
    }
}
