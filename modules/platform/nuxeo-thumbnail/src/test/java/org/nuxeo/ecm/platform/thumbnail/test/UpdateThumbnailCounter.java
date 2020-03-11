/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.thumbnail.test;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;

public class UpdateThumbnailCounter implements EventListener {

    protected static int count;

    @Override
    public void handleEvent(Event event) {
        DocumentEventContext context = (DocumentEventContext) event.getContext();
        DocumentModel doc = context.getSourceDocument();
        if (doc.hasFacet(ThumbnailConstants.THUMBNAIL_FACET)) {
            Property prop = doc.getProperty(ThumbnailConstants.THUMBNAIL_PROPERTY_NAME);
            if (prop.isDirty()) {
                count += 1;
            }
        }
    }

}
