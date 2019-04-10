/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.webdav.resource;

import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import java.net.URI;
import java.net.URLEncoder;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MiscTest {

    public static String getTokenFromHeaders(String headerName, HttpServletRequest request) {
        String header = request.getHeader(headerName);
        if (header == null) {
            return null;
        }
        String token = header.trim();
        int tokenStart = token.indexOf("<urn:uuid:");
        token = token.substring(tokenStart + "<urn:uuid:".length(), token.length());
        int tokenEnd = token.indexOf(">");
        token = token.substring(0, tokenEnd);
        return token;
    }

    @Test
    public void getTokenFromHeadersReturnsRightToken() {
        String TOKEN = "tititoto2010";
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("If")).thenReturn("<urn:uuid:" + TOKEN + ">");

        String result = getTokenFromHeaders("If", request);
        assertThat(result, is(TOKEN));
    }

    @Test
    public void testIf() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getHeader("if")).thenReturn("<urn:uuid:tototiti>");
        assertEquals("tototiti", getTokenFromHeaders("if", request));

        when(request.getHeader("if")).thenReturn(" (<urn:uuid:tototiti>) ");
        assertEquals("tototiti", getTokenFromHeaders("if", request));

        when(request.getHeader("lock-token")).thenReturn(" (<urn:uuid:tototiti>) ");
        assertEquals("tototiti", getTokenFromHeaders("lock-token", request));
    }

    @Test
    public void testUri() throws Exception {
        URI uri;
        uri = new URI(URLEncoder.encode("/ toto /", "UTF8"));
        assertEquals("%2F+toto+%2F", uri.toASCIIString());
        uri = new URI(URLEncoder.encode("workspaces/Desktop/.xvpics/Photo 16.jpg", "UTF8"));
        assertEquals("workspaces%2FDesktop%2F.xvpics%2FPhoto+16.jpg", uri.toASCIIString());
    }

}
