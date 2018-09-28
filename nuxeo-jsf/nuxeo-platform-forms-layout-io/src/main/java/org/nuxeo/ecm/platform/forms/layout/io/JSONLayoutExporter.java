/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.io;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.Layout;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRow;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRowDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.RenderingInfo;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetReference;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOptions;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.converters.LayoutConversionContext;
import org.nuxeo.ecm.platform.forms.layout.api.converters.WidgetDefinitionConverter;
import org.nuxeo.ecm.platform.forms.layout.api.impl.FieldDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutRowDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutTypeConfigurationImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutTypeDefinitionComparator;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutTypeDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.RenderingInfoImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetReferenceImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetSelectOptionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetSelectOptionsImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetTypeConfigurationImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetTypeDefinitionComparator;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetTypeDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.runtime.api.Framework;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * JSON exporter for layout objects
 *
 * @author Anahide Tchertchian
 * @since 5.4.2
 */
public class JSONLayoutExporter {

    private static final Log log = LogFactory.getLog(JSONLayoutExporter.class);

    public static final String ENCODED_VALUES_ENCODING = "UTF-8";

    /**
     * @since 10.1
     */
    public static String encode(String json) throws UnsupportedEncodingException {
        String encodedValues = Base64.getEncoder().encodeToString(json.getBytes(UTF_8));
        return URLEncoder.encode(encodedValues, ENCODED_VALUES_ENCODING);
    }

    public static String encode(JSONObject jsonObject) throws UnsupportedEncodingException {
        String json = jsonObject.toString();
        return encode(json);
    }

    public static JSONObject decode(String json) throws UnsupportedEncodingException {
        String decodedValues = URLDecoder.decode(json, ENCODED_VALUES_ENCODING);
        json = new String(Base64.getDecoder().decode(decodedValues), UTF_8);
        return JSONObject.fromObject(json);
    }

    /**
     * @since 5.5
     * @throws IOException
     */
    public static void export(String category, LayoutDefinition layoutDef, LayoutConversionContext ctx,
            List<WidgetDefinitionConverter> widgetConverters, OutputStream out) throws IOException {
        JSONObject res = exportToJson(category, layoutDef, ctx, widgetConverters);
        out.write(res.toString(2).getBytes(ENCODED_VALUES_ENCODING));
    }

    public static void export(WidgetTypeDefinition def, OutputStream out) throws IOException {
        JSONObject res = exportToJson(def);
        out.write(res.toString(2).getBytes(ENCODED_VALUES_ENCODING));
    }

    public static void export(List<WidgetTypeDefinition> defs, OutputStream out) throws IOException {
        JSONObject res = new JSONObject();
        // sort so that order is deterministic
        Collections.sort(defs, new WidgetTypeDefinitionComparator(false));
        for (WidgetTypeDefinition def : defs) {
            res.element(def.getName(), exportToJson(def));
        }
        out.write(res.toString(2).getBytes(ENCODED_VALUES_ENCODING));
    }

    /**
     * @since 6.0
     */
    public static void exportLayoutType(LayoutTypeDefinition def, OutputStream out) throws IOException {
        JSONObject res = exportToJson(def);
        out.write(res.toString(2).getBytes(ENCODED_VALUES_ENCODING));
    }

    /**
     * @since 6.0
     */
    public static void exportLayoutTypes(List<LayoutTypeDefinition> defs, OutputStream out) throws IOException {
        JSONObject res = new JSONObject();
        // sort so that order is deterministic
        Collections.sort(defs, new LayoutTypeDefinitionComparator());
        for (LayoutTypeDefinition def : defs) {
            res.element(def.getName(), exportToJson(def));
        }
        out.write(res.toString(2).getBytes(ENCODED_VALUES_ENCODING));
    }

    public static JSONObject exportToJson(WidgetTypeDefinition def) {
        JSONObject json = new JSONObject();
        json.element("name", def.getName());
        List<String> caliases = def.getAliases();
        if (caliases != null && !caliases.isEmpty()) {
            JSONArray aliases = new JSONArray();
            for (String alias : caliases) {
                aliases.add(alias);
            }
            json.element("aliases", aliases);
        }
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

    @SuppressWarnings("unchecked")
    public static WidgetTypeDefinition importWidgetTypeDefinition(JSONObject jsonDef) {
        String name = jsonDef.optString("name");
        String handlerClass = jsonDef.optString("handlerClassName");
        Map<String, String> properties = importStringProps(jsonDef.optJSONObject("properties"));
        WidgetTypeConfiguration conf = importWidgetTypeConfiguration(jsonDef.optJSONObject("configuration"));
        List<String> aliases = new ArrayList<String>();
        JSONArray jaliases = jsonDef.optJSONArray("aliases");
        if (jaliases != null) {
            aliases.addAll(jaliases);
        }
        WidgetTypeDefinitionImpl res = new WidgetTypeDefinitionImpl(name, handlerClass, properties, conf);
        res.setAliases(aliases);
        return res;
    }

    public static JSONObject exportToJson(WidgetTypeConfiguration conf) {
        JSONObject json = new JSONObject();
        json.element("title", conf.getTitle());
        json.element("description", conf.getDescription());
        String demoId = conf.getDemoId();
        if (demoId != null) {
            JSONObject demoInfo = new JSONObject();
            demoInfo.element("id", demoId);
            demoInfo.element("previewEnabled", conf.isDemoPreviewEnabled());
            json.element("demo", demoInfo);
        }
        json.element("sinceVersion", conf.getSinceVersion());
        String deprVersion = conf.getDeprecatedVersion();
        if (!StringUtils.isBlank(deprVersion)) {
            json.element("deprecatedVersion", deprVersion);
        }
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

        if (conf.isAcceptingSubWidgets()) {
            json.element("acceptingSubWidgets", conf.isAcceptingSubWidgets());
        }
        if (conf.isHandlingLabels()) {
            json.element("handlingLabels", conf.isHandlingLabels());
        }
        JSONArray supportedControls = new JSONArray();
        List<String> confSupportedControls = conf.getSupportedControls();
        if (confSupportedControls != null) {
            supportedControls.addAll(confSupportedControls);
        }
        if (!supportedControls.isEmpty()) {
            json.element("supportedControls", supportedControls);
        }
        if (conf.isContainingForm()) {
            json.element("containingForm", true);
        }

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

        Map<String, List<LayoutDefinition>> fieldLayouts = conf.getFieldLayouts();
        if (fieldLayouts != null) {
            List<String> modes = new ArrayList<String>(fieldLayouts.keySet());
            // sort so that order is deterministic
            Collections.sort(modes);
            JSONObject layouts = new JSONObject();
            for (String mode : modes) {
                JSONArray modeLayouts = new JSONArray();
                for (LayoutDefinition layoutDef : fieldLayouts.get(mode)) {
                    modeLayouts.add(exportToJson(null, layoutDef));
                }
                layouts.element(mode, modeLayouts);
            }
            if (!layouts.isEmpty()) {
                fields.element("layouts", layouts);
            }
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
                    modeLayouts.add(exportToJson(null, layoutDef));
                }
                layouts.element(mode, modeLayouts);
            }
            if (!layouts.isEmpty()) {
                props.element("layouts", layouts);
            }
        }

