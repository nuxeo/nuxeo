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

package org.nuxeo.ecm.platform.video.listener;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;
import static org.nuxeo.ecm.platform.video.VideoConstants.VIDEO_CHANGED_PROPERTY;
import static org.nuxeo.ecm.platform.video.VideoConstants.VIDEO_FACET;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.video.service.VideoService;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener to launch
 * {@link org.nuxeo.ecm.platform.video.service.AutomaticVideoConversion}s when
 * creating or updating a video file.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class VideoAutomaticConversionListener implements
        PostCommitEventListener {

    @Override
    public void handleEvent(EventBundle events) throws ClientException {
        if (!events.containsEventName(DOCUMENT_CREATED)
                && !events.containsEventName(DOCUMENT_UPDATED)) {
            return;
        }

        for (Event event : events) {
            if (DOCUMENT_CREATED.equals(event.getName())
                    || DOCUMENT_UPDATED.equals(event.getName())) {
                handleEvent(event);
            }
        }
    }

    private void handleEvent(Event event) throws ClientException {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }

        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        DocumentModel doc = docCtx.getSourceDocument();
        if (doc == null || !doc.hasFacet(VIDEO_FACET)
                || !ctx.hasProperty(VIDEO_CHANGED_PROPERTY)) {
            return;
        }

        VideoService videoService = Framework.getLocalService(VideoService.class);
        videoService.launchAutomaticConversions(doc);
    }

}
