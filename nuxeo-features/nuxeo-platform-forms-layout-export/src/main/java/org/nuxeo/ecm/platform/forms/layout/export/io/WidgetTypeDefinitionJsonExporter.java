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
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;

/**
 * JSON exporter for a {@link WidgetTypeDefinition} object
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class WidgetTypeDefinitionJsonExporter {

    private static final Log log = LogFactory.getLog(WidgetTypeDefinitionJsonExporter.class);

    public static final void export(WidgetTypeDefinition def, OutputStream out)
            throws IOException {
        JSONObject res = exportToJson(def);
        out.write(res.toString(2).getBytes("UTF-8"));
    }

    public static final void export(List<WidgetTypeDefinition> defs,
            OutputStream out) throws IOException {
        JSONObject res = new JSONObject();
        if (defs != null) {
            // sort so that order is deterministic
            Collections.sort(defs, new WidgetTypeDefinitionComparator());
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
        json.element("properties", exportStringPropsToJson(def.getProperties()));
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
        JSONObject fields = new JSONObject();
        fields.element("list", conf.isList());
        fields.element("complex", conf.isComplex());
        JSONArray supportedTypes = new JSONArray();
        List<String> confSupportedTypes = conf.getSupportedFieldTypes();
        if (confSupportedTypes != null) {
            supportedTypes.addAll(confSupportedTypes);
        }
        fields.element("supportedTypes", supportedTypes);
        JSONArray defaultTypes = new JSONArray();
        List<String> confDefaultTypes = conf.getDefaultFieldTypes();
        if (confDefaultTypes != null) {
            defaultTypes.addAll(confDefaultTypes);
        }
        fields.element("defaultTypes", defaultTypes);
        json.element("fields", fields);

        JSONArray cats = new JSONArray();
        List<String> confCats = conf.getCategories();
        if (confCats != null) {
            cats.addAll(confCats);
        }
        json.element("categories", cats);

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
            props.element("layouts", layouts);
        }
        json.element("properties", props);
        return json;
    }

    protected static JSONObject exportToJson(LayoutDefinition layoutDef) {
        JSONObject json = new JSONObject();
        json.element("name", layoutDef.getName());
        json.element("templates",
                exportStringPropsToJson(layoutDef.getTemplates()));
        json.element("properties",
                exportPropsByModeToJson(layoutDef.getProperties()));
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
        json.element("rows", rows);
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
        json.element("widgets", widgets);
        return json;
    }

    protected static JSONObject exportToJson(LayoutRowDefinition layoutRowDef) {
        JSONObject json = new JSONObject();
        String name = layoutRowDef.getName();
        if (name != null) {
            json.element("name", name);
        }
        json.element("properties",
                exportPropsByModeToJson(layoutRowDef.getProperties()));
        JSONArray widgets = new JSONArray();
        String[] defWidgets = layoutRowDef.getWidgets();
        if (defWidgets != null) {
            for (String widget : defWidgets) {
                widgets.add(widget);
            }
        }
        json.element("widgets", widgets);
        return json;
    }

    protected static JSONObject exportToJson(WidgetDefinition widgetDef) {
        JSONObject json = new JSONObject();
        json.element("name", widgetDef.getName());
        json.element("type", widgetDef.getType());
        json.element("labels", exportStringPropsToJson(widgetDef.getLabels()));
        json.element("helpLabels",
                exportStringPropsToJson(widgetDef.getHelpLabels()));
        json.element("translated", widgetDef.isTranslated());
        json.element("widgetModes",
                exportStringPropsToJson(widgetDef.getModes()));

        JSONArray fields = new JSONArray();
        FieldDefinition[] fieldDefs = widgetDef.getFieldDefinitions();
        if (fieldDefs != null) {
            for (FieldDefinition fieldDef : fieldDefs) {
                fields.add(exportToJson(fieldDef));
            }
        }
        json.element("fields", fields);

        JSONArray subWidgets = new JSONArray();
        WidgetDefinition[] subWidgetDefs = widgetDef.getSubWidgetDefinitions();
        if (subWidgetDefs != null) {
            for (WidgetDefinition wDef : subWidgetDefs) {
                subWidgets.add(exportToJson(wDef));
            }
        }
        json.element("subWidgets", subWidgets);

        json.element("properties",
                exportPropsByModeToJson(widgetDef.getProperties()));
        json.element("propertiesByWidgetMode",
                exportPropsByModeToJson(widgetDef.getWidgetModeProperties()));

        return json;
    }

    protected static JSONObject exportToJson(FieldDefinition fieldDef) {
        JSONObject json = new JSONObject();
        json.element("fieldName", fieldDef.getFieldName());
        json.element("schemaName", fieldDef.getSchemaName());
        json.element("propertyName", fieldDef.getPropertyName());
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
