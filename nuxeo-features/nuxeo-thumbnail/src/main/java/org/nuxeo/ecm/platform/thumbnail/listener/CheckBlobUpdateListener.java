/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Vladimir Pasquier <vpasquier@nuxeo.com>
 * Laurent Doguin <ldoguin@nuxeo.com>
 */
package org.nuxeo.ecm.platform.thumbnail.listener;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.platform.thumbnail.listener.UpdateThumbnailListener.THUMBNAIL_UPDATED;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * Thumbnail listener handling document blob update and checking changes. Fire an event if it's the case
 *
 * @since 5.7
 */
public class CheckBlobUpdateListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        EventContext ec = event.getContext();
        if (!(ec instanceof DocumentEventContext)) {
            return;
        }
        DocumentEventContext context = (DocumentEventContext) ec;
        DocumentModel doc = context.getSourceDocument();
        if (!doc.hasSchema("file")) {
            return;
        }

        Property content = doc.getProperty("file:content");
        // Only perform the thumbnail update at creation or modification if the content is marked as changed and the
        // thumbnail has not already been updated. This additional check is needed to avoid an infinite loop.
        if (DOCUMENT_CREATED.equals(event.getName())
                || content.isDirty() && !Boolean.TRUE.equals(ec.getProperty(THUMBNAIL_UPDATED))) {

            if (BEFORE_DOC_UPDATE.equals(event.getName()) && doc.hasFacet(ThumbnailConstants.THUMBNAIL_FACET)
                    && content.getValue() == null) {
                doc.setPropertyValue(ThumbnailConstants.THUMBNAIL_PROPERTY_NAME, null);
            }

            if (content.getValue() != null) {
                doc.addFacet(ThumbnailConstants.THUMBNAIL_FACET);
                Framework.getLocalService(EventService.class).fireEvent(
                        ThumbnailConstants.EventNames.scheduleThumbnailUpdate.name(), context);
            }
        }
    }

}
