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

import java.util.Properties;

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ElementFactory;
import org.nuxeo.theme.elements.ElementFormatter;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.formats.FormatFactory;
import org.nuxeo.theme.formats.layouts.Layout;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.formats.widgets.Widget;
import org.nuxeo.theme.nodes.NodeException;
import org.nuxeo.theme.themes.ThemeException;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.themes.ThemeRepairer;

public class TestThemeRepairer extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-contrib.xml");
    }

    public void testMissingFormats() throws ThemeException, NodeException {
        ThemeManager themeManager = Manager.getThemeManager();

        ThemeElement theme = (ThemeElement) ElementFactory.create("theme");
        Element page = ElementFactory.create("page");
        Element section = ElementFactory.create("section");

        theme.addChild(page).addChild(section);

        assertTrue(ElementFormatter.getFormatsFor(theme).isEmpty());
        assertTrue(ElementFormatter.getFormatsFor(page).isEmpty());
        assertTrue(ElementFormatter.getFormatsFor(section).isEmpty());
        assertTrue(themeManager.listFormats().isEmpty());

        ThemeRepairer.repair(theme);

        // Check missing formats
        assertNull(ElementFormatter.getFormatFor(theme, "widget"));
        assertNull(ElementFormatter.getFormatFor(page, "widget"));
        assertNull(ElementFormatter.getFormatFor(section, "widget"));
        assertNull(ElementFormatter.getFormatFor(theme, "layout"));
        assertNotNull(ElementFormatter.getFormatFor(page, "layout"));
        assertNotNull(ElementFormatter.getFormatFor(section, "layout"));
        assertNull(ElementFormatter.getFormatFor(theme, "style"));
        assertNull(ElementFormatter.getFormatFor(page, "style"));
        assertNull(ElementFormatter.getFormatFor(section, "style"));
        assertFalse(themeManager.listFormats().isEmpty());
    }

    public void testLayoutProperties() throws ThemeException, NodeException {
        ThemeManager themeManager = Manager.getThemeManager();
        ThemeElement theme = (ThemeElement) ElementFactory.create("theme");
        Element page = ElementFactory.create("page");
        Element section = ElementFactory.create("section");

        theme.addChild(page).addChild(section);

        Widget widget = (Widget) FormatFactory.create("widget");
        widget.setName("section frame");
        themeManager.registerFormat(widget);
        ElementFormatter.setFormat(section, widget);

        Style style = (Style) FormatFactory.create("style");
        themeManager.registerFormat(style);

        Layout layout = (Layout) FormatFactory.create("layout");
        themeManager.registerFormat(layout);

        Properties styleProperties = new Properties();
        // Disallowed style properties
        styleProperties.setProperty("width", "50px");
        styleProperties.setProperty("height", "150px");
        // Disallowed layout properties
        layout.setProperty("margin", "1em");
        layout.setProperty("padding", "0.5em");
        // Allowed properties
        styleProperties.setProperty("color", "red");
        style.setPropertiesFor("section frame", "", styleProperties);
        ElementFormatter.setFormat(section, style);

        ThemeRepairer.repair(theme);

        // Layout-related properties are moved
        layout = (Layout) ElementFormatter.getFormatFor(section, "layout");
        assertEquals("50px", layout.getProperty("width"));
        assertEquals("150px", layout.getProperty("height"));

        // Disallowed layout properties are removed
        assertNull(layout.getProperty("padding"));
        assertNull(layout.getProperty("margin"));
        assertNull(layout.getProperty("color"));

        // Layout-related properties are removed from styles
        styleProperties = style.getPropertiesFor("section frame", "");
        assertNull(styleProperties.get("width"));
        assertNull(styleProperties.get("height"));

        // Style-related properties are preserved
        assertEquals("red", styleProperties.get("color"));
    }

    public void testStyleProperties() throws ThemeException, NodeException {
        ThemeManager themeManager = Manager.getThemeManager();
        ThemeElement theme = (ThemeElement) ElementFactory.create("theme");
        Element page = ElementFactory.create("page");
        Element section = ElementFactory.create("section");

        theme.addChild(page).addChild(section);

        Widget widget = (Widget) FormatFactory.create("widget");
        widget.setName("section frame");
        themeManager.registerFormat(widget);
        ElementFormatter.setFormat(section, widget);

        Style style = (Style) FormatFactory.create("style");
        themeManager.registerFormat(style);

        Layout layout = (Layout) FormatFactory.create("layout");
        themeManager.registerFormat(layout);

        Properties styleProperties = new Properties();
        // Disallowed style properties
        styleProperties.setProperty("color", "red");
        styleProperties.setProperty("font-size", "11px");
        // Allowed properties
        styleProperties.setProperty("border-top", "1px solid #ccc");
        styleProperties.setProperty("border-left", "1px dashed #ccc");
        styleProperties.setProperty("border-right", "none");
        styleProperties.setProperty("border-bottom", "2px solid #000");
        style.setPropertiesFor("section frame", "", styleProperties);
        ElementFormatter.setFormat(section, style);

        ThemeRepairer.repair(theme);

        // Disallowed style properties are removed
        assertNull(style.getProperty("color"));
        assertNull(style.getProperty("font-size"));

        // Allowed style properties are preserved
        assertEquals("1px solid #ccc", styleProperties.get("border-top"));
        assertEquals("1px dashed #ccc", styleProperties.get("border-left"));
        assertEquals("none", styleProperties.get("border-right"));
        assertEquals("2px solid #000", styleProperties.get("border-bottom"));
    }

    public void testCleanupEmptyStylePaths() throws ThemeException,
            NodeException {
        ThemeManager themeManager = Manager.getThemeManager();
        ThemeElement theme = (ThemeElement) ElementFactory.create("theme");
        Element page = ElementFactory.create("page");
        theme.addChild(page);

        Style style = (Style) FormatFactory.create("style");
        themeManager.registerFormat(style);

        Widget widget = (Widget) FormatFactory.create("widget");
        widget.setName("page frame");
        themeManager.registerFormat(widget);
        ElementFormatter.setFormat(page, widget);

        style.setPropertiesFor("page frame", "h1", new Properties());
        Properties styleProperties = new Properties();
        styleProperties.setProperty("color", "blue");
        style.setPropertiesFor("page frame", "h2", styleProperties);
        ElementFormatter.setFormat(page, style);

        assertTrue(style.getPathsForView("page frame").contains("h1"));
        assertTrue(style.getPathsForView("page frame").contains("h2"));

        ThemeRepairer.repair(theme);

        assertFalse(style.getPathsForView("page frame").contains("h1"));
        assertTrue(style.getPathsForView("page frame").contains("h2"));
    }

    public void testSharedStyles() throws ThemeException, NodeException {
        ThemeManager themeManager = Manager.getThemeManager();
        ThemeElement theme = (ThemeElement) ElementFactory.create("theme");
        Element page = ElementFactory.create("page");
        Element section1 = ElementFactory.create("section");
        Element section2 = ElementFactory.create("section");

        theme.addChild(page);
        page.addChild(section1);
        page.addChild(section2);

        Widget widget = (Widget) FormatFactory.create("widget");
        widget.setName("section frame");
        themeManager.registerFormat(widget);
        ElementFormatter.setFormat(section1, widget);
        ElementFormatter.setFormat(section2, widget);

        Style style = (Style) FormatFactory.create("style");
        themeManager.registerFormat(style);

        Properties styleProperties = new Properties();
        styleProperties.setProperty("width", "100%");
        styleProperties.setProperty("color", "red");

        style.setPropertiesFor("section frame", "", styleProperties);
        ElementFormatter.setFormat(section1, style);
        ElementFormatter.setFormat(section2, style);

        ThemeRepairer.repair(theme);

        // Layout property 'width' is moved to both layout1 and layout2
        Layout layout1 = (Layout) ElementFormatter.getFormatFor(section1,
                "layout");
        assertEquals("100%", layout1.getProperty("width"));

        Layout layout2 = (Layout) ElementFormatter.getFormatFor(section2,
                "layout");
        assertEquals("100%", layout2.getProperty("width"));

        // Style property 'color' is not moved.
        assertEquals(
                "red",
                style.getPropertiesFor("section frame", "").getProperty("color"));
    }

    public void testSharedStylesOnDifferentElementTypes()
            throws ThemeException, NodeException {
        ThemeManager themeManager = Manager.getThemeManager();
        ThemeElement theme = (ThemeElement) ElementFactory.create("theme");
        Element page = ElementFactory.create("page");
        Element section = ElementFactory.create("section");
        Element cell1 = ElementFactory.create("cell");
        Element cell2 = ElementFactory.create("cell");

        theme.addChild(page).addChild(section);
        section.addChild(cell1);
        section.addChild(cell2);

        Widget pageWidget = (Widget) FormatFactory.create("widget");
        pageWidget.setName("page frame");
        themeManager.registerFormat(pageWidget);
        ElementFormatter.setFormat(page, pageWidget);

        Widget sectionWidget = (Widget) FormatFactory.create("widget");
        sectionWidget.setName("section frame");
        themeManager.registerFormat(sectionWidget);
        ElementFormatter.setFormat(section, sectionWidget);

        Widget cell1Widget = (Widget) FormatFactory.create("widget");
        cell1Widget.setName("cell frame");
        themeManager.registerFormat(cell1Widget);
        ElementFormatter.setFormat(cell1, cell1Widget);

        Widget cell2Widget = (Widget) FormatFactory.create("widget");
        cell2Widget.setName("cell frame");
        themeManager.registerFormat(cell2Widget);
        ElementFormatter.setFormat(cell2, cell2Widget);

        Style style = (Style) FormatFactory.create("style");
        themeManager.registerFormat(style);

        Style inheritedStyle = (Style) FormatFactory.create("style");
        themeManager.registerFormat(inheritedStyle);
        themeManager.makeFormatInherit(style, inheritedStyle);

        Properties styleProperties = new Properties();
        styleProperties.setProperty("width", "100%");
        styleProperties.setProperty("color", "red");

        style.setPropertiesFor("page frame", "", styleProperties);
        style.setPropertiesFor("section frame", "", styleProperties);
        style.setPropertiesFor("cell frame", "", styleProperties);

        ElementFormatter.setFormat(page, style);
        ElementFormatter.setFormat(section, style);
        ElementFormatter.setFormat(cell1, style);
        ElementFormatter.setFormat(cell2, style);

        ThemeRepairer.repair(theme);

        Style pageStyle = (Style) ElementFormatter.getFormatFor(page, "style");
        Style sectionStyle = (Style) ElementFormatter.getFormatFor(section,
                "style");
        Style cell1Style = (Style) ElementFormatter.getFormatFor(cell1, "style");
        Style cell2Style = (Style) ElementFormatter.getFormatFor(cell2, "style");

        // The first style is not duplicated
        assertSame(style, pageStyle);

        // The other styles are duplicated
        assertNotSame(pageStyle, sectionStyle);
        assertNotSame(sectionStyle, cell1Style);
        assertNotSame(pageStyle, cell1Style);
        assertNotSame(sectionStyle, cell2Style);
        assertNotSame(pageStyle, cell2Style);

        assertSame(cell1Style, cell2Style);

        // Make sure that inherited styles are preserved
        assertSame(inheritedStyle, ThemeManager.getAncestorFormatOf(pageStyle));
        assertSame(inheritedStyle,
                ThemeManager.getAncestorFormatOf(sectionStyle));
        assertSame(inheritedStyle, ThemeManager.getAncestorFormatOf(cell2Style));
        assertSame(inheritedStyle, ThemeManager.getAncestorFormatOf(cell1Style));
    }

}
