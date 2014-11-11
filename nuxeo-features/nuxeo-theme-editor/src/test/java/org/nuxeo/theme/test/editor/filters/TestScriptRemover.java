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

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.theme.editor.filters.ScriptRemover;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.rendering.Filter;

public class TestScriptRemover {
    DummyRenderingInfo info;

    Filter filter;

    PageElement page;

    @Before
    public void setUp() {
        // create the elements to render
        page = new PageElement();
        page.setUid(1);
        info = new DummyRenderingInfo(page, null);
        filter = new ScriptRemover();
    }

    @Test
    public void testFilter1() {
        info.setMarkup("<span>before</span><script>content</script><span>after</span>");
        filter.process(info, false);
        assertEquals("<span>before</span><span>after</span>", info.getMarkup());
    }

    @Test
    public void testFilter2() {
        info.setMarkup("<script>content</script>");
        filter.process(info, false);
        assertEquals("", info.getMarkup());
    }

    @Test
    public void testFilter3() {
        info.setMarkup("<script type=\"text/javascript\">content</script>");
        filter.process(info, false);
        assertEquals("", info.getMarkup());
    }

    @Test
    public void testFilter4() {
        info.setMarkup("<script type=\"text/javascript\"><!-- content\n //--></script>");
        filter.process(info, false);
        assertEquals("", info.getMarkup());
    }

    @Test
    public void testFilter5() {
        info.setMarkup("before<script>foo</script>,bar,<script>gee</script>after");
        filter.process(info, false);
        assertEquals("before,bar,after", info.getMarkup());
    }
}
