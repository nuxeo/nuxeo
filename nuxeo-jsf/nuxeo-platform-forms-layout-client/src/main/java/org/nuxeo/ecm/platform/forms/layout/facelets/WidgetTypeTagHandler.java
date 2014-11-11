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
import javax.faces.FacesException;
import javax.faces.component.UIComponent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.impl.FieldDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.facelets.plugins.TemplateWidgetTypeHandler;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;
import org.nuxeo.runtime.api.Framework;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.el.VariableMapperWrapper;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagHandler;

/**
 * Widget type tag handler.
 * <p>
 * Applies a {@link WidgetTypeHandler} resolved from a widget created for given
 * type name and mode, and uses other tag attributes to fill the widget
 * properties.
 * <p>
 * Does not handle sub widgets.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class WidgetTypeTagHandler extends TagHandler {

    private static final Log log = LogFactory.getLog(WidgetTypeTagHandler.class);

    protected final TagConfig config;

    protected final TagAttribute name;

    protected final TagAttribute mode;

    protected final TagAttribute value;

    protected final TagAttribute field;

    protected final TagAttribute fields;

    protected final TagAttribute label;

    protected final TagAttribute helpLabel;

    protected final TagAttribute translated;

    protected final TagAttribute properties;

    /**
     * Convenient attribute to remove the "template" property from widget
     * properties (and avoid stack overflow errors when using another widget
     * type in a widget template, for compatibility code for instance).
     *
     * @since 5.6
     */
    protected final TagAttribute ignoreTemplateProperty;

    /**
     * @since 5.6
     */
    protected final TagAttribute subWidgets;

    protected final TagAttribute[] vars;

    protected final String[] reservedVarsArray = { "id", "mode", "type",
            "properties", "ignoreTemplateProperty", "subWidgets" };

    public WidgetTypeTagHandler(TagConfig config) {
        super(config);
        this.config = config;
        name = getRequiredAttribute("name");
        mode = getRequiredAttribute("mode");
        value = getAttribute("value");
        field = getAttribute("field");
        fields = getAttribute("fields");
        label = getAttribute("label");
        helpLabel = getAttribute("helpLabel");
        translated = getAttribute("translated");
        properties = getAttribute("properties");
        ignoreTemplateProperty = getAttribute("ignoreTemplateProperty");
        subWidgets = getAttribute("subWidgets");
        vars = tag.getAttributes().getAll();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public final void apply(FaceletContext ctx, UIComponent parent)
            throws IOException, FacesException, ELException {
        WebLayoutManager layoutService;
        try {
            layoutService = Framework.getService(WebLayoutManager.class);
        } catch (Exception e) {
            throw new FacesException(e);
        }
        if (layoutService == null) {
            throw new FacesException("Layout service not found");
        }

        // build handler
        List<String> reservedVars = Arrays.asList(reservedVarsArray);
        Map<String, Serializable> widgetProps = new HashMap<String, Serializable>();
        if (properties != null) {
            Map<String, Serializable> propertiesValue = (Map<String, Serializable>) properties.getObject(
                    ctx, Map.class);
            if (propertiesValue != null) {
                widgetProps.putAll(propertiesValue);
            }
        }
        for (TagAttribute var : vars) {
            String localName = var.getLocalName();
            if (!reservedVars.contains(localName)) {
                widgetProps.put(localName, var.getValue());
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
        String modeValue = mode.getValue(ctx);
        String valueName = null;
        if (value != null) {
            valueName = value.getValue();
            if (ComponentTagUtils.isStrictValueReference(valueName)) {
                valueName = ComponentTagUtils.getBareValueName(valueName);
            }
        }
        List<FieldDefinition> fieldsValue = new ArrayList<FieldDefinition>();
        if (field != null) {
            Object fieldValue = field.getValue(ctx);
            if (fieldValue instanceof FieldDefinition) {
                fieldsValue.add((FieldDefinition) fieldValue);
            } else if (fieldValue instanceof String) {
                fieldsValue.add(new FieldDefinitionImpl(null,
                        (String) fieldValue));
            } else {
                log.error("Invalid field item => discard: " + fieldValue);
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
            subWidgetsValue = (Widget[]) subWidgets.getObject(ctx,
                    Widget[].class);
        }

        Widget widget = layoutService.createWidget(ctx, typeValue, modeValue,
                valueName, fieldsValue, labelValue, helpLabelValue,
                translatedValue, widgetProps, subWidgetsValue);

        // expose widget variable
        VariableMapper orig = ctx.getVariableMapper();
        VariableMapper vm = new VariableMapperWrapper(orig);
        ctx.setVariableMapper(vm);
        ValueExpression widgetVe = ctx.getExpressionFactory().createValueExpression(
                widget, Widget.class);
        vm.setVariable(RenderVariables.widgetVariables.widget.name(), widgetVe);
        vm.setVariable(
                String.format("%s_%s",
                        RenderVariables.widgetVariables.widget.name(),
                        Integer.valueOf(widget.getLevel())), widgetVe);
        try {
            WidgetTagHandler.applyWidgetHandler(ctx, parent, config, widget,
                    value, true, nextHandler);
        } finally {
            ctx.setVariableMapper(orig);
        }
    }

}
