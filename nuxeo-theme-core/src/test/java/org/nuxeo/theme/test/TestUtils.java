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
        String cssSource = "div {color: red; font: 12px Arial;} li a {text-decoration: none;} .input{color: #ffffff;}"
                + "ul a {color: #FFFFFF;} ul {}";
        StyleFormat style = new StyleFormat();

        String viewName = "vertical menu";
        org.nuxeo.theme.Utils.loadCss(style, cssSource, viewName);

        Object[] paths = style.getPathsForView(viewName).toArray();
        assertEquals("div", paths[0]);
        assertEquals("li a", paths[1]);
        assertEquals(".input", paths[2]);
        assertEquals("ul a", paths[3]);

        Properties props0 = style.getPropertiesFor(viewName, ".input");
        assertNotNull(props0);

        Properties props1 = style.getPropertiesFor(viewName, "div");
        assertEquals("red", props1.getProperty("color"));
        assertEquals("12px Arial", props1.getProperty("font"));

        Properties props2 = style.getPropertiesFor(viewName, "li a");
        assertEquals("none", props2.getProperty("text-decoration"));

        Properties props3 = style.getPropertiesFor(viewName, "ul a");
        assertEquals("#FFFFFF", props3.getProperty("color"));

        Properties props4 = style.getPropertiesFor(viewName, "ul");
        assertEquals(null, props4.getProperty("color"));

        // make sure that old properties are removed
        cssSource = "a {color: blue;}";
        org.nuxeo.theme.Utils.loadCss(style, cssSource, viewName);
        Properties props5 = style.getPropertiesFor(viewName, "a");
        assertEquals("blue", props5.getProperty("color"));

        assertNull(style.getPropertiesFor(viewName, "div"));
        assertNull(style.getPropertiesFor(viewName, "li a"));

        // parse empty selectors
        cssSource = " {color: violet;} li a {text-decoration: none;} {font-size: 12px;}";
        org.nuxeo.theme.Utils.loadCss(style, cssSource, viewName);
        Properties props6 = style.getPropertiesFor(viewName, "");
        assertEquals("violet", props6.getProperty("color"));
    }

}
