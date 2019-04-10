/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.video.adapter;

import static org.nuxeo.ecm.platform.video.VideoConstants.INFO_PROPERTY;
import static org.nuxeo.ecm.platform.video.VideoConstants.TRANSCODED_VIDEOS_PROPERTY;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.video.TranscodedVideo;
import org.nuxeo.ecm.platform.video.Video;
import org.nuxeo.ecm.platform.video.VideoConstants;
import org.nuxeo.ecm.platform.video.VideoDocument;
import org.nuxeo.ecm.platform.video.VideoInfo;

import com.google.common.collect.Maps;

/**
 * Default implementation of {@link VideoDocument}.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class VideoDocumentAdapter implements VideoDocument {

    private static final Log log = LogFactory.getLog(VideoDocumentAdapter.class);

    private final DocumentModel doc;

    private final Video video;

    private Map<String, TranscodedVideo> transcodedVideos;

    @SuppressWarnings("unchecked")
    public VideoDocumentAdapter(DocumentModel doc) {
        if (!doc.hasFacet(VideoConstants.VIDEO_FACET)) {
            throw new NuxeoException(doc + " is not a Video document.");
        }
        this.doc = doc;
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        Blob blob = bh.getBlob();

        Map<String, Serializable> videoInfoMap = (Map<String, Serializable>) doc.getPropertyValue(INFO_PROPERTY);
        VideoInfo videoInfo = VideoInfo.fromMap(videoInfoMap);
        video = Video.fromBlobAndInfo(blob, videoInfo);
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
        if (transcodedVideos == null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Serializable>> videos = (List<Map<String, Serializable>>) doc.getPropertyValue(
                    TRANSCODED_VIDEOS_PROPERTY);
            transcodedVideos = Maps.newHashMap();
            for (int i = 0; i < videos.size(); i++) {
                TranscodedVideo transcodedVideo = TranscodedVideo.fromMapAndPosition(videos.get(i), i);
                transcodedVideos.put(transcodedVideo.getName(), transcodedVideo);
            }
        }
    }

}
