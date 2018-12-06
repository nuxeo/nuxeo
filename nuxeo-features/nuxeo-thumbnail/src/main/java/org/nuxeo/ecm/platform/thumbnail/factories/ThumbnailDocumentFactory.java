/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 *
 */
package org.nuxeo.ecm.platform.thumbnail.factories;

import static org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants.ANY_TO_THUMBNAIL_CONVERTER_NAME;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailFactory;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.runtime.api.Framework;

/**
 * Default thumbnail factory for all non folderish documents Return the main blob converted in thumbnail or get the
 * document big icon as a thumbnail
 *
 * @since 5.7
 */
public class ThumbnailDocumentFactory implements ThumbnailFactory {

    private static final Log log = LogFactory.getLog(ThumbnailDocumentFactory.class);

    @Override
    public Blob getThumbnail(DocumentModel doc, CoreSession session) {
        Blob thumbnailBlob = null;
        try {
            if (doc.hasFacet(ThumbnailConstants.THUMBNAIL_FACET)) {
                thumbnailBlob = (Blob) doc.getPropertyValue(ThumbnailConstants.THUMBNAIL_PROPERTY_NAME);
            }
        } catch (PropertyException e) {
            log.warn("Could not fetch the thumbnail blob", e);
        }
        if (thumbnailBlob == null) {
            thumbnailBlob = getDefaultThumbnail(doc);
        }
        return thumbnailBlob;
    }

    @Override
    public Blob computeThumbnail(DocumentModel doc, CoreSession session) {
        // TODO: convert non pix attachment
        // Image converter before thumbnail converter
        // params.put("targetFilePath", "readyToThumbnail.png");
        // BlobHolder bh = conversionService.convertToMimeType(
        // ThumbnailConstants.THUMBNAIL_MIME_TYPE, blobHolder,
        // params);
        // params.clear();
        ConversionService conversionService;
        Blob thumbnailBlob = null;
        try {
            conversionService = Framework.getService(ConversionService.class);
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            if (bh != null) {
                Map<String, Serializable> params = new HashMap<>();
                // Thumbnail converter
                params.put(ThumbnailConstants.THUMBNAIL_SIZE_PARAMETER_NAME, ThumbnailConstants.THUMBNAIL_DEFAULT_SIZE);
                bh = conversionService.convert(ANY_TO_THUMBNAIL_CONVERTER_NAME, bh, params);
                if (bh != null) {
                    thumbnailBlob = bh.getBlob();
                }
            }
        } catch (NuxeoException e) {
            log.warn("Cannot compute document thumbnail", e);
        }
        return thumbnailBlob;
    }

    protected Blob getDefaultThumbnail(DocumentModel doc) {
        if (doc == null) {
            return null;
        }
        TypeInfo docType = doc.getAdapter(TypeInfo.class);
        String iconPath = docType.getBigIcon();
        if (iconPath == null) {
            iconPath = docType.getIcon();
        }
        if (iconPath == null) {
            return null;
        }

        try {
            File iconFile = FileUtils.getResourceFileFromContext("nuxeo.war" + File.separator + iconPath);
            if (iconFile.exists()) {
                MimetypeRegistry mimetypeRegistry = Framework.getService(MimetypeRegistry.class);
                String mimeType = mimetypeRegistry.getMimetypeFromFile(iconFile);
                if (mimeType == null) {
                    mimeType = mimetypeRegistry.getMimetypeFromFilename(iconPath);
                }
                return Blobs.createBlob(iconFile, mimeType);
            }
        } catch (IOException e) {
            log.warn(String.format("Could not fetch the thumbnail blob from icon path '%s'", iconPath), e);
        }

        return null;
    }

}
