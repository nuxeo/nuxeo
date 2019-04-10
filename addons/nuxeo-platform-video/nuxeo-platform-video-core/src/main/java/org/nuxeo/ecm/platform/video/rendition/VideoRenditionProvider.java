/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.video.rendition;

import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendition.extension.RenditionProvider;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.ecm.platform.video.TranscodedVideo;
import org.nuxeo.ecm.platform.video.VideoDocument;

/**
 * Provides a rendition based on a transcoded video
 * (referenced through the rendition definition name).
 *
 * @since 7.2
 */
public class VideoRenditionProvider implements RenditionProvider {

    @Override
    public boolean isAvailable(DocumentModel doc, RenditionDefinition definition) {
        VideoDocument videoDocument = doc.getAdapter(VideoDocument.class);
        if (videoDocument == null) {
            return false;
        }

        TranscodedVideo transcodedVideo = videoDocument.getTranscodedVideo(definition.getName());
        return transcodedVideo != null && transcodedVideo.getBlob() != null;
    }

    @Override
    public List<Blob> render(DocumentModel doc, RenditionDefinition definition) {
        VideoDocument videoDocument = doc.getAdapter(VideoDocument.class);
        if (videoDocument == null) {
            return Collections.emptyList();
        }

        TranscodedVideo transcodedVideo = videoDocument.getTranscodedVideo(definition.getName());
        if (transcodedVideo != null) {
            return Collections.singletonList(transcodedVideo.getBlob());
        }
        return Collections.emptyList();
    }
}
