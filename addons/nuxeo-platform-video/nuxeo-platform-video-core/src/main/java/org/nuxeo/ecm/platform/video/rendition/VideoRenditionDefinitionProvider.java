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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinitionProvider;
import org.nuxeo.ecm.platform.video.TranscodedVideo;
import org.nuxeo.ecm.platform.video.VideoDocument;
import org.nuxeo.ecm.platform.video.service.VideoConversion;
import org.nuxeo.ecm.platform.video.service.VideoService;
import org.nuxeo.runtime.api.Framework;

/**
 * Provides rendition definitions based on the existing transcoded videos.
 *
 * @since 7.2
 */
public class VideoRenditionDefinitionProvider implements RenditionDefinitionProvider {

    public static final String VIDEO_RENDITION_KIND = "nuxeo:video:conversion";

    @Override
    public List<RenditionDefinition> getRenditionDefinitions(DocumentModel doc) {
        VideoDocument videoDocument = doc.getAdapter(VideoDocument.class);
        if (videoDocument == null) {
            return Collections.emptyList();
        }

        List<RenditionDefinition> renditionDefinitions = new ArrayList<>();
        MimetypeRegistry mimetypeRegistry = Framework.getService(MimetypeRegistry.class);
        VideoService videoService = Framework.getService(VideoService.class);
        for (TranscodedVideo transcodedVideo : videoDocument.getTranscodedVideos()) {
            VideoConversion videoConversion = videoService.getVideoConversion(transcodedVideo.getName());
            if (videoConversion != null && videoConversion.isRendition()) {
                Blob blob = transcodedVideo.getBlob();
                if (blob != null) {
                    RenditionDefinition renditionDefinition = new RenditionDefinition();
                    renditionDefinition.setEnabled(true);
                    renditionDefinition.setName(transcodedVideo.getName());
                    renditionDefinition.setKind(VIDEO_RENDITION_KIND);
                    renditionDefinition.setProvider(new VideoRenditionProvider());
                    renditionDefinition.setVisible(videoConversion.isRenditionVisible());
                    renditionDefinition.setLabel(transcodedVideo.getName());
                    MimetypeEntry mimeType = mimetypeRegistry.getMimetypeEntryByMimeType(blob.getMimeType());
                    renditionDefinition.setIcon("/icons/" + mimeType.getIconPath());
                    renditionDefinitions.add(renditionDefinition);
                }
            }
        }
        return renditionDefinitions;
    }

}
