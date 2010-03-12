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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.picture.api.adapters.AbstractPictureAdapter;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.ecm.platform.video.convert.Constants;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper class to factorize logic than can be either called from the UI or from
 * core event listener.
 *
 * If the need to evolve to make this furter configurable (not just using the
 * existing converter / commandline extensions points), we might want to turn
 * this class into a full blown nuxeo service.
 *
 * @author ogrisel
 */
public class VideoHelper {

    // TODO: make this configurable somehow though an extension point. The
    // imaging package need a similar refactoring, try to make both consistent
    public static final ArrayList<Map<String, Object>> THUMBNAILS_VIEWS = new ArrayList<Map<String, Object>>();
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
            throws ConversionException, PropertyException, ClientException {
        if (video == null) {
            docModel.setPropertyValue(VideoConstants.STORYBOARD_PROPERTY, null);
            docModel.setPropertyValue(VideoConstants.DURATION_PROPERTY, null);
            return;
        }

        BlobHolder result;
        try {
            result = Framework.getService(ConversionService.class).convert(
                    Constants.STORYBOARD_CONVERTER,
                    new SimpleBlobHolder(video), null);
        } catch (Exception e) {
            throw new ConversionException(e.getMessage(), e);
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
        docModel.setPropertyValue(VideoConstants.DURATION_PROPERTY,
                result.getProperty("duration"));
    }

    /**
     * Update the JPEG previews of a Video document from the video blob content
     * by taking a screen-shot of the movie at timecode offset given in seconds.
     */
    public static void updatePreviews(DocumentModel docModel, Blob video,
            Double position, List<Map<String, Object>> templates)
            throws ConversionException, ClientException, IOException {

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
        } catch (Exception e) {
            throw new ConversionException(e.getMessage(), e);
        }

        // compute the thumbnail preview
        if (result != null && result.getBlob() != null) {
            PictureResourceAdapter picture = docModel.getAdapter(PictureResourceAdapter.class);
            picture.createPicture(result.getBlob(),
                    result.getBlob().getFilename(), docModel.getTitle(),
                    new ArrayList<Map<String, Object>>(templates));
        } else {
            // TODO: put a set of default thumbnails here to tell the user that
            // the preview is not available for this document
        }
    }

    /**
     * Update the JPEG previews of a Video document from the video blob content
     * by taking a screen-shot of the movie at 10% of the duration to avoid
     * black screen fade in video.
     */
    public static void updatePreviews(DocumentModel docModel, Blob video)
            throws ConversionException, ClientException, IOException {

        Double duration = (Double) docModel.getPropertyValue(VideoConstants.DURATION_PROPERTY);
        Double position = 0.0;
        if (duration != null) {
            position = duration * 0.1;
        }
        updatePreviews(docModel, video, position, THUMBNAILS_VIEWS);
    }

}
