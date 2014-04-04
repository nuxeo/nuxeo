/*
 * (C) Copyright 2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Arnaud Kervern <akervern@nuxeo.com>
 */

package org.nuxeo.ecm.user.registration;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_REMOVED;
import static org.nuxeo.ecm.user.registration.DocumentRegistrationInfo.DOCUMENT_ID_FIELD;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

public class RegistrationCleanerListener implements EventListener {

    @Override
    public void handleEvent(Event event) throws ClientException {
        if (!event.getName().equals(DOCUMENT_REMOVED)
                || !(event.getContext() instanceof DocumentEventContext)) {
            return;
        }

        DocumentEventContext context = (DocumentEventContext) event.getContext();
        final DocumentModel sourceDocument = context.getSourceDocument();

        if (sourceDocument.getType().equals("UserRegistration") || sourceDocument.isVersion()) {
            return;
        }

        new UnrestrictedSessionRunner(context.getCoreSession()) {
            @Override
            public void run() throws ClientException {
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
