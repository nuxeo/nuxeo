/*
 * (C) Copyright 2010-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.video;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandException;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.picture.api.adapters.AbstractPictureAdapter;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.ecm.platform.video.convert.Constants;
import org.nuxeo.ecm.platform.video.convert.StoryboardConverter;
import org.nuxeo.ecm.platform.video.service.Configuration;
import org.nuxeo.ecm.platform.video.service.VideoService;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper class to factorize logic than can be either called from the UI or from core event listener.
 * <p>
 * If the need to evolve to make this further configurable (not just using the existing converter / commandline
 * extensions points), we might want to turn this class into a full blown nuxeo service.
 *
 * @author ogrisel
 */
public class VideoHelper {

    public static final Log log = LogFactory.getLog(VideoHelper.class);

    public static final String MISSING_PREVIEW_PICTURE = "preview/missing-video-preview.jpeg";

    public static final String FFMPEG_INFO_COMMAND_LINE = "ffmpeg-info";

    /**
     * @since 7.4
     */
    public static final int DEFAULT_MIN_DURATION_FOR_STORYBOARD = 10;

    /**
     * @since 7.4
     */
    public static final int DEFAULT_NUMBER_OF_THUMBNAILS = 9;

    // TODO NXP-4792 OG: make this configurable somehow though an extension point. The imaging package need a similar
    // refactoring, try to make both consistent
    protected static final List<Map<String, Object>> THUMBNAILS_VIEWS = new ArrayList<>();

    // Utility class.
    private VideoHelper() {
    }

    static {
        Map<String, Object> thumbnailView = new LinkedHashMap<>();
        thumbnailView.put("title", "Small");
        thumbnailView.put("maxsize", Long.valueOf(AbstractPictureAdapter.SMALL_SIZE));
        THUMBNAILS_VIEWS.add(thumbnailView);
        Map<String, Object> staticPlayerView = new HashMap<>();
        staticPlayerView.put("title", "StaticPlayerView");
        staticPlayerView.put("maxsize", Long.valueOf(AbstractPictureAdapter.MEDIUM_SIZE));
        THUMBNAILS_VIEWS.add(staticPlayerView);
    }

    /**
     * Update the JPEG story board and duration in seconds of a Video document from the video blob content.
     */
    @SuppressWarnings("unchecked")
    public static void updateStoryboard(DocumentModel docModel, Blob video) {
        if (video == null) {
            docModel.setPropertyValue(VideoConstants.STORYBOARD_PROPERTY, null);
            docModel.setPropertyValue(VideoConstants.DURATION_PROPERTY, 0);
            return;
        }

        VideoService videoService = Framework.getService(VideoService.class);
        Configuration configuration = videoService.getConfiguration();

        VideoDocument videoDocument = docModel.getAdapter(VideoDocument.class);
        double duration = videoDocument.getVideo().getDuration();
        double storyboardMinDuration = DEFAULT_MIN_DURATION_FOR_STORYBOARD;
        if (configuration != null) {
            storyboardMinDuration = configuration.getStoryboardMinDuration();
        }

        BlobHolder result = null;
        if (storyboardMinDuration >= 0 && duration >= storyboardMinDuration) {
            try {
                Map<String, Serializable> parameters = new HashMap<>();
                parameters.put("duration", duration);
                int numberOfThumbnails = DEFAULT_NUMBER_OF_THUMBNAILS;
                if (configuration != null) {
                    numberOfThumbnails = configuration.getStoryboardThumbnailCount();
                }
                parameters.put(StoryboardConverter.THUMBNAIL_NUMBER_PARAM, numberOfThumbnails);
                parameters.put(StoryboardConverter.ORIGINAL_WIDTH_PARAM, videoDocument.getVideo().getWidth());
                parameters.put(StoryboardConverter.ORIGINAL_HEIGHT_PARAM, videoDocument.getVideo().getHeight());

                result = Framework.getService(ConversionService.class)
                                  .convert(Constants.STORYBOARD_CONVERTER, new SimpleBlobHolder(video), parameters);
            } catch (ConversionException e) {
                // this can happen when if the codec is not supported or not
                // readable by ffmpeg and is recoverable by using a dummy preview
                log.warn(String.format("could not extract story board for document '%s' with video file '%s': %s",
                        docModel.getTitle(), video.getFilename(), e.getMessage()));
                log.debug(e, e);
                return;
            }
        }

        if (result != null) {
            List<Blob> blobs = result.getBlobs();
            List<String> comments = (List<String>) result.getProperty("comments");
            List<Double> timecodes = (List<Double>) result.getProperty("timecodes");
            List<Map<String, Serializable>> storyboard = new ArrayList<>();
            for (int i = 0; i < blobs.size(); i++) {
                Map<String, Serializable> item = new HashMap<>();
                item.put("comment", comments.get(i));
                item.put("timecode", timecodes.get(i));
                item.put("content", (Serializable) blobs.get(i));
                storyboard.add(item);
            }
            docModel.setPropertyValue(VideoConstants.STORYBOARD_PROPERTY, (Serializable) storyboard);
        }
    }

