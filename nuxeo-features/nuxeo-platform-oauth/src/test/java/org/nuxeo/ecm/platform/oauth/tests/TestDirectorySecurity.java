/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.oauth.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.nuxeo.runtime.api.login.LoginComponent.SYSTEM_USERNAME;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Test that unrestricted access to the underlying directories is not possible.
 *
 * @since 8.4
 */
@RunWith(FeaturesRunner.class)
@Features(OAuthFeature.class)
public class TestDirectorySecurity {

    private LoginContext loginContext;

    @Inject
    public DirectoryService directoryService;

    // id of entries created during setup
    protected Map<String, Serializable> entryIds = new HashMap<>();

    protected void login(String username) throws LoginException {
        loginContext = Framework.login(username, username);
    }

    protected void logout() throws LoginException {
        loginContext.logout();
    }

    @Before
    public void setUp() throws Exception {
        setUp("oauthConsumers", "oauthConsumer", "consumerKey",
                Collections.singletonMap("consumerKey", (Object) "123"));
        setUp("oauthServiceProviders", "oauthServiceProvider", "id",
                Collections.singletonMap("consumerKey", (Object) "123"));
        setUp("oauthTokens", "oauthToken", "token", Collections.singletonMap("token", (Object) "123"));
        setUp("oauth2ServiceProviders", "oauth2ServiceProvider", "id",
                Collections.singletonMap("serviceName", (Object) "foo"));
        setUp("oauth2Tokens", "oauth2Token", "id", Collections.singletonMap("clientId", (Object) "123"));
        setUp("oauth2Clients", "oauth2Client", "clientId", Collections.singletonMap("clientId", (Object) "123"));
    }

    @Test
    public void testDirectoryRead() throws Exception {
        testDirectoryRead("oauthConsumers");
        testDirectoryRead("oauthServiceProviders");
        testDirectoryRead("oauthTokens");
        testDirectoryRead("oauth2ServiceProviders");
        testDirectoryRead("oauth2Tokens");
        testDirectoryRead("oauth2Clients");
    }

    @Test
    public void testDirectoryQuery() throws Exception {
        testDirectoryQuery("oauthConsumers", "consumerKey");
        testDirectoryQuery("oauthServiceProviders", "id");
        testDirectoryQuery("oauthTokens", "token");
        testDirectoryQuery("oauth2ServiceProviders", "id");
        testDirectoryQuery("oauth2Tokens", "id");
        testDirectoryQuery("oauth2Clients", "clientId");
    }

    protected void setUp(String directoryName, String schemaName, String idField, Map<String, Object> entryMap)
            throws Exception {
        // as system, create an dummy entry
        login(SYSTEM_USERNAME);
        Session session = directoryService.open(directoryName);
        try {
            DocumentModel entry = session.createEntry(new HashMap<>(entryMap));
            Serializable entryId = entry.getPropertyValue(schemaName + ":" + idField);
            entryIds.put(directoryName, entryId);
        } finally {
            session.close();
            logout();
        }
    }

    protected void testDirectoryRead(String directoryName) throws Exception {
        Serializable entryId = entryIds.get(directoryName);

        // as system, we see the entry
        login(SYSTEM_USERNAME);
        Session session = directoryService.open(directoryName);
        try {
            DocumentModel entry = session.getEntry(entryId.toString());
            assertNotNull(entry); // visible entry
        } finally {
            session.close();
            logout();
        }

        // as a random user, we don't see the entry
        login("aRandomUser");
        session = directoryService.open(directoryName);
        try {
            DocumentModel entry = session.getEntry(entryId.toString());
            assertNull(entry); // hidden entry
        } finally {
            session.close();
            logout();
        }
    }

    protected void testDirectoryQuery(String directoryName, String idField) throws Exception {
        Serializable entryId = entryIds.get(directoryName);

        // as system, we see the entry
        login(SYSTEM_USERNAME);
        Session session = directoryService.open(directoryName);
        try {
            DocumentModelList results = session.query(Collections.singletonMap(idField, entryId));
            assertEquals(1, results.size()); // visible entry
        } finally {
            session.close();
            logout();
        }

        // as a random user, we don't see the entry
        login("aRandomUser");
        session = directoryService.open(directoryName);
        try {
            DocumentModelList results = session.query(Collections.singletonMap(idField, entryId));
            assertEquals(0, results.size()); // hidden entry
        } finally {
            session.close();
            logout();
        }
    }

}
