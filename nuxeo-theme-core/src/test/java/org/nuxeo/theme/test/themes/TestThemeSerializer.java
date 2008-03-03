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

package org.nuxeo.theme.test.themes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ElementFactory;
import org.nuxeo.theme.elements.ElementFormatter;
import org.nuxeo.theme.formats.FormatFactory;
import org.nuxeo.theme.formats.layouts.Layout;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.formats.widgets.Widget;
import org.nuxeo.theme.fragments.FragmentFactory;
import org.nuxeo.theme.perspectives.PerspectiveType;
import org.nuxeo.theme.test.DummyFragment;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.themes.ThemeSerializer;

public class TestThemeSerializer extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deploy("nxthemes-core-service.xml");
        deploy("nxthemes-core-contrib.xml");
        deploy("fragment-config.xml");
        deploy("view-config.xml");
    }

    public void testSerializeTheme() throws Exception {
        Element theme = ElementFactory.create("theme");
        theme.setName("default");
        Element page = ElementFactory.create("page");
        page.setName("default");
        page.setDescription("The default page");
        Element section = ElementFactory.create("section");
        Element cell = ElementFactory.create("cell");
        DummyFragment fragment1 = (DummyFragment) FragmentFactory.create("dummy fragment");
        fragment1.setField1("value 1");
        fragment1.setField2("value 2");
        fragment1.setDescription("A dummy fragment");
        PerspectiveType perspective1 = new PerspectiveType("view", "View mode");
        PerspectiveType perspective2 = new PerspectiveType("edit", "Edit mode");
        fragment1.setVisibleInPerspective(perspective1);
        fragment1.setVisibleInPerspective(perspective2);

        DummyFragment fragment2 = (DummyFragment) FragmentFactory.create("dummy fragment");
        fragment2.setField1("value 3");

        // format the elements (e.g. with widgets)
        Widget widget0 = (Widget) FormatFactory.create("widget");
        Widget widget1 = (Widget) FormatFactory.create("widget");
        Widget widget2 = (Widget) FormatFactory.create("widget");
        Widget widget3 = (Widget) FormatFactory.create("widget");
        Widget widget4 = (Widget) FormatFactory.create("widget");

        widget0.setName("theme view");
        widget1.setName("page frame");
        widget2.setName("section frame");
        widget3.setName("cell frame");
        widget4.setName("vertical menu");

        widget0.setDescription("The theme headers");
        widget1.setDescription("This is the main page");
        widget4.setDescription("A vertical menu");

        widget0.setProperty("charset", "utf-8");
        widget0.setProperty("icon", "/nuxeo/icon.png");

        ThemeManager themeManager = Manager.getThemeManager();
        themeManager.registerFormat(widget0);
        themeManager.registerFormat(widget1);
        themeManager.registerFormat(widget2);
        themeManager.registerFormat(widget3);
        themeManager.registerFormat(widget4);

        ElementFormatter.setFormat(theme, widget0);
        ElementFormatter.setFormat(page, widget1);
        ElementFormatter.setFormat(section, widget2);
        ElementFormatter.setFormat(cell, widget3);
        ElementFormatter.setFormat(fragment1, widget4);
        ElementFormatter.setFormat(fragment2, widget4);

        Layout sectionLayout = (Layout) FormatFactory.create("layout");
        sectionLayout.setProperty("width", "100%");
        sectionLayout.setProperty("height", "300px");
        ElementFormatter.setFormat(section, sectionLayout);
        themeManager.registerFormat(sectionLayout);

        Layout cellLayout = (Layout) FormatFactory.create("layout");
        cellLayout.setProperty("width", "100px");
        ElementFormatter.setFormat(cell, cellLayout);
        themeManager.registerFormat(cellLayout);

        Style commonStyle = (Style) FormatFactory.create("style");
        commonStyle.setName("common styles");
        commonStyle.setDescription("Common styles");
        Properties a = new Properties();
        a.setProperty("color", "blue");
        a.setProperty("text-decoration", "none");
        commonStyle.setPropertiesFor("*", "a", a);
        themeManager.registerFormat(commonStyle);
        themeManager.setNamedObject("default", "style", commonStyle);

        Style sectionStyle = (Style) FormatFactory.create("style");
        themeManager.makeFormatInherit(sectionStyle, commonStyle);
        sectionStyle.setDescription("The section's style");
        Properties h1 = new Properties();
        h1.setProperty("color", "red");
        h1.setProperty("font-size", "1.2em");
        h1.setProperty("background-image", "url(\"bg.png\")");
        sectionStyle.setPropertiesFor("section frame", "h1", h1);
        themeManager.registerFormat(sectionStyle);
        ElementFormatter.setFormat(section, sectionStyle);

        theme.addChild(page).addChild(section).addChild(cell);
        cell.addChild(fragment1);
        cell.addChild(fragment2);

        assertEquals(getFileContent("themeSerializerOutput.xml"),
                new ThemeSerializer().serializeToXml(theme, 4));
    }

    private String getFileContent(String name) {
        InputStream is = null;
        StringBuffer content = new StringBuffer();
        try {
            is = getClass().getClassLoader().getResourceAsStream(name);
            Reader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(is));
                int ch;
                while ((ch = in.read()) > -1) {
                    content.append((char) ch);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    is = null;
                }
            }
        }
        return content.toString();
    }

}
