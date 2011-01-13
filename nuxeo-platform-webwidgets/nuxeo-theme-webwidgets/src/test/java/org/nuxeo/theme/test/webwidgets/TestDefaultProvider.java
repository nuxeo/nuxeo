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

package org.nuxeo.theme.test.webwidgets;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.nuxeo.theme.webwidgets.Provider;
import org.nuxeo.theme.webwidgets.ProviderException;
import org.nuxeo.theme.webwidgets.Widget;
import org.nuxeo.theme.webwidgets.WidgetState;

public class TestDefaultProvider extends TestCase {

    public void testCreateWidget() throws ProviderException {
        Provider provider = new FakeDefaultProvider();
        Widget widget1 = provider.createWidget("test widget");
        Widget widget2 = provider.createWidget("test widget 2");
        assertEquals("test widget", widget1.getName());
        assertEquals("test widget 2", widget2.getName());
        assertEquals("0", widget1.getUid());
        assertEquals("1", widget2.getUid());
    }

    public void testGetWidgetByUid() throws ProviderException {
        Provider provider = new FakeDefaultProvider();
        Widget widget1 = provider.createWidget("test widget");
        Widget widget2 = provider.createWidget("test widget 2");
        assertEquals(widget1, provider.getWidgetByUid("0"));
        assertEquals(widget2, provider.getWidgetByUid("1"));
    }

    public void testAddAndGetWidgets() throws ProviderException {
        Provider provider = new FakeDefaultProvider();
        Widget widget1 = provider.createWidget("test widget");
        Widget widget2 = provider.createWidget("test widget 2");
        provider.addWidget(widget1, "region A", 0);
        provider.addWidget(widget2, "region B", 0);
        assertEquals("region A", provider.getRegionOfWidget(widget1));
        assertEquals("region B", provider.getRegionOfWidget(widget2));
        assertTrue(provider.getWidgets("region A").contains(widget1));
        assertTrue(provider.getWidgets("region B").contains(widget2));
        assertFalse(provider.getWidgets("region A").contains(widget2));
        assertFalse(provider.getWidgets("region B").contains(widget1));

        assertEquals(0, provider.getWidgets("region A").indexOf(widget1));
        assertEquals(0, provider.getWidgets("region B").indexOf(widget2));

        Widget widget3 = provider.createWidget("test widget 2");
        provider.addWidget(widget3, "region A", 1);
        assertEquals(1, provider.getWidgets("region A").indexOf(widget3));
    }

    public void testReorderWidget() throws ProviderException {
        Provider provider = new FakeDefaultProvider();
        Widget widget1 = provider.createWidget("test widget");
        Widget widget2 = provider.createWidget("test widget");
        Widget widget3 = provider.createWidget("test widget");
        provider.addWidget(widget1, "region A", 0);
        provider.addWidget(widget2, "region A", 1);
        provider.addWidget(widget3, "region A", 2);

        assertEquals(0, provider.getWidgets("region A").indexOf(widget1));
        assertEquals(1, provider.getWidgets("region A").indexOf(widget2));
        assertEquals(2, provider.getWidgets("region A").indexOf(widget3));

        provider.reorderWidget(widget2, 0);
        assertEquals(0, provider.getWidgets("region A").indexOf(widget2));
        assertEquals(1, provider.getWidgets("region A").indexOf(widget1));
        assertEquals(2, provider.getWidgets("region A").indexOf(widget3));

        provider.reorderWidget(widget3, 1);
        assertEquals(0, provider.getWidgets("region A").indexOf(widget2));
        assertEquals(1, provider.getWidgets("region A").indexOf(widget3));
        assertEquals(2, provider.getWidgets("region A").indexOf(widget1));

        provider.reorderWidget(widget2, 2);
        assertEquals(0, provider.getWidgets("region A").indexOf(widget3));
        assertEquals(1, provider.getWidgets("region A").indexOf(widget1));
        assertEquals(2, provider.getWidgets("region A").indexOf(widget2));

        provider.reorderWidget(widget3, 2);
        assertEquals(0, provider.getWidgets("region A").indexOf(widget1));
        assertEquals(1, provider.getWidgets("region A").indexOf(widget2));
        assertEquals(2, provider.getWidgets("region A").indexOf(widget3));
    }

