/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Peter Di Lorenzo
 */

package org.nuxeo.ecm.platform.video.extension;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.filemanager.service.extension.AbstractFileImporter;
import org.nuxeo.ecm.platform.filemanager.utils.FileManagerUtils;
import org.nuxeo.ecm.platform.picture.api.adapters.AbstractPictureAdapter;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.video.convert.Constants;
import org.nuxeo.runtime.api.Framework;

/**
 * This class will create a Document of type "Video" from the uploaded file, if
 * the uploaded file matches any of the mime types listed in the
 * filemanager-plugins.xml file.
 *
 * If an existing document with the same title is found, it will overwrite it
 * and increment the version number if the overwrite flag is set to true;
 * Otherwise, it will generate a new title and create a new Document of type
 * Video with that title.
 *
 */
public class VideoImporter extends AbstractFileImporter {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(VideoImporter.class);

    public static final String VIDEO_TYPE = "Video";

    // TODO: make this not static and configurable somehow?
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

    protected ConversionService cs;

    public DocumentModel create(CoreSession documentManager, Blob content,
            String path, boolean overwrite, String fullname,
            TypeManager typeService) throws ClientException, IOException {

        String filename = FileManagerUtils.fetchFileName(fullname);

        String title = FileManagerUtils.fetchTitle(filename);

        // Check to see if an existing Document with the same title exists.
        DocumentModel docModel = FileManagerUtils.getExistingDocByTitle(
                documentManager, path, title);

        // if overwrite flag is true and the file already exists, overwrite it
        if (overwrite && (docModel != null)) {
            updateProperties(docModel, content, filename);
            docModel = overwriteAndIncrementversion(documentManager, docModel);
        } else {
            // Creating an unique identifier
            String docId = IdUtils.generateId(title);

            docModel = documentManager.createDocumentModel(path, docId,
                    VIDEO_TYPE);
            docModel.setProperty("dublincore", "title", title);
            updateProperties(docModel, content, filename);

            // updating icon
            Type docType = typeService.getType(VIDEO_TYPE);
            if (docType != null) {
                String iconPath = docType.getIcon();
                docModel.setProperty("common", "icon", iconPath);
            }
            docModel = documentManager.createDocument(docModel);
        }
        return docModel;
    }

    /**
     * Compute the JPEG story-board and previews for a video document from the
     * blob content of the video.
     */
    protected void updateProperties(DocumentModel docModel, Blob content,
            String filename) throws ClientException, IOException {

        // compute the story-board and the duration
        try {
            updateStoryboard(docModel, content);
        } catch (Exception e) {
            log.error(e, e);
        }

        // grab a full size screenshot of the video at a given position and use
        // it a
        try {
            Double duration = (Double) docModel.getPropertyValue("video:duration");
            Double position = 0.0;
            if (duration != null) {
                // take a screen-shot at 10% of the duration to skip intro
                // screen
                position = duration * 0.1;
            }
            updatePreviews(docModel, content, filename, position,
                    THUMBNAILS_VIEWS);
        } catch (Exception e) {
            log.error(e, e);
        }

        docModel.setPropertyValue("file:content", (Serializable) content);
        docModel.setPropertyValue("file:filename", filename);
    }

    /**
     * Update the JPEG story board and duration in seconds of a Video document
     * from the video blob content.
     */
    @SuppressWarnings("unchecked")
    public void updateStoryboard(DocumentModel docModel, Blob video)
            throws ConversionException, Exception, PropertyException,
            ClientException {
        BlobHolder result = getConversionService().convert(
                Constants.STORYBOARD_CONVERTER, new SimpleBlobHolder(video),
                null);
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
        docModel.setPropertyValue("vid:storyboard", (Serializable) storyboard);
        docModel.setPropertyValue("vid:duration",
                result.getProperty("duration"));
    }

    /**
     * Update the JPEG previews of a Video document from the video blob content
     * by taking a screen-shot of the movie at timecode offset given in seconds.
     */
    public void updatePreviews(DocumentModel docModel, Blob video,
            String filename, Double position,
            ArrayList<Map<String, Object>> templates)
            throws ConversionException, Exception, ClientException, IOException {
        Map<String, Serializable> parameters = new HashMap<String, Serializable>();
        parameters.put(Constants.POSITION_PARAMETER, position);
        BlobHolder result = getConversionService().convert(
                Constants.SCREENSHOT_CONVERTER, new SimpleBlobHolder(video),
                parameters);

        // compute the thumbnail preview
        if (result != null && result.getBlob() != null) {
            PictureResourceAdapter picture = docModel.getAdapter(PictureResourceAdapter.class);
            picture.createPicture(result.getBlob(),
                    result.getBlob().getFilename(), docModel.getTitle(),
                    templates);
        } else {
            // TODO: put a set of default thumbnails here to tell the user that
            // the
            // preview is not available for this document
        }
    }

    protected ConversionService getConversionService() throws Exception {
        if (cs == null) {
            cs = Framework.getService(ConversionService.class);
        }
        return cs;
    }
}
