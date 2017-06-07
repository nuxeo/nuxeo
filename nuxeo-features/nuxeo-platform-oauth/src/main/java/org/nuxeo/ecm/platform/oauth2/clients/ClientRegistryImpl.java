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
import java.util.Collections;
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
        OAuth2Client client = getClient(clientId);
        if (client == null) {
            return false;
        }
        return client.isEnabled();
    }

    @Override
    public boolean isValidClient(String clientId, String clientSecret) {
        OAuth2Client client = getClient(clientId);
        if (client == null) {
            return false;
        }
        return client.isValidWith(clientId, clientSecret);
    }

    @Override
    public boolean registerClient(OAuth2Client client) {
        if (!client.isValid()) {
            return false;
        }
        String clientID = client.getId();
        DocumentModel doc = getClientModel(clientID);
        if (doc != null) {
            log.error(String.format("An OAuth2 client with clientId=%s is already registered", clientID));
            return false;
        }

        DirectoryService service = getService();
        try (Session session = service.open(OAUTH2CLIENT_DIRECTORY_NAME)) {
            if (session.hasEntry(clientID)) {
                log.error(String.format("An OAuth2 client with clientId=%s is already registered", clientID));
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
            return session.query(Collections.emptyMap());
        }
    }

    @Override
    public OAuth2Client getClient(String clientId) {
        DocumentModel doc = getClientModel(clientId);
        if (doc == null) {
            return null;
        }
        return OAuth2Client.fromDocumentModel(doc);
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
        return Framework.getService(DirectoryService.class);
    }
}
