/*
 * (C) Copyright 2010-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.video.listener;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.platform.video.VideoConstants.CTX_FORCE_INFORMATIONS_GENERATION;
import static org.nuxeo.ecm.platform.video.VideoConstants.TRANSCODED_VIDEOS_PROPERTY;
import static org.nuxeo.ecm.platform.video.VideoConstants.VIDEO_FACET;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.video.VideoHelper;
import org.nuxeo.ecm.platform.video.service.VideoInfoWork;
import org.nuxeo.runtime.api.Framework;

/**
 * Light synchronous listener that schedules an asynchronous work to process the video info of a document.
 * <p>
 * This {@link VideoInfoWork} will in turn schedule two asynchronous works to process the video storyboard and
 * conversions.
 *
 * @since 5.5
 */
public class VideoChangedListener implements EventListener {

    private static final Logger log = LogManager.getLogger(VideoChangedListener.class);

    /** @since 11.1 **/
    public static final String DISABLE_VIDEO_CONVERSIONS_GENERATION_LISTENER = "disableVideoConversionsGenerationListener";

    @Override
    public void handleEvent(Event event) {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }

        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        DocumentModel doc = docCtx.getSourceDocument();
        if (Boolean.TRUE.equals(ctx.getProperty(DISABLE_VIDEO_CONVERSIONS_GENERATION_LISTENER))) {
            log.trace("Video conversions are disabled for document {}", doc::getId);
            return;
        }

        String eventName = event.getName();
        if (shouldProcess(doc, eventName)) {
            if (BEFORE_DOC_UPDATE.equals(eventName)) {
                try {
                    resetProperties(doc);
                } catch (IOException e) {
                    throw new NuxeoException(
                            String.format("Error while resetting video properties of document %s.", doc), e);
                }
            }
            scheduleAsyncProcessing(doc);
        }
    }

    protected boolean shouldProcess(DocumentModel doc, String eventName) {
        return doc.hasFacet(VIDEO_FACET) && !doc.isProxy()
                && (Boolean.TRUE.equals(doc.getContextData(CTX_FORCE_INFORMATIONS_GENERATION))
                        || DOCUMENT_CREATED.equals(eventName) || doc.getProperty("file:content").isDirty());
    }

    protected void resetProperties(DocumentModel doc) throws IOException {
        log.debug("Resetting video info, storyboard, previews and conversions of document {}", doc);
        VideoHelper.updateVideoInfo(doc, null);
        VideoHelper.updateStoryboard(doc, null);
        VideoHelper.updatePreviews(doc, null);
        doc.setPropertyValue(TRANSCODED_VIDEOS_PROPERTY, null);
    }

    protected void scheduleAsyncProcessing(DocumentModel doc) {
        WorkManager workManager = Framework.getService(WorkManager.class);
        VideoInfoWork work = new VideoInfoWork(doc.getRepositoryName(), doc.getId());
        log.debug("Scheduling work: video info of document {}.", doc);
        workManager.schedule(work, true);
    }

}
