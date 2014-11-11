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

package org.nuxeo.ecm.platform.layout.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.Layout;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRow;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOptions;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetType;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.ecm.platform.layout.facelets.DummyWidgetTypeHandler;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * Test layout service API
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class TestLayoutService extends NXRuntimeTestCase {

    private WebLayoutManager service;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.forms.layout.client.tests",
                "layouts-test-framework.xml");
        service = Framework.getService(WebLayoutManager.class);
        assertNotNull(service);
    }

    public void testWidgetType() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.forms.layout.client.tests",
                "layouts-test-contrib.xml");
        WidgetType wType = service.getWidgetType("test");
        assertEquals("test", wType.getName());
        assertEquals(2, wType.getProperties().size());
        assertEquals(DummyWidgetTypeHandler.class.getName(),
                wType.getWidgetTypeClass().getName());

        WidgetTypeDefinition wTypeDef = service.getWidgetTypeDefinition("test");
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

        List<WidgetTypeDefinition> wTypeDefs = service.getWidgetTypeDefinitions();
        assertNotNull(wTypeDefs);
        assertEquals(1, wTypeDefs.size());
        assertEquals(wTypeDef, wTypeDefs.get(0));
    }

    public void testLayout() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.forms.layout.client.tests",
                "layouts-test-contrib.xml");
        Layout layout = service.getLayout(null, "testLayout",
                BuiltinModes.VIEW, null);
        assertNotNull(layout);
        assertEquals("testLayout", layout.getName());
        assertEquals(BuiltinModes.VIEW, layout.getMode());
        assertNull(layout.getTemplate());

        // test rows
        assertEquals(1, layout.getColumns());
        LayoutRow[] rows = layout.getRows();
        assertEquals(4, rows.length);
        LayoutRow row = rows[0];

        // test widgets
        Widget[] widgets = row.getWidgets();
        assertEquals(1, widgets.length);
        Widget widget = widgets[0];
        assertNotNull(widget);
        assertEquals("testWidget", widget.getName());
        assertEquals("test", widget.getType());
        FieldDefinition[] fieldDefs = widget.getFieldDefinitions();
        assertEquals(1, fieldDefs.length);
        assertEquals("foo", fieldDefs[0].getSchemaName());
        assertEquals("bar", fieldDefs[0].getFieldName());
        assertEquals("label.test.widget", widget.getLabel());
        assertNull(widget.getHelpLabel());
        Map<String, Serializable> props = widget.getProperties();
        assertEquals(1, props.size());

        // test widget default label
        widget = rows[1].getWidgets()[0];
        assertNotNull(widget);
        assertEquals("testWidgetWithoutLabel", widget.getName());
        assertEquals("test", widget.getType());
        assertEquals("label.widget.testLayout.testWidgetWithoutLabel",
                widget.getLabel());
        assertTrue(widget.isTranslated());

        // test widget defined globally
        widget = rows[2].getWidgets()[0];
        assertNotNull(widget);
        assertEquals("globalTestWidget", widget.getName());
        assertEquals("test", widget.getType());

        // test widget with selection options
        widget = rows[3].getWidgets()[0];
        assertNotNull(widget);
        assertEquals("widgetWithSelectOptions", widget.getName());
        assertEquals("test", widget.getType());
        WidgetSelectOption[] options = widget.getSelectOptions();
        assertNotNull(options);
        assertEquals(5, options.length);
        assertFalse(options[0] instanceof WidgetSelectOptions);
        checkCommonSelectOption(options[0], null, null, "bar", "foo", null,
                null);
        assertFalse(options[1] instanceof WidgetSelectOptions);
        checkCommonSelectOption(options[1], "#{currentDocument}", "doc",
                "#{doc.id}", "#{doc.dc.title}", "false", "true");
        assertTrue(options[2] instanceof WidgetSelectOptions);
        checkMultipleSelectOption((WidgetSelectOptions) options[2],
                "#{myBean.myList}", "item", "#{item.id}", "#{item.title}",
                null, null, null, null);
        assertTrue(options[3] instanceof WidgetSelectOptions);
        checkMultipleSelectOption((WidgetSelectOptions) options[3],
                "#{documentList}", "doc", "#{doc.id}", "#{doc.dc.title}",
                "false", "true", "label", Boolean.TRUE);
        assertFalse(options[4] instanceof WidgetSelectOptions);
        checkCommonSelectOption(options[4], null, null, "bar2", "foo2", null,
                null);
    }

    protected void checkCommonSelectOption(WidgetSelectOption option,
            Object value, String var, String itemValue, String itemLabel,
            Object itemDisabled, Object itemRendered) {
        assertEquals(value, option.getValue());
        assertEquals(var, option.getVar());
        assertEquals(itemValue, option.getItemValue());
        assertEquals(itemLabel, option.getItemLabel());
        assertEquals(itemDisabled, option.getItemDisabled());
        assertEquals(itemRendered, option.getItemRendered());
    }

    protected void checkMultipleSelectOption(WidgetSelectOptions option,
            Object value, String var, String itemValue, String itemLabel,
            Object itemDisabled, Object itemRendered, String ordering,
            Boolean caseSensitive) {
        checkCommonSelectOption(option, value, var, itemValue, itemLabel,
                itemDisabled, itemRendered);
        assertEquals(ordering, option.getOrdering());
        assertEquals(caseSensitive, option.getCaseSensitive());
    }

    public void testLayoutRowSelection() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.forms.layout.client.tests",
                "layouts-listing-test-contrib.xml");
        Layout layout = service.getLayout(null, "search_listing_ajax",
                "edit_columns", "", null, false);
        LayoutRow[] rows = layout.getRows();
        assertEquals(4, rows.length);
        assertEquals("selection", rows[0].getName());
        assertEquals("title_link", rows[1].getName());
        assertEquals("modification_date", rows[2].getName());
        assertEquals("lifecycle", rows[3].getName());

        // select all by default
        layout = service.getLayout(null, "search_listing_ajax", "edit_columns",
                "", null, true);
        rows = layout.getRows();
        assertEquals(7, rows.length);
        assertEquals("selection", rows[0].getName());
        assertEquals("title_link", rows[1].getName());
        assertEquals("modification_date", rows[2].getName());
        assertEquals("lifecycle", rows[3].getName());
        assertEquals("description", rows[4].getName());
        assertEquals("subjects", rows[5].getName());
        assertEquals("rights", rows[6].getName());

        List<String> selectedRows = new ArrayList<String>();
        layout = service.getLayout(null, "search_listing_ajax", "edit_columns",
                "", selectedRows, false);
        rows = layout.getRows();
        assertEquals(1, rows.length);
        assertEquals("selection", rows[0].getName());

        // select all by default => no change
        layout = service.getLayout(null, "search_listing_ajax", "edit_columns",
                "", selectedRows, true);
        rows = layout.getRows();
        assertEquals(1, rows.length);
        assertEquals("selection", rows[0].getName());

        selectedRows.add("title_link");
        layout = service.getLayout(null, "search_listing_ajax", "edit_columns",
                "", selectedRows, false);
        rows = layout.getRows();
        assertEquals(2, rows.length);
        assertEquals("selection", rows[0].getName());
        assertEquals("title_link", rows[1].getName());

        // select all by default => no change
        layout = service.getLayout(null, "search_listing_ajax", "edit_columns",
                "", selectedRows, true);
        rows = layout.getRows();
        assertEquals(2, rows.length);
        assertEquals("selection", rows[0].getName());
        assertEquals("title_link", rows[1].getName());
    }

}
