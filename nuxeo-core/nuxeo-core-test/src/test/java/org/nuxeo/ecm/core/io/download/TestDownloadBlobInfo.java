package org.nuxeo.ecm.core.io.download;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestDownloadBlobInfo {

    @Test
    public void testParseDownloadPath() {
        assertParsed(null, null, "");
        assertParsed(null, null, "/");
        assertParsed("foo", null, "/foo");
        assertParsed("foo", null, "/foo/");
        assertParsed("foo", "bar", "/foo/bar");
        assertParsed("foo/bar", "baz", "/foo/bar/baz");
        assertParsed("foo/bar/baz", "moo", "/foo/bar/baz/moo");
        assertParsed("file:content", null, "/file:content");
        assertParsed("file:content", "file.txt", "/file:content/file.txt");
        assertParsed("files:files/0/file", null, "/files:files/0/file");
        assertParsed("files:files/0/file", "image.png", "/files:files/0/file/image.png");
        assertParsed("foo/bar", "baz", "/foo/bar/baz");
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
