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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.picture.api.adapters;

import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.OPERATION_CROP;
import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.OPTION_CROP_X;
import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.OPTION_CROP_Y;
import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.OPTION_RESIZE_HEIGHT;
import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.OPTION_RESIZE_WIDTH;
import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.PICTURE_INFO_PROPERTY;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.picture.api.PictureConversion;
import org.nuxeo.ecm.platform.picture.api.PictureView;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

public abstract class AbstractPictureAdapter implements PictureResourceAdapter {

    public static final String VIEWS_PROPERTY = "picture:views";

    public static final String CONTENT_XPATH = "picture:views/view[%d]/content";

    public static final String FIELD_HEADLINE = "headline";

    public static final String FIELD_SUBHEADLINE = "subheadline";

    public static final String FIELD_BYLINE = "byline";

    public static final String FIELD_DATELINE = "dateline";

    public static final String FIELD_SLUGLINE = "slugline";

    public static final String FIELD_CREDIT = "credit";

    public static final String FIELD_LANGUAGE = "language";

    public static final String FIELD_SOURCE = "source";

    public static final String FIELD_ORIGIN = "origin";

    public static final String FIELD_GENRE = "genre";

    public static final String FIELD_CAPTION = "caption";

    public static final String FIELD_TYPAGE = "typage";

    public static final String SCHEMA_NAME = "picture";

    public static final int MEDIUM_SIZE = 1200;

    public static final int SMALL_SIZE = 350;

    public static final int THUMB_SIZE = 150;

    protected DocumentModel doc;

    protected Integer width;

    protected Integer height;

    protected Integer depth;

    protected String description;

    protected String type;

    protected Blob fileContent;

    /** @since 5.9.5 */
    protected ImageInfo imageInfo;

    private CoreSession session;

    @Override
    public void setDocumentModel(DocumentModel doc) {
        this.doc = doc;
    }

    protected ImagingService getImagingService() {
        return Framework.getService(ImagingService.class);
    }

    protected ConversionService getConversionService() {
        return Framework.getService(ConversionService.class);
    }

    protected CoreSession getSession() {
        if (session == null) {
            if (doc == null) {
                return null;
            }
            session = doc.getCoreSession();
        }
        return session;
    }


    @Override
    public void clearInfo() {
        doc.setPropertyValue(PICTURE_INFO_PROPERTY, null);
    }

    protected void clearViews() {
        List<Map<String, Object>> viewsList = new ArrayList<>();
        doc.getProperty(VIEWS_PROPERTY).setValue(viewsList);
    }

    protected void addViews(List<Map<String, Object>> pictureConversions, String filename, String title, boolean outsideTx)
            throws IOException {
        doc.setProperty("dublincore", "title", title);
        if (pictureConversions != null) {
            // Use PictureBook Properties
            for (Map<String, Object> view : pictureConversions) {
                Integer maxsize;
                if (view.get("maxsize") == null) {
                    maxsize = MEDIUM_SIZE;
                } else {
                    maxsize = ((Long) view.get("maxsize")).intValue();
                }
                createPictureimpl((String) view.get("description"), (String) view.get("tag"),
                        (String) view.get("title"), maxsize, filename, width, height, depth, fileContent);
            }
        } else {
            boolean txWasActive = false;
            try {
                if (outsideTx && TransactionHelper.isTransactionActive()) {
                    txWasActive = true;
                    TransactionHelper.commitOrRollbackTransaction();
                }

                List<PictureView> pictureViews = getImagingService().computeViewsFor(doc, fileContent, getImageInfo(),
                        true);
                addPictureViews(pictureViews, true);
            } finally {
                if (outsideTx && txWasActive && !TransactionHelper.isTransactionActiveOrMarkedRollback()) {
                    TransactionHelper.startTransaction();
                }
            }

        }
    }

    public void createPictureimpl(String description, String tag, String title, Integer maxsize, String filename,
            Integer width, Integer height, Integer depth, Blob fileContent) throws IOException {
        if (fileContent.getFilename() == null) {
            fileContent.setFilename(filename);
        }

        if (maxsize == null) {
            maxsize = 0;
        }

        PictureConversion pictureConversion = new PictureConversion(title, description, tag, maxsize);
        pictureConversion.setChainId("Image.Blob.Resize");

        PictureView view = getImagingService().computeViewFor(fileContent, pictureConversion, getImageInfo(), true);

        addPictureView(view);
    }

    /**
     * Attach new picture views with the document
     *
     * @since 7.1
     */
    protected void addPictureViews(List<PictureView> pictureViews, boolean clearPictureViews) {
        if (clearPictureViews) {
            clearViews();
        }

        List<Map<String, Serializable>> views = getPictureViews();

        for (PictureView pictureView : pictureViews) {
            views.add(pictureView.asMap());
        }

        doc.setPropertyValue(VIEWS_PROPERTY, (Serializable) views);
    }

    @Override
    public boolean fillPictureViews(Blob blob, String filename, String title) throws IOException {
        return fillPictureViews(blob, filename, title, null);
    }

    /**
     * Returns the picture views attached to the document if present, an empty list otherwise.
     *
     * @since 7.1
     */
    @SuppressWarnings("unchecked")
    protected List<Map<String, Serializable>> getPictureViews() {
        List<Map<String, Serializable>> views = (List<Map<String, Serializable>>) doc.getPropertyValue(VIEWS_PROPERTY);
        if (views == null) {
            views = new ArrayList<>();
        }
        return views;
    }

    /**
     * Add a pictureView to the existing picture views attached with the document
     *
     * @since 7.1
     */
    protected void addPictureView(PictureView view) {
        List<Map<String, Serializable>> views = getPictureViews();
        views.add(view.asMap());
        doc.setPropertyValue(VIEWS_PROPERTY, (Serializable) views);
    }

    /**
     * Returns the {@link ImageInfo} for the main Blob ({@code fileContent}).
     *
     * @since 5.9.5.
     */
    protected ImageInfo getImageInfo() {
        if (imageInfo == null) {
            imageInfo = getImagingService().getImageInfo(fileContent);
        }
        return imageInfo;
    }

    protected Blob getContentFromViews(Integer i) {
        return (Blob) doc.getPropertyValue(String.format(CONTENT_XPATH, i));
    }

    protected Blob crop(Blob blob, Map<String, Serializable> coords) {
        try {
            BlobHolder bh = new SimpleBlobHolder(blob);
            String type = blob.getMimeType();

            Map<String, Serializable> options = new HashMap<>();
            options.put(OPTION_CROP_X, coords.get("x"));
            options.put(OPTION_CROP_Y, coords.get("y"));
            options.put(OPTION_RESIZE_HEIGHT, coords.get("h"));
            options.put(OPTION_RESIZE_WIDTH, coords.get("w"));

            if (!"image/png".equals(type)) {
                bh = getConversionService().convert(OPERATION_CROP, bh, options);
                return Blobs.createBlob(bh.getBlob().getStream(), type);
            }
        } catch (IOException e) {
            throw new NuxeoException("Crop failed", e);
        }
        return null;
    }

}
