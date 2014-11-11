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

import java.io.IOException;
import java.util.Properties;

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.Utils;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.formats.styles.StyleFormat;
import org.nuxeo.theme.html.CSSUtils;
import org.nuxeo.theme.themes.ThemeException;

public class TestCSSUtils extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-contrib.xml");
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
                CSSUtils.styleToCss(style, style.getSelectorViewNames(), false, // ignoreViewName
                        false, // ignoreClassName
                        false // indent
                ));

        assertEquals(
                ".nxStyle1 div {border:1px solid #ccc;}\n.nxStyle1 a {color:red;font:12px Arial;}\n",
                CSSUtils.styleToCss(style, style.getSelectorViewNames(), true, // ignoreViewName
                        false, // ignoreClassName
                        false // indent
                ));

        assertEquals(
                "div {border:1px solid #ccc;}\na {color:red;font:12px Arial;}\n",
                CSSUtils.styleToCss(style, style.getSelectorViewNames(), true, // ignoreViewName
                        true, // ignoreClassName
                        false // indent
                ));

        assertEquals(
                "div {\n  border: 1px solid #ccc;\n}\n\na {\n  color: red;\n  font: 12px Arial;\n}\n\n",
                CSSUtils.styleToCss(style, style.getSelectorViewNames(), true, // ignoreViewName
                        true, // ignoreClassName
                        true // indent
                ));

    }

    public void testCollectionStyleToCss() {
        Style style = new StyleFormat();
        style.setCollection("some collection");
        style.setUid(1);
        Properties properties = new Properties();
        properties.setProperty("color", "red");
        properties.setProperty("font", "12px Arial");
        style.setPropertiesFor("vertical menu", "a", properties);

        assertEquals(
                ".someCollection1VerticalMenu a {color:red;font:12px Arial;}\n",
                CSSUtils.styleToCss(style, style.getSelectorViewNames(), false, // ignoreViewName
                        false, // ignoreClassName
                        false // indent
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
                CSSUtils.styleToCss(style, style.getSelectorViewNames(), false, // ignoreViewName
                        false, // ignoreClassName
                        false // indent
                ));

        assertEquals(
                ".nxStyle1 a, .nxStyle1 a:hover, .nxStyle1 a:active {color:red;}\n",
                CSSUtils.styleToCss(style, style.getSelectorViewNames(), true, // ignoreViewName
                        false, // ignoreClassName
                        false // indent
                ));

        assertEquals("a, a:hover, a:active {color:red;}\n",
                CSSUtils.styleToCss(style, style.getSelectorViewNames(), true, // ignoreViewName
                        true, // ignoreClassName
                        false // indent
                ));

        assertEquals("a, a:hover, a:active {\n  color: red;\n}\n\n",
                CSSUtils.styleToCss(style, style.getSelectorViewNames(), true, // ignoreViewName
                        true, // ignoreClassName
                        true // indent
                ));
    }

    public void testExtractCssColors() {
        assertEquals("#fc0", CSSUtils.extractCssColors("#fc0").get(0));
        assertEquals("#f00", CSSUtils.extractCssColors("#FF0000").get(0));
        assertEquals("#fc0", CSSUtils.extractCssColors("#FFcC00").get(0));
        assertEquals("#010203", CSSUtils.extractCssColors("rgb(1,2,3)").get(0));
        assertEquals("#010203",
                CSSUtils.extractCssColors("rgb( 1, 2, 3 )").get(0));
    }

    public void testExtractCssImages() {
        assertEquals("url(image.png)", CSSUtils.extractCssImages(
                "url(image.png)").get(0));
        assertEquals("url(/image.png)", CSSUtils.extractCssImages(
                "url( /image.png )").get(0));
        assertEquals("url(/image.png)", CSSUtils.extractCssImages(
                "url  ( /image.png )").get(0));
        assertEquals("url(/image.png)", CSSUtils.extractCssImages(
                "url  ( \" /image.png \" )").get(0));
        assertEquals("url(/image.png)", CSSUtils.extractCssImages(
                "url  ( \' /image.png \' )").get(0));
        assertEquals("url(image1.png)", CSSUtils.extractCssImages(
                "  url(image1.png)  ").get(0));
    }

    public void testReplaceColor() {
        assertEquals("\"orange\"", CSSUtils.replaceColor("#fc0", "#fc0",
                "\"orange\""));
        assertEquals("\"yellow\"", CSSUtils.replaceColor("#FF0", "#ff0",
                "\"yellow\""));
        assertEquals("\"yellow\"", CSSUtils.replaceColor("#ffff00", "#ff0",
                "\"yellow\""));
        assertEquals("\"yellow\"", CSSUtils.replaceColor("rgb(255, 255,0)",
                "#ff0", "\"yellow\""));
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
                CSSUtils.styleToCss(style1, style1.getSelectorViewNames(),
                        false, false, false));
    }

    public void testToCamelCase() {
        assertEquals("camelCase", CSSUtils.toCamelCase("camel case"));
        assertEquals("camelCase", CSSUtils.toCamelCase("CAMEL CASE"));
        assertEquals("camelCase", CSSUtils.toCamelCase("Camel Case"));
        assertEquals("camelCase", CSSUtils.toCamelCase("camel_case"));
        assertEquals("camelCase", CSSUtils.toCamelCase("camel-case"));
        assertEquals("c", CSSUtils.toCamelCase("c"));
        assertEquals("", CSSUtils.toCamelCase(""));
    }

    public void testToUpperCamelCase() {
        assertEquals("CamelCase", CSSUtils.toUpperCamelCase("camel case"));
        assertEquals("CamelCase", CSSUtils.toUpperCamelCase("CAMEL CASE"));
        assertEquals("CamelCase", CSSUtils.toUpperCamelCase("Camel Case"));
        assertEquals("CamelCase", CSSUtils.toUpperCamelCase("camel_case"));
        assertEquals("CamelCase", CSSUtils.toUpperCamelCase("camel-case"));
        assertEquals("", CSSUtils.toUpperCamelCase(""));
    }

    public void testCompressSource() throws ThemeException, IOException {
        String expected = Utils.readResourceAsString("test1-expected.css");
        String actual = CSSUtils.compressSource(Utils.readResourceAsString("test1.css"));
        assertEquals(expected, actual);
    }

    public void testFixPartialUrls() throws IOException {
        String CSS_CONTEXT_PATH = "/nuxeo/css/";
        String source = Utils.readResourceAsString("resource.css");
        String expandedSource = Utils.readResourceAsString("resource-expanded.css");
        assertEquals(expandedSource, CSSUtils.expandPartialUrls(source,
                CSS_CONTEXT_PATH));
    }

}
