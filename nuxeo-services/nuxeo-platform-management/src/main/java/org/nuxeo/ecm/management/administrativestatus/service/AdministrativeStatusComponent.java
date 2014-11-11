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
import static org.nuxeo.ecm.management.administrativestatus.service.AdministrativeStatusConstants.ADMINISTRATIVE_INSTANCE_ID;
import static org.nuxeo.ecm.management.administrativestatus.service.AdministrativeStatusConstants.ADMINISTRATIVE_EVENT_CATEGORY;
import static org.nuxeo.ecm.management.administrativestatus.service.AdministrativeStatusConstants.ADMINISTRATIVE_STATUS_DOC_CREATED_EVENT;
import static org.nuxeo.ecm.management.administrativestatus.service.AdministrativeStatusConstants.SEVER_LOCKED_EVENT;
import static org.nuxeo.ecm.management.administrativestatus.service.AdministrativeStatusConstants.SERVER_UNLOCKED_EVENT;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Used to control the server administrative status: the status of the server
 * can be locked/unlocked.
 * 
 * @author Mariana Cedica
 */
public class AdministrativeStatusComponent extends DefaultComponent implements
        AdministrativeStatusService {

    private static final Log log = LogFactory.getLog(AdministrativeStatusComponent.class);

    private DocumentModel statusDoc;

    protected EventProducer eventProducer;

    public boolean lockServer(CoreSession session) {
        AdministrativeStatusServerChangeState runner = new AdministrativeStatusServerChangeState(
                session, LOCKED, SEVER_LOCKED_EVENT);
        try {
            runner.runUnrestricted();
            return true;
        } catch (ClientException e) {
            log.error("Unable to lock server", e);
            return false;
        }
    }

    public boolean unlockServer(CoreSession session) {
        AdministrativeStatusServerChangeState runner = new AdministrativeStatusServerChangeState(
                session, UNLOCKED, SERVER_UNLOCKED_EVENT);
        try {
            runner.runUnrestricted();
            return true;
        } catch (ClientException e) {
            log.error("Unable to lock server", e);
            return false;
        }
    }

    public String getServerStatus(CoreSession session) throws ClientException {
        return (String) getOrCreateStatusDocument(session).getPropertyValue(
                ADMINISTRATIVE_STATUS_PROPERTY);
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

    private class AdministrativeStatusServerChangeState extends
            UnrestrictedSessionRunner {

        private DocumentModel statusDoc;

        private String serverState;

        private String eventName;

        public AdministrativeStatusServerChangeState(CoreSession session,
                String state, String eventName) {
            super(session);
            this.serverState = state;
            this.eventName = eventName;

        }

        @Override
        public void run() throws ClientException {
            statusDoc = getOrCreateStatusDocument(session);
            String actualServerState = (String) statusDoc.getPropertyValue(ADMINISTRATIVE_STATUS_PROPERTY);
            if (actualServerState != null
                    && actualServerState.equals(serverState)) {
                return;
            }

            statusDoc.setPropertyValue(ADMINISTRATIVE_STATUS_PROPERTY,
                    serverState);
            session.saveDocument(statusDoc);
            // and notify docStatus created
            Map<String, Serializable> eventProperties = new HashMap<String, Serializable>();
            eventProperties.put("category", ADMINISTRATIVE_EVENT_CATEGORY);
            notifyEvent(session, eventName, statusDoc, eventProperties);
        }

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
                            + getAdministrativeStatusDocName());
            if (!session.exists(statusDocRef)) {
                DocumentModel statusModel = session.createDocumentModel(
                        administrativeContainer.getPathAsString(),
                        getAdministrativeStatusDocName(),
                        ADMINISTRATIVE_STATUS_DOCUMENT_TYPE);

                // set status unlocked by default
                statusModel.setPropertyValue(ADMINISTRATIVE_STATUS_PROPERTY,
                        UNLOCKED);
                statusDoc = session.createDocument(statusModel);
                statusDoc = session.saveDocument(statusModel);
                session.save();

                // and notify docStatus created
                Map<String, Serializable> eventProperties = new HashMap<String, Serializable>();
                eventProperties.put("category", ADMINISTRATIVE_EVENT_CATEGORY);
                notifyEvent(session, ADMINISTRATIVE_STATUS_DOC_CREATED_EVENT,
                        statusDoc, eventProperties);

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

    private String getAdministrativeStatusDocName() throws ClientException {
        return ADMINISTRATIVE_STATUS_DOCUMENT + "-" + getServerInstanceName();
    }

    public String getServerInstanceName() throws ClientException {
        String instanceName = Framework.getProperties().getProperty(
                ADMINISTRATIVE_INSTANCE_ID);
        if (StringUtils.isEmpty(instanceName)) {
            InetAddress addr;
            try {
                addr = InetAddress.getLocalHost();
                instanceName = addr.getHostName();
            } catch (UnknownHostException e) {
                instanceName = "localhost";
            }
        }
        return instanceName;
    }

    public void notifyEvent(CoreSession session, String name,
            DocumentModel document, Map<String, Serializable> eventProperties) {
        DocumentEventContext envContext = new DocumentEventContext(session,
                session.getPrincipal(), document);
        envContext.setProperties(eventProperties);
        try {
            getEventProducer().fireEvent(envContext.newEvent(name));
        } catch (Exception e) {
            log.error("Unable to fire event", e);
        }
    }

    protected EventProducer getEventProducer() throws Exception {
        if (eventProducer == null) {
            eventProducer = Framework.getService(EventProducer.class);
        }
        return eventProducer;
    }

}
