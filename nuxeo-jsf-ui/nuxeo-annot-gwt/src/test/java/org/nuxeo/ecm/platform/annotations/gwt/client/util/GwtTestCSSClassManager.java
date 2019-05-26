/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
