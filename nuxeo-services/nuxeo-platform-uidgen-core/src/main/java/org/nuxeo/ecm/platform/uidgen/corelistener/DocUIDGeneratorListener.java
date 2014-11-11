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

package org.nuxeo.ecm.platform.uidgen.corelistener;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.uidgen.service.ServiceHelper;
import org.nuxeo.ecm.platform.uidgen.service.UIDGeneratorService;

public class DocUIDGeneratorListener implements EventListener {

    private static final Log log = LogFactory
            .getLog(DocUIDGeneratorListener.class);

    public void handleEvent(Event event) throws ClientException {

        if (!DOCUMENT_CREATED.equals(event.getName())) {
            return;
        }
        EventContext ctx = event.getContext();
        if (ctx instanceof DocumentEventContext) {
            DocumentEventContext docCtx = (DocumentEventContext) ctx;
            DocumentModel doc = docCtx.getSourceDocument();
            if(doc.isProxy() || doc.isVersion()) {
                // a proxy or version keeps the uid of the document
                // being proxied or versioned => we're not allowed
                // to modify its field.
                return;
            }
            String eventId = event.getName();
            log.debug("eventId : " + eventId);
            try {
                addUIDtoDoc(doc);
            } catch (DocumentException e) {
                log.error(
                        "Error occurred while generating UID for doc: " + doc,
                        e);
            }
        }
    }

    private static void addUIDtoDoc(DocumentModel doc) throws DocumentException {
        UIDGeneratorService service = ServiceHelper.getUIDGeneratorService();
        if (service == null) {
            log.error("<addUIDtoDoc> UIDGeneratorService service not found ... !");
            return;
        }
        // generate UID for our doc
        service.setUID(doc);
    }

}
