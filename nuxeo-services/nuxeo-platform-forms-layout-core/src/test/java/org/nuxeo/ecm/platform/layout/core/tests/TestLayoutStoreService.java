/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.layout.core.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetType;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * Test layout service API
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.platform.forms.layout.core")
@Deploy("org.nuxeo.ecm.platform.forms.layout.core.tests:layouts-core-test-contrib.xml")
public class TestLayoutStoreService {

    @Inject
    protected LayoutStore service;

    @Inject
    protected HotDeployer hotDeployer;

    /**
     * Non-regression test for NXP-13695.
     */
    @Test
    public void testLayoutUnregister() throws Exception {
        LayoutDefinition l = service.getLayoutDefinition("testCategory", "testLayout");
        assertNotNull(l);
        assertEquals(4, l.getRows().length);

        hotDeployer.deploy("org.nuxeo.ecm.platform.forms.layout.core.tests:layouts-core-test-override-contrib.xml");

        // check override
        l = service.getLayoutDefinition("testCategory", "testLayout");
        assertNotNull(l);
        assertEquals(0, l.getRows().length);

        hotDeployer.undeploy("org.nuxeo.ecm.platform.forms.layout.core.tests:layouts-core-test-override-contrib.xml");

        // check back to original def
        l = service.getLayoutDefinition("testCategory", "testLayout");
        assertNotNull(l);
        assertEquals(4, l.getRows().length);
    }

    @Test
    public void testWidgetType() throws Exception {
        WidgetType wType = service.getWidgetType("testCategory", "test");
        assertEquals("test", wType.getName());
        assertEquals(2, wType.getProperties().size());
        assertEquals(DummyWidgetTypeHandler.class.getName(), wType.getWidgetTypeClass().getName());

        WidgetTypeDefinition wTypeDef = service.getWidgetTypeDefinition("testCategory", "test");
        assertEquals("test", wTypeDef.getName());
        assertEquals(2, wTypeDef.getProperties().size());
        assertEquals(DummyWidgetTypeHandler.class.getName(), wTypeDef.getHandlerClassName());
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
        List<LayoutDefinition> layouts = conf.getPropertyLayouts(BuiltinModes.EDIT, BuiltinModes.ANY);
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
        assertEquals(3, wTypeDefs.size());
        assertEquals(wTypeDef, wTypeDefs.get(0));
        // same contribs (aliases)
        assertEquals(wTypeDef, wTypeDefs.get(1));
    }

    /**
     * Checks that "template" widget type is the implicit one when no class is declared.
     *
     * @since 7.3
     */
    @Test
    public void testWidgetTypeNullHandler() throws Exception {
        WidgetType wType = service.getWidgetType("testCategory", "complex");
        assertNotNull(wType);
        assertEquals("complex", wType.getName());
        assertNull(wType.getWidgetTypeClass());
    }

}
