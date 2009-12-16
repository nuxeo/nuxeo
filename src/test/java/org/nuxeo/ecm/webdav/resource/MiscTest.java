package org.nuxeo.ecm.webdav.resource;

import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MiscTest extends Assert {

    @Test
    public void testIf() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getHeader("if")).thenReturn("<urn:uuid:tototiti>");
        assertEquals("tototiti",
                AbstractResource.getTokenFromHeaders("if", request));

        when(request.getHeader("if")).thenReturn(" (<urn:uuid:tototiti>) ");
        assertEquals("tototiti",
                AbstractResource.getTokenFromHeaders("if", request));

        when(request.getHeader("lock-token")).thenReturn(" (<urn:uuid:tototiti>) ");
        assertEquals("tototiti",
                AbstractResource.getTokenFromHeaders("lock-token", request));
    }

    @Test
    public void testUri() throws URISyntaxException, UnsupportedEncodingException {
        URI uri;
        uri = new URI(URLEncoder.encode("/ toto /", "UTF8"));
        uri = new URI(URLEncoder.encode("workspaces/Desktop/.xvpics/Photo 16.jpg", "UTF8"));
    }

}
