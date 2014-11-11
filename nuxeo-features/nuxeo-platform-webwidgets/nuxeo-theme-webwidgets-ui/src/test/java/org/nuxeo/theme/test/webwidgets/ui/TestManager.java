/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.theme.test.webwidgets.ui;

import java.util.List;

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.webwidgets.DecorationType;
import org.nuxeo.theme.webwidgets.Manager;
import org.nuxeo.theme.webwidgets.ProviderType;
import org.nuxeo.theme.webwidgets.WidgetFieldType;
import org.nuxeo.theme.webwidgets.WidgetType;

public class TestManager extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core", "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.ecm.core.persistence", "OSGI-INF/persistence-service.xml");
        deployContrib("org.nuxeo.theme.webwidgets", "OSGI-INF/nxthemes-webwidgets-service.xml");
        deployContrib("org.nuxeo.theme.test.webwidgets.ui", "webwidgets-contrib.xml");
    }

    public void testGetProviderNames() {
        assertTrue(Manager.getProviderNames().contains("test"));
        assertEquals(1, Manager.getProviderNames().size());
    }

    public void testGetProviderType() {
        ProviderType providerType = Manager.getProviderType("test");
        assertEquals("test", providerType.getName());
        assertEquals("org.nuxeo.theme.webwidgets.providers.DefaultProvider",
                providerType.getClassName());
    }

    public void testGetDecorationType() {
        DecorationType decorationType = Manager.getDecorationType("test");
        assertEquals("style.css", decorationType.getResources()[0]);
    }

    public void testInitializeWidget() {
        WidgetType widgetType = Manager.getWidgetType("test widget");

        assertEquals("/skin/nxthemes-webwidgets/samples/widget-icon.png",
                widgetType.getIcon());

        assertEquals("content", widgetType.getBody());
        assertEquals("\nalert('test &');\n\n", widgetType.getScripts());
        assertEquals("\nh2 {color: red;}\n", widgetType.getStyles());

        List<WidgetFieldType> schema = widgetType.getSchema();
        assertEquals("Title", schema.get(0).getLabel());
        assertEquals("title", schema.get(0).getName());
        assertEquals("text", schema.get(0).getType());

        // textarea
        assertEquals("Text", schema.get(1).getLabel());
        assertEquals("text", schema.get(1).getName());
        assertEquals("textarea", schema.get(1).getType());

        // range
        assertEquals("Choice", schema.get(2).getLabel());
        assertEquals("choice", schema.get(2).getName());
        assertEquals("range", schema.get(2).getType());
        assertEquals("2", schema.get(2).getMin());
        assertEquals("6", schema.get(2).getMax());
        assertEquals("2", schema.get(2).getStep());

        // password
        assertEquals("Password", schema.get(3).getLabel());
        assertEquals("password", schema.get(3).getName());
        assertEquals("password", schema.get(3).getType());

        // list
        assertEquals("Category", schema.get(4).getLabel());
        assertEquals("category", schema.get(4).getName());
        assertEquals("list", schema.get(4).getType());
        List<WidgetFieldType.Option> options = schema.get(4).getOptions();
        assertEquals("all", options.get(0).getLabel());
        assertEquals("all", options.get(0).getValue());
        assertEquals("First category", options.get(1).getLabel());
        assertEquals("1st", options.get(1).getValue());
        assertEquals("Second category", options.get(2).getLabel());
        assertEquals("2nd", options.get(2).getValue());
    }

    public void testGetPanelDecoration() {
        assertEquals("<div>region name</div>", Manager.addPanelDecoration(
                "test", "view", "region name", "Content here"));
        assertEquals("<div>Content here</div>", Manager.addPanelDecoration(
                "test", "edit", "region name", "Content here"));
    }

}
