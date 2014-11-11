package org.nuxeo.ecm.platform.ui.web.download;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
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

    @Test(expected = ClientException.class)
    public void testParseUnsupportedByteRange() throws Exception {
        DownloadServlet.parseRange("blablabla", 12345);
    }

    @Test(expected = ClientException.class)
    public void testParseUnsupportedByteRange2() throws Exception {
        DownloadServlet.parseRange("bytes=", 12345);
    }

    @Test(expected = ClientException.class)
    public void testParseUnsupportedByteRange3() throws Exception {
        DownloadServlet.parseRange("bytes=-", 12345);
    }

    @Test(expected = ClientException.class)
    public void testParseUnsupportedByteRange4() throws Exception {
        DownloadServlet.parseRange("bytes=123-45", 12345); // Start > end
    }

    @Test(expected = ClientException.class)
    public void testParseUnsupportedByteRange5() throws Exception {
        DownloadServlet.parseRange("bytes=0-123,-45", 12345); // Do no support
                                                              // multiple ranges
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
        // Given a blob
        String blobValue = "Hello World";
        SQLBlob blob = getBlobWithFakeDigest(blobValue, "12345");

        // When i send a request a given digest
        String digestToTest = getDigestToTest(match, blob);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletResponse resp = getResponseForETag(digestToTest, blob, out);

        // Then the response differs if the digest match
        if (Boolean.TRUE.equals(match)) {
            assertEquals(0, out.toByteArray().length);
            verify(resp).sendError(HttpServletResponse.SC_NOT_MODIFIED);
        } else {
            assertEquals(blobValue, out.toString());
            verify(resp).setHeader("ETag", blob.getDigest());
        }

    }

    /**
     * Returns a digest wether we want to match the blob's digest
     *
     * @param match may be null
     * @param blob
     * @return
     *
     * @since 5.8
     */
    private String getDigestToTest(Boolean match, SQLBlob blob) {
        if (match == null) {
            return null;
        } else if (match == true) {
            return blob.getDigest();
        }
        return blob.getDigest() + "salt";
    }

    /**
     * Mocks the request to the download servlet with a given ETag
     *
     * @param etag
     * @param blob
     * @param out
     * @return
     * @throws ServletException
     * @throws IOException
     *
     * @since 5.8
     */
    private HttpServletResponse getResponseForETag(String etag, Blob blob,
            OutputStream out) throws IOException, ServletException {

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("If-None-Match")).thenReturn(etag);
        HttpServletResponse resp = getMockResponse(out);

        DownloadServlet servlet = new DownloadServlet();
        servlet.downloadBlob(req, resp, blob, (String) null);
        verify(req, atLeast(1)).getHeader("If-None-Match");
        return resp;
    }

    /**
     * Forges a mock response base on an outpuStream.
     *
     * @return
     * @throws IOException
     *
     * @since 5.8
     */
    private HttpServletResponse getMockResponse(OutputStream out)
            throws IOException {
        HttpServletResponse resp = mock(HttpServletResponse.class);
        BufferingServletOutputStream sos = new BufferingServletOutputStream(out);
        PrintWriter printWriter = new PrintWriter(sos);
        when(resp.getOutputStream()).thenReturn(sos);
        when(resp.getWriter()).thenReturn(printWriter);
        return resp;
    }

    /**
     * Forges a SQLBlob with a string value and an expected digest.
     *
     * @param stringValue
     * @param digest
     * @return
     * @throws IOException
     *
     * @since 5.8
     */
    private SQLBlob getBlobWithFakeDigest(String stringValue, String digest)
            throws IOException {
        Binary binary = mock(Binary.class);
        final byte[] bytes = stringValue.getBytes();
        InputStream in = new ByteArrayInputStream(bytes);
        SQLBlob blob = new SQLBlob(binary, "myFile.txt", "text/plain", "UTF-8",
                digest,bytes.length);
        when(binary.getStream()).thenReturn(in);
        when(binary.getDigest()).thenReturn(digest);
        when(binary.getLength()).thenReturn((long) bytes.length);
        return blob;
    }
}
