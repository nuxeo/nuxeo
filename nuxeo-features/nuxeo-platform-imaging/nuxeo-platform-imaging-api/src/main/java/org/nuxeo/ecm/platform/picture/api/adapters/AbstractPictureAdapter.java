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
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_BY_LINE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_BY_LINE_TITLE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_CAPTION;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_CATEGORY;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_CITY;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_COLORSPACE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_COMMENT;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_COPYRIGHT;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_COPYRIGHT_NOTICE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_COUNTRY_OR_PRIMARY_LOCATION;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_CREDIT;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_DATE_CREATED;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_DESCRIPTION;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_EQUIPMENT;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_EXPOSURE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_FNUMBER;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_FOCALLENGTH;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_HEADLINE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_HEIGHT;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_HRESOLUTION;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_ICCPROFILE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_ISOSPEED;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_KEYWORDS;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_LANGUAGE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_OBJECT_NAME;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_ORIENTATION;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_ORIGINALDATE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_ORIGINAL_TRANSMISSION_REFERENCE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_ORIGINATING_PROGRAM;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_PIXEL_XDIMENSION;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_PIXEL_YDIMENSION;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_PROVINCE_OR_STATE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_RECORD_VERSION;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_RELEASE_DATE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_RELEASE_TIME;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_SOURCE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_SPECIAL_INSTRUCTIONS;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_SUPPLEMENTAL_CATEGORIES;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_TIME_CREATED;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_URGENCY;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_VRESOLUTION;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_WHITEBALANCE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_WIDTH;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_WRITER;

