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
package org.nuxeo.ecm.platform.forms.layout.export.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRowDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOptions;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetTypeDefinitionComparator;

/**
 * JSON exporter for a {@link WidgetTypeDefinition} object
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class WidgetTypeDefinitionJsonExporter {

    private static final Log log = LogFactory.getLog(WidgetTypeDefinitionJsonExporter.class);

    private WidgetTypeDefinitionJsonExporter() {
    }

    public static void export(WidgetTypeDefinition def, OutputStream out)
            throws IOException {
        JSONObject res = exportToJson(def);
        out.write(res.toString(2).getBytes("UTF-8"));
    }

    public static void export(List<WidgetTypeDefinition> defs, OutputStream out)
            throws IOException {
        JSONObject res = new JSONObject();
        if (defs != null) {
            // sort so that order is deterministic
            Collections.sort(defs, new WidgetTypeDefinitionComparator(false));
        }
        for (WidgetTypeDefinition def : defs) {
            res.element(def.getName(), exportToJson(def));
        }
        out.write(res.toString(2).getBytes("UTF-8"));
    }

    protected static JSONObject exportToJson(WidgetTypeDefinition def) {
        JSONObject json = new JSONObject();
        json.element("name", def.getName());
        json.element("handlerClassName", def.getHandlerClassName());
        JSONObject props = exportStringPropsToJson(def.getProperties());
        if (!props.isEmpty()) {
            json.element("properties", props);
        }
        WidgetTypeConfiguration conf = def.getConfiguration();
        if (conf != null) {
            json.element("configuration", exportToJson(conf));
        }
        return json;
    }

    protected static JSONObject exportToJson(WidgetTypeConfiguration conf) {
        JSONObject json = new JSONObject();
        json.element("title", conf.getTitle());
        json.element("description", conf.getDescription());
        json.element("sinceVersion", conf.getSinceVersion());
        JSONObject confProps = exportPropsToJson(conf.getConfProperties());
        if (!confProps.isEmpty()) {
            json.element("confProperties", confProps);
        }

        JSONArray supportedModes = new JSONArray();
        List<String> confSupportedModes = conf.getSupportedModes();
        if (confSupportedModes != null) {
            supportedModes.addAll(confSupportedModes);
        }
        if (!supportedModes.isEmpty()) {
            json.element("supportedModes", supportedModes);
        }

        json.element("acceptingSubWidgets", conf.isAcceptingSubWidgets());

        JSONObject fields = new JSONObject();
        fields.element("list", conf.isList());
        fields.element("complex", conf.isComplex());

        JSONArray supportedTypes = new JSONArray();
        List<String> confSupportedTypes = conf.getSupportedFieldTypes();
        if (confSupportedTypes != null) {
            supportedTypes.addAll(confSupportedTypes);
        }
        if (!supportedTypes.isEmpty()) {
            fields.element("supportedTypes", supportedTypes);
        }

        JSONArray defaultTypes = new JSONArray();
        List<String> confDefaultTypes = conf.getDefaultFieldTypes();
        if (confDefaultTypes != null) {
            defaultTypes.addAll(confDefaultTypes);
        }
        if (!defaultTypes.isEmpty()) {
            fields.element("defaultTypes", defaultTypes);
        }

        JSONArray defaultFieldDefs = new JSONArray();
        List<FieldDefinition> fieldDefs = conf.getDefaultFieldDefinitions();
        if (fieldDefs != null) {
            for (FieldDefinition fieldDef : fieldDefs) {
                defaultFieldDefs.add(exportToJson(fieldDef));
            }
        }
        if (!defaultFieldDefs.isEmpty()) {
            fields.element("defaultConfiguration", defaultFieldDefs);
        }

        json.element("fields", fields);

        JSONArray cats = new JSONArray();
        List<String> confCats = conf.getCategories();
        if (confCats != null) {
            cats.addAll(confCats);
        }
        if (!cats.isEmpty()) {
            json.element("categories", cats);
        }

        JSONObject props = new JSONObject();
        Map<String, List<LayoutDefinition>> confLayouts = conf.getPropertyLayouts();
        if (confLayouts != null) {
            List<String> modes = new ArrayList<String>(confLayouts.keySet());
            // sort so that order is deterministic
            Collections.sort(modes);
            JSONObject layouts = new JSONObject();
            for (String mode : modes) {
                JSONArray modeLayouts = new JSONArray();
                for (LayoutDefinition layoutDef : confLayouts.get(mode)) {
                    modeLayouts.add(exportToJson(layoutDef));
                }
                layouts.element(mode, modeLayouts);
            }
            if (!layouts.isEmpty()) {
                props.element("layouts", layouts);
            }
        }
        if (!props.isEmpty()) {
            json.element("properties", props);
        }
        return json;
    }

    protected static JSONObject exportToJson(LayoutDefinition layoutDef) {
        JSONObject json = new JSONObject();
        json.element("name", layoutDef.getName());

        JSONObject templates = exportStringPropsToJson(layoutDef.getTemplates());
        if (!templates.isEmpty()) {
            json.element("templates", templates);
        }

        JSONObject props = exportPropsByModeToJson(layoutDef.getProperties());
        if (!props.isEmpty()) {
            json.element("properties", props);
        }

        JSONArray rows = new JSONArray();
        LayoutRowDefinition[] defRows = layoutDef.getRows();
        List<String> widgetsToExport = new ArrayList<String>();
        if (defRows != null) {
            for (LayoutRowDefinition layoutRowDef : defRows) {
                rows.add(exportToJson(layoutRowDef));
                String[] widgets = layoutRowDef.getWidgets();
                if (widgets != null) {
                    for (String widget : widgets) {
                        widgetsToExport.add(widget);
                    }
                }
            }
        }
        if (!rows.isEmpty()) {
            json.element("rows", rows);
        }

        JSONArray widgets = new JSONArray();
        for (String widgetName : widgetsToExport) {
            WidgetDefinition widgetDef = layoutDef.getWidgetDefinition(widgetName);
            if (widgetDef == null) {
                log.error(String.format(
                        "No local definition found for widget '%s' in layout '%s' "
                                + "=> cannot export", widgetName,
                        layoutDef.getName()));
                continue;
            }
            widgets.add(exportToJson(widgetDef));
        }
        if (!widgets.isEmpty()) {
            json.element("widgets", widgets);
        }

        return json;
    }

    protected static JSONObject exportToJson(LayoutRowDefinition layoutRowDef) {
        JSONObject json = new JSONObject();
        String name = layoutRowDef.getName();
        if (name != null) {
            json.element("name", name);
        }
        JSONObject props = exportPropsByModeToJson(layoutRowDef.getProperties());
        if (!props.isEmpty()) {
            json.element("properties", props);
        }
        JSONArray widgets = new JSONArray();
        String[] defWidgets = layoutRowDef.getWidgets();
        if (defWidgets != null) {
            for (String widget : defWidgets) {
                widgets.add(widget);
            }
        }
        if (!widgets.isEmpty()) {
            json.element("widgets", widgets);
        }
        return json;
    }

    protected static JSONObject exportToJson(WidgetDefinition widgetDef) {
        JSONObject json = new JSONObject();
        json.element("name", widgetDef.getName());
        json.element("type", widgetDef.getType());
        JSONObject labels = exportStringPropsToJson(widgetDef.getLabels());
        if (!labels.isEmpty()) {
            json.element("labels", labels);
        }
        JSONObject helpLabels = exportStringPropsToJson(widgetDef.getHelpLabels());
        if (!helpLabels.isEmpty()) {
            json.element("helpLabels", helpLabels);
        }
        json.element("translated", widgetDef.isTranslated());
        JSONObject widgetModes = exportStringPropsToJson(widgetDef.getModes());
        if (!widgetModes.isEmpty()) {
            json.element("widgetModes", widgetModes);
        }

        JSONArray fields = new JSONArray();
        FieldDefinition[] fieldDefs = widgetDef.getFieldDefinitions();
        if (fieldDefs != null) {
            for (FieldDefinition fieldDef : fieldDefs) {
                fields.add(exportToJson(fieldDef));
            }
        }
        if (!fields.isEmpty()) {
            json.element("fields", fields);
        }

        JSONArray subWidgets = new JSONArray();
        WidgetDefinition[] subWidgetDefs = widgetDef.getSubWidgetDefinitions();
        if (subWidgetDefs != null) {
            for (WidgetDefinition wDef : subWidgetDefs) {
                subWidgets.add(exportToJson(wDef));
            }
        }
        if (!subWidgets.isEmpty()) {
            json.element("subWidgets", subWidgets);
        }

        JSONObject props = exportPropsByModeToJson(widgetDef.getProperties());
        if (!props.isEmpty()) {
            json.element("properties", props);
        }
        JSONObject widgetModeProps = exportPropsByModeToJson(widgetDef.getWidgetModeProperties());
        if (!widgetModeProps.isEmpty()) {
            json.element("propertiesByWidgetMode", widgetModeProps);
        }

        JSONArray selectOptions = new JSONArray();
        WidgetSelectOption[] selectOptionDefs = widgetDef.getSelectOptions();
        if (selectOptionDefs != null) {
            for (WidgetSelectOption selectOptionDef : selectOptionDefs) {
                selectOptions.add(exportToJson(selectOptionDef));
            }
        }
        if (!selectOptions.isEmpty()) {
            json.element("selectOptions", selectOptions);
        }

        return json;
    }

    protected static JSONObject exportToJson(FieldDefinition fieldDef) {
        JSONObject json = new JSONObject();
        json.element("fieldName", fieldDef.getFieldName());
        json.element("schemaName", fieldDef.getSchemaName());
        json.element("propertyName", fieldDef.getPropertyName());
        return json;
    }

    protected static JSONObject exportToJson(WidgetSelectOption selectOption) {
        JSONObject json = new JSONObject();
        Serializable value = selectOption.getValue();
        boolean isMulti = selectOption instanceof WidgetSelectOptions;
        if (isMulti) {
            json.element("multiple", true);
        } else {
            json.element("multiple", false);
        }
        if (value != null) {
            json.element("value", value);
        }
        String var = selectOption.getVar();
        if (var != null) {
            json.element("var", var);
        }
        String itemLabel = selectOption.getItemLabel();
        if (itemLabel != null) {
            json.element("itemLabel", itemLabel);
        }
        String itemValue = selectOption.getItemValue();
        if (itemValue != null) {
            json.element("itemValue", itemValue);
        }
        Serializable itemDisabled = selectOption.getItemDisabled();
        if (itemDisabled != null) {
            json.element("itemDisabled", itemDisabled);
        }
        Serializable itemRendered = selectOption.getItemRendered();
        if (itemRendered != null) {
            json.element("itemRendered", itemRendered);
        }
        if (isMulti) {
            WidgetSelectOptions selectOptions = (WidgetSelectOptions) selectOption;
            String ordering = selectOptions.getOrdering();
            if (ordering != null) {
                json.element("ordering", ordering);
            }
            Boolean caseSensitive = selectOptions.getCaseSensitive();
            if (caseSensitive != null) {
                json.element("caseSensitive", caseSensitive);
            }
        }
        return json;
    }

    protected static JSONObject exportPropsByModeToJson(
            Map<String, Map<String, Serializable>> propsByMode) {
        JSONObject props = new JSONObject();
        if (propsByMode != null) {
            List<String> defModes = new ArrayList<String>(propsByMode.keySet());
            // sort so that order is deterministic
            Collections.sort(defModes);
            for (String defMode : defModes) {
                props.element(defMode,
                        exportPropsToJson(propsByMode.get(defMode)));
            }
        }
        return props;
    }

    @SuppressWarnings("unchecked")
    protected static JSONObject exportPropsToJson(
            Map<String, Serializable> defProps) {
        JSONObject props = new JSONObject();
        if (defProps != null) {
            List<String> defPropNames = new ArrayList<String>(defProps.keySet());
            // sort so that order is deterministic
            Collections.sort(defPropNames);
            for (String defPropName : defPropNames) {
                Serializable value = defProps.get(defPropName);
                if (value instanceof Collection) {
                    JSONArray array = new JSONArray();
                    array.addAll((Collection) value);
                    props.element(defPropName, array);
                } else if (value instanceof Object[]) {
                    JSONArray array = new JSONArray();
                    for (Object item : (Object[]) value) {
                        array.add(item);
                    }
                    props.element(defPropName, array);
                } else {
                    props.element(defPropName, value);
                }
            }
        }
        return props;
    }

    protected static JSONObject exportStringPropsToJson(
            Map<String, String> defProps) {
        JSONObject props = new JSONObject();
        if (defProps != null) {
            List<String> defPropNames = new ArrayList<String>(defProps.keySet());
            // sort so that order is deterministic
            Collections.sort(defPropNames);
            for (String defPropName : defPropNames) {
                props.element(defPropName, defProps.get(defPropName));
            }
        }
        return props;
    }

}
