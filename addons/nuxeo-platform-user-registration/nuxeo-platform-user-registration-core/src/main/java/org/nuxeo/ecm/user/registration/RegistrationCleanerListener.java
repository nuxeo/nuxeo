/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Arnaud Kervern <akervern@nuxeo.com>
 */

package org.nuxeo.ecm.user.registration;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_REMOVED;
import static org.nuxeo.ecm.user.registration.DocumentRegistrationInfo.DOCUMENT_ID_FIELD;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

public class RegistrationCleanerListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        if (!event.getName().equals(DOCUMENT_REMOVED) || !(event.getContext() instanceof DocumentEventContext)) {
            return;
        }

        DocumentEventContext context = (DocumentEventContext) event.getContext();
        final DocumentModel sourceDocument = context.getSourceDocument();

        if (sourceDocument.getType().equals("UserRegistration") || sourceDocument.isVersion()) {
            return;
        }

        new UnrestrictedSessionRunner(context.getCoreSession()) {
            @Override
            public void run() {
                DocumentModelList docs = session.query(String.format(
                        "Select * from Document where ecm:mixinType = 'UserRegistration' and %s = '%s'",
                        DOCUMENT_ID_FIELD, sourceDocument.getId()));
                for (DocumentModel doc : docs) {
                    session.removeDocument(doc.getRef());
                }
            }
        }.runUnrestricted();
    }
}
