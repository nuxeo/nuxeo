/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.platform.ui.web.auth.plugins;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.eq;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

/**
 * @since 5.9.2
 */
public class BasicAuthenticatorTest {

    static final ImmutableMap<String, String> BA_INIT_NOTOKEN = //
    new ImmutableMap.Builder<String, String>() //
    .put("ExcludeBAHeader_Token", "X-Authorization-token") //
    .put("ExcludeBAHeader_Other", "X-NoBAPrompt")//
    .build();

    private BasicAuthenticator ba;

    @Before
    public void doBefore() {
        ba = new BasicAuthenticator();
        ba.initPlugin(BA_INIT_NOTOKEN);
    }

    @Test
    public void itDoesntSentBAHeaderWhenExcludeHeaderIsPresent() throws Exception {

        HttpServletRequest req = getRequestWithHeader("X-Authorization-token", "bla");

        HttpServletResponse resp = mock(HttpServletResponse.class);
        ba.handleLoginPrompt(req, resp, "/");

        verify(resp, never()).addHeader(eq(BasicAuthenticator.BA_HEADER_NAME), anyString());
    }

    @Test
    public void itDoesntSendBaHeaderWhenExcludedCookieIsPresnt() throws Exception {
        HttpServletRequest req = getRequestWithCookie("X-Authorization-token", "bla");

        HttpServletResponse resp = mock(HttpServletResponse.class);
        ba.handleLoginPrompt(req, resp, "/");

        verify(resp, never()).addHeader(eq(BasicAuthenticator.BA_HEADER_NAME), anyString());
    }

    @Test
    public void itSendsABAHeaderWhenNoExcludeHeaderIsSet() throws Exception {

        HttpServletRequest req = mock(HttpServletRequest.class);

        HttpServletResponse resp = mock(HttpServletResponse.class);
        ba.handleLoginPrompt(req, resp, "/");

        verify(resp).addHeader(eq(BasicAuthenticator.BA_HEADER_NAME), anyString());

    }

    private HttpServletRequest getRequestWithCookie(String cookieName, String value) {
        return getMockRequest(cookieName, value, false, true);
    }

    private HttpServletRequest getRequestWithHeader(String headerName, String value) {
        return getMockRequest(headerName, value, true, false);
    }

    /**
     * Mocks a request with a mocked header or cookie
     *
     * @param name
     * @param value
     * @param header adds a header if true
     * @param cookie adds a cookie if true
     * @return
     */
    private HttpServletRequest getMockRequest(String name, String value, boolean header, boolean cookie) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        if (cookie && value != null) {
            when(req.getCookies()).thenReturn(new Cookie[] { new Cookie(name, value) });
        }
        if (header && value != null) {
            when(req.getHeader(name)).thenReturn(value);
        }
        return req;
    }
}
