/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.video;

import java.io.File;
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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.picture.api.adapters.AbstractPictureAdapter;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.ecm.platform.video.convert.Constants;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper class to factorize logic than can be either called from the UI or from
 * core event listener.
 * <p>
 * If the need to evolve to make this further configurable (not just using the
 * existing converter / commandline extensions points), we might want to turn
 * this class into a full blown nuxeo service.
 *
 * @author ogrisel
 */
public class VideoHelper {

    public static final Log log = LogFactory.getLog(VideoHelper.class);

    public static final String MISSING_PREVIEW_PICTURE = "preview/missing-video-preview.jpeg";

    public static final String FFMPEG_INFO_COMMAND_LINE = "ffmpeg-info";

    // TODO: make this configurable somehow though an extension point. The
    // imaging package need a similar refactoring, try to make both consistent
    public static final ArrayList<Map<String, Object>> THUMBNAILS_VIEWS = new ArrayList<Map<String, Object>>();

    // Utility class.
    private VideoHelper() {
    }

    static {
        Map<String, Object> thumbnailView = new LinkedHashMap<String, Object>();
        thumbnailView.put("title", "Thumbnail");
        thumbnailView.put("maxsize",
                Long.valueOf(AbstractPictureAdapter.THUMB_SIZE));
        THUMBNAILS_VIEWS.add(thumbnailView);
        Map<String, Object> staticPlayerView = new HashMap<String, Object>();
        staticPlayerView.put("title", "StaticPlayerView");
        staticPlayerView.put("maxsize",
                Long.valueOf(AbstractPictureAdapter.MEDIUM_SIZE));
        THUMBNAILS_VIEWS.add(staticPlayerView);
    }

    /**
     * Update the JPEG story board and duration in seconds of a Video document
     * from the video blob content.
     */
    @SuppressWarnings("unchecked")
    public static void updateStoryboard(DocumentModel docModel, Blob video)
            throws PropertyException, ClientException {
        if (video == null) {
            docModel.setPropertyValue(VideoConstants.STORYBOARD_PROPERTY, null);
            docModel.setPropertyValue(VideoConstants.DURATION_PROPERTY, null);
            return;
        }

        BlobHolder result;
        try {

            VideoDocument videoDocument = docModel.getAdapter(VideoDocument.class);
            Map<String, Serializable> parameters = new HashMap<String, Serializable>();
            parameters.put("duration", videoDocument.getVideo().getDuration());
            result = Framework.getService(ConversionService.class).convert(
                    Constants.STORYBOARD_CONVERTER,
                    new SimpleBlobHolder(video), parameters);
        } catch (ConversionException e) {
            // this can happen when if the codec is not supported or not
            // readable by ffmpeg and is recoverable by using a dummy preview
            log.warn(String.format(
                    "could not extract story board for document '%s' with video file '%s': %s",
                    docModel.getTitle(), video.getFilename(), e.getMessage()));
            log.debug(e, e);
            return;
        } catch (Exception e) {
            throw ClientException.wrap(e);
        }
        List<Blob> blobs = result.getBlobs();
        List<String> comments = (List<String>) result.getProperty("comments");
        List<Double> timecodes = (List<Double>) result.getProperty("timecodes");
        List<Map<String, Serializable>> storyboard = new ArrayList<Map<String, Serializable>>();
        for (int i = 0; i < blobs.size(); i++) {
            Map<String, Serializable> item = new HashMap<String, Serializable>();
            item.put("comment", comments.get(i));
            item.put("timecode", timecodes.get(i));
            item.put("content", (Serializable) blobs.get(i));
            storyboard.add(item);
        }
        docModel.setPropertyValue(VideoConstants.STORYBOARD_PROPERTY,
                (Serializable) storyboard);
    }

