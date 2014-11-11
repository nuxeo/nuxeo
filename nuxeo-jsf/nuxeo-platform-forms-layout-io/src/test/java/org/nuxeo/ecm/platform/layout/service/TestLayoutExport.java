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
package org.nuxeo.ecm.platform.layout.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRowDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOptions;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.io.JSONLayoutExporter;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Anahide Tchertchian
 * @since 5.4
 */
// TODO: move to API module?
public class TestLayoutExport extends NXRuntimeTestCase {

    private WebLayoutManager service;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.forms.layout.client",
                "OSGI-INF/layouts-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.forms.layout.export.tests",
                "layouts-test-contrib.xml");
        service = Framework.getService(WebLayoutManager.class);
        assertNotNull(service);
    }

    public void testWidgetTypeExport() throws Exception {
        WidgetTypeDefinition wTypeDef = service.getWidgetTypeDefinition("test");
        assertNotNull(wTypeDef);

        File file = File.createTempFile("widgettype-export", ".json");
        FileOutputStream out = new FileOutputStream(file);
        JSONLayoutExporter.export(wTypeDef, out);

        InputStream written = new FileInputStream(file);
        InputStream expected = new FileInputStream(
                FileUtils.getResourcePathFromContext("widgettype-export.json"));

        String expectedString = FileUtils.read(expected).replaceAll("\r?\n", "");
        String writtenString = FileUtils.read(written).replaceAll("\r?\n", "");
        assertEquals(expectedString, writtenString);
    }

    public void testWidgetTypesExport() throws Exception {
        WidgetTypeDefinition wTypeDef = service.getWidgetTypeDefinition("test");
        assertNotNull(wTypeDef);

        File file = File.createTempFile("widgettypes-export", ".json");
        FileOutputStream out = new FileOutputStream(file);
        List<WidgetTypeDefinition> wTypeDefs = new ArrayList<WidgetTypeDefinition>();
        wTypeDefs.add(wTypeDef);
        JSONLayoutExporter.export(wTypeDefs, out);

        InputStream written = new FileInputStream(file);
        InputStream expected = new FileInputStream(
                FileUtils.getResourcePathFromContext("widgettypes-export.json"));

        String expectedString = FileUtils.read(expected).replaceAll("\r?\n", "");
        String writtenString = FileUtils.read(written).replaceAll("\r?\n", "");
        assertEquals(expectedString, writtenString);
    }

    public void testWidgetTypeImport() throws Exception {
        JSONObject json = null;
        InputStream in = new FileInputStream(
                FileUtils.getResourcePathFromContext("widgettype-export.json"));
        try {
            byte[] bytes = FileUtils.readBytes(in);
            if (bytes.length != 0) {
                json = JSONObject.fromObject(new String(bytes, "UTF-8"));
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }

        WidgetTypeDefinition def = JSONLayoutExporter.importWidgetTypeDefinition(json);
        assertEquals("test", def.getName());
        assertEquals(
                "org.nuxeo.ecm.platform.layout.facelets.DummyWidgetTypeHandler",
                def.getHandlerClassName());
        Map<String, String> defProps = def.getProperties();
        assertNotNull(defProps);
        assertEquals(2, defProps.size());
        assertEquals("bar1", defProps.get("foo1"));
        assertEquals("bar2", defProps.get("foo2"));

        WidgetTypeConfiguration conf = def.getConfiguration();
        assertNotNull(conf);

        assertEquals("5.4.0", conf.getSinceVersion());
        assertEquals("Test widget type", conf.getTitle());
        assertEquals("This is a test widget type", conf.getDescription());
        assertEquals("test", conf.getDemoId());
        assertTrue(conf.isDemoPreviewEnabled());

        Map<String, Serializable> confProps = conf.getConfProperties();
        assertNotNull(confProps);
        assertEquals(2, confProps.size());
        assertEquals("foo", confProps.get("confProp"));
        assertEquals("dc:title", confProps.get("sortProperty"));

        List<String> supportedModes = conf.getSupportedModes();
        assertNotNull(supportedModes);
        assertEquals(2, supportedModes.size());
        assertEquals("edit", supportedModes.get(0));
        assertEquals("view", supportedModes.get(1));

        assertTrue(conf.isAcceptingSubWidgets());

        List<String> cats = conf.getCategories();
        assertNotNull(cats);
        assertEquals(2, cats.size());
        assertEquals("foo", cats.get(0));
        assertEquals("bar", cats.get(1));
        List<String> defaultTypes = conf.getDefaultFieldTypes();
        assertNotNull(defaultTypes);
        assertEquals(1, defaultTypes.size());
        assertEquals("string", defaultTypes.get(0));
        List<String> supportedTypes = conf.getSupportedFieldTypes();
        assertNotNull(supportedTypes);
        assertEquals(2, supportedTypes.size());
        assertEquals("string", supportedTypes.get(0));
        assertEquals("path", supportedTypes.get(1));

        List<FieldDefinition> defaultFieldDefs = conf.getDefaultFieldDefinitions();
        assertNotNull(defaultFieldDefs);
        assertEquals(2, defaultFieldDefs.size());
        assertEquals("dc:title", defaultFieldDefs.get(0).getPropertyName());
        assertEquals("data.ref", defaultFieldDefs.get(1).getPropertyName());

        Map<String, List<LayoutDefinition>> propLayouts = conf.getPropertyLayouts();
        assertNotNull(propLayouts);
        assertEquals(2, propLayouts.size());
        List<LayoutDefinition> anyLayouts = propLayouts.get(BuiltinModes.ANY);
        assertNotNull(anyLayouts);
        assertEquals(1, anyLayouts.size());
        LayoutDefinition anyLayout = anyLayouts.get(0);
        assertNull(anyLayout.getName());
        assertEquals(0, anyLayout.getTemplates().size());
        assertEquals(0, anyLayout.getProperties().size());
        LayoutRowDefinition[] anyRows = anyLayout.getRows();
        assertEquals(1, anyRows.length);
        LayoutRowDefinition anyRow = anyRows[0];
        assertNull(anyRow.getName());
        assertEquals(0, anyRow.getProperties().size());
        String[] anyRowWidgets = anyRow.getWidgets();
        assertEquals(1, anyRowWidgets.length);
        assertEquals("required_property", anyRowWidgets[0]);

        WidgetDefinition requiredWidget = anyLayout.getWidgetDefinition("required_property");
        assertNotNull(requiredWidget);
        assertEquals("required_property", requiredWidget.getName());
        assertEquals(1, requiredWidget.getLabels().size());
        assertEquals("Required", requiredWidget.getLabel(BuiltinModes.ANY));
        assertEquals("text", requiredWidget.getType());
        assertEquals(1, requiredWidget.getFieldDefinitions().length);
        assertEquals("foo",
                requiredWidget.getFieldDefinitions()[0].getSchemaName());
        assertEquals("bar",
                requiredWidget.getFieldDefinitions()[0].getFieldName());
        assertEquals("foo:bar",
                requiredWidget.getFieldDefinitions()[0].getPropertyName());
        assertEquals(0, requiredWidget.getHelpLabels().size());
        assertEquals(0, requiredWidget.getModes().size());
        assertEquals(0, requiredWidget.getProperties().size());
        assertEquals(0, requiredWidget.getWidgetModeProperties().size());
        assertEquals(0, requiredWidget.getSelectOptions().length);
        assertEquals(0, requiredWidget.getSubWidgetDefinitions().length);

        List<LayoutDefinition> editLayouts = propLayouts.get(BuiltinModes.EDIT);
        assertNotNull(editLayouts);
        assertEquals(1, editLayouts.size());
        LayoutDefinition editLayout = editLayouts.get(0);
        assertNull(editLayout.getName());
        assertEquals(0, editLayout.getTemplates().size());
        assertEquals(0, editLayout.getProperties().size());
        LayoutRowDefinition[] editRows = editLayout.getRows();
        assertEquals(2, editRows.length);
        LayoutRowDefinition editRow = editRows[0];
        assertNull(editRow.getName());
        assertEquals(0, editRow.getProperties().size());
        String[] editRowWidgets = editRow.getWidgets();
        assertEquals(1, editRowWidgets.length);
        assertEquals("rendered_property", editRowWidgets[0]);

        WidgetDefinition renderedWidget = editLayout.getWidgetDefinition("rendered_property");
        assertNotNull(renderedWidget);
        assertEquals("rendered_property", renderedWidget.getName());
        assertEquals(1, renderedWidget.getLabels().size());
        assertEquals("Rendered", renderedWidget.getLabel(BuiltinModes.ANY));
        assertEquals("text", renderedWidget.getType());
        assertEquals(1, renderedWidget.getFieldDefinitions().length);
        assertEquals("foo",
                renderedWidget.getFieldDefinitions()[0].getSchemaName());
        assertEquals("bar",
                renderedWidget.getFieldDefinitions()[0].getFieldName());
        assertEquals("foo:bar",
                renderedWidget.getFieldDefinitions()[0].getPropertyName());
        assertEquals(0, renderedWidget.getHelpLabels().size());
        assertEquals(1, renderedWidget.getModes().size());
        assertEquals(BuiltinModes.VIEW,
                renderedWidget.getMode(BuiltinModes.ANY));
        assertEquals(0, renderedWidget.getProperties().size());
        assertEquals(0, renderedWidget.getWidgetModeProperties().size());
        assertEquals(0, renderedWidget.getSelectOptions().length);
        assertEquals(1, renderedWidget.getSubWidgetDefinitions().length);
        WidgetDefinition subWidget = renderedWidget.getSubWidgetDefinitions()[0];
        assertEquals("subwidget", subWidget.getName());
        assertEquals(1, subWidget.getLabels().size());
        assertEquals("subwidget label", subWidget.getLabel(BuiltinModes.ANY));
        assertEquals("text", subWidget.getType());
        assertEquals(1, subWidget.getFieldDefinitions().length);
        assertEquals("foo", subWidget.getFieldDefinitions()[0].getSchemaName());
        assertEquals("bar", subWidget.getFieldDefinitions()[0].getFieldName());
        assertEquals("foo:bar",
                subWidget.getFieldDefinitions()[0].getPropertyName());
        assertEquals(0, subWidget.getHelpLabels().size());
        assertEquals(0, subWidget.getModes().size());
        assertEquals(0, subWidget.getProperties().size());
        assertEquals(0, subWidget.getWidgetModeProperties().size());
        assertEquals(0, subWidget.getSelectOptions().length);
        assertEquals(0, subWidget.getSubWidgetDefinitions().length);

        editRow = editRows[1];
        assertNull(editRow.getName());
        assertEquals(0, editRow.getProperties().size());
        editRowWidgets = editRow.getWidgets();
        assertEquals(1, editRowWidgets.length);
        assertEquals("selection_property", editRowWidgets[0]);

        WidgetDefinition selectionWidget = editLayout.getWidgetDefinition("selection_property");
        assertNotNull(selectionWidget);
        assertEquals("selection_property", selectionWidget.getName());
        assertEquals(1, selectionWidget.getLabels().size());
        assertEquals("Selection", selectionWidget.getLabel(BuiltinModes.ANY));
        assertEquals("selectOneListbox", selectionWidget.getType());
        assertEquals(1, selectionWidget.getFieldDefinitions().length);
        assertEquals("foo2",
                selectionWidget.getFieldDefinitions()[0].getSchemaName());
        assertEquals("bar2",
                selectionWidget.getFieldDefinitions()[0].getFieldName());
        assertEquals("foo2:bar2",
                selectionWidget.getFieldDefinitions()[0].getPropertyName());
        assertEquals(0, selectionWidget.getHelpLabels().size());
        assertEquals(0, selectionWidget.getModes().size());
        assertEquals(0, selectionWidget.getProperties().size());
        assertEquals(0, selectionWidget.getWidgetModeProperties().size());
        WidgetSelectOption[] options = selectionWidget.getSelectOptions();
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
        assertEquals(0, selectionWidget.getSubWidgetDefinitions().length);
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

}
