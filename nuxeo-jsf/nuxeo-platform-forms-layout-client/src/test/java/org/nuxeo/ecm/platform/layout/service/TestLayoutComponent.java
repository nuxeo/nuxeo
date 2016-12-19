/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.layout.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Map;

import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.Tag;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRowDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetReference;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetType;
import org.nuxeo.ecm.platform.forms.layout.facelets.WidgetTypeHandler;
import org.nuxeo.ecm.platform.forms.layout.facelets.plugins.TemplateWidgetTypeHandler;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.ecm.platform.layout.facelets.DummyWidgetTypeHandler;
import org.nuxeo.ecm.platform.ui.web.tag.handler.LeafFaceletHandler;
import org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

import com.sun.faces.facelets.tag.TagAttributesImpl;

/**
 * Test layout component extension points.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class TestLayoutComponent extends NXRuntimeTestCase {

    private WebLayoutManager service;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.forms.layout.core");
        deployContrib("org.nuxeo.ecm.platform.forms.layout.client", "OSGI-INF/layouts-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.forms.layout.client.tests", "layouts-test-contrib.xml");
        service = Framework.getService(WebLayoutManager.class);
        assertNotNull(service);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testLayoutRegistration() {
        LayoutDefinition dublincore = service.getLayoutDefinition("dublincore");
        assertNotNull(dublincore);
        assertEquals("dublincore", dublincore.getName());

        // test templates
        assertEquals("default_template", dublincore.getTemplate(BuiltinModes.ANY));
        assertEquals("view_template", dublincore.getTemplate(BuiltinModes.VIEW));
        assertEquals("edit_template", dublincore.getTemplate(BuiltinModes.EDIT));
        assertEquals("create_template", dublincore.getTemplate(BuiltinModes.CREATE));
        assertEquals("default_template", dublincore.getTemplate("lalal"));

        // test rows
        LayoutRowDefinition[] rows = dublincore.getRows();
        assertEquals(3, rows.length);
        assertEquals(1, rows[0].getWidgetReferences().length);
        assertEquals("title", rows[0].getWidgetReferences()[0].getName());
        assertEquals(1, rows[1].getWidgetReferences().length);
        assertEquals("description", rows[1].getWidgetReferences()[0].getName());
        assertEquals(3, rows[2].getWidgetReferences().length);
        assertEquals("creationDate", rows[2].getWidgetReferences()[0].getName());
        assertEquals("", rows[2].getWidgetReferences()[1].getName());
        assertEquals("modificationDate", rows[2].getWidgetReferences()[2].getName());
        assertEquals(3, dublincore.getColumns());

        // test widgets
        WidgetDefinition title = dublincore.getWidgetDefinition("title");
        assertNotNull(title);
        assertEquals("title", title.getName());
        assertEquals("text", title.getType());
        assertEquals("label.dublincore.title", title.getLabel(BuiltinModes.ANY));
        assertEquals("", title.getLabel(BuiltinModes.EDIT));
        assertEquals("help text", title.getHelpLabel(BuiltinModes.EDIT));
        assertTrue(title.isTranslated());
        assertTrue(title.isHandlingLabels());
        FieldDefinition[] fieldDefs = title.getFieldDefinitions();
        assertEquals(1, fieldDefs.length);
        assertNull(fieldDefs[0].getSchemaName());
        assertEquals("dc:title", fieldDefs[0].getFieldName());
        // props
        Map<String, Serializable> anyProps = title.getProperties(BuiltinModes.ANY, BuiltinModes.ANY);
        assertEquals(2, anyProps.size());
        assertEquals("styleClass", anyProps.get("styleClass"));
        assertEquals("#{!currentUser.administrator}", anyProps.get("required"));
        Map<String, Serializable> editProps = title.getProperties(BuiltinModes.EDIT, BuiltinModes.VIEW);
        assertEquals(3, editProps.size());
        assertEquals("styleClass", editProps.get("styleClass"));
        assertEquals("#{!currentUser.administrator}", editProps.get("required"));
        assertEquals("false", editProps.get("rendered"));

        WidgetDefinition subjects = dublincore.getWidgetDefinition("subjects");
        assertNotNull(subjects);
        assertEquals("subjects", subjects.getName());
        assertEquals("list", subjects.getType());
        assertEquals("list", subjects.getType());
        assertEquals("view", subjects.getMode(BuiltinModes.ANY));
        fieldDefs = subjects.getFieldDefinitions();
        assertEquals(1, fieldDefs.length);
        assertEquals("dublincore", fieldDefs[0].getSchemaName());
        assertEquals("subjects", fieldDefs[0].getFieldName());
        anyProps = subjects.getProperties(BuiltinModes.ANY, BuiltinModes.VIEW);
        assertEquals(3, anyProps.size());
        assertEquals("bar", anyProps.get("foo"));
        assertEquals("subject", anyProps.get("directory"));
        assertTrue(anyProps.get("list") instanceof String[]);
        String[] list = (String[]) anyProps.get("list");
        assertEquals(2, list.length);
        assertEquals("fooListItem", list[0]);
        assertEquals("barListItem", list[1]);
    }

    @Test
    public void testLayoutSubWidgetsRegistration() {
        LayoutDefinition testLayout = service.getLayoutDefinition("testLayout");
        assertNotNull(testLayout);
        assertEquals("testLayout", testLayout.getName());
        assertEquals(7, testLayout.getRows().length);

        WidgetDefinition withSubWidgets = testLayout.getWidgetDefinition("testWidgetWithSubWidgets");
        assertNotNull(withSubWidgets);
        WidgetDefinition[] subDefs = withSubWidgets.getSubWidgetDefinitions();
        assertEquals(1, subDefs.length);
        WidgetDefinition subDef = subDefs[0];
        assertEquals("text", subDef.getType());
        assertEquals("subwidget label", subDef.getLabel(BuiltinModes.ANY));
        WidgetReference[] subRefs = withSubWidgets.getSubWidgetReferences();
        assertEquals(0, subRefs.length);

        WidgetDefinition withSubWidgetRefs = testLayout.getWidgetDefinition("testWidgetWithSubWidgetRefs");
        subDefs = withSubWidgetRefs.getSubWidgetDefinitions();
        assertEquals(0, subDefs.length);
        subRefs = withSubWidgetRefs.getSubWidgetReferences();
        assertEquals(2, subRefs.length);
        assertNull(subRefs[0].getCategory());
        assertEquals("globalSubWidget", subRefs[0].getName());
        assertNull(subRefs[1].getCategory());
        assertEquals("testLocalSubwidget", subRefs[1].getName());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testComplexLayoutRegistration() {
        LayoutDefinition filesLayout = service.getLayoutDefinition("files");
        assertNotNull(filesLayout);
        assertEquals("files", filesLayout.getName());

        // test rows
        LayoutRowDefinition[] rows = filesLayout.getRows();
        assertEquals(1, rows.length);
        assertEquals(1, rows[0].getWidgetReferences().length);
        assertEquals("files", rows[0].getWidgetReferences()[0].getName());
        assertEquals(1, filesLayout.getColumns());

        // test widgets
        WidgetDefinition filesWidget = filesLayout.getWidgetDefinition("files");
        assertNotNull(filesWidget);
        assertEquals("complexList", filesWidget.getType());
        FieldDefinition[] fieldDefs = filesWidget.getFieldDefinitions();
        assertEquals(1, fieldDefs.length);
        assertEquals("files", fieldDefs[0].getSchemaName());
        assertEquals("files", fieldDefs[0].getFieldName());
        WidgetDefinition[] subWidgets = filesWidget.getSubWidgetDefinitions();
        assertEquals(2, subWidgets.length);
        // test blob
        assertEquals("blob", subWidgets[0].getName());
        assertEquals("file", subWidgets[0].getType());
        fieldDefs = subWidgets[0].getFieldDefinitions();
        assertEquals(1, fieldDefs.length);
        assertNull(fieldDefs[0].getSchemaName());
        assertEquals("blob", fieldDefs[0].getFieldName());
        // test filename
        assertEquals("filename", subWidgets[1].getName());
        assertEquals("text", subWidgets[1].getType());
        fieldDefs = subWidgets[1].getFieldDefinitions();
        assertEquals(1, fieldDefs.length);
        assertNull(fieldDefs[0].getSchemaName());
        assertEquals("filename", fieldDefs[0].getFieldName());
    }

    @Test
    public void testWidgetTypeRegistration() {
        WidgetType type = service.getWidgetType("test");
        assertNotNull(type);
        assertEquals("test", type.getName());
        WidgetTypeHandler handler = service.getWidgetTypeHandler(getTestTagConfig(), null, "test");
        assertNotNull(handler);
        assertEquals(DummyWidgetTypeHandler.class.getName(), handler.getClass().getName());
        assertEquals("bar1", handler.getProperty("foo1"));
        assertEquals("bar2", handler.getProperty("foo2"));
        assertNull(handler.getProperty("foo"));
    }

    @Test
    public void testWidgetTypeNullHandlerRegistration() {
        WidgetTypeHandler handler = service.getWidgetTypeHandler(getTestTagConfig(), null, "complex");
        assertNotNull(handler);
        assertEquals(TemplateWidgetTypeHandler.class.getName(), handler.getClass().getName());
        assertEquals("/widgets/complex_widget_template.xhtml", handler.getProperty("template"));
    }

    @Test
    public void testWidgetTypeNullHandlerRegistrationFromStore() {
        WidgetTypeHandler handler = service.getWidgetTypeHandler(getTestTagConfig(), "testCategory",
                "complexFromStore");
        assertNotNull(handler);
        assertEquals(TemplateWidgetTypeHandler.class.getName(), handler.getClass().getName());
        assertEquals("/widgets/complex_widget_template.xhtml", handler.getProperty("template"));
    }

    protected TagConfig getTestTagConfig() {
        TagConfig parent = new TagConfig() {
            @Override
            public String getTagId() {
                return "tagId";
            }

            @Override
            public Tag getTag() {
                return new Tag(null, "ns", "localName", "qName", new TagAttributesImpl(new TagAttribute[0]));
            }

            @Override
            public FaceletHandler getNextHandler() {
                return new LeafFaceletHandler();
            }
        };
        return TagConfigFactory.createTagConfig(parent, "id", null, new LeafFaceletHandler());
    }

    @Test
    public void testLayoutPropertiesRegistration() {
        LayoutDefinition layoutDef = service.getLayoutDefinition("layoutPropertiesTest");
        assertNotNull(layoutDef);
        assertEquals("layoutPropertiesTest", layoutDef.getName());

        assertNotNull(layoutDef.getProperties("any"));
        assertEquals("layoutPropValue", layoutDef.getProperties("any").get("layoutPropName"));

        LayoutRowDefinition[] layoutRows = layoutDef.getRows();
        assertNotNull(layoutRows);
        assertEquals(1, layoutRows.length);
        LayoutRowDefinition layoutRow = layoutRows[0];
        assertNotNull(layoutRow.getProperties("any"));
        assertEquals("layoutRowPropValue", layoutRow.getProperties("any").get("layoutRowPropName"));
    }

    @Test
    public void testLayoutColumnsRegistration() {
        LayoutDefinition layoutDef = service.getLayoutDefinition("layoutColumnsTest");
        assertNotNull(layoutDef);
        assertEquals("layoutColumnsTest", layoutDef.getName());

        LayoutRowDefinition[] layoutColumns = layoutDef.getRows();
        assertNotNull(layoutColumns);
        assertEquals(1, layoutColumns.length);
        LayoutRowDefinition layoutRow = layoutColumns[0];
        assertNotNull(layoutRow.getProperties("any"));
        assertEquals("layoutColumnPropValue", layoutRow.getProperties("any").get("layoutColumnPropName"));
    }

}
