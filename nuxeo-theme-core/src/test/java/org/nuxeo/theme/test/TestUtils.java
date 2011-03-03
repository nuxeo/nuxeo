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

package org.nuxeo.theme.test;

import java.util.Properties;

import junit.framework.TestCase;

import org.nuxeo.theme.Utils;
import org.nuxeo.theme.formats.styles.StyleFormat;

public class TestUtils extends TestCase {

    public void testCleanup() {
        assertEquals("a b c", Utils.cleanUp("\n   \t\t a \r\n  \nb  c\t"));
    }

    public void testCssToStyle() {
        String cssSource = "div {color: red; font: 12px Arial;} li a {text-decoration: none;} .action .input{color: #ffff01;}"
                + "ul a {color: #0F0;background-image:url(image.png)} a > b {color: \"green (nuxeo DM color)\"} ul {}";
        StyleFormat style = new StyleFormat();

        String viewName = "vertical menu";
        Utils.loadCss(style, cssSource, viewName);

        Object[] paths = style.getPathsForView(viewName).toArray();
        assertEquals("div", paths[0]);
        assertEquals("li a", paths[1]);
        assertEquals(".action .input", paths[2]);
        assertEquals("ul a", paths[3]);
        assertEquals("a>b", paths[4]);

        Properties props0 = style.getPropertiesFor(viewName, ".action .input");
        assertNotNull(props0);
        assertEquals("#ffff01", props0.getProperty("color"));

        Properties props1 = style.getPropertiesFor(viewName, "div");
        assertEquals("red", props1.getProperty("color"));
        assertEquals("12px Arial", props1.getProperty("font"));

        Properties props2 = style.getPropertiesFor(viewName, "li a");
        assertEquals("none", props2.getProperty("text-decoration"));

        Properties props3 = style.getPropertiesFor(viewName, "ul a");
        assertEquals("#00ff00", props3.getProperty("color"));
        assertEquals("url(image.png)", props3.getProperty("background-image"));

        Properties props4 = style.getPropertiesFor(viewName, "a>b");
        assertEquals("\"green (nuxeo DM color)\"", props4.getProperty("color"));

        Properties props5 = style.getPropertiesFor(viewName, "ul");
        assertNull(props5);

        // make sure that old properties are removed
        cssSource = "a {color: blue;}";
        Utils.loadCss(style, cssSource, viewName);
        Properties props6 = style.getPropertiesFor(viewName, "a");
        assertEquals("blue", props6.getProperty("color"));

        assertNull(style.getPropertiesFor(viewName, "div"));
        assertNull(style.getPropertiesFor(viewName, "li a"));

        // parse empty selectors
        cssSource = " {color: violet;} li a {text-decoration: none;} {font-size: 12px;}";
        Utils.loadCss(style, cssSource, viewName);
        Properties props7 = style.getPropertiesFor(viewName, "");
        assertEquals("violet", props7.getProperty("color"));
    }

}
