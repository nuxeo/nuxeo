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
 *
 * Contributors:
 *     Benoît Delbosc <bdelbosc@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.platform.video.service;

import static org.nuxeo.ecm.core.api.CoreSession.ALLOW_VERSION_WRITE;
import static org.nuxeo.ecm.platform.video.VideoConstants.HAS_STORYBOARD_FACET;
import static org.nuxeo.ecm.platform.video.VideoConstants.HAS_VIDEO_PREVIEW_FACET;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.video.VideoHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * Work to process the video info of a Video document and schedule two works to process the storyboard and conversions,
 * see {@link VideoStoryboardWork} and {@link VideoConversionWork}.
 *
 * @since 10.1
 */
public class VideoInfoWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(VideoInfoWork.class);

    public static final String CATEGORY_VIDEO_INFO = "videoInfo";

    protected static String computeIdPrefix(String repositoryName, String docId) {
        return repositoryName + ':' + docId + ":videoinfo:";
    }

    public VideoInfoWork(String repositoryName, String docId) {
        super(computeIdPrefix(repositoryName, docId));
        setDocument(repositoryName, docId);
    }

    @Override
    public boolean isIdempotent() {
        return false;
    }

    @Override
    public String getCategory() {
        return CATEGORY_VIDEO_INFO;
    }

    @Override
    public String getTitle() {
        return "Video Info: " + getId();
    }

    @Override
    public void work() {
        setStatus("Updating video info");
        setProgress(Progress.PROGRESS_INDETERMINATE);
        openSystemSession();

        // get video blob and update video info
        DocumentModel doc = session.getDocument(new IdRef(docId));
        BlobHolder blobHolder = doc.getAdapter(BlobHolder.class);
        Blob video = blobHolder.getBlob();
        log.debug(String.format("Updating video info of document %s.", doc));
        VideoHelper.updateVideoInfo(doc, video);
        log.debug(String.format("End updating video info of document %s.", doc));

        // save document
        if (doc.isVersion()) {
            doc.putContextData(ALLOW_VERSION_WRITE, Boolean.TRUE);
        }
        session.saveDocument(doc);

        if (doc.hasFacet(HAS_VIDEO_PREVIEW_FACET) && doc.hasFacet(HAS_STORYBOARD_FACET)) {
            // schedule storyboard work
            WorkManager workManager = Framework.getService(WorkManager.class);
            VideoStoryboardWork work = new VideoStoryboardWork(doc.getRepositoryName(), doc.getId());
            log.debug(String.format("Scheduling work: storyboard of Video document %s.", doc));
            workManager.schedule(work, true);
        }

        // schedule conversion work
        VideoService videoService = Framework.getService(VideoService.class);
        log.debug(String.format("Launching automatic conversions of Video document %s.", doc));
        videoService.launchAutomaticConversions(doc);

        setStatus("Done");
    }

}