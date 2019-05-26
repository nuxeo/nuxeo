/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.user.center;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.nuxeo.ecm.platform.oauth2.Constants.TOKEN_SERVICE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.oauth.tokens.OAuthTokenStoreImpl;
import org.nuxeo.ecm.platform.oauth2.clients.OAuth2Client;
import org.nuxeo.ecm.platform.oauth2.clients.OAuth2ClientService;
import org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token;
import org.nuxeo.ecm.platform.oauth2.tokens.OAuth2TokenStore;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@Name("authorizedApplicationsActions")
@Scope(CONVERSATION)
public class AuthorizedApplicationsActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @In
    protected NuxeoPrincipal currentUser;

    public List<Map<String, Serializable>> getOAuth2AuthorizedApplications() {
        List<Map<String, Serializable>> applications = new ArrayList<>();
        OAuth2ClientService clientService = Framework.getService(OAuth2ClientService.class);
        OAuth2TokenStore tokenStore = new OAuth2TokenStore(TOKEN_SERVICE);
        // Get OAuth2 tokens for the current user
        DocumentModelList tokens = tokenStore.query(getOAuth2QueryFilter());
        // Join them with the related OAuth2 client
        for (DocumentModel token : tokens) {
            OAuth2Client client = clientService.getClient(
                    (String) token.getPropertyValue(NuxeoOAuth2Token.SCHEMA + ":clientId"));
            if (client != null) {
                Map<String, Serializable> application = new HashMap<>();
                application.put("id", token.getPropertyValue(NuxeoOAuth2Token.SCHEMA + ":id"));
                application.put("applicationId", client.getId());
                application.put("applicationName", client.getName());
                Calendar creationDate = (Calendar) token.getPropertyValue(NuxeoOAuth2Token.SCHEMA + ":creationDate");
                if (creationDate != null) {
                    application.put("applicationAuthorizationDate", creationDate.getTime());
                }
                applications.add(application);
            }
        }
        return applications;
    }

    public DocumentModelList getOAuthAuthorizedApplications() {
        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        return Framework.doPrivileged(() -> {
            try (Session session = directoryService.open(OAuthTokenStoreImpl.DIRECTORY_NAME)) {
                Map<String, Serializable> queryFilter = getOAuthQueryFilter();
                return session.query(queryFilter);
            }
        });
    }

    protected Map<String, Serializable> getOAuth2QueryFilter() {
        Map<String, Serializable> filter = new HashMap<>();
        filter.put("nuxeoLogin", currentUser.getName());
        return filter;
    }

    protected Map<String, Serializable> getOAuthQueryFilter() {
        Map<String, Serializable> filter = new HashMap<>();
        filter.put("clientToken", 0);
        filter.put("nuxeoLogin", currentUser.getName());
        return filter;
    }

    public void revokeAccess(String directoryName, String id) {
        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        Framework.doPrivileged(() -> {
            try (Session session = directoryService.open(directoryName)) {
                session.deleteEntry(id);
            }
        });
    }

}
