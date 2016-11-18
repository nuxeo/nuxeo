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
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.runtime.api.Framework;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * The {@link VideoTool} for adding a watermark to a video blob.
 * @since 8.4
 */
public class VideoWatermarker extends VideoTool {

    public final static String NAME = "watermarkerTool";

    public final static String WATERMARK_X_POSITION_PARAM = "x";

    public final static String WATERMARK_Y_POSITION_PARAM = "y";

    public final static String WATERMARK_PARAM = "pictureFilePath";

    protected final static String FILTER_COMPLEX_PARAM = "filterComplex";

    protected final static String VIDEO_WATERMARKER_COMMANDLINE = "videoWatermarkWithPicture";

    public VideoWatermarker() {
        super(NAME, VIDEO_WATERMARKER_COMMANDLINE);
    }

    @Override
    public Map<String, String> setupParameters(BlobHolder input, Map<String, Object> parameters) {
        Map<String, String> cmdParameters = super.setupParameters(input,parameters);

        Blob video = input.getBlob();
        String x = (String) parameters.get(WATERMARK_X_POSITION_PARAM);
        String y = (String) parameters.get(WATERMARK_Y_POSITION_PARAM);
        Blob watermark = (Blob) parameters.get(WATERMARK_PARAM);

        // Prepare parameters
        try {
            String overlay = "overlay=" + x + ":" + y;
            File outputFile = Framework.createTempFile(FilenameUtils.removeExtension(video.getFilename()),
                    "-WM." + FilenameUtils.getExtension(video.getFilename()));

            cmdParameters.put(WATERMARK_PARAM, watermark.getFile().getAbsolutePath());
            cmdParameters.put(FILTER_COMPLEX_PARAM, overlay);
            cmdParameters.put(OUTPUT_FILE_PATH_PARAM, outputFile.getAbsolutePath());
        } catch (IOException e) {
            throw new NuxeoException("Cannot setup parameters for VideoWatermarker.", e);
        }
        return cmdParameters;
    }
}
