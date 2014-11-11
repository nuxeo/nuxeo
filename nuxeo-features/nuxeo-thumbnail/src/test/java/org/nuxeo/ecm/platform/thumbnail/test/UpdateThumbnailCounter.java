/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.thumbnail.test;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;

public class UpdateThumbnailCounter implements EventListener {

    protected static int count;

    @Override
    public void handleEvent(Event event) throws ClientException {
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
