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
 *
 */
package org.nuxeo.ecm.platform.video.adapter;

import static org.nuxeo.ecm.platform.video.VideoConstants.VIDEO_FACET;

import java.io.File;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailFactory;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;

/**
 * Movie thumbnail factory
 * 
 * @since 5.7
 */
public class ThumbnailVideoFactory implements ThumbnailFactory {

    @Override
    public Blob getThumbnail(DocumentModel doc, CoreSession session)
            throws ClientException {
        if (!doc.hasFacet(VIDEO_FACET)) {
            throw new ClientException("Document is not a video");
        }
        // Choose the nuxeo default thumbnail of the picture views (screenshots
        // of the video taken during creation)
        PictureResourceAdapter picResAdapter = doc.getAdapter(PictureResourceAdapter.class);
        Blob thumbnailView = picResAdapter.getPictureFromTitle("Thumbnail");
        if (thumbnailView == null) {
            TypeInfo docType = doc.getAdapter(TypeInfo.class);
            return new FileBlob(
                    FileUtils.getResourceFileFromContext("nuxeo.war"
                            + File.separator + docType.getBigIcon()));
        }
        return thumbnailView;
    }

    @Override
    public Blob computeThumbnail(DocumentModel doc, CoreSession session) {
        return null;
    }
}
