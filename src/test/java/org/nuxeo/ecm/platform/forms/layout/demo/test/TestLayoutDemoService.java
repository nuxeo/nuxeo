/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.demo.test;

import java.util.List;

import org.nuxeo.ecm.platform.forms.layout.demo.service.DemoLayout;
import org.nuxeo.ecm.platform.forms.layout.demo.service.DemoWidgetType;
import org.nuxeo.ecm.platform.forms.layout.demo.service.LayoutDemoManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Anahide Tchertchian
 */
public class TestLayoutDemoService extends NXRuntimeTestCase {

    protected LayoutDemoManager service;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployContrib("org.nuxeo.ecm.platform.forms.layout.demo",
                "OSGI-INF/layout-demo-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.forms.layout.demo",
                "OSGI-INF/layout-demo-contrib.xml");

        service = Framework.getService(LayoutDemoManager.class);
    }

    public void testService() throws Exception {
        assertNotNull(service);
    }

    @Override
    public void tearDown() throws Exception {
        service = null;

        undeployContrib("org.nuxeo.ecm.platform.forms.layout.demo",
                "OSGI-INF/layout-demo-contrib.xml");
        undeployContrib("org.nuxeo.ecm.platform.forms.layout.demo",
                "OSGI-INF/layout-demo-framework.xml");

        super.tearDown();
    }

    public void testRegistration() throws Exception {
        DemoWidgetType stringWidget = service.getWidgetType("string");
        assertEquals("string", stringWidget.getName());
        assertEquals("String widget", stringWidget.getLabel());
        assertEquals("stringWidget", stringWidget.getViewId());
        assertEquals(LayoutDemoManager.APPLICATION_PATH + "stringWidget",
                stringWidget.getUrl());
        assertEquals("standard", stringWidget.getCategory());
        List<DemoLayout> demoLayouts = stringWidget.getDemoLayouts();
        assertNotNull(demoLayouts);
        assertEquals(1, demoLayouts.size());
        assertEquals("stringWidgetLayout", demoLayouts.get(0).getName());
        assertEquals(LayoutDemoManager.APPLICATION_PATH
                + "sources/OSGI-INF/demo/layout-demo-string-widget.xml",
                demoLayouts.get(0).getSourcePath());

        DemoWidgetType textareaWidget = service.getWidgetType("textarea");
        assertEquals("textarea", textareaWidget.getName());
        assertEquals("Textarea widget", textareaWidget.getLabel());
        assertEquals("textareaWidget", textareaWidget.getViewId());
        assertEquals(LayoutDemoManager.APPLICATION_PATH + "textareaWidget",
                textareaWidget.getUrl());
        assertEquals("standard", textareaWidget.getCategory());
        demoLayouts = textareaWidget.getDemoLayouts();
        assertNotNull(demoLayouts);
        assertEquals(1, demoLayouts.size());
        assertEquals("textareaWidgetLayout", demoLayouts.get(0).getName());
        assertEquals(LayoutDemoManager.APPLICATION_PATH
                + "sources/OSGI-INF/demo/layout-demo-textarea-widget.xml",
                demoLayouts.get(0).getSourcePath());
    }

    public void testGetWidgetTypeByViewId() throws Exception {
        DemoWidgetType stringWidget = service.getWidgetTypeByViewId("stringWidget");
        assertEquals("string", stringWidget.getName());
        DemoWidgetType textareaWidget = service.getWidgetTypeByViewId("textareaWidget");
        assertEquals("textarea", textareaWidget.getName());

    }

    public void testGetWidgetTypesByCategory() throws Exception {
        List<DemoWidgetType> widgets = service.getWidgetTypes("standard");
        assertNotNull(widgets);
        assertTrue(widgets.size() >= 2);
        DemoWidgetType stringWidget = widgets.get(0);
        assertEquals("string", stringWidget.getName());
        DemoWidgetType textareaWidget = widgets.get(1);
        assertEquals("textarea", textareaWidget.getName());
    }

}
