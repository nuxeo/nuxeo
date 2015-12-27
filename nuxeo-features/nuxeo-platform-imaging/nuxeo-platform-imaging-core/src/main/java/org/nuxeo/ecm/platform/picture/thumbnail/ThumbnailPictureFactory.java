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
package org.nuxeo.ecm.platform.picture.thumbnail;

import java.io.File;
import java.io.IOException;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailFactory;
import org.nuxeo.ecm.platform.picture.api.PictureView;
import org.nuxeo.ecm.platform.picture.api.adapters.MultiviewPicture;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;

/**
 * Picture thumbnail factory
 *
 * @since 5.7
 */
public class ThumbnailPictureFactory implements ThumbnailFactory {

    @Override
    public Blob getThumbnail(DocumentModel doc, CoreSession session) {
        if (!doc.hasFacet("Picture")) {
            throw new NuxeoException("Document is not a picture");
        }
        // Choose the nuxeo default thumbnail of the picture views if exists
        MultiviewPicture mViewPicture = doc.getAdapter(MultiviewPicture.class);
        PictureView thumbnailView = mViewPicture.getView("Small");
        if (thumbnailView == null || thumbnailView.getBlob() == null) {
            // try thumbnail view
            thumbnailView = mViewPicture.getView("Thumbnail");
            if (thumbnailView == null || thumbnailView.getBlob() == null) {
                TypeInfo docType = doc.getAdapter(TypeInfo.class);
                try {
                    return Blobs.createBlob(FileUtils.getResourceFileFromContext("nuxeo.war" + File.separator
                            + docType.getBigIcon()));
                } catch (IOException e) {
                    throw new NuxeoException(e);
                }
            }
        }
        return thumbnailView.getBlob();
    }

    @Override
    public Blob computeThumbnail(DocumentModel doc, CoreSession session) {
        return null;
    }
}
