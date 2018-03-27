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
}
