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
 *     Tiago Cardoso <tcardoso@nuxeo.com>
 */
package org.nuxeo.ecm.platform.threed.adapter;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailFactory;
import org.nuxeo.ecm.platform.threed.ThreeDDocument;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;

import java.io.File;
import java.io.IOException;

import static org.nuxeo.ecm.platform.threed.ThreeDConstants.THREED_FACET;

/**
 * 3D content thumbnail factory
 *
 * @since 8.4
 */
public class ThumbnailThreeDFactory implements ThumbnailFactory {

    @Override
    public Blob getThumbnail(DocumentModel documentModel, CoreSession coreSession) {
        if (!documentModel.hasFacet(THREED_FACET)) {
            throw new NuxeoException("Document is not 3D");
        }
        ThreeDDocument threeDDoc = documentModel.getAdapter(ThreeDDocument.class);
        Blob thumbnailBlob = null;
        if (!threeDDoc.getRenderViews().isEmpty()) {
            thumbnailBlob = threeDDoc.getRenderViews().iterator().next().getContent();
        }
        if (thumbnailBlob == null) {
            // do default
            TypeInfo docType = documentModel.getAdapter(TypeInfo.class);
            try {
                return Blobs.createBlob(
                        FileUtils.getResourceFileFromContext("nuxeo.war" + File.separator + docType.getBigIcon()));
            } catch (IOException e) {
                throw new NuxeoException(e);
            }
        }
        return thumbnailBlob;
    }

    @Override
    public Blob computeThumbnail(DocumentModel documentModel, CoreSession coreSession) {
        return null;
    }

}
