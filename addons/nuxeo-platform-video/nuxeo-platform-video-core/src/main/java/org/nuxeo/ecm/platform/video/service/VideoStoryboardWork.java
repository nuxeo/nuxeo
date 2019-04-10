/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.video.service;

import static org.nuxeo.ecm.core.api.CoreSession.ALLOW_VERSION_WRITE;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.platform.video.VideoHelper;
import org.nuxeo.ecm.platform.video.VideoInfo;

/**
 * @since 10.1
 */
public class VideoStoryboardWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(VideoStoryboardWork.class);

    public static final String CATEGORY_VIDEO_STORYBOARD = "videoStoryboard";

    protected static String computeIdPrefix(String repositoryName, String docId) {
        return repositoryName + ':' + docId + ":videostoryboard:";
    }

    public VideoStoryboardWork(String repositoryName, String docId) {
        super(computeIdPrefix(repositoryName, docId));
        setDocument(repositoryName, docId);
    }

    @Override
    public boolean isIdempotent() {
        return false;
    }

    @Override
    public String getCategory() {
        return CATEGORY_VIDEO_STORYBOARD;
    }

    @Override
    public String getTitle() {
        return "Video Storyboard " + getId();
    }

    @Override
    public void work() {
        openSystemSession();
        DocumentModel doc = session.getDocument(new IdRef(docId));
        BlobHolder blobHolder = doc.getAdapter(BlobHolder.class);
        Blob blob = blobHolder.getBlob();
        if (blob == null || blob.getLength() == 0) {
            log.debug("Empty blob skip work");
            return;
        }
        Map<String, Serializable> videoInfoMap = (Map<String, Serializable>) doc.getPropertyValue("vid:info");
        VideoInfo videoInfo = VideoInfo.fromMap(videoInfoMap);
        if (videoInfo == null || videoInfo.getHeight() == 0) {
            log.debug("Updating video info");
            VideoHelper.updateVideoInfo(doc, blob);
            doc.putContextData(ALLOW_VERSION_WRITE, Boolean.TRUE);
            session.saveDocument(doc);
        }
        log.debug("Updating storyboard");
        VideoHelper.updateStoryboard(doc, blob);
        try {
            log.debug("Updating previews");
            VideoHelper.updatePreviews(doc, blob);
        } catch (IOException e) {
            // this should only happen if the hard drive is full
            log.error(String.format("Failed to extract previews for video '%s': %s", doc.getTitle(), e.getMessage()),
                    e);
        }
        if (doc.isVersion()) {
            doc.putContextData(ALLOW_VERSION_WRITE, Boolean.TRUE);
        }
        log.debug("Saving");
        session.saveDocument(doc);
        session.save();
        log.debug("Done");
    }

}
