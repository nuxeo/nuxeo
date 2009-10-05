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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.dublincore.listener;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.dublincore.NXDublinCore;
import org.nuxeo.ecm.platform.dublincore.service.DublinCoreStorageService;

/**
 * Core Event Listener for updating DublinCore.
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 * @author <a href="mailto:rspivak@nuxeo.com">Ruslan Spivak</a>
 */
public class DublinCoreListener implements EventListener {

    private static final Log log = LogFactory.getLog(DublinCoreListener.class);

    /**
     * Core event notification.
     * <p>
     * Gets core events and updates DublinCore if needed.
     *
     * @param event event fired at core layer
     */
    public void handleEvent(Event event) throws ClientException {

        DocumentEventContext docCtx = null;
        if (event.getContext() instanceof DocumentEventContext) {
            docCtx = (DocumentEventContext) event.getContext();
        } else {
            return;
        }
        String eventId = event.getName();

        if (!eventId.equals(DOCUMENT_UPDATED)
                && !eventId.equals(DOCUMENT_CREATED)
                && !eventId.equals(BEFORE_DOC_UPDATE)) {
            return;
        }

        DublinCoreStorageService service = NXDublinCore.getDublinCoreStorageService();
        if (service == null) {
            log.error("DublinCoreStorage service not found ... !");
            return;
        }

        DocumentModel doc = docCtx.getSourceDocument();
        if (doc.isVersion()) {
            log.debug("No DublinCore update on versions");
            return;
        }
        Date eventDate = new Date(event.getTime());
        Calendar cEventDate = Calendar.getInstance();
        cEventDate.setTime(eventDate);

        if (eventId.equals(BEFORE_DOC_UPDATE)) {
            service.setModificationDate(doc, cEventDate, event);
            service.addContributor(doc, event);
        } else if (eventId.equals(DOCUMENT_CREATED)) {
            service.setCreationDate(doc, cEventDate, event);
            service.setModificationDate(doc, cEventDate, event);
            service.addContributor(doc, event);
        }
    }

}
