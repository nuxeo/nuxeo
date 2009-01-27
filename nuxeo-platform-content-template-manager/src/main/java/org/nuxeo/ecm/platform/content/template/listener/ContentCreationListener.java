/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.content.template.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.content.template.service.ContentTemplateService;
import org.nuxeo.runtime.api.Framework;

public class ContentCreationListener implements EventListener {

    private static final Log log = LogFactory
            .getLog(ContentCreationListener.class);

    private ContentTemplateService service;

    private ContentTemplateService getService() {
        if (service == null) {
            service = Framework.getLocalService(ContentTemplateService.class);
        }
        return service;
    }

    public void handleEvent(Event event) throws ClientException {

        DocumentEventContext docCtx = null;
        if (event.getContext() instanceof DocumentEventContext) {
            docCtx = (DocumentEventContext) event.getContext();
        } else {
            return;
        }
        String eventId = event.getName();
        DocumentModel createdDocument = docCtx.getSourceDocument();

        if (eventId.equals(DocumentEventTypes.DOCUMENT_CREATED)) {
            try {
                getService().executeFactoryForType(createdDocument);
            } catch (ClientException e) {
                log.error("Error while executing content factory for type "
                        + createdDocument.getType() + " : " + e.getMessage());
            }
        }
    }
}
