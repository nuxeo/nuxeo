/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDIT}9IONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Andre Justo
 */
package org.nuxeo.ecm.platform.video;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.preview.adapter.VideoPreviewer;
import org.nuxeo.ecm.platform.preview.api.PreviewException;

/**
 * @since 8.2
 */
public class VideoDocumentPreviewer extends VideoPreviewer {

    @Override
    public List<Blob> getPreview(Blob blob, DocumentModel dm) throws PreviewException {
        boolean isBlobHolder = dm.getAdapter(BlobHolder.class).getBlob().equals(blob);
        if (isBlobHolder && dm.getType().equals(VideoConstants.VIDEO_TYPE)) {
            VideoDocument videoDocument = dm.getAdapter(VideoDocument.class);
            Collection<TranscodedVideo> transcodedVideos = videoDocument.getTranscodedVideos();
            if (!transcodedVideos.isEmpty()) {
                List<Blob> blobs = transcodedVideos.stream().map(TranscodedVideo::getBlob).collect(Collectors.toList());
                return buildPreview(blobs, dm);
            }
        }
        return super.getPreview(blob, dm);
    }
}