import java.awt.color.ICC_Profile;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
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

    private ConversionService converionService;

    @Override
    public void setDocumentModel(DocumentModel doc) {
        this.doc = doc;
    }

    protected ImagingService getImagingService() {
        return Framework.getLocalService(ImagingService.class);
    }

    protected ConversionService getConversionService() {
        if (converionService == null) {
            converionService = Framework.getService(ConversionService.class);
        }
        return converionService;
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

    protected void setMetadata() throws IOException {
        boolean imageInfoUsed = false;
        ImageInfo imageInfo = getImageInfo();
        if (imageInfo != null) {
            width = imageInfo.getWidth();
            height = imageInfo.getHeight();
            depth = imageInfo.getDepth();
            imageInfoUsed = true;
        }
        Map<String, Object> metadata = getImagingService().getImageMetadata(fileContent);
        description = (String) metadata.get(META_DESCRIPTION);
        if (!imageInfoUsed) {
            width = (Integer) metadata.get(META_WIDTH);
            height = (Integer) metadata.get(META_HEIGHT);
        }
        doc.setPropertyValue("picture:" + FIELD_BYLINE, (String) metadata.get(META_BY_LINE));
        doc.setPropertyValue("picture:" + FIELD_CAPTION, (String) metadata.get(META_CAPTION));
        doc.setPropertyValue("picture:" + FIELD_CREDIT, (String) metadata.get(META_CREDIT));
        if (metadata.containsKey(META_DATE_CREATED)) {
            doc.setPropertyValue("picture:" + FIELD_DATELINE, metadata.get(META_DATE_CREATED).toString());
        }
        doc.setPropertyValue("picture:" + FIELD_HEADLINE, (String) metadata.get(META_HEADLINE));
        doc.setPropertyValue("picture:" + FIELD_LANGUAGE, (String) metadata.get(META_LANGUAGE));
        doc.setPropertyValue("picture:" + FIELD_ORIGIN, (String) metadata.get(META_OBJECT_NAME));
        doc.setPropertyValue("picture:" + FIELD_SOURCE, (String) metadata.get(META_SOURCE));

        // Set EXIF info
        doc.setPropertyValue("imd:image_description", (String) metadata.get(META_DESCRIPTION));
        doc.setPropertyValue("imd:user_comment", (String) metadata.get(META_COMMENT));
        doc.setPropertyValue("imd:equipment", (String) metadata.get(META_EQUIPMENT));
        Date dateTimeOriginal = (Date) metadata.get(META_ORIGINALDATE);
        if (dateTimeOriginal != null) {
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(dateTimeOriginal);
            doc.setPropertyValue("imd:date_time_original", calendar);
        }
        doc.setPropertyValue("imd:xresolution", (Integer) metadata.get(META_HRESOLUTION));
        doc.setPropertyValue("imd:yresolution", (Integer) metadata.get(META_VRESOLUTION));
        doc.setPropertyValue("imd:pixel_xdimension", (Integer) metadata.get(META_PIXEL_XDIMENSION));
        doc.setPropertyValue("imd:pixel_ydimension", (Integer) metadata.get(META_PIXEL_YDIMENSION));
        doc.setPropertyValue("imd:copyright", (String) metadata.get(META_COPYRIGHT));
        doc.setPropertyValue("imd:exposure_time", (String) metadata.get(META_EXPOSURE));
        doc.setPropertyValue("imd:iso_speed_ratings", (String) metadata.get(META_ISOSPEED));
        doc.setPropertyValue("imd:focal_length", (Double) metadata.get(META_FOCALLENGTH));
        doc.setPropertyValue("imd:color_space", (String) metadata.get(META_COLORSPACE));
        doc.setPropertyValue("imd:white_balance", (String) metadata.get(META_WHITEBALANCE));
        ICC_Profile iccProfile = (ICC_Profile) metadata.get(META_ICCPROFILE);
        if (iccProfile != null) {
            doc.setPropertyValue("imd:icc_profile", iccProfile.toString());
        }
        doc.setPropertyValue("imd:orientation", (String) metadata.get(META_ORIENTATION));
        doc.setPropertyValue("imd:fnumber", (Double) metadata.get(META_FNUMBER));

        // Set IPTC info
        doc.setPropertyValue("iptc:by_line", (String) metadata.get(META_BY_LINE));
        doc.setPropertyValue("iptc:by_line_title", (String) metadata.get(META_BY_LINE_TITLE));
        doc.setPropertyValue("iptc:caption", (String) metadata.get(META_CAPTION));
        doc.setPropertyValue("iptc:category", (String) metadata.get(META_CATEGORY));
        doc.setPropertyValue("iptc:city", (String) metadata.get(META_CITY));
        doc.setPropertyValue("iptc:copyright_notice", (String) metadata.get(META_COPYRIGHT_NOTICE));
        doc.setPropertyValue("iptc:country_or_primary_location",
                (String) metadata.get(META_COUNTRY_OR_PRIMARY_LOCATION));
        doc.setPropertyValue("iptc:credit", (String) metadata.get(META_CREDIT));
        Date dateCreated = (Date) metadata.get(META_DATE_CREATED);
        if (dateCreated != null) {
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(dateCreated);
            doc.setPropertyValue("iptc:date_created", calendar);
        }
        doc.setPropertyValue("iptc:headline", (String) metadata.get(META_HEADLINE));
        doc.setPropertyValue("iptc:keywords", (String) metadata.get(META_KEYWORDS));
        doc.setPropertyValue("iptc:language", (String) metadata.get(META_LANGUAGE));
        doc.setPropertyValue("iptc:object_name", (String) metadata.get(META_OBJECT_NAME));
        doc.setPropertyValue("iptc:original_transmission_ref",
                (String) metadata.get(META_ORIGINAL_TRANSMISSION_REFERENCE));
        doc.setPropertyValue("iptc:originating_program", (String) metadata.get(META_ORIGINATING_PROGRAM));
        doc.setPropertyValue("iptc:province_or_state", (String) metadata.get(META_PROVINCE_OR_STATE));
        doc.setPropertyValue("iptc:record_version", (String) metadata.get(META_RECORD_VERSION));
        Date releaseDate = (Date) metadata.get(META_RELEASE_DATE);
        if (releaseDate != null) {
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(releaseDate);
            doc.setPropertyValue("iptc:release_date", calendar);
        }
        doc.setPropertyValue("iptc:release_time", (String) metadata.get(META_RELEASE_TIME));
        doc.setPropertyValue("iptc:source", (String) metadata.get(META_SOURCE));
        doc.setPropertyValue("iptc:special_instructions", (String) metadata.get(META_SPECIAL_INSTRUCTIONS));
        doc.setPropertyValue("iptc:supplemental_categories", (String) metadata.get(META_SUPPLEMENTAL_CATEGORIES));
        doc.setPropertyValue("iptc:time_created", (String) metadata.get(META_TIME_CREATED));
        doc.setPropertyValue("iptc:urgency", (String) metadata.get(META_URGENCY));
        doc.setPropertyValue("iptc:writer", (String) metadata.get(META_WRITER));
    }

    protected void clearViews() {
        List<Map<String, Object>> viewsList = new ArrayList<>();
        doc.getProperty(VIEWS_PROPERTY).setValue(viewsList);
    }

    protected void addViews(List<Map<String, Object>> pictureConversions, String filename, String title)
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
            List<PictureView> pictureViews = getImagingService().computeViewsFor(doc, fileContent, getImageInfo(),
                    true);
            addPictureViews(pictureViews, true);
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
