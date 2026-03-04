/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */
package org.nuxeo.template.processors.fm;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.template.processors.fm.FreeMarkerProcessor.GUESS_MIMETYPE_MAX_SIZE_PROP;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.ByteSize;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * @since 2025.17
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestFreeMarkerProcessor {

    @Inject
    protected MimetypeRegistry mimetypeRegistry;

    private static final String XML_CONTENT_TEMPLATE = "<?xml version=\"1.0\"?><root>%s</root>";

    private static final String HTML_CONTENT_TEMPLATE = "<html><body>%s</body></html>";

    private static final String HTML_DOCTYPE_TEMPLATE = "<!DOCTYPE html><html><body>%s</body></html>";

    private static final String HTML_UPPERCASE_TEMPLATE = "<HTML><BODY>%s</BODY></HTML>";

    private static final String HTML_WHITESPACE_TEMPLATE = "< html >< body >%s</ body ></ html >";

    @Test
    public void testGetTruncatedContentNullBlob() {
        var processor = new FreeMarkerProcessor();
        var result = processor.getTruncatedContent(null);
        assertNull(result);
    }

    @Test
    public void testGetTruncatedContentSmallContent() {
        var processor = new FreeMarkerProcessor();
        var content = String.format(XML_CONTENT_TEMPLATE, "test");
        var blob = Blobs.createBlob(content, "text/xml", UTF_8.name());

        var result = processor.getTruncatedContent(blob);

        assertNotNull(result);
        assertEquals(content, result);
    }

    @Test
    public void testGetTruncatedContentLargeContent() {
        var processor = new FreeMarkerProcessor();
        // Create content larger than 8KiB default
        var largeContent = createLargeXmlContent(10000);
        var blob = Blobs.createBlob(largeContent, "text/xml", UTF_8.name());

        var result = processor.getTruncatedContent(blob);

        assertNotNull(result);
        // Should be truncated to 8KiB (8192 bytes)
        var largeContentBytes = largeContent.getBytes(UTF_8).length;
        var resultBytes = result.getBytes(UTF_8).length;
        assertTrue("Content should be truncated", resultBytes < largeContentBytes);
        assertTrue("Content should be around 8KiB", resultBytes <= ByteSize.parse("8KiB").toBytes());
        // Should still start with the XML header
        assertTrue("Should start with XML header", result.startsWith("<?xml version=\"1.0\"?>"));
    }

    @Test
    @WithFrameworkProperty(name = GUESS_MIMETYPE_MAX_SIZE_PROP, value = "1KiB")
    public void testGetTruncatedContentConfigurableSize() {
        var processor = new FreeMarkerProcessor();
        // Create content larger than configured limit (1KiB)
        var largeContent = createLargeXmlContent(2000);
        var blob = Blobs.createBlob(largeContent, "text/xml", UTF_8.name());

        var result = processor.getTruncatedContent(blob);

        assertNotNull(result);
        // With 1KiB limit from property, should be truncated
        var resultBytes = result.getBytes(UTF_8).length;
        assertTrue("Content should be truncated to configured size", resultBytes <= ByteSize.parse("1KiB").toBytes());
    }

    @Test
    @WithFrameworkProperty(name = GUESS_MIMETYPE_MAX_SIZE_PROP, value = "-1")
    public void testGetTruncatedContentUnlimited() {
        var processor = new FreeMarkerProcessor();
        // Create large content
        var largeContent = createLargeXmlContent(10000);
        var blob = Blobs.createBlob(largeContent, "text/xml", UTF_8.name());

        var result = processor.getTruncatedContent(blob);

        assertNotNull(result);
        // With unlimited (-1), should NOT be truncated
        assertEquals("Content should not be truncated when unlimited", largeContent, result);
    }

    @Test
    public void testGetTruncatedContentEmptyBlob() {
        var processor = new FreeMarkerProcessor();
        var blob = Blobs.createBlob("", "text/plain", UTF_8.name());

        var result = processor.getTruncatedContent(blob);

        assertNotNull(result);
        assertEquals("", result);
    }

    // Tests for guessMimeType with truncation - ensures MIME type detection works correctly
    // when content exceeds the max size limit and truncation is active

    @Test
    public void testGuessMimeTypeWithLargeContent() {
        var processor = new FreeMarkerProcessor();
        // Create content larger than 8KiB default - XML/HTML markers are at the start
        var largeXmlContent = createLargeXmlContent(10000);
        var largeHtmlContent = createLargeHtmlContent(10000);

        var xmlBlob = Blobs.createBlob(largeXmlContent, "application/octet-stream", UTF_8.name());
        var htmlBlob = Blobs.createBlob(largeHtmlContent, "application/octet-stream", UTF_8.name());

        assertEquals("text/xml", processor.guessMimeType(xmlBlob, mimetypeRegistry));
        assertEquals("text/html", processor.guessMimeType(htmlBlob, mimetypeRegistry));
    }

    @Test
    @WithFrameworkProperty(name = GUESS_MIMETYPE_MAX_SIZE_PROP, value = "1KiB")
    public void testGuessMimeTypeWithCustomTruncationLimit() {
        var processor = new FreeMarkerProcessor();
        // Create content larger than configured 1KiB limit
        var largeXmlContent = createLargeXmlContent(2000);
        var largeHtmlContent = createLargeHtmlContent(2000);

        var xmlBlob = Blobs.createBlob(largeXmlContent, "application/octet-stream", UTF_8.name());
        var htmlBlob = Blobs.createBlob(largeHtmlContent, "application/octet-stream", UTF_8.name());

        // With aggressive truncation, detection should still work since markers are at the start
        assertEquals("text/xml", processor.guessMimeType(xmlBlob, mimetypeRegistry));
        assertEquals("text/html", processor.guessMimeType(htmlBlob, mimetypeRegistry));
    }

    @Test
    public void testGuessMimeTypeHtmlVariants() {
        var processor = new FreeMarkerProcessor();
        // Test various HTML formats to ensure the prefix-based pattern works
        var htmlDoctype = String.format(HTML_DOCTYPE_TEMPLATE, "test");
        var htmlUppercase = String.format(HTML_UPPERCASE_TEMPLATE, "test");
        var htmlWhitespace = String.format(HTML_WHITESPACE_TEMPLATE, "test");

        assertEquals("text/html", processor.guessMimeType(
                Blobs.createBlob(htmlDoctype, "application/octet-stream", UTF_8.name()), mimetypeRegistry));
        assertEquals("text/html", processor.guessMimeType(
                Blobs.createBlob(htmlUppercase, "application/octet-stream", UTF_8.name()), mimetypeRegistry));
        assertEquals("text/html", processor.guessMimeType(
                Blobs.createBlob(htmlWhitespace, "application/octet-stream", UTF_8.name()), mimetypeRegistry));
    }

    @Test
    public void testGuessMimeTypeWithLeadingWhitespace() {
        var processor = new FreeMarkerProcessor();
        // Test that patterns work with leading whitespace
        var xmlWithWhitespace = "  \n  <?xml version=\"1.0\"?><root>test</root>";
        var htmlWithWhitespace = "  \n  <html><body>test</body></html>";
        var doctypeWithWhitespace = "  \n  <!DOCTYPE html><html><body>test</body></html>";

        assertEquals("text/xml", processor.guessMimeType(
                Blobs.createBlob(xmlWithWhitespace, "application/octet-stream", UTF_8.name()), mimetypeRegistry));
        assertEquals("text/html", processor.guessMimeType(
                Blobs.createBlob(htmlWithWhitespace, "application/octet-stream", UTF_8.name()), mimetypeRegistry));
        assertEquals("text/html", processor.guessMimeType(
                Blobs.createBlob(doctypeWithWhitespace, "application/octet-stream", UTF_8.name()), mimetypeRegistry));
    }

    @Test
    public void testGuessMimeTypeRejectsFalsePositives() {
        var processor = new FreeMarkerProcessor();
        // Test that <html> in the middle of content is not detected as HTML
        var notHtml = "This is plain text with <html> tag in the middle";
        var notXml = "Some text before <?xml version=\"1.0\"?>";

        assertEquals("text/plain", processor.guessMimeType(
                Blobs.createBlob(notHtml, "application/octet-stream", UTF_8.name()), mimetypeRegistry));
        assertEquals("text/plain", processor.guessMimeType(
                Blobs.createBlob(notXml, "application/octet-stream", UTF_8.name()), mimetypeRegistry));
    }

    @Test
    @WithFrameworkProperty(name = GUESS_MIMETYPE_MAX_SIZE_PROP, value = "50B")
    public void testGuessMimeTypeHtmlWithTruncationInsideTag() {
        var processor = new FreeMarkerProcessor();
        // Create HTML where truncation happens inside the opening <html> tag or shortly after
        // With a very small limit (50 bytes), the closing </body></html> tags will be cut off
        // This tests that we don't need matching closing tags anymore
        var largeHtmlContent = createLargeHtmlContent(1000);
        var htmlBlob = Blobs.createBlob(largeHtmlContent, "application/octet-stream", UTF_8.name());

        // Should still detect as HTML even though closing tags are truncated
        assertEquals("text/html", processor.guessMimeType(htmlBlob, mimetypeRegistry));
    }

    private static String createLargeXmlContent(int repeatCount) {
        return String.format(XML_CONTENT_TEMPLATE, "x".repeat(repeatCount));
    }

    private static String createLargeHtmlContent(int repeatCount) {
        return String.format(HTML_CONTENT_TEMPLATE, "x".repeat(repeatCount));
    }

}
