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
 *     dmetzler
 */
package org.nuxeo.ecm.platform.oauth.tests;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.nuxeo.ecm.platform.ui.web.auth.oauth.NuxeoOAuth1Authenticator;

/**
 * @since 5.9.5
 */
public class TestForwardedProto {

    /**
     *
     */
    private static final String HTTP = "http";

    /**
     *
     */
    private static final String HTTPS = "https";

    /**
     *
     */
    private static final String TEST_SITE_URL = "http://mysite.org/bla";

    @Test
    public void itCanReplaceRequestSchemeByTheOneInXFPHeader() throws Exception {
        HttpServletRequest req = mockRequestWithForwardedHeader(TEST_SITE_URL, HTTPS);
        assertEquals("https://mysite.org/bla", NuxeoOAuth1Authenticator.getRequestURL(req));

        req = mockRequestWithForwardedHeader("https://mysite.org/bla", HTTPS);
        assertEquals("https://mysite.org/bla", NuxeoOAuth1Authenticator.getRequestURL(req));

        req = mockRequestWithForwardedHeader(TEST_SITE_URL, null);
        assertEquals(TEST_SITE_URL, NuxeoOAuth1Authenticator.getRequestURL(req));

        req = mockRequestWithForwardedHeader(TEST_SITE_URL, HTTP);
        assertEquals(TEST_SITE_URL, NuxeoOAuth1Authenticator.getRequestURL(req));

    }

    private HttpServletRequest mockRequestWithForwardedHeader(String url, String xfp) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURL()).thenReturn(new StringBuffer(url));
        if (xfp != null) {
            when(req.getHeader("X-Forwarded-Proto")).thenReturn(xfp);
        } else {
            when(req.getHeader("X-Forwarded-Proto")).thenReturn(null);
        }
        return req;
    }
}
