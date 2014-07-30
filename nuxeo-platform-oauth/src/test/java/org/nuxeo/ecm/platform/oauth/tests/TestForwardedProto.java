/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.oauth.tests;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.nuxeo.ecm.platform.ui.web.auth.oauth.NuxeoOAuthFilter;

/**
 *
 *
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
        HttpServletRequest req = mockRequestWithForwardedHeader(TEST_SITE_URL,HTTPS);
        assertEquals("https://mysite.org/bla", NuxeoOAuthFilter.getRequestURL(req));


        req = mockRequestWithForwardedHeader("https://mysite.org/bla",HTTPS);
        assertEquals("https://mysite.org/bla", NuxeoOAuthFilter.getRequestURL(req));


        req = mockRequestWithForwardedHeader(TEST_SITE_URL,null);
        assertEquals(TEST_SITE_URL, NuxeoOAuthFilter.getRequestURL(req));


        req = mockRequestWithForwardedHeader(TEST_SITE_URL,HTTP);
        assertEquals(TEST_SITE_URL, NuxeoOAuthFilter.getRequestURL(req));

    }

    private HttpServletRequest mockRequestWithForwardedHeader(String url, String xfp) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURL()).thenReturn(new StringBuffer(url));
        if(xfp != null) {
            when(req.getHeader("X-Forwarded-Proto")).thenReturn(xfp);
        } else {
            when(req.getHeader("X-Forwarded-Proto")).thenReturn(null);
        }
        return req;
    }
}
