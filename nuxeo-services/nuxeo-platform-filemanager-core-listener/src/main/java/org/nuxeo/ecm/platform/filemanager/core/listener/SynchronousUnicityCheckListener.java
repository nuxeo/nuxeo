/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.filemanager.core.listener;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

public class SynchronousUnicityCheckListener extends AbstractUnicityChecker implements EventListener {

    private static final Log log = LogFactory.getLog(SynchronousUnicityCheckListener.class);

    public void handleEvent(Event event) throws ClientException {
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
    protected void onDuplicatedDoc(CoreSession session, Principal principal,
            DocumentModel newDoc, List<DocumentLocation> existingDocs, Event event) {
        // simply send a message
        log.info("Duplicated file detected");
        raiseDuplicatedFileEvent(session, principal, newDoc, existingDocs);
    }

}
