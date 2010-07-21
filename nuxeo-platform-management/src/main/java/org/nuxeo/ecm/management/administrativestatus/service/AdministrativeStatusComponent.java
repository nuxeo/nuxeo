/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     mcedica
 */
package org.nuxeo.ecm.management.administrativestatus.service;

import static org.nuxeo.ecm.management.administrativestatus.service.AdministrativeStatusConstants.ADMINISTRATIVE_INFO_CONTAINER;
import static org.nuxeo.ecm.management.administrativestatus.service.AdministrativeStatusConstants.ADMINISTRATIVE_INFO_CONTAINER_TYPE;
import static org.nuxeo.ecm.management.administrativestatus.service.AdministrativeStatusConstants.ADMINISTRATIVE_STATUS_DOCUMENT;
import static org.nuxeo.ecm.management.administrativestatus.service.AdministrativeStatusConstants.ADMINISTRATIVE_STATUS_DOCUMENT_TYPE;
import static org.nuxeo.ecm.management.administrativestatus.service.AdministrativeStatusConstants.ADMINISTRATIVE_STATUS_PROPERTY;
import static org.nuxeo.ecm.management.administrativestatus.service.AdministrativeStatusConstants.LOCKED;
import static org.nuxeo.ecm.management.administrativestatus.service.AdministrativeStatusConstants.UNLOCKED;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.runtime.model.DefaultComponent;

public class AdministrativeStatusComponent extends DefaultComponent implements
        AdministrativeStatusService {

    private static Log log = LogFactory.getLog(AdministrativeStatusComponent.class);

    private DocumentModel statusDoc;

    public boolean lockServer(CoreSession session) throws ClientException {
        return setStatusProperty(session, LOCKED);
    }

    public boolean unlockServer(CoreSession session) throws ClientException {
        return setStatusProperty(session, UNLOCKED);
    }

    public String getServerStatus(CoreSession session) throws ClientException {
        return (String) getOrCreateStatusDocument(session).getPropertyValue(
                ADMINISTRATIVE_STATUS_PROPERTY);
    }

    private boolean setStatusProperty(CoreSession session, String status)
            throws ClientException {
        try {
            statusDoc = getOrCreateStatusDocument(session);
            statusDoc.setPropertyValue(ADMINISTRATIVE_STATUS_PROPERTY, status);
            session.saveDocument(statusDoc);
        } catch (ClientException e) {
            log.error("Unable to change the status of the server to " + status,
                    e);
            return false;
        }
        return true;
    }

    private DocumentModel getOrCreateStatusDocument(CoreSession session)
            throws ClientException {
        if (statusDoc == null) {
            AdministrativeStatusDocCreator docCreator = new AdministrativeStatusDocCreator(
                    session);
            try {
                docCreator.runUnrestricted();
            } catch (ClientException e) {
                log.error("Unable to fetch the administrative status document",
                        e);
                throw new ClientException(e);
            }
            statusDoc = docCreator.getStatusDocument();
        }
        return statusDoc;
    }

    private class AdministrativeStatusDocCreator extends
            UnrestrictedSessionRunner {

        private DocumentModel statusDoc;

        public AdministrativeStatusDocCreator(CoreSession session) {
            super(session);
        }

        @Override
        public void run() throws ClientException {
            DocumentModel administrativeContainer = getOrCreateAdministrativeContainter(session);
            DocumentRef statusDocRef = new PathRef(
                    administrativeContainer.getPathAsString() + "/"
                            + ADMINISTRATIVE_STATUS_DOCUMENT);
            if (!session.exists(statusDocRef)) {
                DocumentModel statusModel = session.createDocumentModel(
                        administrativeContainer.getPathAsString(),
                        ADMINISTRATIVE_STATUS_DOCUMENT,
                        ADMINISTRATIVE_STATUS_DOCUMENT_TYPE);
                statusDoc = session.createDocument(statusModel);
                // set staus unlocked by default
                statusDoc.setPropertyValue(ADMINISTRATIVE_STATUS_PROPERTY,
                        UNLOCKED);
                session.save();
            } else {
                statusDoc = session.getDocument(statusDocRef);
            }

        }

        private DocumentModel getOrCreateAdministrativeContainter(
                CoreSession session) throws ClientException {
            DocumentRef admRootDocRef = new PathRef("/"
                    + ADMINISTRATIVE_INFO_CONTAINER);
            if (!session.exists(admRootDocRef)) {
                DocumentModel model = session.createDocumentModel("/",
                        ADMINISTRATIVE_INFO_CONTAINER,
                        ADMINISTRATIVE_INFO_CONTAINER_TYPE);
                model = session.createDocument(model);
                session.save();
            }

            return session.getDocument(admRootDocRef);
        }

        public DocumentModel getStatusDocument() {
            return statusDoc;
        }

    }

}
