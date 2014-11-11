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

package org.nuxeo.theme.test.html.filters;

import junit.framework.TestCase;

import org.nuxeo.theme.html.filters.standalone.XmlNamespaces;
import org.nuxeo.theme.rendering.Filter;

public class TestXmlNamespacesFilter extends TestCase {
    DummyRenderingInfo info;

    Filter filter;

    @Override
    public void setUp() {
        info = new DummyRenderingInfo(null, null);
        filter = new XmlNamespaces();
    }

    public void testFilter1() {
        info.setMarkup("<html><div>content</div></html>");
        filter.process(info, false);
        assertEquals("<html><div>content</div></html>", info.getMarkup());
    }

    public void testFilter2() {
        info.setMarkup("<html><div xmlns:c=\"http://java.sun.com/jstl/core\">"
                + "content</div></html>");
        filter.process(info, false);
        assertEquals("<html xmlns:c=\"http://java.sun.com/jstl/core\">"
                + "<div>content</div></html>", info.getMarkup());
    }

    public void testFilter3() {
        info.setMarkup("<html><div xmlns:c=\"http://java.sun.com/jstl/core\">"
                + "<p xmlns:f=\"http://java.sun.com/jsf/core\">"
                + "content</p></div></html>");
        filter.process(info, false);
        assertEquals("<html xmlns:c=\"http://java.sun.com/jstl/core\" "
                + "xmlns:f=\"http://java.sun.com/jsf/core\">"
                + "<div><p>content</p></div></html>", info.getMarkup());
    }

    public void testFilter4() {
        info.setMarkup("<html xmlns:ui=\"http://java.sun.com/jsf/facelets\">"
                + "<div xmlns:c=\"http://java.sun.com/jstl/core\">"
                + "<p xmlns:f=\"http://java.sun.com/jsf/core\">"
                + "content</p></div></html>");
        filter.process(info, false);
        assertEquals("<html xmlns:ui=\"http://java.sun.com/jsf/facelets\" "
                + "xmlns:c=\"http://java.sun.com/jstl/core\" "
                + "xmlns:f=\"http://java.sun.com/jsf/core\">"
                + "<div><p>content</p></div></html>", info.getMarkup());
    }

    public void testFilter5() {
        info.setMarkup("<html xmlns:ui=\"http://java.sun.com/jsf/facelets\">"
                + "<div xmlns:c=\"http://java.sun.com/jstl/core\">"
                + "<p xmlns:c=\"http://java.sun.com/jstl/core\">"
                + "content</p></div></html>");
        filter.process(info, false);
        assertEquals("<html xmlns:ui=\"http://java.sun.com/jsf/facelets\" "
                + "xmlns:c=\"http://java.sun.com/jstl/core\">"
                + "<div><p>content</p></div></html>", info.getMarkup());
    }

    public void testFilter6() {
        info.setMarkup("<html><div xmlns=\"http://www.w3.org/1999/xhtml\">content</div></html>");
        filter.process(info, false);
        assertEquals(
                "<html xmlns=\"http://www.w3.org/1999/xhtml\"><div>content</div></html>",
                info.getMarkup());
    }

    public void testFilter7() {
        info.setMarkup("<@abc></@abc><html xmlns=\"http://www.w3.org/1999/xhtml\"><div>content</div></html>");
        filter.process(info, false);
        assertEquals(
                "<@abc></@abc><html xmlns=\"http://www.w3.org/1999/xhtml\"><div>content</div></html>",
                info.getMarkup());
    }

    public void testFilter8() {
        info.setMarkup("<!-- comment --><html xmlns=\"http://www.w3.org/1999/xhtml\"><div>content</div></html>");
        filter.process(info, false);
        assertEquals(
                "<!-- comment --><html xmlns=\"http://www.w3.org/1999/xhtml\"><div>content</div></html>",
                info.getMarkup());
    }

    public void testFilter9() {
        info.setMarkup("<!-- comment -->");
        filter.process(info, false);
        assertEquals("<!-- comment -->", info.getMarkup());
    }

    public void testFilter10() {
        info.setMarkup("<div></div>");
        filter.process(info, false);
        assertEquals("<div></div>", info.getMarkup());
    }

    public void testFilter11() {
        info.setMarkup("no tags");
        filter.process(info, false);
        assertEquals("no tags", info.getMarkup());
    }
}
