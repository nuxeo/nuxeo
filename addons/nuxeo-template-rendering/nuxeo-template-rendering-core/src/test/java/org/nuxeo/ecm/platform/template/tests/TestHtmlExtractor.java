package org.nuxeo.ecm.platform.template.tests;

import org.junit.Test;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.template.processors.HtmlBodyExtractor;

import static org.junit.Assert.*;

public class TestHtmlExtractor extends NXRuntimeTestCase {

    @Test
    public void testHtmlBoduExtractor() throws Exception {
        String html = "<html><body>PAGE</body></html>";
        assertEquals("PAGE", HtmlBodyExtractor.extractHtmlBody(html));

        html = "<html><body style=\"body\" >PAGE</body></html>";
        assertEquals("PAGE", HtmlBodyExtractor.extractHtmlBody(html));

        html = "<html><Body style=\"body\" >PAGE</BODY></html>";
        assertEquals("PAGE", HtmlBodyExtractor.extractHtmlBody(html));

    }
}
