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
 * $Id: TestLayoutService.java 28493 2008-01-04 19:51:30Z sfermigier $
 */

package org.nuxeo.ecm.platform.layout.core.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetType;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * Test layout service API
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class TestLayoutStoreService extends NXRuntimeTestCase {

    private LayoutStore service;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.forms.layout.core");
        deployContrib("org.nuxeo.ecm.platform.forms.layout.core.tests",
                "layouts-core-test-contrib.xml");
        service = Framework.getService(LayoutStore.class);
        assertNotNull(service);
    }

    @Test
    public void testWidgetType() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.forms.layout.core.tests",
                "layouts-core-test-contrib.xml");
        WidgetType wType = service.getWidgetType("testCategory", "test");
        assertEquals("test", wType.getName());
        assertEquals(2, wType.getProperties().size());
        assertEquals(DummyWidgetTypeHandler.class.getName(),
                wType.getWidgetTypeClass().getName());

        WidgetTypeDefinition wTypeDef = service.getWidgetTypeDefinition(
                "testCategory", "test");
        assertEquals("test", wTypeDef.getName());
        assertEquals(2, wTypeDef.getProperties().size());
        assertEquals(DummyWidgetTypeHandler.class.getName(),
                wTypeDef.getHandlerClassName());
        WidgetTypeConfiguration conf = wTypeDef.getConfiguration();
        assertNotNull(conf);
        assertEquals("Test widget type", conf.getTitle());
        assertEquals("<p>This is a test widget type</p>", conf.getDescription());
        assertEquals("test", conf.getDemoId());
        assertTrue(conf.isDemoPreviewEnabled());
        Map<String, List<LayoutDefinition>> fieldLayouts = conf.getFieldLayouts();
        assertNotNull(fieldLayouts);
        assertEquals(1, fieldLayouts.size());
        assertEquals(1, fieldLayouts.get(BuiltinModes.ANY).size());
        assertEquals(1, fieldLayouts.get(BuiltinModes.ANY).get(0).getColumns());

        Map<String, Serializable> confProps = conf.getConfProperties();
        assertNotNull(confProps);
        assertEquals(2, confProps.size());
        assertEquals("foo", confProps.get("confProp"));
        assertEquals("dc:title", confProps.get("sortProperty"));
        assertFalse(conf.isComplex());
        assertFalse(conf.isList());
        List<String> supportedTypes = conf.getSupportedFieldTypes();
        assertNotNull(supportedTypes);
        assertEquals(1, supportedTypes.size());
        assertEquals("string", supportedTypes.get(0));
        List<String> defaultTypes = conf.getDefaultFieldTypes();
        assertNotNull(defaultTypes);
        assertEquals(1, defaultTypes.size());
        assertEquals("string", defaultTypes.get(0));
        List<FieldDefinition> defaultFieldDefs = conf.getDefaultFieldDefinitions();
        assertNotNull(defaultFieldDefs);
        assertEquals(2, defaultFieldDefs.size());
        assertEquals("dc:title", defaultFieldDefs.get(0).getPropertyName());
        assertEquals("data.ref", defaultFieldDefs.get(1).getPropertyName());
        List<String> categories = conf.getCategories();
        assertNotNull(categories);
        assertEquals(2, categories.size());
        List<LayoutDefinition> layouts = conf.getPropertyLayouts(
                BuiltinModes.EDIT, BuiltinModes.ANY);
        assertNotNull(layouts);
        assertEquals(2, layouts.size());

        List<String> supportedControls = conf.getSupportedControls();
        assertNotNull(supportedControls);
        assertEquals(2, supportedControls.size());
        assertTrue(supportedControls.contains("requireSurroundingForm"));
        assertTrue(supportedControls.contains("handlingLabels"));
        assertTrue(conf.isHandlingLabels());

        List<WidgetTypeDefinition> wTypeDefs = service.getWidgetTypeDefinitions("testCategory");
        assertNotNull(wTypeDefs);
        assertEquals(2, wTypeDefs.size());
        assertEquals(wTypeDef, wTypeDefs.get(0));
        // same contribs (aliases)
        assertEquals(wTypeDef, wTypeDefs.get(1));
    }

}
