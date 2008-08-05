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

import java.net.URL;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.elements.ElementFactory;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.formats.DefaultFormat;
import org.nuxeo.theme.formats.FormatFilter;
import org.nuxeo.theme.formats.FormatType;
import org.nuxeo.theme.services.ThemeService;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;

public class TestLayoutFilterView extends NXRuntimeTestCase {
    DummyRenderingInfo info;

    FormatFilter filter;

    DefaultFormat format;

    FormatType formatType;

    TypeRegistry typeRegistry;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-contrib.xml");
        deployContrib("org.nuxeo.theme.html",
                "OSGI-INF/nxthemes-html-contrib.xml");

        // create the elements to render
        PageElement page = (PageElement) ElementFactory.create("page");
        page.setUid(1);

        ThemeService themeService = (ThemeService) Framework.getRuntime().getComponent(
                ThemeService.ID);

        URL themeUrl = new URL(
                "nxtheme://theme/default/mode/html/theme/page/perspective");

        typeRegistry = (TypeRegistry) themeService.getRegistry("types");
        formatType = (FormatType) typeRegistry.lookup(TypeFamily.FORMAT,
                "layout");

        info = new DummyRenderingInfo(page, themeUrl);

        // set the format
        format = new DefaultFormat();
        info.setFormat(format);

        // set the filter
        filter = new FormatFilter();
        filter.setFormatType(formatType);
    }

    public void testFilter1() {
        info.setMarkup("<div>content</div>");
        filter.process(info, false);
        assertEquals("<div>content</div>", info.getMarkup());
    }

    public void testFilter2() {
        info.setMarkup("<div>content</div>");
        format.setProperty("width", "100px");
        filter.process(info, false);
        assertEquals("<div style=\"width:100px;\">content</div>",
                info.getMarkup());
    }

    public void testFilter3() {
        info.setMarkup("<div>content</div>");
        format.setProperty("width", "50px");
        format.setProperty("height", "60px");
        filter.process(info, false);
        assertEquals("<div style=\"height:60px;width:50px;\">content</div>",
                info.getMarkup());
    }

    public void testFilter4() {
        info.setMarkup("<div style=\"color:red;\">content</div>");
        format.setProperty("width", "10px");
        format.setProperty("height", "20px");
        filter.process(info, false);
        assertEquals(
                "<div style=\"color:red;height:20px;width:10px;\">content</div>",
                info.getMarkup());
    }

    public void testFilter5() {
        info.setMarkup("<div class=\"test\">content</div>");
        format.setProperty("width", "10px");
        format.setProperty("height", "20px");
        filter.process(info, false);
        assertEquals(
                "<div class=\"test\" style=\"height:20px;width:10px;\">content</div>",
                info.getMarkup());
    }

    public void testFilter6() {
        // Unix line termination \n
        info.setMarkup("<div\n>content\n</div>");
        format.setProperty("width", "10px");
        filter.process(info, false);
        assertEquals("<div\n style=\"width:10px;\">content\n</div>",
                info.getMarkup());
    }

    public void testFilter7() {
        // Win32 line termination \r\n
        info.setMarkup("<div\r\n>content\r\n</div>");
        format.setProperty("width", "10px");
        filter.process(info, false);
        assertEquals("<div\r\n style=\"width:10px;\">content\r\n</div>",
                info.getMarkup());
    }

    public void testFilter8() {
        // Mac line termination \r
        info.setMarkup("<div\r>content\r</div>");
        format.setProperty("width", "10px");
        filter.process(info, false);
        assertEquals("<div\r style=\"width:10px;\">content\r</div>",
                info.getMarkup());
    }

    public void testFilter9() {
        info.setMarkup("<div><div style=\"color:red\">content</div></div>");
        format.setProperty("width", "10px");
        format.setProperty("height", "20px");
        filter.process(info, false);
        assertEquals(
                "<div style=\"height:20px;width:10px;\"><div style=\"color:red\">content</div></div>",
                info.getMarkup());
    }

    public void testFilter10() {
        info.setMarkup("<div style=\"color:red\">"
                + "<div style=\"color:red\">content</div></div>");
        format.setProperty("width", "10px");
        format.setProperty("height", "20px");
        filter.process(info, false);
        assertEquals(
                "<div style=\"color:red;height:20px;width:10px;\"><div style=\"color:red\">content</div></div>",
                info.getMarkup());
    }

    public void testFilter11() {
        info.setMarkup("<img src=\"/logo.png\" />");
        format.setProperty("width", "10px");
        format.setProperty("height", "20px");
        filter.process(info, false);
        assertEquals(
                "<img src=\"/logo.png\" style=\"height:20px;width:10px;\" />",
                info.getMarkup());
    }
}
