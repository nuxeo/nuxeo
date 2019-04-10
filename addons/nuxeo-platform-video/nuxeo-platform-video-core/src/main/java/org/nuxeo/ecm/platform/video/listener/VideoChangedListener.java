/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.PICTURE_VIEWS_PROPERTY;
import static org.nuxeo.ecm.platform.video.VideoConstants.CTX_FORCE_INFORMATIONS_GENERATION;
import static org.nuxeo.ecm.platform.video.VideoConstants.HAS_VIDEO_PREVIEW_FACET;
import static org.nuxeo.ecm.platform.video.VideoConstants.STORYBOARD_PROPERTY;
import static org.nuxeo.ecm.platform.video.VideoConstants.TRANSCODED_VIDEOS_PROPERTY;
import static org.nuxeo.ecm.platform.video.VideoConstants.VIDEO_CHANGED_EVENT;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants;
import org.nuxeo.ecm.platform.video.VideoHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * Core event listener to trigger the {@link org.nuxeo.ecm.platform.video.VideoConstants#VIDEO_CHANGED_EVENT} event if
 * the main video has changed. This is useful to update the video information, thumbnails and story board in a dedicated
 * async event listener.
 *
 * @author ogrisel
 * @since 5.5
 */
public class VideoChangedListener implements EventListener {

    private static final Log log = LogFactory.getLog(VideoChangedListener.class);

    @Override
    public void handleEvent(Event event) {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }
        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        DocumentModel doc = docCtx.getSourceDocument();
        if (doc.hasFacet(HAS_VIDEO_PREVIEW_FACET) && !doc.isProxy()) {
            boolean forceGeneration = Boolean.TRUE.equals(doc.getContextData(CTX_FORCE_INFORMATIONS_GENERATION));
            Property origVideoProperty = doc.getProperty("file:content");
            if (forceGeneration || DOCUMENT_CREATED.equals(event.getName()) || origVideoProperty.isDirty()) {

                Blob video = (Blob) origVideoProperty.getValue();
                updateVideoInfo(doc, video);

                if (BEFORE_DOC_UPDATE.equals(event.getName())) {
                    doc.setPropertyValue(TRANSCODED_VIDEOS_PROPERTY, null);
                    doc.setPropertyValue(STORYBOARD_PROPERTY, null);
                    doc.setPropertyValue(PICTURE_VIEWS_PROPERTY, null);
                }

                // only trigger the event if we really have a video
                if (video != null) {
                    Event trigger = docCtx.newEvent(VIDEO_CHANGED_EVENT);
                    EventService eventService = Framework.getService(EventService.class);
                    eventService.fireEvent(trigger);
                }
            }
        }
    }

    protected void updateVideoInfo(DocumentModel doc, Blob video) {
        try {
            VideoHelper.updateVideoInfo(doc, video);
        } catch (NuxeoException e) {
            // may happen if ffmpeg is not installed
            log.error(String.format("Unable to retrieve video info: %s", e.getMessage()));
            log.debug(e, e);
        }
    }

}
