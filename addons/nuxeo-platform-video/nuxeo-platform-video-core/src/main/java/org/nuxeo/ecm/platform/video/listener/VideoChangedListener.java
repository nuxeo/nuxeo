/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.video.listener;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.platform.video.VideoConstants.HAS_VIDEO_PREVIEW_FACET;
import static org.nuxeo.ecm.platform.video.VideoConstants.TRANSCODED_VIDEOS_PROPERTY;
import static org.nuxeo.ecm.platform.video.VideoConstants.VIDEO_CHANGED_EVENT;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.video.VideoHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * Core event listener to trigger the
 * {@link org.nuxeo.ecm.platform.video.VideoConstants#VIDEO_CHANGED_EVENT} event
 * if the main video has changed.
 *
 * This is useful to update the video information, thumbnails and story board in
 * a dedicated async event listener.
 *
 * @author ogrisel
 * @since 5.5
 */
public class VideoChangedListener implements EventListener {

    private static final Log log = LogFactory.getLog(VideoChangedListener.class);

    @Override
    public void handleEvent(Event event) throws ClientException {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }
        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        DocumentModel doc = docCtx.getSourceDocument();
        if (doc.hasFacet(HAS_VIDEO_PREVIEW_FACET)) {
            Property origVideoProperty = doc.getProperty("file:content");
            if (DOCUMENT_CREATED.equals(event.getName())
                    || origVideoProperty.isDirty()) {
                Blob video = (Blob) origVideoProperty.getValue();
                updateVideoInfo(doc, video);

                // only trigger the event if we really have a video
                if (video != null) {
                    Event trigger = docCtx.newEvent(VIDEO_CHANGED_EVENT);
                    EventService eventService = Framework.getLocalService(EventService.class);
                    eventService.fireEvent(trigger);
                } else {
                    // reset the transcoded videos
                    doc.setPropertyValue(TRANSCODED_VIDEOS_PROPERTY, null);
                }
            }
        }
    }

    protected void updateVideoInfo(DocumentModel doc, Blob video) {
        try {
            VideoHelper.updateVideoInfo(doc, video);
        } catch (ClientException e) {
            // may happen if ffmpeg is not installed
            log.error(String.format("Unable to retrieve video info: %s",
                    e.getMessage()));
            log.debug(e, e);
        }
    }

}
