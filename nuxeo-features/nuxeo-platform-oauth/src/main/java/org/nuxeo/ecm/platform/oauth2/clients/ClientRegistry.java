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

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
public interface ClientRegistry {

    public static final String OAUTH2CLIENT_DIRECTORY_NAME = "oauth2Clients";

    public static final String OAUTH2CLIENT_SCHEMA = "oauth2Client";

    boolean hasClient(String clientId);

    boolean isValidClient(String clientId, String clientSecret);

    boolean registerClient(OAuth2Client client);

    boolean deleteClient(String clientId);

    List<DocumentModel> listClients();

    OAuth2Client getClient(String clientId);
}
