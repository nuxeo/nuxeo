/*
t * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.jwt;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.api.local.LoginStack;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.jwt")
@Deploy("org.nuxeo.ecm.jwt.tests:OSGI-INF/test-jwt-config.xml")
public class TestJWTAuthenticator {

    protected static final String USERNAME = "bob";

    protected static final String BEARER_SP = "Bearer ";

    protected static final String ACCESS_TOKEN = "access_token";

    @Inject
    protected JWTService service;

    @Before
    public void setUp() throws Exception {
        LoginStack loginStack = ClientLoginModule.getThreadLocalLogin();
        loginStack.push(new UserPrincipal(USERNAME, Collections.emptyList(), false, false), null, null);
    }

    @After
    public void teardown() throws Exception {
        LoginStack loginStack = ClientLoginModule.getThreadLocalLogin();
        loginStack.pop();
    }

    @Test
    public void testValidateTicket() throws Exception {
        JWTAuthenticator auth = new JWTAuthenticator();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        // test with a valid ticket
        String ticket = service.newBuilder().build();
        when(request.getHeader(eq(AUTHORIZATION))).thenReturn(BEARER_SP + ticket);

        UserIdentificationInfo uii = auth.handleRetrieveIdentity(request, response);

        // auth plugin succeeds
        assertNotNull(uii);
        assertEquals(USERNAME, uii.getUserName());
    }

    @Test
    public void testValidateTicketFromURI() throws Exception {
        JWTAuthenticator auth = new JWTAuthenticator();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        // test with a valid ticket in the query params
        String ticket = service.newBuilder().build();
        when(request.getParameter(eq(ACCESS_TOKEN))).thenReturn(ticket);

        UserIdentificationInfo uii = auth.handleRetrieveIdentity(request, response);

        // auth plugin succeeds
        assertNotNull(uii);
        assertEquals(USERNAME, uii.getUserName());
    }

    @Test
    public void testValidateTicketWithAudience() throws Exception {
        JWTAuthenticator auth = new JWTAuthenticator();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        // test with a valid ticket, with a specific audience
        String ticket = service.newBuilder().withClaim(JWTClaims.CLAIM_AUDIENCE, "nxfile").build();
        when(request.getHeader(eq(AUTHORIZATION))).thenReturn(BEARER_SP + ticket);
        when(request.getServletPath()).thenReturn("/nxfile");
        when(request.getPathInfo()).thenReturn("/default/1234456");

        UserIdentificationInfo uii = auth.handleRetrieveIdentity(request, response);

        // auth plugin succeeds
        assertNotNull(uii);
        assertEquals(USERNAME, uii.getUserName());
    }

    @Test
    public void testValidateTicketWithBadAudience() throws Exception {
        JWTAuthenticator auth = new JWTAuthenticator();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        // test with a valid ticket, with a specific audience
        String ticket = service.newBuilder().withClaim(JWTClaims.CLAIM_AUDIENCE, "nxfile").build();
        when(request.getHeader(eq(AUTHORIZATION))).thenReturn(BEARER_SP + ticket);
        when(request.getServletPath()).thenReturn("/api/v1");
        when(request.getPathInfo()).thenReturn("/foo/bar");

        UserIdentificationInfo uii = auth.handleRetrieveIdentity(request, response);

        // auth plugin fails
        assertNull(uii);
    }

    @Test
    public void testNoAuthorizationHeader() throws Exception {
        JWTAuthenticator auth = new JWTAuthenticator();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        // test without Authorization header
        when(request.getHeader(eq(AUTHORIZATION))).thenReturn(null);

        UserIdentificationInfo uii = auth.handleRetrieveIdentity(request, response);

        // auth plugin fails
        assertNull(uii);
    }

    @Test
    public void testCorruptedTicket() throws Exception {
        JWTAuthenticator auth = new JWTAuthenticator();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        // test with invalid Bearer token
        when(request.getHeader(eq(AUTHORIZATION))).thenReturn(BEARER_SP + "foobar");

        UserIdentificationInfo uii = auth.handleRetrieveIdentity(request, response);

        // auth plugin fails
        assertNull(uii);
    }

}
