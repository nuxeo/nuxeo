package org.nuxeo.ecm.core.io.download;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class TestDownloadBlobInfo {

    @Test
    public void testParseDownloadPath() {
        assertParsed("blobholder:0", null, "");
        assertParsed("blobholder:0", null, "/");
        assertParsed("foo", null, "/foo");
        assertParsed("foo", null, "/foo/");
        assertParsed("foo", "bar", "/foo/bar");
        assertParsed("foo/bar", "baz", "/foo/bar/baz");
        assertParsed("foo/bar/baz", "moo", "/foo/bar/baz/moo");
        assertParsed("files:files/0/file", "image.png", "/files:files/0/file/image.png");
    }

    protected void assertParsed(String xpath, String filename, String string) {
        DownloadBlobInfo downloadBlobInfo = new DownloadBlobInfo("somerepo/someid" + string);
        assertEquals("somerepo", downloadBlobInfo.repository);
        assertEquals("someid", downloadBlobInfo.docId);
        assertEquals(xpath, downloadBlobInfo.xpath);
        assertEquals(filename, downloadBlobInfo.filename);
    }

    @Test
    public void cannotConstructDownloadBlobInfo() {
        try {
            new DownloadBlobInfo("foo");
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

}
