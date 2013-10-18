/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 *
 */
package org.nuxeo.ecm.platform.thumbnail.factories;

import static org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants.ANY_TO_THUMBNAIL_CONVERTER_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailFactory;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.runtime.api.Framework;

/**
 * Default thumbnail factory for all non folderish documents Return the main
 * blob converted in thumbnail or get the document big icon as a thumbnail
 *
 * @since 5.7
 */
public class ThumbnailDocumentFactory implements ThumbnailFactory {

    private static final Log log = LogFactory.getLog(ThumbnailDocumentFactory.class);

    @Override
    public Blob getThumbnail(DocumentModel doc, CoreSession session)
            throws ClientException {
        Blob thumbnailBlob = null;
        try {
            if (doc.hasFacet(ThumbnailConstants.THUMBNAIL_FACET)) {
                thumbnailBlob = (Blob) doc.getPropertyValue(ThumbnailConstants.THUMBNAIL_PROPERTY_NAME);
            }
        } catch (ClientException e) {
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
                Map<String, Serializable> params = new HashMap<String, Serializable>();
                // Thumbnail converter
                params.put(ThumbnailConstants.THUMBNAIL_SIZE_PARAMETER_NAME,
                        ThumbnailConstants.THUMBNAIL_DEFAULT_SIZE);
                bh = conversionService.convert(ANY_TO_THUMBNAIL_CONVERTER_NAME,
                        bh, params);
                if (bh != null) {
                    thumbnailBlob = bh.getBlob();
                }
            }
        } catch (Exception e) {
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
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) {
            return null;
        }
        try {
            InputStream iconStream = ctx.getExternalContext().getResourceAsStream(
                    iconPath);
            if (iconStream != null) {
                return new FileBlob(iconStream);
            }
        } catch (IOException e) {
            log.warn(String.format(
                    "Could not fetch the thumbnail blob from icon path '%s'",
                    iconPath), e);
        }
        return null;
    }

}
