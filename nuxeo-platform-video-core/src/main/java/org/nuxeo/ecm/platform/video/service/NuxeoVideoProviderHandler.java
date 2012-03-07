/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.video.service;

import static org.nuxeo.ecm.platform.video.VideoConstants.HAS_VIDEO_PREVIEW_FACET;
import static org.nuxeo.ecm.platform.video.VideoConstants.VIDEO_CHANGED_PROPERTY;
import static org.nuxeo.ecm.platform.video.VideoConstants.VIDEO_FACET;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.platform.video.VideoHelper;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
public class NuxeoVideoProviderHandler implements VideoProviderHandler {

    @Override
    public void onVideoCreated(VideoProvider videoProvider, DocumentModel doc,
            EventContext ctx) {
        updateVideo(doc, ctx);
    }

    @Override
    public void onVideoModified(VideoProvider videoProvider, DocumentModel doc,
            EventContext ctx) {
        updateVideo(doc, ctx);
    }

    @Override
    public void onVideoRemoved(VideoProvider videoProvider, DocumentModel doc,
            EventContext ctx) {
        // do nothing
    }

    protected void updateVideo(DocumentModel doc, EventContext ctx) {
        try {
            if (doc.hasFacet(VIDEO_FACET)) {
                Property origVideoProperty = doc.getProperty("file:content");
                if (origVideoProperty.isDirty()) {
                    Blob blob = origVideoProperty.getValue(Blob.class);
                    VideoHelper.updateVideoInfo(doc, blob);
                    if (doc.hasFacet(HAS_VIDEO_PREVIEW_FACET)) {
                        VideoHelper.updatePreviews(doc, blob);
                    }
                    ctx.setProperty(VIDEO_CHANGED_PROPERTY, true);
                }
            }
        } catch (Exception e) {
            throw new ClientRuntimeException(e);
        }
    }

}
