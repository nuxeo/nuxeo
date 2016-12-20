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
package org.nuxeo.ecm.platform.pictures.tiles.api.adapter;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTilingService;
import org.nuxeo.runtime.api.Framework;

/**
 * Factory method for the DocumentModelAdapter for PictureTiles Contains the logic to choose the correct implementation
 * according to DocumentModel.
 *
 * @author tiry
 */
public class PictureTilesAdapterFactory implements DocumentAdapterFactory {

    protected static final String ORIGINAL_JPEG_VIEW_NAME = "OriginalJpeg";

    /**
     * @deprecated since 7.2. The Original view does not exist anymore. See NXP-16070.
     */
    @Deprecated
    protected static final String ORIGINAL_VIEW_NAME = "Original";

    public Object getAdapter(DocumentModel doc, Class itf) {
        PictureTilingService tilingService = Framework.getService(PictureTilingService.class);
        String blobProperty = tilingService.getBlobProperty(doc.getType());
        PictureTilesAdapter ptAdapter = getPictureTilesAdapterFor(doc, blobProperty);
        if (ptAdapter != null) {
            return ptAdapter;
        }
        // else fall back on default
        if (doc.hasSchema("file")) {
            Blob blob = (Blob) doc.getProperty("file", "content");
            if (blob == null) {
                return null;
            }
            return new PictureTilesAdapterImpl(doc, "file:content");
        } else {
            return new PictureTilesAdapterImpl(doc);
        }
    }

    private PictureTilesAdapter getPictureTilesAdapterFor(DocumentModel doc, String blobProperty) {
        if (blobProperty != null) {
            try {
                return getPictureTilesAdapter(doc, blobProperty);
            } catch (PropertyException | IndexOutOfBoundsException e) {
                return getPictureTilesAdapterForPicture(doc);
            }
        }
        return getPictureTilesAdapterForPicture(doc);
    }

    private PictureTilesAdapter getPictureTilesAdapterForPicture(DocumentModel doc) {
        if (doc.hasSchema("picture")) {
            PictureResourceAdapter adapter = doc.getAdapter(PictureResourceAdapter.class);
            // try OriginalJpeg view xpath
            String blobProperty = adapter.getViewXPath(ORIGINAL_JPEG_VIEW_NAME) + "content";
            return getPictureTilesAdapter(doc, blobProperty);
        }
        return null;
    }

    private PictureTilesAdapter getPictureTilesAdapter(DocumentModel doc, String blobProperty) {
        Blob blob = (Blob) doc.getPropertyValue(blobProperty);
        if (blob != null) {
            return new PictureTilesAdapterImpl(doc, blobProperty);
        }
        return null;
    }

}
