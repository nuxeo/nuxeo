/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.filesystem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.filesystem.FilesystemService;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener to synchronize title, filename and name.
 * <p>
 * This depends on the configured {@link FilesystemService}.
 */
public class FilesystemServiceListener implements EventListener {

    private static final Log log = LogFactory.getLog(FilesystemServiceListener.class);

    @Override
    public void handleEvent(Event event) throws ClientException {
        String eventId = event.getName();
        if (!eventId.equals(DocumentEventTypes.BEFORE_DOC_UPDATE)
                && !eventId.equals(DocumentEventTypes.ABOUT_TO_CREATE)) {
            return;
        }
        if (!(event.getContext() instanceof DocumentEventContext)) {
            return;
        }
        DocumentEventContext context = (DocumentEventContext) event.getContext();
        DocumentModel doc = context.getSourceDocument();
        DocumentModel oldDoc = (DocumentModel) context.getProperty(CoreEventConstants.PREVIOUS_DOCUMENT_MODEL);
        set(doc, oldDoc);
    }

    protected void set(DocumentModel doc, DocumentModel oldDoc)
            throws ClientException {
        FilesystemService service;
        try {
            service = Framework.getService(FilesystemService.class);
        } catch (Exception e) {
            service = null;
        }
        if (service == null) {
            log.error("FilesystemService not found");
            return;
        }
        service.set(doc, oldDoc);
    }

}
