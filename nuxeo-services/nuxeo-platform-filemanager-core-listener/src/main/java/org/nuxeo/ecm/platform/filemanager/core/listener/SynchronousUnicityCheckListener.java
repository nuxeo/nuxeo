/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.filemanager.core.listener;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

public class SynchronousUnicityCheckListener extends AbstractUnicityChecker implements EventListener {

    private static final Log log = LogFactory.getLog(SynchronousUnicityCheckListener.class);

    public void handleEvent(Event event) {
        if (!isUnicityCheckEnabled()) {
            return;
        }

        List<String> uuids = new ArrayList<String>();
        if (DocumentEventTypes.DOCUMENT_CREATED.equals(event.getName())
                || DocumentEventTypes.DOCUMENT_UPDATED.equals(event.getName())) {
            EventContext ctx = event.getContext();
            if (ctx instanceof DocumentEventContext) {
                DocumentEventContext docCtx = (DocumentEventContext) ctx;

                DocumentModel doc2Check = docCtx.getSourceDocument();
                if (doc2Check.isProxy()) {
                    // NOP
                }
                if (!uuids.contains(doc2Check.getId())) {
                    uuids.add(doc2Check.getId());
                    doUnicityCheck(doc2Check, docCtx.getCoreSession(), event);
                }
            }
        }
    }

    @Override
    protected void onDuplicatedDoc(CoreSession session, NuxeoPrincipal principal, DocumentModel newDoc,
            List<DocumentLocation> existingDocs, Event event) {
        // simply send a message
        log.info("Duplicated file detected");
        raiseDuplicatedFileEvent(session, principal, newDoc, existingDocs);
    }

}