        Map<String, Map<String, Serializable>> defaultPropValues = conf.getDefaultPropertyValues();
        if (defaultPropValues != null && !defaultPropValues.isEmpty()) {
            json.element("defaultPropertyValues", exportPropsByModeToJson(defaultPropValues));
        }

        if (!props.isEmpty()) {
            json.element("properties", props);
        }

        Map<String, Map<String, Serializable>> defaultControlValues = conf.getDefaultControlValues();
        if (defaultControlValues != null && !defaultControlValues.isEmpty()) {
            json.element("defaultControlValues", exportPropsByModeToJson(defaultControlValues));
        }

        return json;
    }

    @SuppressWarnings("unchecked")
    public static WidgetTypeConfiguration importWidgetTypeConfiguration(JSONObject conf) {
        WidgetTypeConfigurationImpl res = new WidgetTypeConfigurationImpl();
        if (conf == null) {
            return res;
        }
        res.setTitle(conf.getString("title"));
        res.setDescription(conf.optString("description"));
        res.setSinceVersion(conf.optString("sinceVersion"));
        res.setDeprecatedVersion(conf.optString("deprecatedVersion"));

        JSONObject demoInfo = conf.optJSONObject("demo");
        String demoId = null;
        boolean demoPreviewEnabled = false;
        if (demoInfo != null && !demoInfo.isNullObject()) {
            demoId = demoInfo.optString("id");
            demoPreviewEnabled = demoInfo.optBoolean("previewEnabled");
        }
        res.setDemoId(demoId);
        res.setDemoPreviewEnabled(demoPreviewEnabled);

        res.setProperties(importProps(conf.optJSONObject("confProperties")));

        List<String> confSupportedModes = new ArrayList<String>();
        JSONArray supportedModes = conf.optJSONArray("supportedModes");
        if (supportedModes != null) {
            confSupportedModes.addAll(supportedModes);
        }
        res.setSupportedModes(confSupportedModes);

        res.setAcceptingSubWidgets(conf.optBoolean("acceptingSubWidgets", false));
        res.setHandlingLabels(conf.optBoolean("handlingLabels", false));
        List<String> confSupportedControls = new ArrayList<String>();
        JSONArray supportedControls = conf.optJSONArray("supportedControls");
        if (supportedControls != null) {
            confSupportedControls.addAll(supportedControls);
        }
        res.setSupportedControls(confSupportedControls);
        res.setContainingForm(conf.optBoolean("containingForm", false));

        JSONObject fields = conf.optJSONObject("fields");
        boolean list = false;
        boolean complex = false;
        List<String> confSupportedTypes = new ArrayList<String>();
        List<String> confDefaultTypes = new ArrayList<String>();
        List<FieldDefinition> defaultFieldDefinitions = new ArrayList<FieldDefinition>();
        Map<String, List<LayoutDefinition>> fieldLayouts = new HashMap<String, List<LayoutDefinition>>();
        if (fields != null && !fields.isNullObject()) {
            list = fields.optBoolean("list", false);
            complex = fields.optBoolean("complex", false);
            JSONArray supportedTypes = fields.optJSONArray("supportedTypes");
            if (supportedTypes != null) {
                confSupportedTypes.addAll(supportedTypes);
            }
            JSONArray defaultTypes = fields.optJSONArray("defaultTypes");
            if (defaultTypes != null) {
                confDefaultTypes.addAll(defaultTypes);
            }
            JSONArray jfields = fields.optJSONArray("defaultConfiguration");
            if (jfields != null) {
                for (Object item : jfields) {
                    defaultFieldDefinitions.add(importFieldDefinition((JSONObject) item));
                }
            }
            JSONObject layouts = fields.optJSONObject("layouts");
            if (layouts != null && !layouts.isNullObject()) {
                for (Object item : layouts.keySet()) {
                    String mode = (String) item;
                    List<LayoutDefinition> layoutDefs = new ArrayList<LayoutDefinition>();
                    JSONArray modeLayouts = layouts.getJSONArray(mode);
                    if (modeLayouts != null && !mode.isEmpty()) {
                        for (Object subitem : modeLayouts) {
                            layoutDefs.add(importLayoutDefinition((JSONObject) subitem));
                        }
                    }
                    fieldLayouts.put(mode, layoutDefs);
                }
            }
        }
        res.setList(list);
        res.setComplex(complex);
        res.setSupportedFieldTypes(confSupportedTypes);
        res.setDefaultFieldTypes(confDefaultTypes);
        res.setDefaultFieldDefinitions(defaultFieldDefinitions);
        res.setFieldLayouts(fieldLayouts);

        JSONArray cats = conf.optJSONArray("categories");
        List<String> confCats = new ArrayList<String>();
        if (cats != null) {
            confCats.addAll(cats);
        }
        res.setCategories(confCats);

        JSONObject props = conf.optJSONObject("properties");
        Map<String, List<LayoutDefinition>> confLayouts = new HashMap<String, List<LayoutDefinition>>();
        if (props != null && !props.isNullObject()) {
            JSONObject layouts = props.optJSONObject("layouts");
            if (layouts != null && !layouts.isNullObject()) {
                for (Object item : layouts.keySet()) {
                    String mode = (String) item;
                    List<LayoutDefinition> layoutDefs = new ArrayList<LayoutDefinition>();
                    JSONArray modeLayouts = layouts.getJSONArray(mode);
                    if (modeLayouts != null && !mode.isEmpty()) {
                        for (Object subitem : modeLayouts) {
                            layoutDefs.add(importLayoutDefinition((JSONObject) subitem));
                        }
                    }
                    confLayouts.put(mode, layoutDefs);
                }
            }
        }

        res.setPropertyLayouts(confLayouts);

        JSONObject defaultPropertyValues = conf.optJSONObject("defaultPropertyValues");
        Map<String, Map<String, Serializable>> confDefaultProps = importPropsByMode(defaultPropertyValues);
        res.setDefaultPropertyValues(confDefaultProps);

        JSONObject defaultControlValues = conf.optJSONObject("defaultControlValues");
        Map<String, Map<String, Serializable>> confDefaultControls = importPropsByMode(defaultControlValues);
        res.setDefaultControlValues(confDefaultControls);

        return res;
    }

    /**
     * @since 6.0
     */
    public static JSONObject exportToJson(LayoutTypeDefinition def) {
        JSONObject json = new JSONObject();
        json.element("name", def.getName());

        List<String> caliases = def.getAliases();
        if (caliases != null && !caliases.isEmpty()) {
            JSONArray aliases = new JSONArray();
            for (String alias : caliases) {
                aliases.add(alias);
            }
            json.element("aliases", aliases);
        }

        JSONObject templates = exportStringPropsToJson(def.getTemplates());
        if (!templates.isEmpty()) {
            json.element("templates", templates);
        }

        LayoutTypeConfiguration conf = def.getConfiguration();
        if (conf != null) {
            json.element("configuration", exportToJson(conf));
        }
        return json;
    }

    /**
     * @since 6.0
     */
    @SuppressWarnings("unchecked")
    public static LayoutTypeDefinition importLayoutTypeDefinition(JSONObject jsonDef) {
        String name = jsonDef.optString("name");
        Map<String, String> templates = importStringProps(jsonDef.optJSONObject("templates"));
        LayoutTypeConfiguration conf = importLayoutTypeConfiguration(jsonDef.optJSONObject("configuration"));
        List<String> aliases = new ArrayList<String>();
        JSONArray jaliases = jsonDef.optJSONArray("aliases");
        if (jaliases != null) {
            aliases.addAll(jaliases);
        }
        LayoutTypeDefinitionImpl res = new LayoutTypeDefinitionImpl(name, templates, conf);
        res.setAliases(aliases);
        return res;
    }

    /**
     * @since 6.0
     */
    public static JSONObject exportToJson(LayoutTypeConfiguration conf) {
        JSONObject json = new JSONObject();
        json.element("title", conf.getTitle());
        json.element("description", conf.getDescription());
        String demoId = conf.getDemoId();
        if (demoId != null) {
            JSONObject demoInfo = new JSONObject();
            demoInfo.element("id", demoId);
            demoInfo.element("previewEnabled", conf.isDemoPreviewEnabled());
            json.element("demo", demoInfo);
        }
        json.element("sinceVersion", conf.getSinceVersion());
        String deprVersion = conf.getDeprecatedVersion();
        if (!StringUtils.isBlank(deprVersion)) {
            json.element("deprecatedVersion", deprVersion);
        }

        JSONArray supportedModes = new JSONArray();
        List<String> confSupportedModes = conf.getSupportedModes();
        if (confSupportedModes != null) {
            supportedModes.addAll(confSupportedModes);
        }
        if (!supportedModes.isEmpty()) {
            json.element("supportedModes", supportedModes);
        }

        if (conf.isHandlingLabels()) {
            json.element("handlingLabels", conf.isHandlingLabels());
        }
        JSONArray supportedControls = new JSONArray();
        List<String> confSupportedControls = conf.getSupportedControls();
        if (confSupportedControls != null) {
            supportedControls.addAll(confSupportedControls);
        }
        if (!supportedControls.isEmpty()) {
            json.element("supportedControls", supportedControls);
        }
        if (conf.isContainingForm()) {
            json.element("containingForm", true);
        }

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
                    modeLayouts.add(exportToJson(null, layoutDef));
                }
                layouts.element(mode, modeLayouts);
            }
            if (!layouts.isEmpty()) {
                props.element("layouts", layouts);
            }
        }

        Map<String, Map<String, Serializable>> defaultPropValues = conf.getDefaultPropertyValues();
        if (defaultPropValues != null && !defaultPropValues.isEmpty()) {
            json.element("defaultPropertyValues", exportPropsByModeToJson(defaultPropValues));
        }

        if (!props.isEmpty()) {
            json.element("properties", props);
        }

        return json;

    }

    /**
     * @since 6.0
     */
    @SuppressWarnings("unchecked")
    public static LayoutTypeConfiguration importLayoutTypeConfiguration(JSONObject conf) {
        LayoutTypeConfigurationImpl res = new LayoutTypeConfigurationImpl();
        if (conf == null) {
            return res;
        }
        res.setTitle(conf.getString("title"));
        res.setDescription(conf.optString("description"));
        res.setSinceVersion(conf.optString("sinceVersion"));
        res.setDeprecatedVersion(conf.optString("deprecatedVersion"));

        JSONObject demoInfo = conf.optJSONObject("demo");
        String demoId = null;
        boolean demoPreviewEnabled = false;
        if (demoInfo != null && !demoInfo.isNullObject()) {
            demoId = demoInfo.optString("id");
            demoPreviewEnabled = demoInfo.optBoolean("previewEnabled");
        }
        res.setDemoId(demoId);
        res.setDemoPreviewEnabled(demoPreviewEnabled);

        List<String> confSupportedModes = new ArrayList<String>();
        JSONArray supportedModes = conf.optJSONArray("supportedModes");
        if (supportedModes != null) {
            confSupportedModes.addAll(supportedModes);
        }
        res.setSupportedModes(confSupportedModes);

        res.setHandlingLabels(conf.optBoolean("handlingLabels", false));
        List<String> confSupportedControls = new ArrayList<String>();
        JSONArray supportedControls = conf.optJSONArray("supportedControls");
        if (supportedControls != null) {
            confSupportedControls.addAll(supportedControls);
        }
        res.setSupportedControls(confSupportedControls);
        res.setContainingForm(conf.optBoolean("containingForm", false));

        JSONArray cats = conf.optJSONArray("categories");
        List<String> confCats = new ArrayList<String>();
        if (cats != null) {
            confCats.addAll(cats);
        }
        res.setCategories(confCats);

        JSONObject props = conf.optJSONObject("properties");
        Map<String, List<LayoutDefinition>> confLayouts = new HashMap<String, List<LayoutDefinition>>();
        if (props != null && !props.isNullObject()) {
            JSONObject layouts = props.optJSONObject("layouts");
            if (layouts != null && !layouts.isNullObject()) {
                for (Object item : layouts.keySet()) {
                    String mode = (String) item;
                    List<LayoutDefinition> layoutDefs = new ArrayList<LayoutDefinition>();
                    JSONArray modeLayouts = layouts.getJSONArray(mode);
                    if (modeLayouts != null && !mode.isEmpty()) {
                        for (Object subitem : modeLayouts) {
                            layoutDefs.add(importLayoutDefinition((JSONObject) subitem));
                        }
                    }
                    confLayouts.put(mode, layoutDefs);
                }
            }
        }

        res.setPropertyLayouts(confLayouts);

        JSONObject defaultPropertyValues = conf.optJSONObject("defaultPropertyValues");
        Map<String, Map<String, Serializable>> confDefaultProps = importPropsByMode(defaultPropertyValues);
        res.setDefaultPropertyValues(confDefaultProps);

        return res;
    }

    public static JSONObject exportToJson(String category, LayoutDefinition layoutDef) {
        return exportToJson(category, layoutDef, null, null);
    }

    /**
     * Returns the JSON export of this layout definition
     *
     * @since 5.5
     * @param category the category of the layout, needed for retrieval of referenced global widgets.
     * @param layoutDef the layout definition
     * @param ctx the widget conversion context
     * @param widgetConverters the list of ordered widget converters to use before export
     */
    public static JSONObject exportToJson(String category, LayoutDefinition layoutDef, LayoutConversionContext ctx,
            List<WidgetDefinitionConverter> widgetConverters) {
        JSONObject json = new JSONObject();
        json.element("name", layoutDef.getName());

        String type = layoutDef.getType();
        if (type != null) {
            json.element("type", type);
        }

        String typeCat = layoutDef.getTypeCategory();
        if (typeCat != null) {
            json.element("typeCategory", typeCat);
        }

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
        List<WidgetReference> widgetsToExport = new ArrayList<WidgetReference>();
        if (defRows != null) {
            int rowIndex = -1;
            for (LayoutRowDefinition layoutRowDef : defRows) {
                rowIndex++;
                rows.add(exportToJson(layoutRowDef, layoutRowDef.getDefaultName(rowIndex)));
                WidgetReference[] widgets = layoutRowDef.getWidgetReferences();
                if (widgets != null) {
                    for (WidgetReference widget : widgets) {
                        widgetsToExport.add(widget);
                    }
                }
            }
        }

        if (!rows.isEmpty()) {
            json.element("rows", rows);
        }
        LayoutStore webLayoutManager = Framework.getService(LayoutStore.class);
        JSONArray widgets = new JSONArray();
        for (WidgetReference widgetRef : widgetsToExport) {
            WidgetDefinition widgetDef = exportWidgetReference(widgetRef, category, layoutDef, ctx, webLayoutManager,
                    widgetConverters);
            if (widgetDef != null) {
                widgets.add(exportToJson(widgetDef, ctx, widgetConverters));

                // also export local subwidgets references
                WidgetReference[] subwidgetRefs = widgetDef.getSubWidgetReferences();
                if (subwidgetRefs != null) {
                    for (WidgetReference subwidgetRef : subwidgetRefs) {
                        WidgetDefinition subwidgetDef = exportWidgetReference(subwidgetRef, category, layoutDef, ctx,
                                webLayoutManager, widgetConverters);
                        if (subwidgetDef != null) {
                            widgets.add(exportToJson(subwidgetDef, ctx, widgetConverters));
                        }
                    }
                }
            }
        }
        if (!widgets.isEmpty()) {
            json.element("widgets", widgets);
        }

        JSONObject renderingInfos = exportRenderingInfosByModeToJson(layoutDef.getRenderingInfos());
        if (!renderingInfos.isEmpty()) {
            json.element("renderingInfos", renderingInfos);
        }

        List<String> caliases = layoutDef.getAliases();
        if (caliases != null && !caliases.isEmpty()) {
            JSONArray aliases = new JSONArray();
            for (String alias : caliases) {
                aliases.add(alias);
            }
            json.element("aliases", aliases);
        }

        return json;
    }

    protected static WidgetDefinition exportWidgetReference(WidgetReference widgetRef, String category,
            LayoutDefinition layoutDef, LayoutConversionContext ctx, LayoutStore webLayoutManager,
            List<WidgetDefinitionConverter> widgetConverters) {
        String widgetName = widgetRef.getName();
        WidgetDefinition widgetDef = layoutDef.getWidgetDefinition(widgetName);
        if (widgetDef == null) {
            String cat = widgetRef.getCategory();
            if (cat == null) {
                cat = category;
            }
            widgetDef = webLayoutManager.getWidgetDefinition(cat, widgetName);
        }
        if (widgetDef == null) {
            log.error(String.format("No definition found for widget '%s' in layout '%s' " + "=> cannot export",
                    widgetName, layoutDef.getName()));
        } else {
            if (widgetConverters != null) {
                for (WidgetDefinitionConverter conv : widgetConverters) {
                    widgetDef = conv.getWidgetDefinition(widgetDef, ctx);
                }
            }
        }
        return widgetDef;
    }

    @SuppressWarnings("unchecked")
    public static LayoutDefinition importLayoutDefinition(JSONObject layoutDef) {
        String name = layoutDef.optString("name", null);
        String type = layoutDef.optString("type", null);
        String typeCat = layoutDef.optString("typeCategory", null);
        Map<String, String> templates = importStringProps(layoutDef.optJSONObject("templates"));
        Map<String, Map<String, Serializable>> properties = importPropsByMode(layoutDef.optJSONObject("properties"));

        List<LayoutRowDefinition> rows = new ArrayList<LayoutRowDefinition>();
        JSONArray jrows = layoutDef.optJSONArray("rows");
        if (jrows != null) {
            for (Object item : jrows) {
                rows.add(importLayoutRowDefinition((JSONObject) item));
            }
        }

        List<WidgetDefinition> widgets = new ArrayList<WidgetDefinition>();
        JSONArray jwidgets = layoutDef.optJSONArray("widgets");
        if (jwidgets != null) {
            for (Object item : jwidgets) {
                widgets.add(importWidgetDefinition((JSONObject) item));
            }
        }

        Map<String, List<RenderingInfo>> renderingInfos = importRenderingInfosByMode(layoutDef.optJSONObject("renderingInfos"));

        List<String> aliases = new ArrayList<String>();
        JSONArray jaliases = layoutDef.optJSONArray("aliases");
        if (jaliases != null) {
            aliases.addAll(jaliases);
        }

        LayoutDefinitionImpl res = new LayoutDefinitionImpl(name, properties, templates, rows, widgets);
        res.setRenderingInfos(renderingInfos);
        res.setType(type);
        res.setTypeCategory(typeCat);
        res.setAliases(aliases);
        return res;
    }

    /**
     * @since 6.0
     */
    public static JSONObject exportToJson(LayoutRowDefinition layoutRowDef, String defaultRowName) {
        JSONObject json = new JSONObject();
        String name = layoutRowDef.getName();
        if (name != null) {
            json.element("name", name);
        } else if (defaultRowName != null) {
            json.element("name", defaultRowName);
        }
        // fill selection info only if that's not the default value from the
        // definition
        if (layoutRowDef.isAlwaysSelected()) {
            json.element("alwaysSelected", true);
        }
        if (!layoutRowDef.isSelectedByDefault()) {
            json.element("selectedByDefault", false);
        }
        JSONObject props = exportPropsByModeToJson(layoutRowDef.getProperties());
        if (!props.isEmpty()) {
            json.element("properties", props);
        }
        JSONArray widgets = new JSONArray();
        WidgetReference[] defWidgets = layoutRowDef.getWidgetReferences();
        if (defWidgets != null) {
            for (WidgetReference widget : defWidgets) {
                widgets.add(exportToJson(widget));
            }
        }
        if (!widgets.isEmpty()) {
            json.element("widgets", widgets);
        }
        return json;
    }

    public static JSONObject exportToJson(LayoutRowDefinition layoutRowDef) {
        return exportToJson(layoutRowDef, null);
    }

    public static LayoutRowDefinition importLayoutRowDefinition(JSONObject layoutRowDef) {
        String name = layoutRowDef.optString("name", null);

        boolean alwaysSelected = layoutRowDef.optBoolean("alwaysSelected", false);
        boolean selectedByDefault = layoutRowDef.optBoolean("selectedByDefault", true);

        Map<String, Map<String, Serializable>> properties = importPropsByMode(layoutRowDef.optJSONObject("properties"));

        List<WidgetReference> widgets = new ArrayList<WidgetReference>();
        JSONArray jwidgets = layoutRowDef.optJSONArray("widgets");
        if (jwidgets != null) {
            for (Object item : jwidgets) {
                if (item instanceof String) {
                    // BBB
                    widgets.add(new WidgetReferenceImpl((String) item));
                } else {
                    widgets.add(importWidgetReference((JSONObject) item));
                }
            }
        }
        return new LayoutRowDefinitionImpl(name, properties, widgets, alwaysSelected, selectedByDefault);
    }

    /**
     * @since 5.5
     * @param widgetDef
     * @param ctx
     * @param widgetConverters
     * @return
     */
    @SuppressWarnings("deprecation")
    public static JSONObject exportToJson(WidgetDefinition widgetDef, LayoutConversionContext ctx,
            List<WidgetDefinitionConverter> widgetConverters) {
        JSONObject json = new JSONObject();
        json.element("name", widgetDef.getName());
        json.element("type", widgetDef.getType());
        json.element("typeCategory", widgetDef.getTypeCategory());
        JSONObject labels = exportStringPropsToJson(widgetDef.getLabels());
        if (!labels.isEmpty()) {
            json.element("labels", labels);
        }
        JSONObject helpLabels = exportStringPropsToJson(widgetDef.getHelpLabels());
        if (!helpLabels.isEmpty()) {
            json.element("helpLabels", helpLabels);
        }
        json.element("translated", widgetDef.isTranslated());
        json.element("handlingLabels", widgetDef.isHandlingLabels());
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
                subWidgets.add(exportToJson(wDef, ctx, widgetConverters));
            }
        }
        if (!subWidgets.isEmpty()) {
            json.element("subWidgets", subWidgets);
        }

        JSONArray subWidgetRefs = new JSONArray();
        WidgetReference[] subWidgetRefDefs = widgetDef.getSubWidgetReferences();
        if (subWidgetRefDefs != null) {
            for (WidgetReference ref : subWidgetRefDefs) {
                subWidgetRefs.add(exportToJson(ref));
            }
        }
        if (!subWidgetRefs.isEmpty()) {
            json.element("subWidgetRefs", subWidgetRefs);
        }

        JSONObject props = exportPropsByModeToJson(widgetDef.getProperties());
        if (!props.isEmpty()) {
            json.element("properties", props);
        }
        JSONObject widgetModeProps = exportPropsByModeToJson(widgetDef.getWidgetModeProperties());
        if (!widgetModeProps.isEmpty()) {
            json.element("propertiesByWidgetMode", widgetModeProps);
        }

        JSONObject controls = exportPropsByModeToJson(widgetDef.getControls());
        if (!controls.isEmpty()) {
            json.element("controls", controls);
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

        JSONObject renderingInfos = exportRenderingInfosByModeToJson(widgetDef.getRenderingInfos());
        if (!renderingInfos.isEmpty()) {
            json.element("renderingInfos", renderingInfos);
        }

        List<String> caliases = widgetDef.getAliases();
        if (caliases != null && !caliases.isEmpty()) {
            JSONArray aliases = new JSONArray();
            for (String alias : caliases) {
                aliases.add(alias);
            }
            json.element("aliases", aliases);
        }

        return json;
    }

    @SuppressWarnings({ "unchecked" })
    public static WidgetDefinition importWidgetDefinition(JSONObject widgetDef) {
        String name = widgetDef.getString("name");
        String type = widgetDef.getString("type");
        String typeCategory = widgetDef.optString("typeCategory");
        Map<String, String> labels = importStringProps(widgetDef.optJSONObject("labels"));
        Map<String, String> helpLabels = importStringProps(widgetDef.optJSONObject("helpLabels"));
        boolean translated = widgetDef.optBoolean("translated", false);
        boolean handlingLabels = widgetDef.optBoolean("handlingLabels", false);
        Map<String, String> modes = importStringProps(widgetDef.optJSONObject("widgetModes"));

        List<FieldDefinition> fieldDefinitions = new ArrayList<FieldDefinition>();
        JSONArray jfields = widgetDef.optJSONArray("fields");
        if (jfields != null) {
            for (Object item : jfields) {
                fieldDefinitions.add(importFieldDefinition((JSONObject) item));
            }
        }

        List<WidgetDefinition> subWidgets = new ArrayList<WidgetDefinition>();
        JSONArray jsubwidgets = widgetDef.optJSONArray("subWidgets");
        if (jsubwidgets != null) {
            for (Object item : jsubwidgets) {
                subWidgets.add(importWidgetDefinition((JSONObject) item));
            }
        }

        List<WidgetReference> subWidgetRefs = new ArrayList<WidgetReference>();
        JSONArray jsubwidgetRefs = widgetDef.optJSONArray("subWidgetRefs");
        if (jsubwidgetRefs != null) {
            for (Object item : jsubwidgetRefs) {
                subWidgetRefs.add(importWidgetReference((JSONObject) item));
            }
        }

        Map<String, Map<String, Serializable>> properties = importPropsByMode(widgetDef.optJSONObject("properties"));
        Map<String, Map<String, Serializable>> widgetModeProperties = importPropsByMode(widgetDef.optJSONObject("propertiesByWidgetMode"));
        Map<String, Map<String, Serializable>> controls = importPropsByMode(widgetDef.optJSONObject("controls"));

        // select options
        List<WidgetSelectOption> selectOptions = new ArrayList<WidgetSelectOption>();
        JSONArray jselectOptions = widgetDef.optJSONArray("selectOptions");
        if (jselectOptions != null) {
            for (Object item : jselectOptions) {
                selectOptions.add(importWidgetSelectionOption((JSONObject) item));
            }
        }

        Map<String, List<RenderingInfo>> renderingInfos = importRenderingInfosByMode(widgetDef.optJSONObject("renderingInfos"));

        List<String> aliases = new ArrayList<String>();
        JSONArray jaliases = widgetDef.optJSONArray("aliases");
        if (jaliases != null) {
            aliases.addAll(jaliases);
        }

        WidgetDefinitionImpl res = new WidgetDefinitionImpl(name, type, labels, helpLabels, translated, modes,
                fieldDefinitions.toArray(new FieldDefinition[] {}), properties, widgetModeProperties,
                subWidgets.toArray(new WidgetDefinition[] {}), selectOptions.toArray(new WidgetSelectOption[] {}));
        res.setRenderingInfos(renderingInfos);
        res.setSubWidgetReferences(subWidgetRefs.toArray(new WidgetReference[] {}));
        res.setHandlingLabels(handlingLabels);
        res.setControls(controls);
        res.setTypeCategory(typeCategory);
        res.setAliases(aliases);
        return res;
    }

    public static JSONObject exportToJson(FieldDefinition fieldDef) {
        JSONObject json = new JSONObject();
        json.element("fieldName", fieldDef.getFieldName());
        json.element("schemaName", fieldDef.getSchemaName());
        json.element("propertyName", fieldDef.getPropertyName());
        return json;
    }

    public static FieldDefinition importFieldDefinition(JSONObject fieldDef) {
        // ignore property name: it can be deduced from schema and field name
        FieldDefinition res = new FieldDefinitionImpl(fieldDef.optString("schemaName", null),
                fieldDef.getString("fieldName"));
        return res;
    }

    public static JSONObject exportToJson(WidgetReference widgetRef) {
        JSONObject json = new JSONObject();
        json.element("name", widgetRef.getName());
        json.element("category", widgetRef.getCategory());
        return json;
    }

    public static WidgetReference importWidgetReference(JSONObject widgetRef) {
        WidgetReference res = new WidgetReferenceImpl(widgetRef.optString("category"),
                widgetRef.optString("name", null));
        return res;
    }

    /**
     * @since 7.3
     */
    public static JSONObject exportToJson(Layout layout) {
        JSONObject json = new JSONObject();
        json.element("name", layout.getName());

        String type = layout.getType();
        if (type != null) {
            json.element("type", type);
        }

        String typeCat = layout.getTypeCategory();
        if (typeCat != null) {
            json.element("typeCategory", typeCat);
        }

        json.element("mode", layout.getMode());

        String template = layout.getTemplate();
        if (template != null) {
            json.element("template", template);
        }

        JSONObject props = exportPropsToJson(layout.getProperties());
        if (!props.isEmpty()) {
            json.element("properties", props);
        }

        JSONArray rows = new JSONArray();
        LayoutRow[] lRows = layout.getRows();
        if (lRows != null) {
            for (LayoutRow lRow : lRows) {
                rows.add(exportToJson(lRow));
            }
        }

        if (!rows.isEmpty()) {
            json.element("rows", rows);
        }

        return json;
    }

    /**
     * @since 7.3
     */
    public static JSONObject exportToJson(LayoutRow layoutRow) {
        JSONObject json = new JSONObject();
        String name = layoutRow.getName();
        if (name != null) {
            json.element("name", name);
        }
        // fill selection info only if that's not the default value from the
        // definition
        if (layoutRow.isAlwaysSelected()) {
            json.element("alwaysSelected", true);
        }
        if (!layoutRow.isSelectedByDefault()) {
            json.element("selectedByDefault", false);
        }
        layoutRow.isSelectedByDefault();
        JSONObject props = exportPropsToJson(layoutRow.getProperties());
        if (!props.isEmpty()) {
            json.element("properties", props);
        }
        JSONArray widgets = new JSONArray();
        Widget[] rowWidgets = layoutRow.getWidgets();
        if (rowWidgets != null) {
            for (Widget widget : rowWidgets) {
                widgets.add(exportToJson(widget));
            }
        }
        if (!widgets.isEmpty()) {
            json.element("widgets", widgets);
        }
        return json;
    }

    /**
     * @since 7.3
     */
    public static JSONObject exportToJson(Widget widget) {
        JSONObject json = new JSONObject();
        json.element("name", widget.getName());
        json.element("type", widget.getType());
        json.element("typeCategory", widget.getTypeCategory());
        json.element("mode", widget.getMode());
        json.element("label", widget.getLabel());
        json.element("helpLabel", widget.getHelpLabel());
        json.element("translated", widget.isTranslated());
        json.element("handlingLabels", widget.isHandlingLabels());
        JSONArray fields = new JSONArray();
        FieldDefinition[] fieldDefs = widget.getFieldDefinitions();
        if (fieldDefs != null) {
            for (FieldDefinition fieldDef : fieldDefs) {
                fields.add(exportToJson(fieldDef));
            }
        }
        if (!fields.isEmpty()) {
            json.element("fields", fields);
        }

        JSONArray subWidgets = new JSONArray();
        Widget[] wSubWidgets = widget.getSubWidgets();
        if (wSubWidgets != null) {
            for (Widget wDef : wSubWidgets) {
                subWidgets.add(exportToJson(wDef));
            }
        }
        if (!subWidgets.isEmpty()) {
            json.element("subWidgets", subWidgets);
        }

        JSONObject props = exportPropsToJson(widget.getProperties());
        if (!props.isEmpty()) {
            json.element("properties", props);
        }

        JSONObject controls = exportPropsToJson(widget.getControls());
        if (!controls.isEmpty()) {
            json.element("controls", controls);
        }

        JSONArray selectOptions = new JSONArray();
        WidgetSelectOption[] selectOptionDefs = widget.getSelectOptions();
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

    public static JSONObject exportToJson(WidgetSelectOption selectOption) {
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
        Map<String, String> labels = selectOption.getItemLabels();
        if (labels != null && !labels.isEmpty()) {
            JSONObject jsonLabels = new JSONObject();
            for (Map.Entry<String, String> entry : labels.entrySet()) {
                jsonLabels.put(entry.getKey(), entry.getValue());
            }
            json.element("itemLabels", jsonLabels);
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

    @SuppressWarnings("unchecked")
    public static WidgetSelectOption importWidgetSelectionOption(JSONObject selectOption) {
        boolean isMulti = selectOption.getBoolean("multiple");
        Serializable value = selectOption.optString("value", null);
        String var = selectOption.optString("var", null);
        String itemLabel = selectOption.optString("itemLabel", null);

        Map<String, String> labels = new HashMap<String, String>();
        JSONObject jsonLabels = selectOption.optJSONObject("itemLabels");
        if (jsonLabels != null && !jsonLabels.isNullObject()) {
            labels.putAll(jsonLabels);
        }

        String itemValue = selectOption.optString("itemValue", null);
        Serializable itemDisabled = selectOption.optString("itemDisabled", null);
        Serializable itemRendered = selectOption.optString("itemRendered", null);
        if (isMulti) {
            String ordering = selectOption.optString("ordering", null);
            Boolean caseSensitive = null;
            if (selectOption.has("caseSensitive")) {
                caseSensitive = new Boolean(selectOption.getBoolean("caseSensitive"));
            }
            WidgetSelectOptionsImpl res = new WidgetSelectOptionsImpl(value, var, itemLabel, itemValue, itemDisabled,
                    itemRendered, ordering, caseSensitive);
            res.setItemLabels(labels);
            return res;
        } else {
            WidgetSelectOptionImpl res = new WidgetSelectOptionImpl(value, var, itemLabel, itemValue, itemDisabled,
                    itemRendered);
            res.setItemLabels(labels);
            return res;
        }
    }

    public static JSONObject exportPropsByModeToJson(Map<String, Map<String, Serializable>> propsByMode) {
        JSONObject props = new JSONObject();
        if (propsByMode != null) {
            List<String> defModes = new ArrayList<String>(propsByMode.keySet());
            // sort so that order is deterministic
            Collections.sort(defModes);
            for (String defMode : defModes) {
                props.element(defMode, exportPropsToJson(propsByMode.get(defMode)));
            }
        }
        return props;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Map<String, Serializable>> importPropsByMode(JSONObject propsByMode) {
        Map<String, Map<String, Serializable>> props = new HashMap<String, Map<String, Serializable>>();
        if (propsByMode != null && !propsByMode.isNullObject()) {
            List<String> defModes = new ArrayList<String>(propsByMode.keySet());
            // sort so that order is deterministic
            Collections.sort(defModes);
            for (String defMode : defModes) {
                props.put(defMode, importProps(propsByMode.getJSONObject(defMode)));
            }
        }
        return props;
    }

    @SuppressWarnings({ "rawtypes" })
    public static JSONObject exportPropsToJson(Map<String, Serializable> defProps) {
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

    @SuppressWarnings("unchecked")
    public static Map<String, Serializable> importProps(JSONObject defProps) {
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        if (defProps != null && !defProps.isNullObject()) {
            List<String> defPropNames = new ArrayList<String>(defProps.keySet());
            // sort so that order is deterministic
            Collections.sort(defPropNames);
            for (String defPropName : defPropNames) {
                Object value = defProps.opt(defPropName);
                if (value instanceof JSONArray) {
                    ArrayList<Object> listValue = new ArrayList<Object>();
                    listValue.addAll(((JSONArray) value));
                } else {
                    props.put(defPropName, value.toString());
                }
            }
        }
        return props;
    }

    public static JSONObject exportStringPropsToJson(Map<String, String> defProps) {
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

    public static Map<String, String> importStringProps(JSONObject defProps) {
        Map<String, String> props = new HashMap<String, String>();
        if (defProps != null && !defProps.isNullObject()) {
            for (Object item : defProps.keySet()) {
                String key = (String) item;
                props.put(key, defProps.getString(key));
            }
        }
        return props;
    }

    public static JSONObject exportRenderingInfosByModeToJson(Map<String, List<RenderingInfo>> infosByMode) {
        JSONObject props = new JSONObject();
        if (infosByMode != null) {
            List<String> defModes = new ArrayList<String>(infosByMode.keySet());
            // sort so that order is deterministic
            Collections.sort(defModes);
            for (String defMode : defModes) {
                props.element(defMode, exportRenderingInfosToJson(infosByMode.get(defMode)));
            }
        }
        return props;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, List<RenderingInfo>> importRenderingInfosByMode(JSONObject infosByMode) {
        Map<String, List<RenderingInfo>> props = new HashMap<String, List<RenderingInfo>>();
        if (infosByMode != null && !infosByMode.isNullObject()) {
            List<String> defModes = new ArrayList<String>(infosByMode.keySet());
            // sort so that order is deterministic
            Collections.sort(defModes);
            for (String defMode : defModes) {
                props.put(defMode, importRenderingInfos(infosByMode.getJSONArray(defMode)));
            }
        }
        return props;
    }

    public static JSONArray exportRenderingInfosToJson(List<RenderingInfo> infos) {
        JSONArray jinfos = new JSONArray();
        if (infos != null) {
            for (RenderingInfo info : infos) {
                jinfos.add(exportToJson(info));
            }
        }
        return jinfos;
    }

    public static List<RenderingInfo> importRenderingInfos(JSONArray jinfos) {
        List<RenderingInfo> infos = new ArrayList<RenderingInfo>();
        if (jinfos != null) {
            for (Object item : jinfos) {
                infos.add(importRenderingInfo((JSONObject) item));
            }
        }
        return infos;
    }

    public static JSONObject exportToJson(RenderingInfo info) {
        JSONObject json = new JSONObject();
        json.element("level", info.getLevel());
        json.element("message", info.getMessage());
        json.element("translated", info.isTranslated());
        return json;
    }

    public static RenderingInfo importRenderingInfo(JSONObject fieldDef) {
        RenderingInfo res = new RenderingInfoImpl(fieldDef.optString("level", ""), fieldDef.optString("message"),
                fieldDef.optBoolean("translated", false));
        return res;
    }

}
