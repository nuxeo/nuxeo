/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.platform.oauth.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.oauth2.events.GarbageCollectExpiredOAuth2TokensListener.EVENT_NAME;
import static org.nuxeo.ecm.platform.oauth2.tokens.OAuth2TokenServiceImpl.TOKEN_DIR;

import javax.inject.Inject;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token;
import org.nuxeo.ecm.platform.oauth2.tokens.OAuth2TokenService;
import org.nuxeo.ecm.platform.oauth2.tokens.OAuth2TokenStore;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.NuxeoLoginContext;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 2025.1
 */
@RunWith(FeaturesRunner.class)
@Features(OAuthFeature.class)
public class TestOAuth2ExpiredTokensGC {

    protected OAuth2TokenStore tokenStore = new OAuth2TokenStore("test_OAuth2");

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Inject
    protected BulkService bulkService;

    @Inject
    protected DirectoryService directoryService;

    @Inject
    protected OAuth2TokenService oauth2TokenService;

    protected void storeToken(String username, NuxeoOAuth2Token token) {
        tokenStore.store(username, token);
    }

    protected void assertTokensCount(int expected) {
        try (Session dirSession = directoryService.open(TOKEN_DIR)) {
            assertEquals(expected, dirSession.queryIds(new QueryBuilder()).size());
        }
    }

    @Test
    public void testEffectiveGC() {
        storeToken("jdoe", new NuxeoOAuth2Token(1, "client1"));
        storeToken("jsmith", new NuxeoOAuth2Token(3600000, "client1"));
        // Create token as if Nuxeo is acting as a client without storing token expiration time
        storeToken("marc", new NuxeoOAuth2Token("a", "b", null));
        transactionalFeature.nextTransaction();
        assertTokensCount(3);
        var commandId = oauth2TokenService.garbageCollectExpiredTokens();
        transactionalFeature.nextTransaction();
        var status = bulkService.getStatus(commandId);
        assertTrue(status.isCompleted());
        assertEquals(3, status.getTotal());
        assertEquals(2, status.getSkipCount());
        assertTokensCount(2);
    }

    @Test
    public void testEffectiveGCTriggeredByEvent() {
        storeToken("jdoe", new NuxeoOAuth2Token(1, "client1"));
        storeToken("jsmith", new NuxeoOAuth2Token(2, "client1"));
        transactionalFeature.nextTransaction();
        assertTokensCount(2);
        Event event = new EventContextImpl().newEvent(EVENT_NAME);
        Framework.getService(EventService.class).fireEvent(event);
        transactionalFeature.nextTransaction();
        assertTokensCount(0);
    }

    @Test
    public void testEmptyGC() {
        var commandId = oauth2TokenService.garbageCollectExpiredTokens();
        transactionalFeature.nextTransaction();
        var status = bulkService.getStatus(commandId);
        assertTrue(status.isCompleted());
        assertEquals(0, status.getTotal());
    }

    @Test
    public void testUnauthorizedGC() throws LoginException {
        try (NuxeoLoginContext ignored = Framework.loginUser("notAdmin")) {
            NuxeoException e = assertThrows(NuxeoException.class,
                    () -> oauth2TokenService.garbageCollectExpiredTokens());
            assertEquals(HttpServletResponse.SC_FORBIDDEN, e.getStatusCode());
        }
    }
}