    /**
     * Update the JPEG previews of a Video document from the video blob content
     * by taking a screen-shot of the movie at timecode offset given in seconds.
     */
    public static void updatePreviews(DocumentModel docModel, Blob video,
            Double position, List<Map<String, Object>> templates)
            throws ClientException, IOException {

        if (video == null) {
            docModel.setPropertyValue("picture:views", null);
            return;
        }

        Map<String, Serializable> parameters = new HashMap<String, Serializable>();
        parameters.put(Constants.POSITION_PARAMETER, position);
        BlobHolder result;
        try {
            result = Framework.getService(ConversionService.class).convert(
                    Constants.SCREENSHOT_CONVERTER,
                    new SimpleBlobHolder(video), parameters);
        } catch (ConversionException e) {
            // this can happen when if the codec is not supported or not
            // readable by ffmpeg and is recoverable by using a dummy preview
            log.warn(String.format(
                    "could not extract screenshot from document '%s' with video file '%s': %s",
                    docModel.getTitle(), video.getFilename(), e.getMessage()));
            log.debug(e, e);
            return;
        } catch (Exception e) {
            throw ClientException.wrap(e);
        }

        // compute the thumbnail preview
        if (result != null && result.getBlob() != null
                && result.getBlob().getLength() > 0) {
            PictureResourceAdapter picture = docModel.getAdapter(PictureResourceAdapter.class);
            try {
                picture.createPicture(result.getBlob(),
                        result.getBlob().getFilename(), docModel.getTitle(),
                        new ArrayList<Map<String, Object>>(templates));
            } catch (Exception e) {
                log.warn("failed to video compute previews for "
                        + docModel.getTitle() + ": " + e.getMessage());
            }
        }

        // put a black screen if the video or its screen-shot is unreadable
        if (docModel.getProperty("picture:views").getValue(List.class).isEmpty()) {
            InputStream is = VideoHelper.class.getResourceAsStream("/"
                    + MISSING_PREVIEW_PICTURE);
            Blob blob = StreamingBlob.createFromStream(is, "image/jpeg").persist();
            blob.setFilename(MISSING_PREVIEW_PICTURE.replace('/', '-'));
            PictureResourceAdapter picture = docModel.getAdapter(PictureResourceAdapter.class);
            picture.createPicture(blob, blob.getFilename(),
                    docModel.getTitle(), new ArrayList<Map<String, Object>>(
                            templates));
        }
    }

    /**
     * Update the JPEG previews of a Video document from the video blob content
     * by taking a screen-shot of the movie at 10% of the duration to avoid
     * black screen fade in video.
     */
    public static void updatePreviews(DocumentModel docModel, Blob video)
            throws ClientException, IOException {

        Double duration = (Double) docModel.getPropertyValue(VideoConstants.DURATION_PROPERTY);
        Double position = 0.0;
        if (duration != null) {
            position = duration * 0.1;
        }
        updatePreviews(docModel, video, position, THUMBNAILS_VIEWS);
    }

    public static void updateVideoInfo(DocumentModel docModel, Blob video)
            throws ClientException {
        VideoInfo videoInfo = getVideoInfo(video);
        if (videoInfo == null) {
            docModel.setPropertyValue("vid:info",
                    (Serializable) VideoInfo.EMPTY_INFO.toMap());
            return;
        }
        docModel.setPropertyValue("vid:info", (Serializable) videoInfo.toMap());
    }

    public static VideoInfo getVideoInfo(Blob video) throws ClientException {
        if (video == null) {
            return null;
        }

        File file = null;
        try {
            CommandLineExecutorService cleService = Framework.getLocalService(CommandLineExecutorService.class);

            file = File.createTempFile("ffmpegInfo",
                    "." + FilenameUtils.getExtension(video.getFilename()));
            video.transferTo(file);

            CmdParameters params = new CmdParameters();
            params.addNamedParameter("inFilePath", file.getAbsolutePath());

            // read the duration with a first command to adjust the best rate:
            ExecResult result = cleService.execCommand(
                    FFMPEG_INFO_COMMAND_LINE, params);
            return VideoInfo.fromFFmpegOutput(result.getOutput());
        } catch (Exception e) {
            throw ClientException.wrap(e);
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }

}
