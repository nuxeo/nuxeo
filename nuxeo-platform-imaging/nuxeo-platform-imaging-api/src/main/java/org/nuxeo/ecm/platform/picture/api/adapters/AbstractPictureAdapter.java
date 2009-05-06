/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.picture.api.adapters;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.picture.api.MetadataConstants;
import org.nuxeo.runtime.api.Framework;

public abstract class AbstractPictureAdapter implements PictureResourceAdapter {

    private static final Log log = LogFactory.getLog(PictureResourceAdapter.class);

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

    public static final int MEDIUM_SIZE = 550;

    public static final int THUMB_SIZE = 100;

    protected DocumentModel doc;

    protected Integer width;

    protected Integer height;

    protected String description;

    protected String type;

    protected File file;

    protected Blob fileContent;

    private CoreSession session;

    private ImagingService imagingService;

    private ConversionService converionService;

    public void setDocumentModel(DocumentModel doc) {
        this.doc = doc;
    }

    protected ImagingService getImagingService() {
        if (imagingService == null) {
            try {
                imagingService = Framework.getService(ImagingService.class);
            } catch (Exception e) {
                log.error("Unable to get Imaging Service.", e);
            }

        }
        return imagingService;
    }

    protected ConversionService getConversionService() throws ClientException {
        if (converionService == null) {
            try {
                converionService = Framework.getService(ConversionService.class);
            } catch (Exception e) {
                log.error("Unable to get converion Service.", e);
                throw new ClientException(e);
            }
        }
        return converionService;
    }

    protected CoreSession getSession() {
        if (session == null) {
            if (doc == null) {
                return null;
            }
            String sid = doc.getSessionId();
            session = CoreInstance.getInstance().getSession(sid);
        }

        return session;
    }

    protected void setMetadata() throws IOException, ClientException {
        Map<String, Object> metadata = getImagingService().getImageMetadata(
                fileContent.getStream());
        description = (String) metadata.get(MetadataConstants.META_DESCRIPTION);
        width = (Integer) metadata.get(MetadataConstants.META_WIDTH);
        height = (Integer) metadata.get(MetadataConstants.META_HEIGHT);

        doc.setPropertyValue("picture:" + FIELD_BYLINE,
                (String) metadata.get(MetadataConstants.META_BYLINE));
        doc.setPropertyValue("picture:" + FIELD_CAPTION,
                (String) metadata.get(MetadataConstants.META_CAPTION));
        doc.setPropertyValue("picture:" + FIELD_CREDIT,
                (String) metadata.get(MetadataConstants.META_CREDIT));
        if (metadata.containsKey(MetadataConstants.META_DATE)) {
            doc.setPropertyValue("picture:" + FIELD_DATELINE, metadata.get(
                    MetadataConstants.META_DATE).toString());
        }
        doc.setPropertyValue("picture:" + FIELD_HEADLINE,
                (String) metadata.get(MetadataConstants.META_HEADLINE));
        doc.setPropertyValue("picture:" + FIELD_LANGUAGE,
                (String) metadata.get(MetadataConstants.META_LANGUAGE));
        doc.setPropertyValue("picture:" + FIELD_ORIGIN,
                (String) metadata.get(MetadataConstants.META_OBJECTNAME));
        doc.setPropertyValue("picture:" + FIELD_SOURCE,
                (String) metadata.get(MetadataConstants.META_SOURCE));
    }

    protected void addViews(List<Map<String, Object>> pictureTemplates,
            String filename, String title) throws IOException,
            ClientException {
        doc.setProperty("dublincore", "title", title);
        if (pictureTemplates != null) {
            // Use PictureBook Properties
            for (Map<String, Object> view : pictureTemplates) {
                Integer maxsize;
                if (view.get("maxsize") == null) {
                    maxsize = MEDIUM_SIZE;
                } else {
                    maxsize = ((Long) view.get("maxsize")).intValue();
                }
                createPictureimpl((String) view.get("description"),
                        (String) view.get("tag"), (String) view.get("title"),
                        maxsize, filename, width, height, fileContent);
            }
        } else {
            // Default properties When PictureBook doesn't exist
            createPictureimpl("Medium Size", "medium", "Medium", MEDIUM_SIZE, filename,
                    width, height, fileContent);
            createPictureimpl(description, "original", "Original", null, filename,
                    width, height, fileContent);
            createPictureimpl("Thumbnail Size", "thumb", "Thumbnail", THUMB_SIZE, filename,
                    width, height, fileContent);
        }
    }

    @SuppressWarnings({"unchecked"})
    public void createPictureimpl(String description, String tag, String title,
            Integer maxsize, String filename, Integer width, Integer height,
            Blob fileContent) throws IOException, ClientException {

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("title", title);
        map.put("description", description);
        map.put("filename", filename);
        map.put("tag", tag);
        if (title.equals("Original")) {
            map.put("width", width);
            map.put("height", height);
            FileBlob fileBlob = new FileBlob(file, type);
            fileBlob.setFilename(title + "_" + filename);
            map.put("content", fileBlob);
        } else {
            Point size = new Point(width, height);
            size = getSize(size, maxsize);
            map.put("width", size.x);
            map.put("height", size.y);
            Map<String, Serializable> options = new HashMap<String, Serializable>();
            options.put(ImagingConvertConstants.OPTION_RESIZE_WIDTH, size.x);
            options.put(ImagingConvertConstants.OPTION_RESIZE_HEIGHT, size.y);
            BlobHolder bh = new SimpleBlobHolder(fileContent);
            bh = getConversionService().convert(ImagingConvertConstants.OPERATION_RESIZE, bh, options);
            Blob blob = bh.getBlob() != null ? bh.getBlob()
                    : new FileBlob(file, type);
            blob.setFilename(title + "_" + filename);
            map.put("content", blob);
        }
        Serializable views = doc.getPropertyValue("picture:views");
        List<Map<String, Object>> viewsList = (List<Map<String, Object>>) views;
        viewsList.add(map);
        doc.getProperty("picture:views").setValue(viewsList);
    }

    protected static Point getSize(Point current, int max) {
        int x = current.x;
        int y = current.y;
        int newx;
        int newy;
        if (x > y) { // landscape
            newy = (y * max) / x;
            newx = max;
        } else { // portrait
            newx = (x * max) / y;
            newy = max;
        }
        if (newx > x || newy > y) {
            return current;
        }
        return new Point(newx, newy);
    }

    protected Blob getContentFromViews(Integer i) throws ClientException {
        return (Blob) doc.getPropertyValue("picture:views/view[" + i
                + "]/content");
    }

    protected FileBlob crop(Blob blob, Map<String, Serializable> coords)
            throws ClientException {
        try {
            BlobHolder bh = new SimpleBlobHolder(blob);
            String type = blob.getMimeType();

            Map<String, Serializable> options = new HashMap<String, Serializable>();
            options.put(ImagingConvertConstants.OPTION_CROP_X,  coords.get("x"));
            options.put(ImagingConvertConstants.OPTION_CROP_Y, coords.get("y"));
            options.put(ImagingConvertConstants.OPTION_RESIZE_HEIGHT, coords.get("h"));
            options.put(ImagingConvertConstants.OPTION_RESIZE_WIDTH, coords.get("w"));

            if (type != "image/png") {
                bh = getConversionService().convert(ImagingConvertConstants.OPERATION_CROP, bh, options);
                return new FileBlob(bh.getBlob().getStream(), type);
            }
        } catch (Exception e) {
            throw new ClientException("Crop failed", e);
        }
        return null;
    }

}
