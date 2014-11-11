/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.util;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * @author Alexandre Russel
 *
 */
public class GwtTestCSSClassManager extends GWTTestCase {
    @Override
    public String getModuleName() {
        return "org.nuxeo.ecm.platform.annotations.gwt.AnnotationPanel";
    }

    public void testCSSClassManager() {
        createDocument();
        Element element = RootPanel.get("myspan").getElement();
        assertNotNull(element);
        CSSClassManager manager = new CSSClassManager(element);
        assertNotNull(manager);
        assertTrue(manager.isClassPresent("foo"));
        assertTrue(manager.removeClass("foo"));
        assertFalse(manager.isClassPresent("foo"));
        assertTrue(manager.addClass("foo"));
        assertTrue(manager.isClassPresent("foo"));
    }

    private static void createDocument() {
        Element e = DOM.createElement("p").cast();
        SpanElement span = DOM.createSpan().cast();
        e.appendChild(span);
        e.setInnerHTML("<span id=\"myspan\" class=\"foo bar\">bob</span>");
        RootPanel.getBodyElement().appendChild(e);
    }

}
