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
 */

package org.nuxeo.ecm.platform.video.listener;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.PICTURE_VIEWS_PROPERTY;
import static org.nuxeo.ecm.platform.video.VideoConstants.HAS_STORYBOARD_FACET;
import static org.nuxeo.ecm.platform.video.VideoConstants.HAS_VIDEO_PREVIEW_FACET;
import static org.nuxeo.ecm.platform.video.VideoConstants.STORYBOARD_PROPERTY;
import static org.nuxeo.ecm.platform.video.VideoConstants.TRANSCODED_VIDEOS_PROPERTY;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.video.service.VideoService;
import org.nuxeo.ecm.platform.video.service.VideoStoryboardWork;
import org.nuxeo.runtime.api.Framework;

/**
 * Light sync listener that schedules 2 async works to process conversion and storyboard.
 *
 * @since 10.1
 */
public class VideoImportListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        if (!DOCUMENT_CREATED.equals(event.getName())) {
            return;
        }
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }
        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        DocumentModel doc = docCtx.getSourceDocument();
        if (!doc.hasFacet(HAS_VIDEO_PREVIEW_FACET) || doc.isProxy()) {
            return;
        }
        scheduleAsyncProcessing(doc);
    }

    protected void scheduleAsyncProcessing(DocumentModel doc) {
        doc.setPropertyValue(TRANSCODED_VIDEOS_PROPERTY, null);
        doc.setPropertyValue(STORYBOARD_PROPERTY, null);
        doc.setPropertyValue(PICTURE_VIEWS_PROPERTY, null);
        if (doc.hasFacet(HAS_STORYBOARD_FACET)) {
            VideoStoryboardWork work = new VideoStoryboardWork(doc.getRepositoryName(), doc.getId());
            WorkManager workManager = Framework.getService(WorkManager.class);
            if (workManager == null) {
                throw new RuntimeException("No WorkManager available");
            }
            workManager.schedule(work);
        }
        VideoService videoService = Framework.getService(VideoService.class);
        videoService.launchAutomaticConversions(doc);
    }

}
