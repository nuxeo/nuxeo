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

package org.nuxeo.theme.test.editor.filters;

import junit.framework.TestCase;

import org.nuxeo.theme.editor.filters.ElementUid;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.rendering.Filter;

public class TestElementUidFilter extends TestCase {
    DummyRenderingInfo info;

    Filter filter;

    PageElement page;

    @Override
    public void setUp() {
        // create the elements to render
        page = new PageElement();
        page.setUid(1);
        info = new DummyRenderingInfo(page, null);
        filter = new ElementUid();
    }

    public void testFilter2() {
        // insert the element e
        info.setMarkup("<div>content</div>");
        filter.process(info, false);
        assertEquals("<div id=\"e1\">content</div>", info.getMarkup());
    }

    public void testFilter3() {
        // if the element already has a e attribute, override it
        info.setMarkup("<div id=\"e2\">content</div>");
        filter.process(info, false);
        assertEquals("<div id=\"e1\">content</div>", info.getMarkup());
    }

    public void testFilter4() {
        // edge-case
        info.setMarkup("<div id=\"e2\" id=\"e3\">content</div>");
        filter.process(info, false);
        assertEquals("<div id=\"e1\">content</div>", info.getMarkup());
    }

    public void testFilter5() {
        // make sure that other attributes are preserved
        info.setMarkup("<div class=\"someClass\">content</div>");
        filter.process(info, false);
        assertEquals("<div class=\"someClass\" id=\"e1\">content</div>",
                info.getMarkup());
    }

    public void testFilter6() {
        // test line breaks
        info.setMarkup("<div>content\n</div>\n");
        filter.process(info, false);
        assertEquals("<div id=\"e1\">content\n</div>\n", info.getMarkup());
    }

    public void testFilter7() {
        info.setMarkup("<img src=\"/logo.png\" />");
        filter.process(info, false);
        assertEquals("<img src=\"/logo.png\" id=\"e1\" />", info.getMarkup());
    }

}
