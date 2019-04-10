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
 *
 */
package org.nuxeo.ecm.platform.video.adapter;

import static org.nuxeo.ecm.platform.video.VideoConstants.VIDEO_FACET;

import java.io.File;
import java.io.IOException;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
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
    public Blob getThumbnail(DocumentModel doc, CoreSession session) {
        if (!doc.hasFacet(VIDEO_FACET)) {
            throw new NuxeoException("Document is not a video");
        }
        // Choose the nuxeo default thumbnail of the picture views (screenshots
        // of the video taken during creation)
        PictureResourceAdapter picResAdapter = doc.getAdapter(PictureResourceAdapter.class);
        Blob thumbnailView = picResAdapter.getPictureFromTitle("Small");
        if (thumbnailView == null) {
            // try Thumbnail view
            thumbnailView = picResAdapter.getPictureFromTitle("Thumbnail");
            if (thumbnailView == null) {
                TypeInfo docType = doc.getAdapter(TypeInfo.class);
                try {
                    return Blobs.createBlob(FileUtils.getResourceFileFromContext("nuxeo.war" + File.separator
                            + docType.getBigIcon()));
                } catch (IOException e) {
                    throw new NuxeoException(e);
                }
            }
        }
        return thumbnailView;
    }

    @Override
    public Blob computeThumbnail(DocumentModel doc, CoreSession session) {
        return null;
    }
}
