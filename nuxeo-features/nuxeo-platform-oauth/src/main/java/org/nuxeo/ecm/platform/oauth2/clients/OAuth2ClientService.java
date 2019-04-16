/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 *     Arnaud Kervern
 *
 */
package org.nuxeo.ecm.platform.oauth2.clients;

import java.util.List;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * @since 9.2
 */
public interface OAuth2ClientService {

    String OAUTH2CLIENT_DIRECTORY_NAME = "oauth2Clients";

    String OAUTH2CLIENT_SCHEMA = "oauth2Client";

    boolean hasClient(String clientId);

    boolean isValidClient(String clientId, String clientSecret);

    OAuth2Client getClient(String clientId);

    /**
     * @since 10.2
     */
    List<OAuth2Client> getClients();

    /**
     * Registers a new oAuth2 client as the given principal.
     *
     * @param oAuth2Client the {@link OAuth2Client} to register
     * @param principal the current user
     * @return the newly registered client
     * @throws NuxeoException with 403 status code if the given principal doesn't have access to the oAuth2 clients
     * @since 11.1
     */
    OAuth2Client create(OAuth2Client oAuth2Client, NuxeoPrincipal principal);

    /**
     * Updates an exiting oAuth2 client as the given principal.
     *
     * @param clientId the client id of oAuth2Client to update
     * @param oAuth2Client the new {@link OAuth2Client} data
     * @param principal the current user
     * @return the updated oAuth2Client
     * @throws NuxeoException with 403 status code if the given principal doesn't have access to the oAuth2 clients
     * @since 11.1
     */
    OAuth2Client update(String clientId, OAuth2Client oAuth2Client, NuxeoPrincipal principal);

    /**
     * Deletes an oAuth2 client as the given principal.
     *
     * @param clientId the client id of the oAuth2Client to delete
     * @param principal the current user
     * @throws NuxeoException with 403 status code if the given principal doesn't have access to the oAuth2 clients
     * @since 11.1
     */
    void delete(String clientId, NuxeoPrincipal principal);
}
