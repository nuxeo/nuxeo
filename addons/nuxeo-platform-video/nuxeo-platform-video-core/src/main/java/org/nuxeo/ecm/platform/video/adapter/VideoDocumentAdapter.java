/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.video.adapter;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.video.TranscodedVideo;
import org.nuxeo.ecm.platform.video.Video;
import org.nuxeo.ecm.platform.video.VideoConstants;
import org.nuxeo.ecm.platform.video.VideoDocument;
import org.nuxeo.ecm.platform.video.VideoHelper;
import org.nuxeo.ecm.platform.video.VideoInfo;

import com.google.common.collect.Maps;

/**
 * Default implementation of {@link VideoDocument}.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class VideoDocumentAdapter implements VideoDocument {

    private final DocumentModel doc;

    private final Video video;

    private Map<String, TranscodedVideo> transcodedVideos;

    @SuppressWarnings("unchecked")
    public VideoDocumentAdapter(DocumentModel doc) {
        if (!doc.hasFacet(VideoConstants.VIDEO_FACET)) {
            throw new ClientRuntimeException(doc + " is not a Video document.");
        }
        try {
            this.doc = doc;
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            Blob blob = bh.getBlob();

            Map<String, Serializable> videoInfoMap = (Map<String, Serializable>) doc.getPropertyValue("vid:info");
            if (videoInfoMap == null || videoInfoMap.get("duration") == null) {
                // Lazy extraction of video info if missing.
                VideoHelper.updateVideoInfo(doc, blob);
                videoInfoMap = (Map<String, Serializable>) doc.getPropertyValue("vid:info");
            }
            VideoInfo videoInfo = VideoInfo.fromMap(videoInfoMap);
            video = Video.fromBlobAndInfo(blob, videoInfo);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public Video getVideo() {
        return video;
    }

    @Override
    public Collection<TranscodedVideo> getTranscodedVideos() {
        if (transcodedVideos == null) {
            initTranscodedVideos();
        }
        return transcodedVideos.values();
    }

    @Override
    public TranscodedVideo getTranscodedVideo(String name) {
        if (transcodedVideos == null) {
            initTranscodedVideos();
        }
        return transcodedVideos.get(name);
    }

    private void initTranscodedVideos() {
        try {
            if (transcodedVideos == null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Serializable>> videos = (List<Map<String, Serializable>>) doc.getPropertyValue("vid:transcodedVideos");
                transcodedVideos = Maps.newHashMap();
                for (int i = 0; i < videos.size(); i++) {
                    TranscodedVideo transcodedVideo = TranscodedVideo.fromMapAndPosition(
                            videos.get(i), i);
                    transcodedVideos.put(transcodedVideo.getName(),
                            transcodedVideo);
                }
            }
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

}
