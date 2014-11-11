/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.test.webwidgets;

import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.nuxeo.theme.webwidgets.Utils;
import org.nuxeo.theme.webwidgets.WidgetFieldType;

public class TestUtils extends TestCase {

    public void testExtractBody() {
        assertEquals("text",
                Utils.extractBody("<html><body>text</body></html>"));
        assertEquals(
                "line 1\nline 2",
                Utils.extractBody("<html><head></head><body>line 1\nline 2</body></html>"));
        assertEquals("no body", Utils.extractBody("no body"));
    }

    public void testExtractScripts() {
        assertEquals("", Utils.extractScripts("<html><body>text</body></html>"));
        assertEquals(
                "var a=1;\nalert('test');\n",
                Utils.extractScripts("<html><head><script>var a=1;\nalert('test');</script></head><body></body></html>"));
        assertEquals(
                "alert('test');\nalert('test 2');\n",
                Utils.extractScripts("<html><head><script>alert('test');</script><script>alert('test 2');</script></head><body></body></html>"));
        assertEquals(
                "",
                Utils.extractScripts("<html><body><script>alert('test');</script></body></html>"));
    }

    public void testExtractStyles() {
        assertEquals("", Utils.extractStyles("<html><body>text</body></html>"));
        assertEquals(
                "div {color. red;}",
                Utils.extractStyles("<html><head><style>div {color. red;}</style></head><body></body></html>"));
        assertEquals(
                "div {color. red;}p {color. blue;}",
                Utils.extractStyles("<html><head><style>div {color. red;}</style><style>p {color. blue;}</style></head><body></body></html>"));
        assertEquals(
                "",
                Utils.extractStyles("<html><body><style>div {color. red;}</style></body></html>"));
    }

    public void testExtractMetadata() {
        assertEquals("Author name", Utils.extractMetadata(
                "<meta name=\"author\" content=\"Author name\" />", "author"));
        assertEquals("A description", Utils.extractMetadata(
                "<meta name=\"description\" content=\"A description\" />",
                "description"));
        assertNull(Utils.extractMetadata(
                "<meta name=\"known\" content=\"Unknown\" />", "unknown"));
    }

    public void testExtractIcon() {
        assertEquals(
                "icon.png",
                Utils.extractIcon("<link rel=\"icon\" type=\"image/png\" href=\"icon.png\" />"));
        assertEquals(
                "icon.png",
                Utils.extractIcon("<link rel=\"icon\" href=\"icon.png\" type=\"image/png\" />"));
    }

    public void testExtractSchema() throws IOException {
        List<WidgetFieldType> fields = Utils.extractSchema(org.nuxeo.theme.Utils.readResourceAsString("test-widget.html"));
        WidgetFieldType field1 = fields.get(0);
        assertEquals("Title", field1.label);
        assertEquals("Widget title", field1.defaultValue);
        assertEquals("text", field1.type);
        assertEquals("title", field1.name);
    }
}
