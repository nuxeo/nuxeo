/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.htmlsanitizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.htmlsanitizer",
        "org.nuxeo.ecm.platform.htmlsanitizer.test:OSGI-INF/core-types-contrib.xml" })
public class TestHtmlSanitizerServiceImpl {

    public static final String BAD_HTML = "<b>foo<script>bar</script></b>";

    public static final String SANITIZED_HTML = "<b>foo</b>";

    public static final String BAD_XML = "<b>caf\u00e9</b>";

    public static final String SANITIZED_XML = "<b>caf&eacute;</b>";

    public static final String NORMAL_TEXT = "Caf\u00e9 < Tea";

    public static final String MARKDOWN_TEXT = "Caf\u00e9 < Tea";

    // script tag is added here just to be sure sanitizer is not run
    public static final String WIKI_MARKUP = "<script></script>[image:http://server/path/image.jpg My Image]";

    public static final String BAD_HTML5 =
            "<video id=\"test\"><source src=\"test\" type=\"video/mp4\"/><img src=\"a wrong image location specification\"/></video>";

    public static final String SANITIZED_HTML5 = "<video id=\"test\"><source src=\"test\" type=\"video/mp4\" /></video>";

    @Inject
    CoreSession session;

    @Test
    public void sanitizeNoteHtml() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "n", "Note");
        doc.setPropertyValue("note", BAD_HTML);
        doc.setPropertyValue("mime_type", "text/html");
        doc = session.createDocument(doc);
        String note = (String) doc.getPropertyValue("note");
        assertEquals(SANITIZED_HTML, note);

        session.save();
        doc.setPropertyValue("note", BAD_HTML);
        doc = session.saveDocument(doc);
        note = (String) doc.getPropertyValue("note");
        assertEquals(SANITIZED_HTML, note);
    }

    @Test
    public void sanitizeNoteXml() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "n", "Note");
        doc.setPropertyValue("note", BAD_XML);
        doc.setPropertyValue("mime_type", "text/xml");
        doc = session.createDocument(doc);
        String note = (String) doc.getPropertyValue("note");
        assertEquals(SANITIZED_XML, note);

        session.save();
        doc.setPropertyValue("note", BAD_XML);
        doc = session.saveDocument(doc);
        note = (String) doc.getPropertyValue("note");
        assertEquals(SANITIZED_XML, note);
    }

    // but text/plain notes must not be sanitized
    @Test
    public void sanitizeNoteText() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "n", "Note");
        doc.setPropertyValue("note", NORMAL_TEXT);
        doc.setPropertyValue("mime_type", "text/plain");
        doc = session.createDocument(doc);
        String note = (String) doc.getPropertyValue("note");
        assertEquals(NORMAL_TEXT, note);

        session.save();
        doc.setPropertyValue("note", NORMAL_TEXT);
        doc = session.saveDocument(doc);
        note = (String) doc.getPropertyValue("note");
        assertEquals(NORMAL_TEXT, note);
    }

    // but text/markdown notes must not be sanitized
    @Test
    public void sanitizeNoteMarkdown() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "n", "Note");
        doc.setPropertyValue("note", MARKDOWN_TEXT);
        doc.setPropertyValue("mime_type", "text/x-web-markdown");
        doc = session.createDocument(doc);
        String note = (String) doc.getPropertyValue("note");
        assertEquals(MARKDOWN_TEXT, note);

        session.save();
        doc.setPropertyValue("note", MARKDOWN_TEXT);
        doc = session.saveDocument(doc);
        note = (String) doc.getPropertyValue("note");
        assertEquals(MARKDOWN_TEXT, note);
    }

    @Test
    public void sanitizeNullFilterField() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "n", "Note");
        doc.setPropertyValue("note", BAD_XML);
        doc.setPropertyValue("mime_type", null); // null filter field
        doc = session.createDocument(doc);
        String note = (String) doc.getPropertyValue("note");
        assertEquals(SANITIZED_XML, note);
    }

    @Test
    public void sanitizeWebPage() throws Exception {

        // Html page that must be sanitized
        DocumentModel doc = session.createDocumentModel("/", "wp", "WebPage");
        doc.setPropertyValue("webp:content", BAD_HTML);
        doc.setPropertyValue("webp:isRichtext", true);
        doc = session.createDocument(doc);
        String webpage = (String) doc.getPropertyValue("webp:content");
        assertEquals(SANITIZED_HTML, webpage);
        session.save();

        // Wiki page that must not be sanitized
        DocumentModel doc2 = session.createDocumentModel("/", "wp2", "WebPage");
        doc2.setPropertyValue("webp:content", WIKI_MARKUP);
        doc2.setPropertyValue("webp:isRichtext", false);
        doc2 = session.createDocument(doc2);
        String webpage2 = (String) doc2.getPropertyValue("webp:content");
        assertEquals(WIKI_MARKUP, webpage2);
        session.save();

        DocumentModel doc3 = session.createDocumentModel("/", "wp3", "WebPage");
        doc3.setPropertyValue("webp:content", BAD_HTML);
        doc3.setPropertyValue("webp:isRichtext", false);
        doc3 = session.createDocument(doc3);
        String webpage3 = (String) doc3.getPropertyValue("webp:content");
        assertEquals(BAD_HTML, webpage3);
        session.save();

        DocumentModel doc4 = session.createDocumentModel("/", "wp4", "WebPage");
        doc4.setPropertyValue("webp:content", WIKI_MARKUP);
        doc4.setPropertyValue("webp:isRichtext", true);
        doc4 = session.createDocument(doc4);
        String webpage4 = (String) doc4.getPropertyValue("webp:content");
        assertFalse(WIKI_MARKUP.equals(webpage4));
        session.save();
    }

    @Test
    public void sanitizeKeepLinkTargetBlank() throws Exception {
        String html = "<a href=\"foo\" target=\"_blank\">link</a>";
        HtmlSanitizerService service = Framework.getService(HtmlSanitizerService.class);
        String res = service.sanitizeString(html, null);
        assertEquals(html, res);
    }

    @Test
    public void testFieldToString() {
        FieldDescriptor fd = new FieldDescriptor();
        fd.contentField = "a";
        assertEquals("a", fd.toString());
        fd.filterField = "b";
        fd.filterValue = "c";
        fd.sanitize = true;
        assertEquals("a if b=c", fd.toString());
        fd.sanitize = false;
        assertEquals("a if b!=c", fd.toString());
    }

    @Test
    public void testSanitizeSpaces() {
        HtmlSanitizerService service = Framework.getService(HtmlSanitizerService.class);
        assertEquals("<strong>strong</strong>\n<em>content</em>",
                service.sanitizeString("<strong>strong</strong><em>content</em>", null));
        assertEquals("<p><strong>strong</strong><em>content</em></p>",
                service.sanitizeString("<p><strong>strong</strong><em>content</em></p>", null));
    }

    @Test
    public void sanitizeNoteHtml5() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "n2", "Note");
        doc.setPropertyValue("note", BAD_HTML5);
        doc.setPropertyValue("mime_type", "text/xml");
        doc = session.createDocument(doc);
        String note = (String) doc.getPropertyValue("note");
        assertEquals(SANITIZED_HTML5, note);

        session.save();
    }
}
