/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.tokenauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import javax.inject.Inject;

import org.apache.commons.httpclient.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.client.RemoteException;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.core.operations.document.FetchDocument;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.ui.web.auth.token.TokenAuthenticator;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Tests the {@link TokenAuthenticator}.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features({ TokenAuthenticationServiceFeature.class, EmbeddedAutomationServerFeature.class })
@Jetty(port = 18080)
@RepositoryConfig(init = TokenAuthenticationRepositoryInit.class, cleanup = Granularity.METHOD)
public class TestTokenAuthenticator {

    @Inject
    protected CoreSession session;

    @Inject
    protected HttpAutomationClient automationClient;

    @Test
    public void testAuthenticator() throws Exception {

        // Try to get client session using a bad token, should throw a RemoteException with HTTP 401 status code
        try {
            automationClient.getSession("badToken");
            fail("Getting an Automation client session with a bad token should throw a RemoteException with HTTP 401 status code");
        } catch (RemoteException e) {
            assertEquals(HttpStatus.SC_UNAUTHORIZED, e.getStatus());
        }

        // Mock token authentication callback
        TokenAuthenticationCallback cb = new TokenAuthenticationCallback("Administrator", "myFavoriteApp",
                "Ubuntu box 64 bits", "This is my personal Linux box", "rw");
        assertNull(cb.getLocalToken());

        // Get client session using callback, should acquire a remote token,
        // store it locally and return a session as Administrator
        Session clientSession = automationClient.getSession(cb);
        String token = cb.getLocalToken();
        assertNotNull(token);
        assertEquals("Administrator", clientSession.getLogin().getUsername());

        // Check automation call
        String testDocId = session.getDocument(new PathRef(TokenAuthenticationRepositoryInit.getTestDocPath())).getId();
        Document testDoc = (Document) clientSession.newRequest(FetchDocument.ID).setHeader("X-NXDocumentProperties",
                "dublincore").set("value", testDocId).execute();
        assertNotNull(testDoc);
        assertEquals("My test doc", testDoc.getTitle());
        assertEquals("For test purpose.", testDoc.getString("dc:description"));

        // Get client session using callback, should use local token and return
        // a session as Administrator
        clientSession = automationClient.getSession(cb);
        assertEquals("Administrator", clientSession.getLogin().getUsername());

        // Revoke token
        TokenAuthenticationService tokenAuthenticationService = Framework.getLocalService(
                TokenAuthenticationService.class);
        tokenAuthenticationService.revokeToken(token);
        // commit transaction
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        // Assert that an operation will fail to error 401
        try {
            testDoc = (Document) clientSession.newRequest(FetchDocument.ID)
                    .setHeader("X-NXDocumentProperties", "dublincore")
                    .set("value", testDocId)
                    .execute();
            fail("Performing operation with a revoked token should throw a RemoteException with HTTP 401 status code");
        } catch (RemoteException e) {
            assertEquals(HttpStatus.SC_UNAUTHORIZED, e.getStatus());
        }
    }

}
