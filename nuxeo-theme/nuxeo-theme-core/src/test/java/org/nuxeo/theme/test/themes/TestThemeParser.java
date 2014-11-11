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

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.CellElement;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ElementFormatter;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.elements.SectionElement;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.formats.Format;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.fragments.Fragment;
import org.nuxeo.theme.presets.PresetManager;
import org.nuxeo.theme.presets.PresetType;
import org.nuxeo.theme.themes.ThemeDescriptor;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.themes.ThemeParser;

public class TestThemeParser extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-contrib.xml");
        deployContrib("org.nuxeo.theme.core.tests", "fragment-config.xml");

        ThemeDescriptor themeDef = new ThemeDescriptor();
        themeDef.setSrc("theme.xml");
        final boolean preload = false;
        ThemeParser.registerTheme(themeDef, preload);
    }

    public void testParseTheme() {
        ThemeManager themeManager = Manager.getThemeManager();
        ThemeElement theme1 = themeManager.getThemeByName("theme1");
        PageElement page1 = themeManager.getPageByPath("theme1/page1");
        assertNotNull(theme1);
        assertNotNull(page1);

        assertEquals("theme1", theme1.getName());
        assertEquals("page1", page1.getName());

        assertEquals("theme", theme1.getElementType().getTypeName());
        assertEquals("page", page1.getElementType().getTypeName());

        assertEquals("Theme 1", theme1.getDescription());
        assertEquals("Page 1", page1.getDescription());

        // Theme presets
        PresetType preset1 = PresetManager.getPresetByName("theme1/red");
        assertEquals("theme1", preset1.getGroup());
        assertEquals("red", preset1.getName());
        assertEquals("#f00", preset1.getValue());
        assertEquals("color", preset1.getCategory());

        PresetType preset2 = PresetManager.getPresetByName("theme1/nice background");
        assertEquals("theme1", preset2.getGroup());
        assertEquals("nice background", preset2.getName());
        assertEquals("#fff url(/skin/app/nice-bg.png) top left repeat-y",
                preset2.getValue());
        assertEquals("background", preset2.getCategory());
    }

    public void testWidgets() {
        ThemeManager themeManager = Manager.getThemeManager();
        ThemeElement theme1 = themeManager.getThemeByName("theme1");
        PageElement page1 = themeManager.getPageByPath("theme1/page1");
        Format themeWidget = ElementFormatter.getFormatFor(theme1, "widget");
        assertEquals("theme-icon.png", themeWidget.getProperty("icon"));
        assertEquals("utf-8", themeWidget.getProperty("charset"));
    }

    public void testThemeStructure() {
        ThemeManager themeManager = Manager.getThemeManager();
        ThemeElement theme1 = themeManager.getThemeByName("theme1");
        PageElement page1 = themeManager.getPageByPath("theme1/page1");
        assertFalse(theme1.hasSiblings());
        assertTrue(theme1.hasChildren());
        assertTrue(theme1.getChildren().contains(page1));

        assertEquals(7, theme1.getDescendants().size());

        SectionElement section1 = (SectionElement) page1.getChildren().get(0);
        assertEquals("header", section1.getName());

        CellElement cell2 = (CellElement) section1.getChildren().get(1);
        assertEquals("right column", cell2.getName());

        Fragment fragment1 = (Fragment) cell2.getChildren().get(0);
        assertEquals("main", fragment1.getName());
    }

    public void testStyles() {
        ThemeManager themeManager = Manager.getThemeManager();
        ThemeElement theme1 = themeManager.getThemeByName("theme1");
        PageElement page1 = themeManager.getPageByPath("theme1/page1");
        Style style1 = (Style) themeManager.getNamedObject(theme1.getName(),
                "style", "default fonts");
        assertNotNull(style1);
        assertEquals("default fonts", style1.getName());
        assertTrue(style1.getPathsForView("*").contains("h3"));
        assertEquals("green", style1.getPropertiesFor("*", "h3").getProperty(
                "color"));

        // test style inheritance
        Element fragment = (Element) page1.getChildren().get(0).getChildren().get(
                1).getChildren().get(1);
        Style fragmentStyle = (Style) ElementFormatter.getFormatByType(
                fragment, style1.getFormatType());
        assertTrue(ThemeManager.listAncestorFormatsOf(fragmentStyle).contains(
                style1));
    }

    public void testCommonStyles() {
        ThemeManager themeManager = Manager.getThemeManager();
        ThemeElement theme1 = themeManager.getThemeByName("theme1");
        PageElement page1 = themeManager.getPageByPath("theme1/page1");
        // Make sure that new common styles are created if no style selector is
        // set on a given style
        Style common1 = (Style) themeManager.getNamedObject(theme1.getName(),
                "style", "common style 1");
        assertNotNull(common1);
        assertEquals("common style 1", common1.getName());
        assertTrue(common1.getPathsForView("*").contains("div"));
        assertEquals("blue", common1.getPropertiesFor("*", "div").getProperty(
                "background-color"));
    }

    public void testCommonStylesWithInheritance() {
        ThemeManager themeManager = Manager.getThemeManager();
        ThemeElement theme1 = themeManager.getThemeByName("theme1");
        PageElement page1 = themeManager.getPageByPath("theme1/page1");
        // if the style already inherits make it inherit from a common style
        // while preserving inheritance
        Style common2 = (Style) themeManager.getNamedObject(theme1.getName(),
                "style", "common style 2");
        assertNotNull(common2);
        assertEquals("common style 2", common2.getName());
        assertTrue(common2.getPathsForView("*").contains("table"));

        assertEquals("orange",
                common2.getPropertiesFor("*", "table").getProperty(
                        "border-color"));

        Style ancestor = (Style) ThemeManager.getAncestorFormatOf(common2);
        assertEquals("default colors", ancestor.getName());
    }

}
