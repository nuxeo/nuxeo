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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.picture.api.BlobHelper;
import org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants;
import org.nuxeo.runtime.api.Framework;

public class DefaultPictureAdapter extends AbstractPictureAdapter {
    private static final Log log = LogFactory.getLog(DefaultPictureAdapter.class);

    // needs trailing slash
    private static final String VIEW_XPATH = "picture:views/item[%d]/";

    private static final String TITLE_PROPERTY = "title";

    private static final String FILENAME_PROPERTY = "filename";

    @Override
    public boolean createPicture(Blob blob, String filename, String title,
            ArrayList<Map<String, Object>> pictureTemplates)
            throws IOException, ClientException {
        if (blob == null) {
            clearViews();
            return true;
        }

        file = BlobHelper.getFileFromBlob(blob);
        if (file == null) {
            file = File.createTempFile("nuxeoImage", ".jpg");
            Framework.trackFile(file, this);
            blob.transferTo(file);
            // use a persistent blob with our file
            if (!blob.isPersistent()) {
                blob = new FileBlob(file, blob.getMimeType(), blob.getEncoding(),
                        blob.getFilename(), blob.getDigest());
            }
        }

        fileContent = blob;

        type = blob.getMimeType();
        if (type == null) {
            // TODO : use MimetypeRegistry instead
            type = getImagingService().getImageMimeType(file);
            blob.setMimeType(type);
        }
        if (type == null || type.equals("application/octet-stream")) {
            return false;
        }
        try {
            setMetadata();
        } catch (Exception e) {
            log.debug("An error occured while trying to set metadata for "
                    + filename, e);
        }
        if (width != null && height != null) {
            clearViews();
            addViews(pictureTemplates, filename, title);
        }
        return true;
    }

    @Override
    public void doRotate(int angle) throws ClientException {
        int size = doc.getProperty(VIEWS_PROPERTY).size();
        for (int i = 0; i < size; i++) {
            String xpath = "picture:views/view[" + i + "]/";
            try {
                BlobHolder blob = new SimpleBlobHolder(doc.getProperty(
                        xpath + "content").getValue(Blob.class));
                String type = blob.getBlob().getMimeType();
                if (type != "image/png") {
                    Map<String, Serializable> options = new HashMap<String, Serializable>();
                    options.put(ImagingConvertConstants.OPTION_ROTATE_ANGLE,
                            angle);
                    blob = getConversionService().convert(
                            ImagingConvertConstants.OPERATION_ROTATE, blob,
                            options);
                    doc.getProperty(xpath + "content").setValue(blob.getBlob());
                    Long height = (Long) doc.getProperty(xpath + "height").getValue();
                    Long width = (Long) doc.getProperty(xpath + "width").getValue();
                    doc.getProperty(xpath + "height").setValue(width);
                    doc.getProperty(xpath + "width").setValue(height);
                }
            } catch (Exception e) {
                log.error("Rotation Failed", e);
            }
        }
    }

    @Override
    public void doCrop(String coords) throws ClientException {
        doc.setPropertyValue("picture:cropCoords", coords);
    }

    @Override
    public Blob getPictureFromTitle(String title) throws PropertyException,
            ClientException {
        if (title == null) {
            return null;
        }
        Collection<Property> views = doc.getProperty(VIEWS_PROPERTY).getChildren();
        for (Property property : views) {
            if (title.equals(property.getValue(TITLE_PROPERTY))) {
                Blob blob = (Blob) property.getValue("content");
                blob.setFilename((String) property.getValue(FILENAME_PROPERTY));
                return blob;
            }
        }
        return null;
    }

    @Override
    public String getFirstViewXPath() {
        return getViewXPathFor(0);
    }

    @Override
    public String getViewXPath(String viewName) {
        try {
            Property views = doc.getProperty(VIEWS_PROPERTY);
            for (int i = 0; i < views.size(); i++) {
                if (views.get(i).getValue(TITLE_PROPERTY).equals(viewName)) {
                    return getViewXPathFor(i);
                }
            }
        } catch (ClientException e) {
            log.error("Unable to get picture views", e);
        }
        return null;
    }

    protected String getViewXPathFor(int index) {
        return String.format(VIEW_XPATH, index);
    }

}
