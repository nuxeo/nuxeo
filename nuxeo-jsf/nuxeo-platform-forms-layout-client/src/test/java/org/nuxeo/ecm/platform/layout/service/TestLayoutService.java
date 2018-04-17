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
package org.nuxeo.ecm.platform.layout.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinWidgetModes;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.Layout;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRow;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOptions;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetType;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.impl.FieldDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.ecm.platform.layout.facelets.DummyWidgetTypeHandler;
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
@Deploy("org.nuxeo.ecm.platform.forms.layout.client:OSGI-INF/layouts-framework.xml")
public class TestLayoutService {

    @Inject
    public WebLayoutManager service;

    @Inject
    protected HotDeployer hotDeployer;

    @Test
    @Deploy("org.nuxeo.ecm.platform.forms.layout.client.tests:layouts-test-contrib.xml")
    public void testWidgetType() throws Exception {

        WidgetType wType = service.getWidgetType("test");
        assertEquals("test", wType.getName());
        assertEquals(2, wType.getProperties().size());
        assertEquals(DummyWidgetTypeHandler.class.getName(), wType.getWidgetTypeClass().getName());

        WidgetTypeDefinition wTypeDef = service.getWidgetTypeDefinition("test");
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

        List<WidgetTypeDefinition> wTypeDefs = service.getWidgetTypeDefinitions();
        assertNotNull(wTypeDefs);
        assertEquals(2, wTypeDefs.size());
        assertEquals(wTypeDef, wTypeDefs.get(0));
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.forms.layout.client.tests:layouts-test-contrib.xml")
    public void testLayoutType() throws Exception {

        LayoutTypeDefinition lType = service.getLayoutTypeDefinition("myLayoutType");
        assertNotNull(lType);
        assertEquals("myLayoutType", lType.getName());
        Map<String, String> templates = lType.getTemplates();
        assertNotNull(templates);
        assertEquals(3, templates.size());
        LayoutTypeConfiguration conf = lType.getConfiguration();
        assertNotNull(conf);
        Map<String, Map<String, Serializable>> defaultProps = conf.getDefaultPropertyValues();
        assertNotNull(defaultProps);
        assertEquals(1, defaultProps.size());

        hotDeployer.deploy("org.nuxeo.ecm.platform.forms.layout.client.tests:layouts-listing-test-contrib.xml");

        LayoutDefinition ldef = service.getLayoutDefinition("search_listing_ajax_with_type");
        assertNotNull(ldef);
        assertEquals("search_listing_ajax_with_type", ldef.getName());
        assertEquals("listing", ldef.getType());
        assertNull(ldef.getTypeCategory());
        Map<String, String> ltemplates = ldef.getTemplates();
        assertNotNull(ltemplates);
        assertEquals(0, ltemplates.size());
        Map<String, Map<String, Serializable>> lprops = ldef.getProperties();
        assertNotNull(lprops);
        assertEquals(0, lprops.size());

        Layout layout = service.getLayout(null, "search_listing_ajax_with_type", BuiltinModes.VIEW, null);
        assertNotNull(layout);
        assertEquals("search_listing_ajax_with_type", layout.getName());
        assertEquals("listing", layout.getType());
        assertEquals("jsf", layout.getTypeCategory());
        assertEquals("/layouts/layout_listing_template.xhtml", layout.getTemplate());
        Map<String, Serializable> props = layout.getProperties();
        assertNotNull(props);
        assertEquals(2, props.size());
        assertEquals("true", props.get("showRowEvenOddClass"));
        assertEquals("true", props.get("showListingHeader"));

        Layout csvLayout = service.getLayout(null, "search_listing_ajax_with_type", BuiltinModes.CSV, null);
        assertNotNull(csvLayout);
        assertEquals("search_listing_ajax_with_type", layout.getName());
        assertEquals("listing", layout.getType());
        assertEquals("jsf", layout.getTypeCategory());
        assertEquals("/layouts/layout_listing_csv_template.xhtml", csvLayout.getTemplate());
        Map<String, Serializable> csvprops = csvLayout.getProperties();
        assertNotNull(csvprops);
        assertEquals(2, csvprops.size());
        assertEquals("true", csvprops.get("showRowEvenOddClass"));
        assertEquals("true", csvprops.get("showListingHeader"));

        Layout editColumnsLayout = service.getLayout(null, "search_listing_ajax_with_type", "edit_columns", null);
        assertNotNull(editColumnsLayout);
        assertEquals("search_listing_ajax_with_type", layout.getName());
        assertEquals("listing", layout.getType());
        assertEquals("jsf", layout.getTypeCategory());
        assertEquals("/layouts/layout_listing_template.xhtml", editColumnsLayout.getTemplate());
        Map<String, Serializable> editColumnsProps = editColumnsLayout.getProperties();
        assertNotNull(editColumnsProps);
        assertEquals(8, editColumnsProps.size());
        assertEquals("false", editColumnsProps.get("displayAlwaysSelectedColumns"));
        assertEquals("true", editColumnsProps.get("columnSelectionRequired"));
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.forms.layout.client.tests:layouts-test-contrib.xml")
    public void testLayout() throws Exception {

        Layout layout = service.getLayout(null, "testLayout", BuiltinModes.VIEW, null);
        assertNotNull(layout);
        assertEquals("testLayout", layout.getName());
        assertEquals(BuiltinModes.VIEW, layout.getMode());
        assertNull(layout.getTemplate());

        // test rows
        assertEquals(1, layout.getColumns());
        LayoutRow[] rows = layout.getRows();
        assertEquals(7, rows.length);
        LayoutRow row = rows[0];

        // test widgets
        Widget[] widgets = row.getWidgets();
        assertEquals(1, widgets.length);
        Widget widget = widgets[0];
        assertNotNull(widget);
        assertEquals("testWidget", widget.getName());
        assertEquals("test", widget.getType());
        assertEquals("jsf", widget.getTypeCategory());
        assertTrue(widget.isTranslated());
        assertTrue(widget.isHandlingLabels());
        FieldDefinition[] fieldDefs = widget.getFieldDefinitions();
        assertEquals(1, fieldDefs.length);
        assertEquals("foo", fieldDefs[0].getSchemaName());
        assertEquals("bar", fieldDefs[0].getFieldName());
        assertEquals("label.test.widget", widget.getLabel());
        assertNull(widget.getHelpLabel());
        Map<String, Serializable> props = widget.getProperties();
        assertEquals(2, props.size());
        assertEquals("cssClass", props.get("styleClass"));
        // prop set by default on type
        assertEquals("true", props.get("rendered"));

        // test widget default label
        widget = rows[1].getWidgets()[0];
        assertNotNull(widget);
        assertEquals("testWidgetWithoutLabel", widget.getName());
        assertEquals("test", widget.getType());
        assertEquals("jsf", widget.getTypeCategory());
        assertEquals("label.widget.testLayout.testWidgetWithoutLabel", widget.getLabel());
        assertTrue(widget.isTranslated());

        // test widget defined globally
        widget = rows[2].getWidgets()[0];
        assertNotNull(widget);
        assertEquals("globalTestWidget", widget.getName());
        assertEquals("test", widget.getType());
        assertEquals("jsf", widget.getTypeCategory());

        // test widget defined globally on another category
        widget = rows[3].getWidgets()[0];
        assertNotNull(widget);
        assertEquals("globalTestWidgetWithTestCategory", widget.getName());
        assertEquals("test", widget.getType());
        assertEquals("jsf", widget.getTypeCategory());

        // test widget with selection options
        widget = rows[4].getWidgets()[0];
        assertNotNull(widget);
        assertEquals("widgetWithSelectOptions", widget.getName());
        assertEquals("test", widget.getType());
        assertEquals("jsf", widget.getTypeCategory());
        WidgetSelectOption[] options = widget.getSelectOptions();
        assertNotNull(options);
        assertEquals(5, options.length);
        assertFalse(options[0] instanceof WidgetSelectOptions);
        checkCommonSelectOption(options[0], null, null, "bar", "foo", null, null);
        assertFalse(options[1] instanceof WidgetSelectOptions);
        checkCommonSelectOption(options[1], "#{currentDocument}", "doc", "#{doc.id}", "#{doc.dc.title}", "false",
                "true");
        assertTrue(options[2] instanceof WidgetSelectOptions);
        checkMultipleSelectOption((WidgetSelectOptions) options[2], "#{myBean.myList}", "item", "#{item.id}",
                "#{item.title}", null, null, null, null);
        assertTrue(options[3] instanceof WidgetSelectOptions);
        checkMultipleSelectOption((WidgetSelectOptions) options[3], "#{documentList}", "doc", "#{doc.id}",
                "#{doc.dc.title}", "false", "true", "label", Boolean.TRUE);
        assertFalse(options[4] instanceof WidgetSelectOptions);
        checkCommonSelectOption(options[4], null, null, "bar2", "foo2", null, null);

        // test widget with subwidgets
        widget = rows[5].getWidgets()[0];
        assertNotNull(widget);
        assertEquals("testWidgetWithSubWidgets", widget.getName());
        assertEquals("test", widget.getType());
        assertEquals("jsf", widget.getTypeCategory());
        Widget[] subWidgets = widget.getSubWidgets();
        assertNotNull(subWidgets);
        assertEquals(1, subWidgets.length);
        Widget subWidget = subWidgets[0];
        assertEquals("subwidget", subWidget.getName());
        assertEquals("text", subWidget.getType());
        assertEquals("jsf", subWidget.getTypeCategory());

        // test widget with subbwidget refs
        widget = rows[6].getWidgets()[0];
        assertNotNull(widget);
        assertEquals("testWidgetWithSubWidgetRefs", widget.getName());
        assertEquals("test", widget.getType());
        assertEquals("jsf", widget.getTypeCategory());
        subWidgets = widget.getSubWidgets();
        assertNotNull(subWidgets);
        assertEquals(2, subWidgets.length);
        subWidget = subWidgets[0];
        assertEquals("globalSubWidget", subWidget.getName());
        assertEquals("test", subWidget.getType());
        assertEquals("jsf", subWidget.getTypeCategory());
        subWidget = subWidgets[1];
        assertEquals("testLocalSubwidget", subWidget.getName());
        assertEquals("test", subWidget.getType());
        assertEquals("jsf", subWidget.getTypeCategory());
    }

    protected void checkCommonSelectOption(WidgetSelectOption option, Object value, String var, String itemValue,
            String itemLabel, Object itemDisabled, Object itemRendered) {
        assertEquals(value, option.getValue());
        assertEquals(var, option.getVar());
        assertEquals(itemValue, option.getItemValue());
        assertEquals(itemLabel, option.getItemLabel());
        assertEquals(itemDisabled, option.getItemDisabled());
        assertEquals(itemRendered, option.getItemRendered());
    }

    protected void checkMultipleSelectOption(WidgetSelectOptions option, Object value, String var, String itemValue,
            String itemLabel, Object itemDisabled, Object itemRendered, String ordering, Boolean caseSensitive) {
        checkCommonSelectOption(option, value, var, itemValue, itemLabel, itemDisabled, itemRendered);
        assertEquals(ordering, option.getOrdering());
        assertEquals(caseSensitive, option.getCaseSensitive());
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.forms.layout.client.tests:layouts-listing-test-contrib.xml")
    public void testLayoutRowSelection() throws Exception {

        Layout layout = service.getLayout(null, "search_listing_ajax", "edit_columns", "", null, false);
        LayoutRow[] rows = layout.getRows();
        assertEquals(4, rows.length);
        assertEquals("selection", rows[0].getName());
        assertEquals("title_link", rows[1].getName());
        assertEquals("modification_date", rows[2].getName());
        assertEquals("lifecycle", rows[3].getName());

        // select all by default
        layout = service.getLayout(null, "search_listing_ajax", "edit_columns", "", null, true);
        rows = layout.getRows();
        assertEquals(7, rows.length);
        assertEquals("selection", rows[0].getName());
        assertEquals("title_link", rows[1].getName());
        assertEquals("modification_date", rows[2].getName());
        assertEquals("lifecycle", rows[3].getName());
        assertEquals("description", rows[4].getName());
        assertEquals("subjects", rows[5].getName());
        assertEquals("rights", rows[6].getName());

        List<String> selectedRows = new ArrayList<>();
        layout = service.getLayout(null, "search_listing_ajax", "edit_columns", "", selectedRows, false);
        rows = layout.getRows();
        assertEquals(1, rows.length);
        assertEquals("selection", rows[0].getName());

        // select all by default => no change
        layout = service.getLayout(null, "search_listing_ajax", "edit_columns", "", selectedRows, true);
        rows = layout.getRows();
        assertEquals(1, rows.length);
        assertEquals("selection", rows[0].getName());

        selectedRows.add("title_link");
        layout = service.getLayout(null, "search_listing_ajax", "edit_columns", "", selectedRows, false);
        rows = layout.getRows();
        assertEquals(2, rows.length);
        assertEquals("selection", rows[0].getName());
        assertEquals("title_link", rows[1].getName());

        // select all by default => no change
        layout = service.getLayout(null, "search_listing_ajax", "edit_columns", "", selectedRows, true);
        rows = layout.getRows();
        assertEquals(2, rows.length);
        assertEquals("selection", rows[0].getName());
        assertEquals("title_link", rows[1].getName());
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.forms.layout.client.tests:layouts-test-contrib.xml")
    public void testWidgetModeProperties() throws Exception {

        Layout editLayout = service.getLayout(null, "testWidgetModeProperties", BuiltinModes.EDIT, "", null, false);
        assertNotNull(editLayout);
        Widget editWidget = editLayout.getWidget("testWidgetMode");
        assertNotNull(editWidget);
        assertEquals("testWidgetMode", editWidget.getName());
        assertEquals(BuiltinWidgetModes.EDIT, editWidget.getMode());
        assertEquals("layout", editWidget.getType());
        assertEquals("jsf", editWidget.getTypeCategory());
        Map<String, Serializable> editProps = editWidget.getProperties();
        assertNotNull(editProps);
        assertEquals(2, editProps.size());
        assertEquals("layout_in_a_widget", editProps.get("name"));
        assertEquals(BuiltinWidgetModes.EDIT, editProps.get("mode"));

        Layout viewLayout = service.getLayout(null, "testWidgetModeProperties", BuiltinModes.VIEW, "", null, false);
        assertNotNull(viewLayout);
        Widget viewWidget = viewLayout.getWidget("testWidgetMode");
        assertNotNull(viewWidget);
        assertEquals("testWidgetMode", viewWidget.getName());
        assertEquals(BuiltinWidgetModes.VIEW, viewWidget.getMode());
        assertEquals("layout", viewWidget.getType());
        Map<String, Serializable> viewProps = viewWidget.getProperties();
        assertNotNull(viewProps);
        assertEquals(2, viewProps.size());
        assertEquals("layout_in_a_widget", viewProps.get("name"));
        assertEquals(BuiltinWidgetModes.VIEW, viewProps.get("mode"));
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.forms.layout.client.tests:layouts-test-props-contrib.xml")
    public void testPropertyReference() throws Exception {

        assertTrue(service.referencePropertyAsExpression("foo", null, null, "jsf", null, null));
        assertTrue(service.referencePropertyAsExpression("foo", "bar", null, "jsf", null, null));
        assertTrue(service.referencePropertyAsExpression("foo", "bar", "bar", "jsf", null, null));
        assertTrue(service.referencePropertyAsExpression("foo", "bar", "bar", "jsf", "bar", null));
        assertTrue(service.referencePropertyAsExpression("foo", "bar", "bar", "jsf", "bar", "bar"));

        assertFalse(service.referencePropertyAsExpression("validator", null, null, "jsf", null, null));
        assertFalse(service.referencePropertyAsExpression("validator", "bar", null, "jsf", null, null));
        assertFalse(service.referencePropertyAsExpression("validator", "bar", "bar", "jsf", null, null));
        assertFalse(service.referencePropertyAsExpression("validator", "bar", "bar", "jsf", "bar", null));
        assertFalse(service.referencePropertyAsExpression("validator", "bar", "bar", "jsf", "bar", "bar"));

        assertTrue(service.referencePropertyAsExpression("defaultTime", null, null, null, "jsf", null));
        assertTrue(service.referencePropertyAsExpression("defaultTime", "bar", null, null, "jsf", null));
        assertTrue(service.referencePropertyAsExpression("defaultTime", "bar", "bar", null, "jsf", null));
        assertTrue(service.referencePropertyAsExpression("defaultTime", "bar", "bar", "bar", "jsf", null));
        assertTrue(service.referencePropertyAsExpression("defaultTime", "bar", "bar", "bar", "jsf", "bar"));

        assertTrue(service.referencePropertyAsExpression("defaultTime", null, null, null, "jsf", null));
        assertTrue(service.referencePropertyAsExpression("defaultTime", "bar", null, null, "jsf", null));
        assertFalse(service.referencePropertyAsExpression("defaultTime", "bar", "datetime", null, "jsf", null));
        assertFalse(service.referencePropertyAsExpression("defaultTime", "bar", "datetime", "jsf", "bar", null));
        assertFalse(service.referencePropertyAsExpression("defaultTime", "bar", "datetime", "jsf", "bar", "bar"));

        assertFalse(service.referencePropertyAsExpression("disabled", null, null, "jsf", null, null));
        assertFalse(service.referencePropertyAsExpression("disabled", "bar", null, "jsf", null, null));
        assertFalse(service.referencePropertyAsExpression("disabled", "bar", "bar", "jsf", null, null));
        assertFalse(service.referencePropertyAsExpression("disabled", "bar", "bar", "jsf", "bar", null));
        assertFalse(service.referencePropertyAsExpression("disabled", "bar", "bar", "jsf", "bar", "bar"));

        // override to check merge
        hotDeployer.deploy("org.nuxeo.ecm.platform.forms.layout.client.tests:layouts-test-props-contrib-override.xml");

        assertTrue(service.referencePropertyAsExpression("foo", null, null, "jsf", null, null));
        assertTrue(service.referencePropertyAsExpression("foo", "bar", null, "jsf", null, null));
        assertTrue(service.referencePropertyAsExpression("foo", "bar", "bar", "jsf", null, null));
        assertTrue(service.referencePropertyAsExpression("foo", "bar", "bar", "jsf", "bar", null));
        assertTrue(service.referencePropertyAsExpression("foo", "bar", "bar", "jsf", "bar", "bar"));

        assertTrue(service.referencePropertyAsExpression("validator", null, null, "jsf", null, null));
        assertTrue(service.referencePropertyAsExpression("validator", "bar", null, "jsf", null, null));
        assertTrue(service.referencePropertyAsExpression("validator", "bar", "bar", "jsf", null, null));
        assertTrue(service.referencePropertyAsExpression("validator", "bar", "bar", "jsf", "bar", null));
        assertTrue(service.referencePropertyAsExpression("validator", "bar", "bar", "jsf", "bar", "bar"));

        assertTrue(service.referencePropertyAsExpression("defaultTime", null, null, "jsf", null, null));
        assertTrue(service.referencePropertyAsExpression("defaultTime", "bar", null, "jsf", null, null));
        assertTrue(service.referencePropertyAsExpression("defaultTime", "bar", "bar", "jsf", null, null));
        assertTrue(service.referencePropertyAsExpression("defaultTime", "bar", "bar", "jsf", "bar", null));
        assertTrue(service.referencePropertyAsExpression("defaultTime", "bar", "bar", "jsf", "bar", "bar"));

        assertTrue(service.referencePropertyAsExpression("defaultTime", null, null, "jsf", null, null));
        assertTrue(service.referencePropertyAsExpression("defaultTime", "bar", null, "jsf", null, null));
        assertTrue(service.referencePropertyAsExpression("defaultTime", "bar", "datetime", "jsf", null, null));
        assertTrue(service.referencePropertyAsExpression("defaultTime", "bar", "datetime", "jsf", "bar", null));
        assertTrue(service.referencePropertyAsExpression("defaultTime", "bar", "datetime", "jsf", "bar", "bar"));
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.forms.layout.client.tests:layouts-test-contrib.xml")
    public void testWidget() throws Exception {

        Widget widget = service.getWidget(null, "globalTestWidget", null, BuiltinModes.VIEW, null, "pseudoLayout");
        assertNotNull(widget);
        assertEquals("globalTestWidget", widget.getName());
        assertEquals("test", widget.getType());
        assertEquals("jsf", widget.getTypeCategory());
        assertEquals("pseudoLayout", widget.getLayoutName());
        FieldDefinition[] fieldDefs = widget.getFieldDefinitions();
        assertEquals(1, fieldDefs.length);
        assertEquals("foo", fieldDefs[0].getSchemaName());
        assertEquals("bar", fieldDefs[0].getFieldName());

        Map<String, Serializable> properties = new HashMap<>();
        properties.put("myPropName", "myPropValue");
        List<FieldDefinition> fieldDefinitions = new ArrayList<>();
        fieldDefinitions.add(new FieldDefinitionImpl("foo", "bar"));
        WidgetDefinition widgetDef = new WidgetDefinitionImpl("testDynamicWidget", "test", "my.widget.label",
                "my.widget.help.label", true, null, fieldDefinitions, properties, null);

        widget = service.getWidget(null, widgetDef, BuiltinModes.VIEW, null, "pseudoLayout");
        assertNotNull(widget);
        assertEquals("testDynamicWidget", widget.getName());
        assertEquals("test", widget.getType());
        assertEquals("jsf", widget.getTypeCategory());
        assertEquals("my.widget.label", widget.getLabel());
        assertEquals("my.widget.help.label", widget.getHelpLabel());
        assertTrue(widget.isTranslated());
        assertEquals("pseudoLayout", widget.getLayoutName());
        fieldDefs = widget.getFieldDefinitions();
        assertEquals(1, fieldDefs.length);
        assertEquals("foo", fieldDefs[0].getSchemaName());
        assertEquals("bar", fieldDefs[0].getFieldName());
        assertNull(widget.getControl("addForm"));

        Map<String, Serializable> props = widget.getProperties();
        assertEquals(2, props.size());
        assertEquals("myPropValue", props.get("myPropName"));
        // prop set by default on type
        assertEquals("true", props.get("rendered"));
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.forms.layout.client.tests:layouts-test-contrib.xml")
    public void testWidgetWithTypeCategory() throws Exception {

        Widget widget = service.getWidget(null, "globalTestWidgetWithTypeCategory", "testCategory", BuiltinModes.VIEW,
                null, "pseudoLayout");
        assertNotNull(widget);
        assertEquals("globalTestWidgetWithTypeCategory", widget.getName());
        assertEquals("test", widget.getType());
        assertEquals("testTypeCategory", widget.getTypeCategory());
        assertEquals("pseudoLayout", widget.getLayoutName());
        FieldDefinition[] fieldDefs = widget.getFieldDefinitions();
        assertEquals(1, fieldDefs.length);
        assertEquals("foo", fieldDefs[0].getSchemaName());
        assertEquals("bar", fieldDefs[0].getFieldName());
        Map<String, Serializable> props = widget.getProperties();
        assertEquals(2, props.size());
        assertEquals("cssClass", props.get("styleClass"));
        // prop set by default on type
        assertEquals("bar", props.get("foo"));
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.forms.layout.client.tests:layouts-test-contrib.xml")
    public void testWidgetExceptions() throws Exception {

        Widget widget = service.getWidget(null, "unknownWidget", null, BuiltinModes.VIEW, null, "pseudoLayout");
        assertNull(widget);
        widget = service.getWidget(null, null, BuiltinModes.VIEW, null, "pseudoLayout");
        assertNull(widget);
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.forms.layout.client.tests:layouts-test-contrib.xml")
    public void testWidgetWithFormInfo() throws Exception {

        Widget widget = service.getWidget(null, "widgetWithControls", null, BuiltinModes.VIEW, null, "pseudoLayout");
        assertNotNull(widget);
        assertEquals("widgetWithControls", widget.getName());
        assertEquals("test", widget.getType());
        assertEquals("jsf", widget.getTypeCategory());
        assertEquals("pseudoLayout", widget.getLayoutName());
        assertEquals("true", widget.getControl("requireSurroundingForm"));
        assertEquals("true", widget.getControl("useAjaxForm"));
        assertTrue(widget.isHandlingLabels());
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.forms.layout.client.tests:layouts-test-contrib.xml")
    public void testEmptyLayout() throws Exception {

        LayoutDefinition layout = service.getLayoutDefinition("testEmptyLayout");
        assertTrue(layout.isEmpty());
        layout = service.getLayoutDefinition("testLayout");
        assertFalse(layout.isEmpty());
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.forms.layout.client.tests:layouts-test-contrib.xml")
    public void testLayoutNonJSFWidgetTypeCategory() throws Exception {

        Layout layout = service.getLayout(null, "testLayoutForCategory", BuiltinModes.VIEW, null);
        assertNotNull(layout);
        assertEquals("testLayoutForCategory", layout.getName());
        assertEquals("jsf", layout.getTypeCategory());

        LayoutRow[] rows = layout.getRows();
        assertNotNull(rows);
        assertEquals(1, rows.length);
        LayoutRow row = rows[0];
        Widget[] widgets = row.getWidgets();
        assertNotNull(widgets);
        assertEquals(1, widgets.length);
        Widget widget = widgets[0];
        assertEquals("globalTestWidgetWithTestCategory", widget.getName());
        assertEquals("jsf", widget.getTypeCategory());
        Widget[] subs = widget.getSubWidgets();
        assertNotNull(subs);
        // check retrieval is ok: sub widget ref category is taken from widget
        assertEquals(1, subs.length);
        Widget sub = subs[0];
        assertEquals("globalTestWidgetWithTypeCategory", sub.getName());
        // make sure it's not "jsf"
        assertEquals("testTypeCategory", sub.getTypeCategory());
    }

}
