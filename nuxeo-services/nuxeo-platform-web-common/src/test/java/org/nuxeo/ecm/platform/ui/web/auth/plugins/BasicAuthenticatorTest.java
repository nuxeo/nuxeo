/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 *
 *
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
    public void itDoesntSentBAHeaderWhenExcludeHeaderIsPresent()
            throws Exception {

        HttpServletRequest req = getRequestWithHeader("X-Authorization-token",
                "bla");

        HttpServletResponse resp = mock(HttpServletResponse.class);
        ba.handleLoginPrompt(req, resp, "/");

        verify(resp, never()).addHeader(eq(BasicAuthenticator.BA_HEADER_NAME),
                anyString());
    }

    @Test
    public void itDoesntSendBaHeaderWhenExcludedCookieIsPresnt() throws Exception {
        HttpServletRequest req = getRequestWithCookie("X-Authorization-token",
                "bla");

        HttpServletResponse resp = mock(HttpServletResponse.class);
        ba.handleLoginPrompt(req, resp, "/");

        verify(resp, never()).addHeader(eq(BasicAuthenticator.BA_HEADER_NAME),
                anyString());
    }


    @Test
    public void itSendsABAHeaderWhenNoExcludeHeaderIsSet() throws Exception {

        HttpServletRequest req = mock(HttpServletRequest.class);

        HttpServletResponse resp = mock(HttpServletResponse.class);
        ba.handleLoginPrompt(req, resp, "/");

        verify(resp).addHeader(eq(BasicAuthenticator.BA_HEADER_NAME),
                anyString());

    }


    private HttpServletRequest getRequestWithCookie(String cookieName,
            String value) {
        return getMockRequest(cookieName, value, false, true);
    }


    private HttpServletRequest getRequestWithHeader(String headerName,
            String value) {
        return getMockRequest(headerName, value, true, false);
    }

    /**
     * Mocks a request with a mocked header or cookie
     * @param name
     * @param value
     * @param header adds a header if true
     * @param cookie adds a cookie if true
     * @return
     *
     */
    private HttpServletRequest getMockRequest(String name, String value,
            boolean header, boolean cookie) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        if (cookie && value != null) {
            when(req.getCookies()).thenReturn(
                    new Cookie[] { new Cookie(name, value) });
        }
        if (header && value != null) {
            when(req.getHeader(name)).thenReturn(value);
        }
        return req;
    }
}
