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

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.listener.AbstractEventListener;
import org.nuxeo.ecm.core.listener.AsynchronousEventListener;
import org.nuxeo.ecm.core.listener.DocumentModelEventListener;
import org.nuxeo.ecm.platform.dublincore.NXDublinCore;
import org.nuxeo.ecm.platform.dublincore.service.DublinCoreStorageService;

/**
 * Core Event Listener for updating DublinCore.
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 * @author <a href="mailto:rspivak@nuxeo.com">Ruslan Spivak</a>
 */
public class DublinCoreListener extends AbstractEventListener implements
        AsynchronousEventListener, DocumentModelEventListener {

    private static final Log log = LogFactory.getLog(DublinCoreListener.class);

    /**
     * Core event notification.
     * <p>
     * Gets core events and updates DublinCore if needed.
     *
     * @param coreEvent event fired at core layer
     *
     */
    public void notifyEvent(CoreEvent coreEvent) {
        Object source = coreEvent.getSource();
        if (source instanceof DocumentModel) {
            try {

                DocumentModel doc = (DocumentModel) source;
                String eventId = coreEvent.getEventId();

                if (!eventId.equals(DOCUMENT_UPDATED)
                        && !eventId.equals(DOCUMENT_CREATED)
                        && !eventId.equals(DocumentEventTypes.BEFORE_DOC_UPDATE)) {
                    return;
                }

                DublinCoreStorageService service = NXDublinCore.getDublinCoreStorageService();
                log.debug("Processing event " + eventId);
                if (service == null) {
                    log.error("DublinCoreStorage service not found ... !");
                    return;
                }

                Date eventDate = coreEvent.getDate();
                Calendar cEventDate = Calendar.getInstance();
                cEventDate.setTime(eventDate);
                Boolean updateResult;

                if (eventId.equals(DocumentEventTypes.BEFORE_DOC_UPDATE)) {
                    updateResult = service.setModificationDate(doc, cEventDate,
                            coreEvent);
                    if (updateResult) {
                        log.debug("Modification Date updated");
                    }
                    return;
                }

                if (eventId.equals(DOCUMENT_CREATED)) {
                    updateResult = service.setCreationDate(doc, cEventDate,
                            coreEvent);
                    if (updateResult) {
                        log.debug("Creation Date updated");
                    }
                    updateResult = service.setModificationDate(doc, cEventDate,
                            coreEvent);
                    if (updateResult) {
                        log.debug("Modification Date updated");
                    }
                }

            } catch (Exception e) {
                log.error("An error occurred trying to notify: ", e);
            }
        }
    }

}
