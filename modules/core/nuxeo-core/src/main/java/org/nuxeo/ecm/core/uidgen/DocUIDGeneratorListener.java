/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.core.uidgen;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

public class DocUIDGeneratorListener implements EventListener {

    private static final Log log = LogFactory.getLog(DocUIDGeneratorListener.class);

    @Override
    public void handleEvent(Event event) {

        if (!DOCUMENT_CREATED.equals(event.getName())) {
            return;
        }
        EventContext ctx = event.getContext();
        if (ctx instanceof DocumentEventContext) {
            DocumentEventContext docCtx = (DocumentEventContext) ctx;
            DocumentModel doc = docCtx.getSourceDocument();
            if (doc.isProxy() || doc.isVersion()) {
                // a proxy or version keeps the uid of the document
                // being proxied or versioned => we're not allowed
                // to modify its field.
                return;
            }
            String eventId = event.getName();
            log.debug("eventId : " + eventId);
            try {
                addUIDtoDoc(doc);
            } catch (PropertyNotFoundException e) {
                log.error("Error occurred while generating UID for doc: " + doc, e);
            }
        }
    }

    private static void addUIDtoDoc(DocumentModel doc) throws PropertyNotFoundException {
        UIDGeneratorService service = Framework.getService(UIDGeneratorService.class);
        if (service == null) {
            log.error("<addUIDtoDoc> UIDGeneratorService service not found ... !");
            return;
        }
        // generate UID for our doc
        service.setUID(doc);
    }

}
