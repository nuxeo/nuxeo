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

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ElementFactory;
import org.nuxeo.theme.elements.ElementFormatter;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.formats.DefaultFormat;
import org.nuxeo.theme.formats.Format;
import org.nuxeo.theme.formats.FormatFactory;
import org.nuxeo.theme.formats.layouts.Layout;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.formats.styles.StyleFormat;
import org.nuxeo.theme.formats.widgets.Widget;
import org.nuxeo.theme.fragments.FragmentFactory;
import org.nuxeo.theme.perspectives.PerspectiveManager;
import org.nuxeo.theme.perspectives.PerspectiveType;
import org.nuxeo.theme.presets.PresetType;
import org.nuxeo.theme.test.DummyFragment;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.themes.ThemeParser;
import org.nuxeo.theme.types.TypeRegistry;

public class TestThemeManager extends NXRuntimeTestCase {

    private ThemeManager themeManager;

    private TypeRegistry typeRegistry;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deploy("nxthemes-core-service.xml");
        deploy("nxthemes-core-contrib.xml");
        deploy("fragment-config.xml");
        themeManager = Manager.getThemeManager();
        typeRegistry = Manager.getTypeRegistry();
    }

    @Override
    public void tearDown() throws Exception {
        Manager.getRelationStorage().clear();
        Manager.getPerspectiveManager().clear();
        Manager.getTypeRegistry().clear();
        Manager.getUidManager().clear();
        themeManager.clear();
        themeManager = null;
        typeRegistry.clear();
        typeRegistry = null;
        super.tearDown();
    }

    public void testGetThemeNames() {
        assertTrue(themeManager.getThemeNames().isEmpty());
        ThemeElement theme = new ThemeElement();
        theme.setName("default");
        themeManager.registerTheme(theme);
        assertTrue(themeManager.getThemeNames().contains("default"));
    }

    public void testGetPageNames() {
        assertTrue(themeManager.getPageNames("default").isEmpty());

        ThemeElement theme = new ThemeElement();
        theme.setName("default");
        PageElement page1 = new PageElement();
        page1.setName("page1");
        PageElement page2 = new PageElement();
        page2.setName("page2");
        theme.addChild(page1);
        theme.addChild(page2);
        themeManager.registerTheme(theme);
        assertTrue(themeManager.getPageNames("default").contains("page1"));
        assertTrue(themeManager.getPageNames("default").contains("page2"));
    }

    public void testGetThemeOf() {
        Element theme = ElementFactory.create("theme");
        Element page = ElementFactory.create("page");
        Element section = ElementFactory.create("section");
        Element cell = ElementFactory.create("cell");
        theme.addChild(page).addChild(section);

        assertSame(theme, themeManager.getThemeOf(section));
        assertSame(theme, themeManager.getThemeOf(page));
        assertSame(theme, themeManager.getThemeOf(theme));
        assertNull(themeManager.getThemeOf(cell));
    }

    public void testBelongToSameThemef() {
        Element theme1 = ElementFactory.create("theme");
        Element page11 = ElementFactory.create("page");
        Element page12 = ElementFactory.create("page");
        Element theme2 = ElementFactory.create("theme");
        Element page21 = ElementFactory.create("page");
        Element page22 = ElementFactory.create("page");
        theme1.addChild(page11);
        theme1.addChild(page12);
        theme2.addChild(page21);
        theme2.addChild(page22);

        assertTrue(themeManager.belongToSameTheme(page11, page12));
        assertTrue(themeManager.belongToSameTheme(page21, page22));
        assertFalse(themeManager.belongToSameTheme(page11, page21));
        assertFalse(themeManager.belongToSameTheme(page11, page22));
        assertFalse(themeManager.belongToSameTheme(page12, page21));
        assertFalse(themeManager.belongToSameTheme(page12, page22));
    }

    public void testDuplicateElement() throws IOException {
        Element element = ElementFactory.create("page");
        element.setName("page 1");

        Format widget = FormatFactory.create("widget");
        widget.setName("page frame");
        ElementFormatter.setFormat(element, widget);

        Element duplicate = themeManager.duplicateElement(element, false);

        assertNotNull(duplicate);
        // do not duplicate the element's name
        assertNull(duplicate.getName());

        assertFalse(duplicate.getUid().equals(element.getUid()));
        assertSame(duplicate.getElementType(), element.getElementType());

        // compare formats
        assertSame(widget,
                ElementFormatter.getFormatsFor(duplicate).iterator().next());

    }

    public void testDuplicateFragment() throws IOException {
        DummyFragment fragment = (DummyFragment) FragmentFactory.create("dummy fragment");
        fragment.setField1("value of field 1");
        fragment.setField2("value of field 2");

        Widget widget = (Widget) FormatFactory.create("widget");
        widget.setDescription("description");
        themeManager.registerFormat(widget);
        ElementFormatter.setFormat(fragment, widget);

        // Duplicate the element, relink its formats
        Element duplicate = themeManager.duplicateElement(fragment, false);
        assertNotNull(duplicate);

        // Do not duplicate the element's name
        assertNull(duplicate.getName());
        assertFalse(duplicate.getUid().equals(fragment.getUid()));
        assertSame(duplicate.getElementType(), fragment.getElementType());

        // Compare fields
        assertEquals("value of field 1",
                ((DummyFragment) duplicate).getField1());
        assertEquals("value of field 2",
                ((DummyFragment) duplicate).getField2());

        // Compare formats
        Format duplicatedFormat1 = ElementFormatter.getFormatFor(duplicate,
                "widget");
        assertSame(ElementFormatter.getFormatFor(fragment, "widget"),
                duplicatedFormat1);
        assertEquals("description", duplicatedFormat1.getDescription());

        // Duplicate element, physically duplicate its formats
        Element duplicate2 = themeManager.duplicateElement(fragment, true);
        Format duplicatedFormat2 = ElementFormatter.getFormatFor(duplicate2,
                "widget");
        assertNotSame(ElementFormatter.getFormatFor(fragment, "widget"),
                duplicatedFormat2);
        assertEquals("description", duplicatedFormat2.getDescription());
    }

    public void testDuplicateWidget() {
        Widget widget = (Widget) FormatFactory.create("widget");
        widget.setName("vertical menu");
        widget.setDescription("Description");
        Widget duplicate = (Widget) themeManager.duplicateFormat(widget);

        assertNotNull(duplicate);
        assertEquals("vertical menu", duplicate.getName());
        assertEquals("Description", duplicate.getDescription());
        assertFalse(duplicate.getUid().equals(widget.getUid()));
        assertSame(duplicate.getFormatType(), widget.getFormatType());
    }

    public void testDuplicateLayout() {
        Layout layout = (Layout) FormatFactory.create("layout");
        layout.setProperty("width", "100%");
        layout.setProperty("height", "50px");

        Layout duplicate = (Layout) themeManager.duplicateFormat(layout);

        assertEquals("100%", duplicate.getProperty("width"));
        assertEquals("50px", duplicate.getProperty("height"));
    }

    public void testDuplicateStyle() {
        Style style = (Style) FormatFactory.create("style");
        Properties properties1 = new Properties();
        properties1.setProperty("color", "red");
        style.setPropertiesFor("vertical menu", "", properties1);
        Properties properties2 = new Properties();
        properties2.setProperty("color", "green");
        style.setPropertiesFor("vertical menu", "h1", properties2);

        Style duplicate = (Style) themeManager.duplicateFormat(style);
        Properties duplicateProperties1 = duplicate.getPropertiesFor(
                "vertical menu", "");
        Properties duplicateProperties2 = duplicate.getPropertiesFor(
                "vertical menu", "h1");

        assertEquals(properties1, duplicateProperties1);
        assertEquals(properties2, duplicateProperties2);
    }

    public void testDuplicateFormatWithAncestors() {
        Style style = (Style) FormatFactory.create("style");

        Style ancestor1= (Style) FormatFactory.create("style");
        ancestor1.setName("common styles 1");

        Style ancestor2 = (Style) FormatFactory.create("style");
        ancestor2.setName("common styles 2");

        themeManager.makeFormatInherit(ancestor1, ancestor2);
        themeManager.makeFormatInherit(style, ancestor1);

        // Ancestors
        Style duplicate = (Style) themeManager.duplicateFormat(style);
        assertSame(ancestor1, themeManager.getAncestorFormatOf(duplicate));
        assertSame(ancestor2, themeManager.getAncestorFormatOf(ancestor1));
    }

    public void testListFormats() {
        DefaultFormat widget0 = (DefaultFormat) FormatFactory.create("widget");
        DefaultFormat widget1 = (DefaultFormat) FormatFactory.create("widget");
        assertTrue(themeManager.listFormats().isEmpty());
        themeManager.registerFormat(widget0);
        themeManager.registerFormat(widget1);
        assertTrue(themeManager.listFormats().contains(widget0));
        assertTrue(themeManager.listFormats().contains(widget1));
        themeManager.unregisterFormat(widget0);
        themeManager.unregisterFormat(widget1);
        assertTrue(themeManager.listFormats().isEmpty());
    }

    public void testRemoveOrphanedFormats() {
        ThemeManager themeManager = Manager.getThemeManager();
        Element theme = ElementFactory.create("theme");

        DefaultFormat widget0 = (DefaultFormat) FormatFactory.create("widget");
        themeManager.registerFormat(widget0);

        DefaultFormat style1 = (DefaultFormat) FormatFactory.create("style");
        DefaultFormat style2 = (DefaultFormat) FormatFactory.create("style");
        DefaultFormat style3 = (DefaultFormat) FormatFactory.create("style");
        style3.setName("common styles");
        themeManager.registerFormat(style1);
        themeManager.registerFormat(style2);
        themeManager.registerFormat(style3);
        themeManager.makeFormatInherit(style1, style3);
        themeManager.makeFormatInherit(style2, style3);

        ElementFormatter.setFormat(theme, widget0);
        ElementFormatter.setFormat(theme, style1);
        assertTrue(themeManager.listFormats().contains(widget0));
        assertTrue(themeManager.listFormats().contains(style1));
        assertTrue(themeManager.listFormats().contains(style2));
        assertTrue(themeManager.listFormats().contains(style3));

        themeManager.removeOrphanedFormats();
        assertTrue(themeManager.listFormats().contains(style1));
        assertFalse(themeManager.listFormats().contains(style2));
        assertTrue(themeManager.listFormats().contains(style3));

        ElementFormatter.removeFormat(theme, widget0);
        assertTrue(themeManager.listFormats().contains(widget0));

        themeManager.removeOrphanedFormats();

        assertFalse(themeManager.listFormats().contains(widget0));
        assertTrue(themeManager.listFormats().contains(style1));
        assertTrue(themeManager.listFormats().contains(style3));

        ElementFormatter.removeFormat(theme, style1);
        themeManager.removeOrphanedFormats();

        assertFalse(themeManager.listFormats().contains(style1));
        assertFalse(themeManager.listFormats().contains(style3));
    }

    public void testRemoveOrphanedFormatsOnTestTheme() {
        URL url = getClass().getClassLoader().getResource("theme.xml");
        ThemeParser.registerTheme(url);
        List<Format> formatsBefore = themeManager.listFormats();
        themeManager.removeOrphanedFormats();
        List<Format> formatsAfter = themeManager.listFormats();
        assertEquals(formatsBefore, formatsAfter);
    }

    public void testStyleInheritance() {
        ThemeManager themeManager = Manager.getThemeManager();

        Style style = new StyleFormat();
        style.setUid(1);
        Style ancestor1 = new StyleFormat();
        ancestor1.setUid(2);
        Style ancestor2 = new StyleFormat();
        ancestor2.setUid(3);
        Style ancestor3 = new StyleFormat();
        ancestor3.setUid(4);

        assertNull(themeManager.getAncestorFormatOf(style));

        themeManager.makeFormatInherit(style, ancestor1);
        assertEquals(ancestor1, themeManager.getAncestorFormatOf(style));
        assertTrue(themeManager.listAncestorFormatsOf(style).contains(ancestor1));

        assertTrue(themeManager.listAncestorFormatsOf(ancestor2).isEmpty());
        themeManager.makeFormatInherit(ancestor1, ancestor2);
        assertEquals(ancestor2, themeManager.getAncestorFormatOf(ancestor1));

        // test transitivity
        themeManager.makeFormatInherit(ancestor2, ancestor3);
        assertTrue(themeManager.listAncestorFormatsOf(style).contains(ancestor3));
    }

    public void testStyleInheritanceCycles() {
        ThemeManager themeManager = Manager.getThemeManager();

        Style style = new StyleFormat();
        style.setUid(1);
        Style ancestor1 = new StyleFormat();
        ancestor1.setUid(2);
        Style ancestor2 = new StyleFormat();
        ancestor2.setUid(3);
        Style ancestor3 = new StyleFormat();
        ancestor3.setUid(4);

        // a style cannot inherit from itself.
        themeManager.makeFormatInherit(style, style);
        assertNull(themeManager.getAncestorFormatOf(style));

        // detect direct cycles
        themeManager.makeFormatInherit(style, ancestor1);
        themeManager.makeFormatInherit(ancestor1, style);
        assertNull(themeManager.getAncestorFormatOf(ancestor1));

        // test cycle through transitivity
        themeManager.makeFormatInherit(ancestor1, ancestor2);
        themeManager.makeFormatInherit(ancestor2, ancestor3);
        themeManager.makeFormatInherit(ancestor3, ancestor1);
        assertTrue(themeManager.listAncestorFormatsOf(ancestor3).isEmpty());
    }

    public void testDestroyElement() {
        ThemeManager themeManager = Manager.getThemeManager();

        Element theme = ElementFactory.create("theme");
        Element page = ElementFactory.create("page");
        Element section = ElementFactory.create("section");

        PerspectiveType perspective = new PerspectiveType("default",
                "default perspective");
        PerspectiveManager perspectiveManager = Manager.getPerspectiveManager();

        perspectiveManager.setVisibleInPerspective(theme, perspective);
        perspectiveManager.setVisibleInPerspective(page, perspective);
        perspectiveManager.setVisibleInPerspective(section, perspective);

        DefaultFormat widget0 = (DefaultFormat) FormatFactory.create("widget");
        themeManager.registerFormat(widget0);
        ElementFormatter.setFormat(theme, widget0);

        DefaultFormat widget1 = (DefaultFormat) FormatFactory.create("widget");
        themeManager.registerFormat(widget1);
        ElementFormatter.setFormat(page, widget1);

        DefaultFormat widget3 = (DefaultFormat) FormatFactory.create("widget");
        themeManager.registerFormat(widget3);
        ElementFormatter.setFormat(section, widget3);

        theme.addChild(page).addChild(section);

        assertFalse(themeManager.listFormats().isEmpty());
        assertFalse(Manager.getRelationStorage().list().isEmpty());
        assertFalse(Manager.getUidManager().listUids().isEmpty());

        themeManager.destroyElement(theme);

        assertTrue(themeManager.listFormats().isEmpty());
        assertTrue(Manager.getRelationStorage().list().isEmpty());
        assertTrue(Manager.getUidManager().listUids().isEmpty());
    }

    public void testResolvePresets() {
        PresetType preset1 = new PresetType("orange", "#fc0", "colors", "color");
        PresetType preset2 = new PresetType("green", "#0f0", "colors", "color");
        typeRegistry.register(preset1);
        typeRegistry.register(preset2);
        assertEquals(
                "#000 #fc0 #0f0 #fff",
                themeManager.resolvePresets("#000 \"orange (colors)\" \"green (colors)\" #fff"));
        assertEquals("1px solid \"red\"",
                themeManager.resolvePresets("1px solid \"red\""));
        assertEquals("1px solid #fc0",
                themeManager.resolvePresets("1px solid \"orange (colors)\""));
        assertEquals("#fc0", themeManager.resolvePresets("\"orange (colors)\""));
    }

}
