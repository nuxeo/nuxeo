/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Arnaud Kervern
 */
package org.nuxeo.ecm.platform.oauth2.clients;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * OAuth2 Client registry component
 *
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
public class ClientRegistryImpl extends DefaultComponent implements ClientRegistry {

    private static final Log log = LogFactory.getLog(ClientRegistry.class);

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        switch (extensionPoint) {
        case "clients":
            OAuth2Client client = (OAuth2Client) contribution;
            registerClient(client);
            break;
        default:
            break;
        }
    }

    @Override
    public boolean hasClient(String clientId) {
        DirectoryService service = getService();
        try (Session session = service.open(OAUTH2CLIENT_DIRECTORY_NAME)) {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("clientId", clientId);
            DocumentModelList docs = session.query(filter);
            if (docs.size() == 0) {
                return false;
            }

            DocumentModel entry = docs.get(0);
            return OAuth2Client.fromDocumentModel(entry).isEnabled();
        }
    }

    @Override
    public boolean isValidClient(String clientId, String clientSecret) {
        DocumentModel docClient = getClientModel(clientId);
        if (docClient != null) {
            OAuth2Client client = OAuth2Client.fromDocumentModel(docClient);
            return client.isValidWith(clientId, clientSecret);
        }
        return false;
    }

    @Override
    public boolean registerClient(OAuth2Client client) {
        DocumentModel doc = getClientModel(client.getId());
        if (doc != null) {
            log.info("Trying to register an exisiting client");
            return false;
        }

        DirectoryService service = getService();
        try (Session session = service.open(OAUTH2CLIENT_DIRECTORY_NAME)) {
            if (session.hasEntry(client.getId())) {
                log.debug(String.format("ClientId is already registered: %s", client.getId()));
                return false;
            }
            session.createEntry(client.toMap());
        }
        return true;
    }

    @Override
    public boolean deleteClient(String clientId) {
        DirectoryService service = getService();
        try (Session session = service.open(OAUTH2CLIENT_DIRECTORY_NAME)) {
            session.deleteEntry(clientId);
            return true;
        } catch (DirectoryException e) {
            return false;
        }
    }

    @Override
    public List<DocumentModel> listClients() {
        DirectoryService service = getService();
        try (Session session = service.open(OAUTH2CLIENT_DIRECTORY_NAME)) {
            return session.getEntries();
        }
    }

    public OAuth2Client getClient(String clientId) {
        DocumentModel doc = getClientModel(clientId);
        return doc != null ? OAuth2Client.fromDocumentModel(doc) : null;
    }

    protected DocumentModel getClientModel(String clientId) {
        DirectoryService service = getService();
        try (Session session = service.open(OAUTH2CLIENT_DIRECTORY_NAME)) {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("clientId", clientId);
            DocumentModelList docs = session.query(filter);
            if (docs.size() > 0) {
                return docs.get(0);
            }
        }
        return null;
    }

    protected DirectoryService getService() {
        return Framework.getLocalService(DirectoryService.class);
    }
}
