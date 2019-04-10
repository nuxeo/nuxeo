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
import static org.nuxeo.ecm.platform.video.VideoConstants.VIDEO_CHANGED_EVENT;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * Core event listener to trigger the {@link VIDEO_CHANGED_EVENT} event if the
 * main video has changed.
 *
 * This is useful to update the video information, thumbnails and story board in
 * a dedicated async event listener.
 *
 * @author ogrisel
 * @since 5.5
 */
public class VideoChangedListener implements EventListener {

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
                Event trigger = docCtx.newEvent(VIDEO_CHANGED_EVENT);
                EventService eventService = Framework.getLocalService(EventService.class);
                eventService.fireEvent(trigger);
            }
        }
    }

}
