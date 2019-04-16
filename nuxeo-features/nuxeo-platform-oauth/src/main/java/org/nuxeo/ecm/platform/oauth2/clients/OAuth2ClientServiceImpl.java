/*
 * (C) Copyright 2014-2017 Nuxeo (http://nuxeo.com/) and others.
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * OAuth2 Client service
 *
 * @since 9.2
 */
public class OAuth2ClientServiceImpl extends DefaultComponent implements OAuth2ClientService {

    @Override
    public boolean hasClient(String clientId) {
        OAuth2Client client = getClient(clientId);
        return client != null && client.isEnabled();
    }

    @Override
    public boolean isValidClient(String clientId, String clientSecret) {
        OAuth2Client client = getClient(clientId);
        return client != null && client.isValidWith(clientId, clientSecret);
    }

    @Override
    public OAuth2Client getClient(String clientId) {
        DocumentModel doc = getClientModel(clientId);
        if (doc == null) {
            return null;
        }
        return OAuth2Client.fromDocumentModel(doc);
    }

    @Override
    public List<OAuth2Client> getClients() {
        return queryClients().stream().map(OAuth2Client::fromDocumentModel).collect(Collectors.toList());
    }

    protected DocumentModel getClientModel(String clientId) {
        DirectoryService service = Framework.getService(DirectoryService.class);
        return Framework.doPrivileged(() -> {
            try (Session session = service.open(OAUTH2CLIENT_DIRECTORY_NAME)) {
                Map<String, Serializable> filter = Collections.singletonMap("clientId", clientId);
                DocumentModelList docs = session.query(filter);
                if (docs.size() == 1) {
                    return docs.get(0);
                } else if (docs.size() > 1) {
                    throw new NuxeoException(
                            String.format("More than one client registered for the '%s' id", clientId));
                }
            }
            return null;
        });
    }

    protected List<DocumentModel> queryClients() {
        DirectoryService service = Framework.getService(DirectoryService.class);
        return Framework.doPrivileged(() -> {
            try (Session session = service.open(OAUTH2CLIENT_DIRECTORY_NAME)) {
                return session.query(Collections.emptyMap());
            } catch (DirectoryException e) {
                throw new NuxeoException("Error while fetching client directory", e);
            }
        });
    }
}
