package org.nuxeo.ecm.platform.ui.web.download;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.impl.blob.InputStreamBlob;
import org.nuxeo.ecm.core.storage.sql.Binary;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLBlob;
import org.nuxeo.ecm.platform.ui.web.download.DownloadServlet.ByteRange;
import org.nuxeo.ecm.platform.web.common.requestcontroller.filter.BufferingServletOutputStream;

public class TestDownloadServlet {

    @Test
    public void testParseByteRange() throws Exception {
        ByteRange byteRange = DownloadServlet.parseRange("bytes=42-169", 12345);
        assertEquals(42, byteRange.getStart());
        assertEquals(169, byteRange.getEnd());
        assertEquals(128, byteRange.getLength());
    }

    @Test
    public void testParseByteRangeWithoutEnd() throws Exception {
        ByteRange byteRange = DownloadServlet.parseRange("bytes=0-", 12345);
        assertEquals(0, byteRange.getStart());
        assertEquals(12344, byteRange.getEnd());
        assertEquals(12345, byteRange.getLength());
    }

    @Test
    public void testParseByteRangeWithoutStart() throws Exception {
        ByteRange byteRange = DownloadServlet.parseRange("bytes=-128", 12345);
        assertEquals(12217, byteRange.getStart());
        assertEquals(12344, byteRange.getEnd());
        assertEquals(128, byteRange.getLength());
    }

    @Test(expected=ClientException.class)
    public void testParseUnsupportedByteRange() throws Exception {
        DownloadServlet.parseRange("blablabla", 12345);
    }

    @Test(expected=ClientException.class)
    public void testParseUnsupportedByteRange2() throws Exception {
        DownloadServlet.parseRange("bytes=", 12345);
    }

    @Test(expected=ClientException.class)
    public void testParseUnsupportedByteRange3() throws Exception {
        DownloadServlet.parseRange("bytes=-", 12345);
    }

    @Test(expected=ClientException.class)
    public void testParseUnsupportedByteRange4() throws Exception {
        DownloadServlet.parseRange("bytes=123-45", 12345); // Start > end
    }

    @Test(expected=ClientException.class)
    public void testParseUnsupportedByteRange5() throws Exception {
        DownloadServlet.parseRange("bytes=0-123,-45", 12345); // Do no support multiple ranges
    }

    @Test
    public void testWriteStream() throws Exception {
        InputStream in = new ByteArrayInputStream("Hello, world!".getBytes());
        OutputStream out = new ByteArrayOutputStream();
        ByteRange range = new ByteRange(7, 11);
        DownloadServlet.writeStream(in, out, range);
        assertEquals("world", out.toString());
    }
    
    @Test
    public void testETagHeaderNone() throws Exception {
        doTestETagHeader(null);
    }
    
    @Test
    public void testETagHeaderNotMatched() throws Exception {
        doTestETagHeader(Boolean.FALSE);
    }

    @Test
    public void testETagHeaderMatched() throws Exception {
        doTestETagHeader(Boolean.TRUE);
    }
    
    private void doTestETagHeader(Boolean match) throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);       
        HttpServletResponse resp = mock(HttpServletResponse.class);
        Binary binary = mock(Binary.class);
        String s = "Hello, world!";
        final byte[] bytes = s.getBytes();
        InputStream in = new ByteArrayInputStream(bytes);
        String digest = "12345";
        String bogusDigest = "67890";
        SQLBlob blob = new SQLBlob(binary, "myFile.txt", "text/plain", 
        		"UTF-8", digest);
        when(binary.getStream()).thenReturn(in);
        when(binary.getDigest()).thenReturn(digest);
        when(binary.getLength()).thenReturn(Long.valueOf(bytes.length));
        String ifNoneMatchHeader = null;
        if (match != null) {
            ifNoneMatchHeader = (match) ? getETag(digest) : getETag(bogusDigest); 
        }
        when(req.getHeader("If-None-Match")).thenReturn(ifNoneMatchHeader);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferingServletOutputStream sos = new BufferingServletOutputStream(baos);
        PrintWriter printWriter = new PrintWriter(sos);
        when(resp.getOutputStream()).thenReturn(sos);
        when(resp.getWriter()).thenReturn(printWriter);
        
        DownloadServlet servlet = new DownloadServlet();
        Method m = servlet.getClass().getDeclaredMethod("downloadBlob",	new Class[] 
        		{HttpServletRequest.class, HttpServletResponse.class, 
        		Blob.class, String.class});
        m.setAccessible(true);
        m.invoke(servlet, new Object[] {req, resp, blob, (String) null});

        verify(req, atLeast(1)).getHeader("If-None-Match");
        if (Boolean.TRUE.equals(match)) {
            assertEquals(0, baos.toByteArray().length);
            verify(resp).sendError(HttpServletResponse.SC_NOT_MODIFIED);
        } else {
            assertEquals(s, baos.toString());
            verify(resp).setHeader("ETag", getETag(digest));
        }
    }

    private String getETag(String digest) {
        return digest;
    }
}
