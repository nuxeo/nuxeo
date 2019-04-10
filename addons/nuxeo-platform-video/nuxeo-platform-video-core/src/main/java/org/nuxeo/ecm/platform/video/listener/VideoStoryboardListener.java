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

import static org.nuxeo.ecm.core.api.CoreSession.ALLOW_VERSION_WRITE;
import static org.nuxeo.ecm.platform.video.VideoConstants.HAS_STORYBOARD_FACET;
import static org.nuxeo.ecm.platform.video.VideoConstants.VIDEO_CHANGED_EVENT;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.video.VideoHelper;

/**
 * Core event listener to compute / update the storyboard of a Video document
 *
 * @author ogrisel
 */
public class VideoStoryboardListener implements PostCommitFilteringEventListener {

    public static final Log log = LogFactory.getLog(VideoStoryboardListener.class);

    @Override
    public void handleEvent(EventBundle events) {
        for (Event event : events) {
            if (VIDEO_CHANGED_EVENT.equals(event.getName())) {
                handleEvent(event);
            }
        }
    }

    public void handleEvent(Event event) {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }
        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        DocumentModel doc = docCtx.getSourceDocument();
        if (doc.hasFacet(HAS_STORYBOARD_FACET)) {
            BlobHolder blobHolder = doc.getAdapter(BlobHolder.class);
            VideoHelper.updateStoryboard(doc, blobHolder.getBlob());
            try {
                VideoHelper.updatePreviews(doc, blobHolder.getBlob());
            } catch (IOException e) {
                // this should only happen if the hard drive is full
                log.error(
                        String.format("Failed to extract previews for video '%s': %s", doc.getTitle(), e.getMessage()),
                        e);
            }
            CoreSession session = docCtx.getCoreSession();
            if (doc.isVersion()) {
                doc.putContextData(ALLOW_VERSION_WRITE, Boolean.TRUE);
            }
            session.saveDocument(doc);
            session.save();
        }
    }

    @Override
    public boolean acceptEvent(Event event) {
        return VIDEO_CHANGED_EVENT.equals(event.getName());
    }
}
