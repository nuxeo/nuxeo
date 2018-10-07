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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.runtime.api.Framework;

public abstract class AbstractUnicityChecker {

    private static final Log log = LogFactory.getLog(AbstractUnicityChecker.class);

    protected FileManager fileManager;

    protected static Boolean unicityCheckEnabled;

    protected static final String DUPLICATED_FILE = "duplicatedFile";

    protected void doUnicityCheck(DocumentModel doc2Check, CoreSession session, Event event) {

        List<String> xpathFields = getFileManagerService().getFields();

        if (xpathFields == null || xpathFields.isEmpty()) {
            unicityCheckEnabled = false;
            log.info("Unicity check has been automatically disabled");
            return;
        }

        for (String field : xpathFields) {
            Blob blob;
            try {
                blob = (Blob) doc2Check.getPropertyValue(field);
            } catch (PropertyNotFoundException pnfe) {
                continue;
            }
            if (blob == null) {
                log.debug("No blob retrieved");
                continue;
            }

            String digest = blob.getDigest();
            if (digest == null) {
                log.debug("Blob has no digest, can not check for unicity");
                continue;
            }

            List<DocumentLocation> existingDocuments = null;
            existingDocuments = fileManager.findExistingDocumentWithFile(session, doc2Check.getPathAsString(), digest,
                    session.getPrincipal());

            if (!existingDocuments.isEmpty()) {
                Iterator<DocumentLocation> existingDocumentsIterator = existingDocuments.iterator();
                while (existingDocumentsIterator.hasNext()) {
                    if (existingDocumentsIterator.next().getDocRef() == doc2Check.getRef()) {
                        existingDocumentsIterator.remove();
                    }
                }
                log.debug("Existing Documents[" + existingDocuments.size() + "]");

                onDuplicatedDoc(session, session.getPrincipal(), doc2Check, existingDocuments, event);
            }
        }
    }

    protected abstract void onDuplicatedDoc(CoreSession session, NuxeoPrincipal principal, DocumentModel newDoc,
            List<DocumentLocation> existingDocs, Event event);

    protected void raiseDuplicatedFileEvent(CoreSession session, NuxeoPrincipal principal, DocumentModel newDoc,
            List<DocumentLocation> existingDocs) {

        DocumentEventContext ctx = new DocumentEventContext(session, principal, newDoc);

        Map<String, Serializable> props = new HashMap<String, Serializable>();

        props.put("category", DocumentEventCategories.EVENT_CLIENT_NOTIF_CATEGORY);
        props.put("duplicatedDocLocation", (Serializable) existingDocs);

        Event event = ctx.newEvent(DUPLICATED_FILE);
        EventProducer producer = Framework.getService(EventProducer.class);
        producer.fireEvent(event);
    }

    protected boolean isUnicityCheckEnabled() {
        if (unicityCheckEnabled == null) {
            unicityCheckEnabled = getFileManagerService().isUnicityEnabled();
        }
        return unicityCheckEnabled;
    }

    private FileManager getFileManagerService() {
        if (fileManager == null) {
            fileManager = Framework.getRuntime().getService(FileManager.class);
        }
        return fileManager;
    }

}
