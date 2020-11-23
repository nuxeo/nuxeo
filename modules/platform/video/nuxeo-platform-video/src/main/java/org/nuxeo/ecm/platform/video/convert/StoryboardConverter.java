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
 *     YannisJULIENNE
 */
package org.nuxeo.ecm.platform.video.convert;

import static org.nuxeo.ecm.platform.video.convert.Constants.INPUT_FILE_PATH_PARAMETER;
import static org.nuxeo.ecm.platform.video.convert.Constants.OUTPUT_FILE_PATH_PARAMETER;
import static org.nuxeo.ecm.platform.video.convert.Constants.POSITION_PARAMETER;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolderWithProperties;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandException;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.runtime.api.Framework;

/**
 * Converter to extract a list of equally spaced JPEG thumbnails to represent the story-line of a movie file using the
 * ffmpeg commandline tool.
 *
 * @author ogrisel
 */
public class StoryboardConverter implements Converter {

    public static final Log log = LogFactory.getLog(StoryboardConverter.class);

    /** @deprecated since 11.1, not used. */
    @Deprecated
    public static final String FFMPEG_INFO_COMMAND = "ffmpeg-info";

    public static final String FFMPEG_SCREENSHOT_RESIZE_COMMAND = "ffmpeg-screenshot-resize";

    /**
     * @since 11.1
     */
    public static final String ORIGINAL_WIDTH_PARAM = "original_width";

    /**
     * @since 11.1
     */
    public static final String ORIGINAL_HEIGHT_PARAM = "original_height";

    public static final String WIDTH_PARAM = "width";

    public static final String HEIGHT_PARAM = "height";

    public static final String THUMBNAIL_NUMBER_PARAM = "thumbnail_number";

    protected Map<String, String> commonParams = new HashMap<>();

    @Override
    public void init(ConverterDescriptor descriptor) {
        commonParams = descriptor.getParameters();
    }

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) {
        // Build the empty output structure
        Map<String, Serializable> properties = new HashMap<>();
        List<Blob> blobs = new ArrayList<>();
        List<Double> timecodes = new ArrayList<>();
        List<String> comments = new ArrayList<>();
        properties.put("timecodes", (Serializable) timecodes);
        properties.put("comments", (Serializable) comments);
        SimpleBlobHolderWithProperties bh = new SimpleBlobHolderWithProperties(blobs, properties);

        Blob blob = blobHolder.getBlob();
        try (CloseableFile source = blob.getCloseableFile("." + FilenameUtils.getExtension(blob.getFilename()))) {

            CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
            CmdParameters params = cles.getDefaultCmdParameters();
            params.addNamedParameter(INPUT_FILE_PATH_PARAMETER, source.getFile().getAbsolutePath());

            Double duration = (Double) parameters.get("duration");
            if (duration == null) {
                log.warn(String.format("Cannot extract storyboard for file '%s'" + " with missing duration info.",
                        blob.getFilename()));
                return bh;
            }

            // add the command line parameters for the storyboard extraction and run it
            int numberOfThumbnails = getNumberOfThumbnails(parameters);
            for (int i = 0; i < numberOfThumbnails; i++) {
                double timecode = BigDecimal.valueOf(i * duration / numberOfThumbnails)
                                            .setScale(2, RoundingMode.HALF_UP)
                                            .doubleValue();
                Blob thumbBlob = Blobs.createBlobWithExtension(".jpeg");
                params.addNamedParameter(OUTPUT_FILE_PATH_PARAMETER, thumbBlob.getFile().getAbsolutePath());
                params.addNamedParameter(POSITION_PARAMETER, String.valueOf(timecode));
                fillWidthAndHeightParameters(params, parameters);
                ExecResult result = cles.execCommand(FFMPEG_SCREENSHOT_RESIZE_COMMAND, params);
                if (!result.isSuccessful()) {
                    throw result.getError();
                }
                thumbBlob.setMimeType("image/jpeg");
                thumbBlob.setFilename(String.format(Locale.ENGLISH, "%.2f-seconds.jpeg", timecode));
                blobs.add(thumbBlob);
                timecodes.add(timecode);
                comments.add(String.format("%s %d", blob.getFilename(), i));
            }
            return bh;
        } catch (IOException | CommandNotAvailable | CommandException e) {
            throw new ConversionException("Error extracting story board from '" + blob.getFilename() + "'", e);
        }
    }

    protected int getNumberOfThumbnails(Map<String, Serializable> parameters) {
        int numberOfThumbnails = 9;
        if (parameters.containsKey(THUMBNAIL_NUMBER_PARAM)) {
            numberOfThumbnails = (int) parameters.get(THUMBNAIL_NUMBER_PARAM);
        }
        // param from converter descriptor still overrides the video service configuration to keep compat
        if (commonParams.containsKey(THUMBNAIL_NUMBER_PARAM)) {
            numberOfThumbnails = Integer.parseInt(commonParams.get(THUMBNAIL_NUMBER_PARAM));
        }
        if (numberOfThumbnails < 1) {
            numberOfThumbnails = 1;
        }
        return numberOfThumbnails;
    }

    protected void fillWidthAndHeightParameters(CmdParameters cmdParameters, Map<String, Serializable> parameters) {
        String screenshotWidth = commonParams.getOrDefault(WIDTH_PARAM, "130");
        String screenshotHeight = commonParams.getOrDefault(HEIGHT_PARAM, "80");
        if (parameters.containsKey(ORIGINAL_WIDTH_PARAM) && parameters.containsKey(ORIGINAL_HEIGHT_PARAM)) {
            long originalWidth = (long) parameters.get(ORIGINAL_WIDTH_PARAM);
            long originalHeight = (long) parameters.get(ORIGINAL_HEIGHT_PARAM);
            if (originalHeight > originalWidth) {
                // invert screenshot size
                String temp = screenshotWidth;
                screenshotWidth = screenshotHeight;
                screenshotHeight = temp;
            }
        }
        cmdParameters.addNamedParameter(WIDTH_PARAM, screenshotWidth);
        cmdParameters.addNamedParameter(HEIGHT_PARAM, screenshotHeight);
    }
}
