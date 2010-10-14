package org.nuxeo.ecm.webdav.resource;

import org.junit.Test;
import org.nuxeo.ecm.webdav.Util;

import javax.servlet.http.HttpServletRequest;

import java.net.URI;
import java.net.URLEncoder;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MiscTest {

    @Test
    public void testIf() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getHeader("if")).thenReturn("<urn:uuid:tototiti>");
        assertEquals("tototiti",
                Util.getTokenFromHeaders("if", request));

        when(request.getHeader("if")).thenReturn(" (<urn:uuid:tototiti>) ");
        assertEquals("tototiti",
                Util.getTokenFromHeaders("if", request));

        when(request.getHeader("lock-token")).thenReturn(" (<urn:uuid:tototiti>) ");
        assertEquals("tototiti",
                Util.getTokenFromHeaders("lock-token", request));
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
