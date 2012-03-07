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

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_CREATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_REMOVE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.platform.video.VideoConstants.HAS_VIDEO_PREVIEW_FACET;
import static org.nuxeo.ecm.platform.video.VideoConstants.VIDEO_CHANGED_PROPERTY;

import java.io.IOException;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.video.VideoHelper;
import org.nuxeo.ecm.platform.video.service.VideoProvider;
import org.nuxeo.ecm.platform.video.service.VideoService;
import org.nuxeo.runtime.api.Framework;

/**
 * Core event listener to update the preview and the info of a Video document.
 * <p>
 * It also set the context property {@link VIDEO_CHANGED_PROPERTY} to
 * {@code true} if the main video has changed.
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

        VideoService videoService = Framework.getLocalService(VideoService.class);
        VideoProvider videoProvider = videoService.getDefaultVideoProvider();

        if (ABOUT_TO_CREATE.equals(event.getName())) {
            videoProvider.onVideoCreated(doc, ctx);
        } else if (BEFORE_DOC_UPDATE.equals(event.getName())) {
            videoProvider.onVideoModified(doc, ctx);
        } else if (ABOUT_TO_REMOVE.equals(event.getName())) {
            videoProvider.onVideoRemoved(doc, ctx);
        }
    }

}
