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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: WidgetTypeTagHandler.java 26053 2007-10-16 01:45:43Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.impl.FieldDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.facelets.plugins.TemplateWidgetTypeHandler;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.ecm.platform.ui.web.binding.BlockingVariableMapper;
import org.nuxeo.ecm.platform.ui.web.tag.handler.SetTagHandler;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;
import org.nuxeo.runtime.api.Framework;

import com.sun.faces.facelets.el.VariableMapperWrapper;

/**
 * Widget type tag handler.
 * <p>
 * Applies a {@link WidgetTypeHandler} resolved from a widget created for given type name and mode, and uses other tag
 * attributes to fill the widget properties.
 * <p>
 * Does not handle sub widgets.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class WidgetTypeTagHandler extends TagHandler {

    private static final Log log = LogFactory.getLog(WidgetTypeTagHandler.class);

    protected final TagConfig config;

    protected final TagAttribute name;

    protected final TagAttribute category;

    protected final TagAttribute mode;

    protected final TagAttribute value;

    protected final TagAttribute field;

    protected final TagAttribute fields;

    protected final TagAttribute label;

    protected final TagAttribute helpLabel;

    protected final TagAttribute translated;

    protected final TagAttribute properties;

    /**
     * @since 5.7
     */
    protected final TagAttribute widgetName;

    /**
     * Convenient attribute to remove the "template" property from widget properties (and avoid stack overflow errors
     * when using another widget type in a widget template, for compatibility code for instance).
     *
     * @since 5.6
     */
    protected final TagAttribute ignoreTemplateProperty;

    /**
     * @since 5.6
     */
    protected final TagAttribute subWidgets;

    protected final TagAttribute resolveOnly;

    protected final TagAttribute[] vars;

    protected final String[] reservedVarsArray = { "id", "name", "category", "mode", "value", "type", "field", "fields",
            "widgetName", "label", "helpLabel", "translated", "properties", "ignoreTemplateProperty", "subWidgets",
            "resolveOnly" };

    public WidgetTypeTagHandler(TagConfig config) {
        super(config);
        this.config = config;
        name = getRequiredAttribute("name");
        category = getAttribute("category");
        mode = getRequiredAttribute("mode");
        value = getAttribute("value");
        field = getAttribute("field");
        fields = getAttribute("fields");
        widgetName = getAttribute("widgetName");
        label = getAttribute("label");
        helpLabel = getAttribute("helpLabel");
        translated = getAttribute("translated");
        properties = getAttribute("properties");
        ignoreTemplateProperty = getAttribute("ignoreTemplateProperty");
        subWidgets = getAttribute("subWidgets");
        resolveOnly = getAttribute("resolveOnly");
        vars = tag.getAttributes().getAll();
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public final void apply(FaceletContext ctx, UIComponent parent) throws IOException, ELException {
        WebLayoutManager layoutService = Framework.getService(WebLayoutManager.class);

        // compute field definitions
        List<FieldDefinition> fieldsValue = new ArrayList<FieldDefinition>();
        if (field != null) {
            Object fieldValue = field.getObject(ctx, Object.class);
            if (fieldValue instanceof FieldDefinition) {
                fieldsValue.add((FieldDefinition) fieldValue);
            } else if (fieldValue instanceof String) {
                fieldsValue.add(new FieldDefinitionImpl(null, (String) fieldValue));
            } else {
                fieldsValue.add(new FieldDefinitionImpl(null, field.getValue()));
            }
        }
        if (fields != null) {
            List resolvedfields = (List) fields.getObject(ctx, List.class);
            for (Object item : resolvedfields) {
                if (item instanceof FieldDefinition) {
                    fieldsValue.add((FieldDefinition) item);
                } else if (item instanceof String) {
                    fieldsValue.add(new FieldDefinitionImpl(null, (String) item));
                } else {
                    log.error("Invalid field item => discard: " + item);
                }
            }
        }

        // build handler
        List<String> reservedVars = Arrays.asList(reservedVarsArray);
        Map<String, Serializable> widgetProps = new HashMap<String, Serializable>();
        if (properties != null) {
            Map<String, Serializable> propertiesValue = (Map<String, Serializable>) properties.getObject(ctx,
                    Map.class);
            if (propertiesValue != null) {
                widgetProps.putAll(propertiesValue);
            }
        }

        // do not propagate value the value attribute to the widget
        // properties if field definitions should be taken into account
        // instead
        String widgetPropertyMarker = RenderVariables.widgetVariables.widgetProperty.name() + "_";
        boolean includeValueInProps = fieldsValue.isEmpty();
        for (TagAttribute var : vars) {
            String localName = var.getLocalName();
            if ((!reservedVars.contains(localName)) || ("value".equals(localName) && includeValueInProps)) {
                String varName = localName;
                if (localName != null && localName.startsWith(widgetPropertyMarker)) {
                    varName = localName.substring(widgetPropertyMarker.length());
                }
                widgetProps.put(varName, var.getValue());
            }
        }

        boolean ignoreTemplatePropValue = false;
        if (ignoreTemplateProperty != null) {
            ignoreTemplatePropValue = ignoreTemplateProperty.getBoolean(ctx);
        }
        if (ignoreTemplatePropValue) {
            widgetProps.remove(TemplateWidgetTypeHandler.TEMPLATE_PROPERTY_NAME);
        }

        String typeValue = name.getValue(ctx);
        String categoryValue = null;
        if (category != null) {
            categoryValue = category.getValue(ctx);
        }
        String modeValue = mode.getValue(ctx);
        String valueName = null;
        if (value != null) {
            valueName = value.getValue();
            if (ComponentTagUtils.isStrictValueReference(valueName)) {
                valueName = ComponentTagUtils.getBareValueName(valueName);
            }
        }
        String widgetNameValue = null;
        if (widgetName != null) {
            widgetNameValue = widgetName.getValue(ctx);
        }
        String labelValue = null;
        if (label != null) {
            labelValue = label.getValue(ctx);
        }
        String helpLabelValue = null;
        if (helpLabel != null) {
            helpLabelValue = helpLabel.getValue(ctx);
        }
        Boolean translatedValue = Boolean.FALSE;
        if (translated != null) {
            translatedValue = Boolean.valueOf(translated.getBoolean(ctx));
        }

        Widget[] subWidgetsValue = null;
        if (subWidgets != null) {
            subWidgetsValue = (Widget[]) subWidgets.getObject(ctx, Widget[].class);
        }

        // avoid double markers
        if (widgetNameValue != null && widgetNameValue.startsWith(FaceletHandlerHelper.WIDGET_ID_PREFIX)) {
            widgetNameValue = widgetNameValue.substring(FaceletHandlerHelper.WIDGET_ID_PREFIX.length());
        }
        if (StringUtils.isBlank(widgetNameValue)) {
            widgetNameValue = typeValue;
        }
        WidgetDefinitionImpl wDef = new WidgetDefinitionImpl(widgetNameValue, typeValue, labelValue, helpLabelValue,
                translatedValue.booleanValue(), null, fieldsValue, widgetProps, null);
        wDef.setTypeCategory(categoryValue);
        wDef.setDynamic(true);
        Widget widget = layoutService.createWidget(ctx, wDef, modeValue, valueName, subWidgetsValue);

        if (FaceletHandlerHelper.isAliasOptimEnabled()) {
            applyOptimized(ctx, parent, widget);
        } else {
            applyCompat(ctx, parent, widget);
        }
    }

    protected void applyOptimized(FaceletContext ctx, UIComponent parent, Widget widget)
            throws IOException, ELException {
        // expose widget variable
        VariableMapper orig = ctx.getVariableMapper();
        try {
            BlockingVariableMapper vm = new BlockingVariableMapper(orig);
            ctx.setVariableMapper(vm);

            // set unique id on widget before exposing it to the context
            FaceletHandlerHelper helper = new FaceletHandlerHelper(config);
            WidgetTagHandler.generateWidgetId(ctx, helper, widget, false);

            // TODO NXP-13280: retrieve widget controls from tag attributes before exposure
            WidgetTagHandler.exposeWidgetVariables(ctx, vm, widget, null, true);

            boolean resolveOnlyBool = false;
            if (resolveOnly != null) {
                resolveOnlyBool = resolveOnly.getBoolean(ctx);
            }
            if (resolveOnlyBool) {
                nextHandler.apply(ctx, parent);
            } else {
                WidgetTagHandler.applyWidgetHandler(ctx, parent, config, widget, value, true, nextHandler);
            }
        } finally {
            ctx.setVariableMapper(orig);
        }
    }

    protected void applyCompat(FaceletContext ctx, UIComponent parent, Widget widget) throws IOException, ELException {
        // expose widget variable
        VariableMapper orig = ctx.getVariableMapper();
        VariableMapper vm = new VariableMapperWrapper(orig);
        ctx.setVariableMapper(vm);
        ValueExpression widgetVe = ctx.getExpressionFactory().createValueExpression(widget, Widget.class);
        vm.setVariable(RenderVariables.widgetVariables.widget.name(), widgetVe);
        vm.setVariable(RenderVariables.widgetVariables.widget.name() + "_" + widget.getLevel(), widgetVe);
        // TODO NXP-13280: expose widget controls too when they can be
        // retrieved from tag attributes
        try {
            // set unique id on widget before exposing it to the context
            FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, config);
            WidgetTagHandler.generateWidgetId(helper, widget, false);

            boolean resolveOnlyBool = false;
            if (resolveOnly != null) {
                resolveOnlyBool = resolveOnly.getBoolean(ctx);
            }
            if (resolveOnlyBool) {
                // NXP-12882: wrap handler in an nxu:set tag to avoid duplicate
                // id issue when widget definition changes, as component ids
                // can be cached and not generated-again on ajax re-render,
                // this is a quick fix that can be optimized, as the widget
                // variable is already exposed in the current variable mapper.
                // Update after NXP-15050: this does not seem to be necessary
                // anymore, could not reproduce the corresponding bug, to
                // remove after complementary tests.
                String setTagConfigId = widget.getTagConfigId();
                ComponentConfig aliasConfig = org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory.createAliasTagConfig(
                        this.config, setTagConfigId, RenderVariables.widgetVariables.widget.name(), "#{widget}", "true",
                        "true", nextHandler);
                FaceletHandler handler = new SetTagHandler(aliasConfig);
                handler.apply(ctx, parent);
            } else {
                WidgetTagHandler.applyWidgetHandler(ctx, parent, config, widget, value, true, nextHandler);
            }
        } finally {
            ctx.setVariableMapper(orig);
        }
    }

}
