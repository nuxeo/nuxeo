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

package org.nuxeo.ecm.platform.modifier.corelistener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.listener.AbstractEventListener;
import org.nuxeo.ecm.core.listener.AsynchronousEventListener;
import org.nuxeo.ecm.platform.modifier.DocModifierException;
import org.nuxeo.ecm.platform.modifier.service.DocModifierService;
import org.nuxeo.ecm.platform.modifier.service.ServiceHelper;

public class DocModifierListener extends AbstractEventListener implements
        AsynchronousEventListener {

    private static final Log log = LogFactory.getLog(DocModifierListener.class);

    /**
     * Core event notification.
     * <p>
     * Checks with the Document Modifier service for the document type. If it is
     * a candidate further processing will be performed on the document.
     *
     * @param coreEvent instance thrown at core layer
     */
    public void notifyEvent(CoreEvent coreEvent) throws Exception {

        Object source = coreEvent.getSource();
        if (source instanceof DocumentModel) {
            DocumentModel doc = (DocumentModel) source;
            String eventId = coreEvent.getEventId();

            log.debug(String.format("notify event '%s' for document '%s'",
                    eventId, doc.getTitle()));

            // call process for all events and the service will decide whether
            // and what modifier to apply
            getService().processDocument(doc, eventId);
        }
    }

    /**
     * Doesn't return null. If the service is not available an exception is
     * thrown so the caller code won't need to check.
     *
     * @return
     * @throws DocModifierException
     */
    private DocModifierService getService() throws DocModifierException {
        DocModifierService service = ServiceHelper.getDocModifierService();
        if (service == null) {
            log.error("DocModifierService service not found");
            throw new DocModifierException(
                    "DocModifierService service not found");
        }
        return service;
    }

}
