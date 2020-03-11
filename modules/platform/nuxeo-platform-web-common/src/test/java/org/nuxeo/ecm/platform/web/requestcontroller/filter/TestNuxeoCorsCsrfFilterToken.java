/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.web.requestcontroller.filter;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * CSRF Token tests.
 *
 * @since 10.3
 */
@Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-csrf-token-config.xml")
public class TestNuxeoCorsCsrfFilterToken extends TestNuxeoCorsCsrfFilter {

    protected static final String CSRF_TOKEN_ATTRIBUTE = "NuxeoCSRFToken";

    protected static final String CSRF_TOKEN_HEADER = "CSRF-Token";

    protected static final String CSRF_TOKEN_FETCH = "fetch";

    protected static final String CSRF_TOKEN_INVALID = "invalid";

    protected static final String CSRF_TOKEN_PARAM = "csrf-token";

    @Override
    protected void maybeSetupToken() {
        Map<String, Object> sessionAttributes = mockSessionAttributes();
        String token = "realtoken";

        // put token in session
        sessionAttributes.put(CSRF_TOKEN_ATTRIBUTE, token);
        // pass token in request
        when(request.getHeader(eq(CSRF_TOKEN_HEADER))).thenReturn(token);
    }


    /**
     * Browser sending a header "CSRF-Token: fetch".
     */
    @Test
    public void testCSRFTokenAcquire() throws Exception {
        mockRequestURI(request, "GET", "");
        when(request.getHeader(eq(CSRF_TOKEN_HEADER))).thenReturn(CSRF_TOKEN_FETCH);
        Map<String, Object> sessionAttributes = mockSessionAttributes();

        filter.doFilter(request, response, chain);
        // chain not called
        assertFalse(chain.called);
        // but a token was created in session
        String token = (String) sessionAttributes.get(CSRF_TOKEN_ATTRIBUTE);
        assertNotNull(token);
        // and we have a response
        verify(response).setStatus(eq(SC_OK));
        verify(response).setHeader(CSRF_TOKEN_HEADER, token);
    }

    /**
     * Browser sending no token.
     */
    @Test
    public void testCSRFTokenMissing() throws Exception {
        doTestCSRFTokenInvalid(null, null);
    }

    /**
     * Browser sending no token when a real one exists in the session.
     */
    @Test
    public void testCSRFTokenMissingButExistsInSession() throws Exception {
        doTestCSRFTokenInvalid("realtoken", null);
    }

    /**
     * Browser sending an invalid token when there is none in the session.
     */
    @Test
    public void testCSRFTokenInvalid() throws Exception {
        doTestCSRFTokenInvalid(null, "badtoken");
    }

    /**
     * Browser sending an invalid token when a real one exists in the session.
     */
    @Test
    public void testCSRFTokenInvalidButExistsInSession() throws Exception {
        doTestCSRFTokenInvalid("realtoken", "badtoken");
    }

    @SuppressWarnings("boxing")
    protected void doTestCSRFTokenInvalid(String token, String requestToken) throws Exception {
        mockRequestURI(request, "POST", "/site/something");
        when(request.getHeader(eq(CSRF_TOKEN_HEADER))).thenReturn(requestToken);
        Map<String, Object> sessionAttributes = mockSessionAttributes();
        MutableObject<InvocationOnMock> error = new MutableObject<>();
        doAnswer(invocation -> {
            error.setValue(invocation);
            return null;
        }).when(response).sendError(anyInt(), anyString());
        sessionAttributes.put(CSRF_TOKEN_ATTRIBUTE, token);

        filter.doFilter(request, response, chain);
        // chain not called
        assertFalse(chain.called);
        // no new token was created in session
        if (token == null) {
            assertNull(sessionAttributes.get(CSRF_TOKEN_ATTRIBUTE));
        }
        // and we have an error status
        assertNotNull(error.getValue());
        Object[] arguments = error.getValue().getArguments();
        assertEquals(SC_FORBIDDEN, arguments[0]); // 403
        assertEquals("CSRF check failure", arguments[1]);
        // and a header saying this is due to invalid CSRF token
        verify(response).setHeader(CSRF_TOKEN_HEADER, CSRF_TOKEN_INVALID);
    }

    /**
     * Some endpoints can be configured to allow POST without a CSRF token. This is needed for SAML.
     */
    @Test
    public void testCSRFTokenMissingOnAllowedEndpoint() throws Exception {
        mockRequestURI(request, "POST", "/mysaml/mylogin");
        Map<String, Object> sessionAttributes = mockSessionAttributes();

        filter.doFilter(request, response, chain);
        // chain called
        assertTrue(chain.called);
        // no new token was created in session
        assertNull(sessionAttributes.get(CSRF_TOKEN_ATTRIBUTE));
    }

}