    /**
     * Update the JPEG previews of a Video document from the video blob content by taking a screen-shot of the movie at
     * timecode offset given in seconds.
     */
    public static void updatePreviews(DocumentModel docModel, Blob video, Double position,
            List<Map<String, Object>> templates) throws IOException {
        if (video == null) {
            docModel.setPropertyValue("picture:views", null);
            return;
        }
        Map<String, Serializable> parameters = new HashMap<>();
        parameters.put(Constants.POSITION_PARAMETER, position);
        BlobHolder result;
        try {
            result = Framework.getService(ConversionService.class)
                              .convert(Constants.SCREENSHOT_CONVERTER, new SimpleBlobHolder(video), parameters);
        } catch (ConversionException e) {
            // this can happen when if the codec is not supported or not
            // readable by ffmpeg and is recoverable by using a dummy preview
            log.warn(String.format("could not extract screenshot from document '%s' with video file '%s': %s",
                    docModel.getTitle(), video.getFilename(), e.getMessage()));
            log.debug(e, e);
            return;
        }

        // compute the thumbnail preview
        if (result != null && result.getBlob() != null && result.getBlob().getLength() > 0) {
            PictureResourceAdapter picture = docModel.getAdapter(PictureResourceAdapter.class);
            try {
                picture.fillPictureViews(result.getBlob(), result.getBlob().getFilename(), docModel.getTitle(),
                        new ArrayList<>(templates));
            } catch (IOException e) {
                log.warn("failed to video compute previews for " + docModel.getTitle() + ": " + e.getMessage());
            }
        }

        // put a black screen if the video or its screen-shot is unreadable
        if (docModel.getProperty("picture:views").getValue(List.class).isEmpty()) {
            try (InputStream is = VideoHelper.class.getResourceAsStream("/" + MISSING_PREVIEW_PICTURE)) {
                Blob blob = Blobs.createBlob(is, "image/jpeg");
                blob.setFilename(MISSING_PREVIEW_PICTURE.replace('/', '-'));
                PictureResourceAdapter picture = docModel.getAdapter(PictureResourceAdapter.class);
                picture.fillPictureViews(blob, blob.getFilename(), docModel.getTitle(), new ArrayList<>(templates));
            }
        }
    }

    /**
     * Update the JPEG previews of a Video document from the video blob content by taking a screen-shot of the movie.
     */
    public static void updatePreviews(DocumentModel docModel, Blob video) throws IOException {
        Double duration = (Double) docModel.getPropertyValue(VideoConstants.DURATION_PROPERTY);
        double position = 0.0;
        if (duration != null) {
            VideoService videoService = Framework.getService(VideoService.class);
            Configuration configuration = videoService.getConfiguration();
            if (configuration != null) {
                position = duration * configuration.getPreviewScreenshotInDurationPercent() / 100;
            } else {
                position = duration * 0.1;
            }
        }
        updatePreviews(docModel, video, position, THUMBNAILS_VIEWS);
    }

    public static void updateVideoInfo(DocumentModel docModel, Blob video) {
        VideoInfo videoInfo = getVideoInfo(video);
        if (videoInfo == null || video.getLength() == 0) {
            docModel.setPropertyValue("vid:info", (Serializable) VideoInfo.EMPTY_INFO.toMap());
            return;
        }
        docModel.setPropertyValue("vid:info", (Serializable) videoInfo.toMap());
    }

    public static VideoInfo getVideoInfo(Blob video) {
        if (video == null || video.getLength() == 0) {
            return null;
        }
        try {
            ExecResult result;
            try (CloseableFile cf = video.getCloseableFile("." + FilenameUtils.getExtension(video.getFilename()))) {
                CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
                CmdParameters params = cles.getDefaultCmdParameters();
                params.addNamedParameter("inFilePath", cf.getFile().getAbsolutePath());

                // read the duration with a first command to adjust the best rate:
                result = cles.execCommand(FFMPEG_INFO_COMMAND_LINE, params);
            }
            if (!result.isSuccessful()) {
                throw result.getError();
            }
            return VideoInfo.fromFFmpegOutput(result.getOutput());
        } catch (CommandNotAvailable | CommandException | IOException e) {
            throw new NuxeoException(e);
        }
    }

}
