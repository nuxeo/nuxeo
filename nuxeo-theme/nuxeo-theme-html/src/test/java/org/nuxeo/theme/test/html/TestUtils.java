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

package org.nuxeo.theme.test.html;

import java.util.Properties;

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ElementFactory;
import org.nuxeo.theme.elements.ElementFormatter;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.formats.styles.StyleFormat;
import org.nuxeo.theme.html.JSUtils;
import org.nuxeo.theme.html.Utils;
import org.nuxeo.theme.presets.CustomPresetType;
import org.nuxeo.theme.presets.PresetType;

public class TestUtils extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-contrib.xml");
    }

    @Override
    public void tearDown() throws Exception {
        Manager.getRelationStorage().clear();
        super.tearDown();
    }

    public void testCssToStyle() {
        String cssSource = "div {color: red; font: 12px Arial;} li a {text-decoration: none;}";
        StyleFormat style = new StyleFormat();

        String viewName = "vertical menu";
        Utils.loadCss(style, cssSource, viewName);

        Object[] paths = style.getPathsForView(viewName).toArray();
        assertEquals("div", paths[0]);
        assertEquals("li a", paths[1]);

        Properties props1 = style.getPropertiesFor(viewName, "div");
        assertEquals("red", props1.getProperty("color"));
        assertEquals("12px Arial", props1.getProperty("font"));

        Properties props2 = style.getPropertiesFor(viewName, "li a");
        assertEquals("none", props2.getProperty("text-decoration"));

        // make sure that old properties are removed
        cssSource = "a {color: blue;}";
        Utils.loadCss(style, cssSource, viewName);
        Properties props3 = style.getPropertiesFor(viewName, "a");
        assertEquals("blue", props3.getProperty("color"));

        assertNull(style.getPropertiesFor(viewName, "div"));
        assertNull(style.getPropertiesFor(viewName, "li a"));

        // parse empty selectors
        cssSource = " {color: violet;} li a {text-decoration: none;} {font-size: 12px;}";
        Utils.loadCss(style, cssSource, viewName);
        Properties props4 = style.getPropertiesFor(viewName, "");
        assertEquals("violet", props4.getProperty("color"));
    }

    public void testStyleToCss() {
        Style style = new StyleFormat();
        style.setUid(1);
        Properties properties1 = new Properties();
        properties1.setProperty("color", "red");
        properties1.setProperty("font", "12px Arial");
        Properties properties2 = new Properties();
        properties2.setProperty("border", "1px solid #ccc");
        style.setPropertiesFor("vertical menu", "a", properties1);
        style.setPropertiesFor("horizontal menu", "div", properties2);

        assertEquals(
                ".nxStyle1HorizontalMenu div {border:1px solid #ccc;}\n.nxStyle1VerticalMenu a {color:red;font:12px Arial;}\n",
                Utils.styleToCss(style, style.getSelectorViewNames(), false, // resolvePresets
                        false, // ignoreViewName
                        false, // ignoreClassName
                        false // indent
                ));

        assertEquals(
                ".nxStyle1 div {border:1px solid #ccc;}\n.nxStyle1 a {color:red;font:12px Arial;}\n",
                Utils.styleToCss(style, style.getSelectorViewNames(), false, // resolvePresets
                        true, // ignoreViewName
                        false, // ignoreClassName
                        false // indent
                ));

        assertEquals(
                "div {border:1px solid #ccc;}\na {color:red;font:12px Arial;}\n",
                Utils.styleToCss(style, style.getSelectorViewNames(), false, // resolvePresets
                        true, // ignoreViewName
                        true, // ignoreClassName
                        false // indent
                ));

        assertEquals(
                "div {\n  border: 1px solid #ccc;\n}\n\na {\n  color: red;\n  font: 12px Arial;\n}\n\n",
                Utils.styleToCss(style, style.getSelectorViewNames(), false, // resolvePresets
                        true, // ignoreViewName
                        true, // ignoreClassName
                        true // indent
                ));

    }

    public void testStyleToCssWithCommaSeparatedPaths() {
        Style style = new StyleFormat();
        style.setUid(1);
        Properties properties1 = new Properties();
        properties1.setProperty("color", "red");
        style.setPropertiesFor("vertical menu", "a, a:hover, a:active",
                properties1);

        assertEquals(
                ".nxStyle1VerticalMenu a, .nxStyle1VerticalMenu a:hover, .nxStyle1VerticalMenu a:active {color:red;}\n",
                Utils.styleToCss(style, style.getSelectorViewNames(), false, // resolvePresets
                        false, // ignoreViewName
                        false, // ignoreClassName
                        false // indent
                ));

        assertEquals(
                ".nxStyle1 a, .nxStyle1 a:hover, .nxStyle1 a:active {color:red;}\n",
                Utils.styleToCss(style, style.getSelectorViewNames(), false, // resolvePresets
                        true, // ignoreViewName
                        false, // ignoreClassName
                        false // indent
                ));

        assertEquals("a, a:hover, a:active {color:red;}\n", Utils.styleToCss(
                style, style.getSelectorViewNames(), false, // resolvePresets
                true, // ignoreViewName
                true, // ignoreClassName
                false // indent
        ));

        assertEquals("a, a:hover, a:active {\n  color: red;\n}\n\n",
                Utils.styleToCss(style, style.getSelectorViewNames(), false, // resolvePresets
                        true, // ignoreViewName
                        true, // ignoreClassName
                        true // indent
                ));
    }

    public void testStyleToCssWithPresets() {
        Element theme = ElementFactory.create("theme");
        theme.setName("theme1");
        Style style = Manager.getThemeManager().createStyle();
        style.setUid(1);
        ElementFormatter.setFormat(theme, style);

        Properties properties = new Properties();

        PresetType preset = new PresetType("default font", "11px Verdana",
                "test fonts", "font");
        PresetType customPreset1 = new CustomPresetType("custom color", "#f00",
                "theme1", "color");
        PresetType customPreset2 = new CustomPresetType("custom bg", "url(image.png)",
                "theme1", "background");
        Manager.getTypeRegistry().register(preset);
        Manager.getTypeRegistry().register(customPreset1);
        Manager.getTypeRegistry().register(customPreset2);

        properties.setProperty("font", "\"default font (test fonts)\"");
        properties.setProperty("color", "\"custom color\" #dc0 \"custom color\" #123");
        properties.setProperty("background", "\"custom color\" \"custom bg\" no-repeat");
        style.setPropertiesFor("horizontal menu", "a", properties);

        assertEquals(
                ".nxStyle1HorizontalMenu a {color:#f00 #dc0 #f00 #123;background:#f00 url(image.png) no-repeat;font:11px Verdana;}\n",
                Utils.styleToCss(style, style.getSelectorViewNames(), true, // resolvePresets
                        false, // ignoreViewName
                        false, // ignoreClassName
                        false // indent
                ));
    }

    public void testExtractCssColors() {
        assertEquals("#fc0", Utils.extractCssColors("#fc0").get(0));
        assertEquals("#f00", Utils.extractCssColors("#FF0000").get(0));
        assertEquals("#fc0", Utils.extractCssColors("#FFcC00").get(0));
        assertEquals("#010203", Utils.extractCssColors("rgb(1,2,3)").get(0));
        assertEquals("#010203", Utils.extractCssColors("rgb( 1, 2, 3 )").get(0));
    }

    public void testExtractCssImages() {
        assertEquals("url(image.png)", Utils.extractCssImages("url(image.png)").get(
                0));
        assertEquals("url(/image.png)",
                Utils.extractCssImages("url( /image.png )").get(0));
        assertEquals("url(/image.png)",
                Utils.extractCssImages("url  ( /image.png )").get(0));
        assertEquals("url(/image.png)", Utils.extractCssImages(
                "url  ( \" /image.png \" )").get(0));
        assertEquals("url(/image.png)", Utils.extractCssImages(
                "url  ( \' /image.png \' )").get(0));
        assertEquals("url(image1.png)", Utils.extractCssImages(
                "  url(image1.png)  ").get(0));
    }

    public void testReplaceColor() {
        assertEquals("\"orange\"", Utils.replaceColor("#fc0", "#fc0",
                "\"orange\""));
        assertEquals("\"yellow\"", Utils.replaceColor(
                "#FF0", "#ff0", "\"yellow\""));
        assertEquals("\"yellow\"", Utils.replaceColor(
                "#ffff00", "#ff0", "\"yellow\""));
        assertEquals("\"yellow\"", Utils.replaceColor(
                "rgb(255, 255,0)", "#ff0", "\"yellow\""));
    }

    public void testNamedStyles() {
        Style style1 = new StyleFormat();
        style1.setUid(1);
        Properties properties1 = new Properties();
        properties1.setProperty("color", "red");
        properties1.setProperty("font", "12px Arial");
        style1.setName("common");
        style1.setPropertiesFor("*", "a", properties1);

        assertEquals(".nxStyle1 a {color:red;font:12px Arial;}\n",
                Utils.styleToCss(style1, style1.getSelectorViewNames(), false,
                        false, false, false));
    }

    public void testToCamelCase() {
        assertEquals("camelCase", Utils.toCamelCase("camel case"));
        assertEquals("camelCase", Utils.toCamelCase("CAMEL CASE"));
        assertEquals("camelCase", Utils.toCamelCase("Camel Case"));
        assertEquals("camelCase", Utils.toCamelCase("camel_case"));
        assertEquals("camelCase", Utils.toCamelCase("camel-case"));
        assertEquals("c", Utils.toCamelCase("c"));
        assertEquals("", Utils.toCamelCase(""));
    }

    public void testToUpperCamelCase() {
        assertEquals("CamelCase", Utils.toUpperCamelCase("camel case"));
        assertEquals("CamelCase", Utils.toUpperCamelCase("CAMEL CASE"));
        assertEquals("CamelCase", Utils.toUpperCamelCase("Camel Case"));
        assertEquals("CamelCase", Utils.toUpperCamelCase("camel_case"));
        assertEquals("CamelCase", Utils.toUpperCamelCase("camel-case"));
        assertEquals("", Utils.toUpperCamelCase(""));
    }

    public void testJSCompress1() {
        assertEquals(
                "var global_variable=10;\nfunction test(){\nvar _1=0;\n};\n",
                JSUtils.compressSource("var global_variable = 10;  \n  function test() { var local_variable = 0 }"));
    }

}
