package org.nuxeo.ecm.platform.ui.web.download;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.ui.web.download.DownloadServlet.ByteRange;

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
}
