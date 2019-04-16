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

import static java.util.Objects.requireNonNull;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
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

    @Override
    public OAuth2Client create(OAuth2Client oAuth2Client, NuxeoPrincipal principal) {
        validate(oAuth2Client);
        checkUnicity(oAuth2Client.getId());

        return execute(session -> {
            DocumentModel documentModel = OAuth2Client.fromOAuth2Client(oAuth2Client);
            return OAuth2Client.fromDocumentModel(session.createEntry(documentModel));
        }, principal);
    }

    @Override
    public OAuth2Client update(String clientId, OAuth2Client oAuth2Client, NuxeoPrincipal principal) {
        validate(oAuth2Client);
        if (!oAuth2Client.getId().equals(clientId)) {
            checkUnicity(oAuth2Client.getId());
        }

        DocumentModel doc = getDocument(clientId);
        return execute(session -> {
            DocumentModel documentModel = OAuth2Client.updateDocument(doc, oAuth2Client);
            session.updateEntry(documentModel);
            return OAuth2Client.fromDocumentModel(documentModel);
        }, principal);
    }

    @Override
    public void delete(String clientId, NuxeoPrincipal principal) {
        DocumentModel document = getDocument(clientId);
        execute(session -> {
            session.deleteEntry(document);
            return null;
        }, principal);
    }

    protected DocumentModel getClientModel(String clientId) {
        return execute(session -> {
            Map<String, Serializable> filter = Collections.singletonMap("clientId", clientId);
            DocumentModelList docs = session.query(filter);
            if (docs.size() == 1) {
                return docs.get(0);
            } else if (docs.size() > 1) {
                throw new NuxeoException(String.format("More than one client registered for the '%s' id", clientId));
            }
            return null;
        });
    }

    protected List<DocumentModel> queryClients() {
        return execute(session -> session.query(Collections.emptyMap()));
    }

    /**
     * @since 11.1
     */
    protected <T> T execute(Function<Session, T> function) {
        return execute(function, null);
    }

    /**
     * @since 11.1
     */
    protected <T> T execute(Function<Session, T> function, NuxeoPrincipal principal) {
        if (principal != null) {
            checkPermission(principal);
        }
        DirectoryService service = Framework.getService(DirectoryService.class);
        return Framework.doPrivileged(() -> {
            try (Session session = service.open(OAUTH2CLIENT_DIRECTORY_NAME)) {
                return function.apply(session);
            }
        });
    }

    protected void checkPermission(NuxeoPrincipal principal) {
        if (!principal.isAdministrator()) {
            throw new NuxeoException("You do not have permissions to perform this operation.", SC_FORBIDDEN);
        }
    }

    /**
     * Validates the {@link OAuth2Client}. An {@link OAuth2Client} is valid if and only if
     * <ul>
     * <li>It is not {@code null}</li>
     * <li>The required fields are filled in:
     * {@link OAuth2Client#getId()},{@link OAuth2Client#getName()},{@link OAuth2Client#getRedirectURIs()}</li>
     * <li>The {@link OAuth2Client#getRedirectURIs()} is a valid URI,
     * {@link OAuth2Client#isRedirectURIValid(String)}</li>
     * </ul>
     *
     * @param oAuth2Client the {@code not null} oAuth2Client to validate
     * @throws NullPointerException if the oAuth2Client is {@code null}
     * @throws NuxeoException if oAuth2Client is not valid
     * @since 11.1
     */
    protected void validate(OAuth2Client oAuth2Client) {
        requireNonNull(oAuth2Client, "oAuth2Client is required");
        String message;
        if (StringUtils.isBlank(oAuth2Client.getName())) {
            message = "Client name is required";
        } else if (StringUtils.isBlank(oAuth2Client.getId())) {
            message = "Client Id is required";
        } else if (oAuth2Client.getRedirectURIs().isEmpty()) {
            message = "Redirect URIs is required";
        } else {
            message = oAuth2Client.getRedirectURIs()
                                  .stream()
                                  .filter(uri -> !OAuth2Client.isRedirectURIValid(uri))
                                  .findAny()
                                  .map(uri -> String.format("'%s' is not a valid redirect URI", uri))
                                  .orElse(null);
        }

        if (StringUtils.isNotEmpty(message)) {
            throw new NuxeoException(String.format("%s", message), SC_BAD_REQUEST);
        }
    }

    /**
     * Checks if a client with the {@code clientId} is unique.
     *
     * @param clientId the client id to check
     * @throws NuxeoException if an oAuth2 client with the given {@code clientId} already exists
     * @since 11.1
     */
    protected void checkUnicity(String clientId) {
        if (getClientModel(clientId) != null) {
            throw new NuxeoException(String.format("Client with id '%s' already exists", clientId), SC_BAD_REQUEST);
        }
    }

    /**
     * Gets the document model from a given {@code clientId}
     *
     * @param clientId the oAuth client id
     * @throws NuxeoException if there is no document model for the given {@code clientId}
     * @since 11.1
     */
    protected DocumentModel getDocument(String clientId) {
        DocumentModel doc = getClientModel(clientId);
        if (doc == null) {
            throw new NuxeoException(SC_NOT_FOUND);
        }

        return doc;
    }

}
