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
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.ElementFactory;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.formats.Format;
import org.nuxeo.theme.formats.FormatFactory;
import org.nuxeo.theme.formats.FormatFilter;
import org.nuxeo.theme.formats.FormatType;
import org.nuxeo.theme.services.ThemeService;
import org.nuxeo.theme.themes.ThemeException;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;

public class TestStyleFilterView extends NXRuntimeTestCase {
    DummyRenderingInfo info;

    FormatFilter filter;

    Format format1;

    Format format2;

    FormatType formatType;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-contrib.xml");
        deployContrib("org.nuxeo.theme.html",
                "OSGI-INF/nxthemes-html-contrib.xml");

        ThemeService themeService = (ThemeService) Framework.getRuntime().getComponent(
                ThemeService.ID);
        themeService.getRegistry("uids").clear();
        themeService.getRegistry("relations").clear();

        // create the elements to render
        PageElement page = (PageElement) ElementFactory.create("page");
        page.setUid(1);

        TypeRegistry typeRegistry = (TypeRegistry) themeService.getRegistry("types");
        formatType = (FormatType) typeRegistry.lookup(TypeFamily.FORMAT,
                "style");

        URL themeUrl = new URL(
                "nxtheme://theme/default/mode/html/theme/page/perspective");

        info = new DummyRenderingInfo(page, themeUrl);

        // set the formats
        format1 = FormatFactory.create("style");
        format1.setUid(1);
        format1.setProperty("dummy", "property");

        info.setFormat(format1);

        // set the filter
        filter = new FormatFilter();
        filter.setFormatType(formatType);
    }

    public void testFilter1() {
        info.setMarkup("<div>content</div>");
        filter.process(info, false);
        assertEquals("<div class=\"nxStyle1\">content</div>", info.getMarkup());
    }

    public void testFilter2() {
        info.setMarkup("<div style=\"color:red;\">content</div>");
        filter.process(info, false);
        assertEquals(
                "<div style=\"color:red;\" class=\"nxStyle1\">content</div>",
                info.getMarkup());
    }

    public void testFilter3() {
        info.setMarkup("<div class=\"test\">content</div>");
        filter.process(info, false);
        assertEquals("<div class=\"test nxStyle1\">content</div>",
                info.getMarkup());
    }

    public void testFilter4() {
        // Unix line termination \n
        info.setMarkup("<div\n>content\n</div>");
        filter.process(info, false);
        assertEquals("<div\n class=\"nxStyle1\">content\n</div>",
                info.getMarkup());
    }

    public void testFilter5() {
        // Win32 line termination \r\n
        info.setMarkup("<div\r\n>content\r\n</div>");
        filter.process(info, false);
        assertEquals("<div\r\n class=\"nxStyle1\">content\r\n</div>",
                info.getMarkup());
    }

    public void testFilter6() {
        // Mac line termination \r
        info.setMarkup("<div\r>content\r</div>");
        filter.process(info, false);
        assertEquals("<div\r class=\"nxStyle1\">content\r</div>",
                info.getMarkup());
    }

    public void testFilter7() {
        info.setMarkup("<div><div style=\"color:red\">content</div></div>");
        filter.process(info, false);
        assertEquals(
                "<div class=\"nxStyle1\"><div style=\"color:red\">content</div></div>",
                info.getMarkup());
    }

    public void testFilter8() {
        info.setMarkup("<div style=\"color:red\"><div style=\"color:red\">content</div></div>");
        filter.process(info, false);
        assertEquals(
                "<div style=\"color:red\" class=\"nxStyle1\"><div style=\"color:red\">content</div></div>",
                info.getMarkup());
    }

    public void testFilter9() {
        info.setMarkup("<img src=\"/logo.png\" />");
        filter.process(info, false);
        assertEquals("<img src=\"/logo.png\" class=\"nxStyle1\" />",
                info.getMarkup());
    }

    public void testFilter10() {
        info.setMarkup("<br />");
        filter.process(info, false);
        assertEquals("<br class=\"nxStyle1\" />", info.getMarkup());
    }

    public void testStyleInheritance() throws ThemeException {
        format2 = FormatFactory.create("style");
        format2.setUid(2);
        format2.setProperty("dummy", "property");

        Manager.getThemeManager().makeFormatInherit(format1, format2);

        info.setFormat(format1);

        info.setMarkup("<div></div>");
        filter.process(info, false);
        assertEquals("<div class=\"nxStyle2 nxStyle1\"></div>",
                info.getMarkup());
    }

}