    public void testRemoveWidget() throws ProviderException {
        Provider provider = new FakeDefaultProvider();
        Widget widget1 = provider.createWidget("test widget");
        Widget widget2 = provider.createWidget("test widget");
        Widget widget3 = provider.createWidget("test widget");
        provider.addWidget(widget1, "region A", 0);
        provider.addWidget(widget2, "region A", 1);
        provider.addWidget(widget3, "region A", 2);

        assertEquals(0, provider.getWidgets("region A").indexOf(widget1));
        assertEquals(1, provider.getWidgets("region A").indexOf(widget2));
        assertEquals(2, provider.getWidgets("region A").indexOf(widget3));

        provider.removeWidget(widget2);
        assertEquals(0, provider.getWidgets("region A").indexOf(widget1));
        assertEquals(1, provider.getWidgets("region A").indexOf(widget3));

        provider.removeWidget(widget1);
        assertEquals(0, provider.getWidgets("region A").indexOf(widget3));

        provider.removeWidget(widget3);
        assertTrue(provider.getWidgets("region A").isEmpty());
    }

    public void testMoveWidget() throws ProviderException {
        Provider provider = new FakeDefaultProvider();
        Widget widget1 = provider.createWidget("test widget");
        Widget widget2 = provider.createWidget("test widget");
        Widget widget3 = provider.createWidget("test widget");
        provider.addWidget(widget1, "region A", 0);
        provider.addWidget(widget2, "region A", 1);
        provider.addWidget(widget3, "region A", 2);

        assertEquals(0, provider.getWidgets("region A").indexOf(widget1));
        assertEquals(1, provider.getWidgets("region A").indexOf(widget2));
        assertEquals(2, provider.getWidgets("region A").indexOf(widget3));

        provider.moveWidget(widget1, "region A", 1);
        assertEquals(0, provider.getWidgets("region A").indexOf(widget2));
        assertEquals(1, provider.getWidgets("region A").indexOf(widget1));
        assertEquals(2, provider.getWidgets("region A").indexOf(widget3));

        provider.moveWidget(widget3, "region A", 0);
        assertEquals(0, provider.getWidgets("region A").indexOf(widget3));
        assertEquals(1, provider.getWidgets("region A").indexOf(widget2));
        assertEquals(2, provider.getWidgets("region A").indexOf(widget1));

        provider.moveWidget(widget3, "region A", 0);
        assertEquals(0, provider.getWidgets("region A").indexOf(widget3));
        assertEquals(1, provider.getWidgets("region A").indexOf(widget2));
        assertEquals(2, provider.getWidgets("region A").indexOf(widget1));

        provider.moveWidget(widget2, "region A", 2);
        assertEquals(0, provider.getWidgets("region A").indexOf(widget3));
        assertEquals(1, provider.getWidgets("region A").indexOf(widget1));
        assertEquals(2, provider.getWidgets("region A").indexOf(widget2));

        provider.moveWidget(widget1, "region B", 0);
        assertEquals(0, provider.getWidgets("region A").indexOf(widget3));
        assertEquals(1, provider.getWidgets("region A").indexOf(widget2));
        assertEquals(0, provider.getWidgets("region B").indexOf(widget1));

        provider.moveWidget(widget2, "region B", 0);
        assertEquals(0, provider.getWidgets("region A").indexOf(widget3));
        assertEquals(0, provider.getWidgets("region B").indexOf(widget2));
        assertEquals(1, provider.getWidgets("region B").indexOf(widget1));
    }

    public void testState() throws ProviderException {
        Provider provider = new FakeDefaultProvider();
        Widget widget = provider.createWidget("test widget");
        provider.setWidgetState(widget, WidgetState.DEFAULT);
        assertEquals(WidgetState.DEFAULT, provider.getWidgetState(widget));
        provider.setWidgetState(widget, WidgetState.SHADED);
        assertEquals(WidgetState.SHADED, provider.getWidgetState(widget));
    }

    public void testPreferences() throws ProviderException {
        Provider provider = new FakeDefaultProvider();
        Widget widget = provider.createWidget("test widget");
        Map<String, String> preferences = new HashMap<String, String>();
        preferences.put("key1", "value 1");
        preferences.put("key2", "value 2");
        preferences.put("key3", "value 3");
        provider.setWidgetPreferences(widget, preferences);
        Map<String, String> retrievedPreferences = provider.getWidgetPreferences(widget);
        assertEquals("value 1", retrievedPreferences.get("key1"));
        assertEquals("value 2", retrievedPreferences.get("key2"));
        assertEquals("value 3", retrievedPreferences.get("key3"));
    }

}
