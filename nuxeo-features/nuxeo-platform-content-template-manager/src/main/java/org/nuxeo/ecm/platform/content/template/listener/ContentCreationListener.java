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
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.listener.AbstractEventListener;
import org.nuxeo.ecm.platform.content.template.service.ContentTemplateService;
import org.nuxeo.runtime.api.Framework;

public class ContentCreationListener extends AbstractEventListener {

    private static final Log log = LogFactory.getLog(ContentCreationListener.class);

    private ContentTemplateService service;

    public void notifyEvent(CoreEvent event) throws Exception {

        String eventId = event.getEventId();
        Object ob = event.getSource();

        if (!(ob instanceof DocumentModel)) {
            return;
        }

        DocumentModel createdDocument = (DocumentModel) ob;

        if (eventId.equals(DocumentEventTypes.DOCUMENT_CREATED)) {
            try {
                getService().executeFactoryForType(createdDocument);
            }
            catch (ClientException e) {
                log.error(
                        "Error while executing content factory for type " + createdDocument.getType() + " : " + e.getMessage());
            }
        }
    }

    private ContentTemplateService getService() {
        if (service == null) {
            service = Framework.getLocalService(ContentTemplateService.class);
        }
        return service;
    }

}
