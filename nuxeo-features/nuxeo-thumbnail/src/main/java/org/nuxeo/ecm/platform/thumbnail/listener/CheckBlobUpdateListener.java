/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Vladimir Pasquier <vpasquier@nuxeo.com>
 * Laurent Doguin <ldoguin@nuxeo.com>
 */
package org.nuxeo.ecm.platform.thumbnail.listener;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * Thumbnail listener handling document blob update and checking changes. Fire
 * an event if it's the case
 *
 * @since 5.7
 */
public class CheckBlobUpdateListener implements EventListener {

    @Override
    public void handleEvent(Event event) throws ClientException {
        EventContext ec = event.getContext();
        if (!(ec instanceof DocumentEventContext)) {
            return;
        }
        DocumentEventContext context = (DocumentEventContext) ec;
        DocumentModel doc = context.getSourceDocument();
        if (!doc.hasSchema("file")) {
            return;
        }
        BlobProperty content = (BlobProperty)doc.getProperty("file:content");
        if (DocumentEventTypes.DOCUMENT_CREATED.equals(event.getName())) {
            if (content.getValue() == null) {
                return;
            }
        } else if (DocumentEventTypes.BEFORE_DOC_UPDATE.equals(event.getName())) {
            if (!content.isDirty()) {
                return;
            }
        }
        Framework.getLocalService(EventService.class).fireEvent(
                ThumbnailConstants.EventNames.afterBlobUpdateCheck.name(),
                context);
    }

}