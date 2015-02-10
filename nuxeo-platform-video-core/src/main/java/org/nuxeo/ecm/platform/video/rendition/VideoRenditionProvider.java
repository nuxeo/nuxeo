/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.video.rendition;

import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendition.RenditionException;
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
    public List<Blob> render(DocumentModel doc, RenditionDefinition definition) throws RenditionException {
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
