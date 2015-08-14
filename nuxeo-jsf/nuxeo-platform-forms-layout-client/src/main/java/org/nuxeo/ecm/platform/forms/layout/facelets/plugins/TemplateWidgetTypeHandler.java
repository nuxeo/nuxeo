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
 * $Id: TemplateWidgetTypeHandler.java 28244 2007-12-18 19:44:57Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets.plugins;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.view.facelets.CompositeFaceletHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributes;
import javax.faces.view.facelets.TagConfig;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.WidgetException;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.forms.layout.facelets.RenderVariables;
import org.nuxeo.ecm.platform.forms.layout.facelets.ValueExpressionHelper;
import org.nuxeo.ecm.platform.ui.web.binding.BlockingVariableMapper;
import org.nuxeo.ecm.platform.ui.web.binding.MapValueExpression;
import org.nuxeo.ecm.platform.ui.web.tag.handler.LeafFaceletHandler;
import org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory;

import com.sun.faces.facelets.tag.ui.DecorateHandler;

/**
 * Template widget type.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class TemplateWidgetTypeHandler extends AbstractWidgetTypeHandler {

    private static final Log log = LogFactory.getLog(TemplateWidgetTypeHandler.class);

    private static final long serialVersionUID = 6886289896957398368L;

    public static final String TEMPLATE_PROPERTY_NAME = "template";

    /**
     * Property that can be put on the widget type definition to decide whether the widget type should bind to parent
     * value when no field is set
     *
     * @since 5.6
     */
    public static final String BIND_VALUE_IF_NO_FIELD_PROPERTY_NAME = "bindValueIfNoField";

    @Override
    public FaceletHandler getFaceletHandler(FaceletContext ctx, TagConfig tagConfig, Widget widget,
            FaceletHandler[] subHandlers) throws WidgetException {
        String template = getTemplateValue(widget);
        FaceletHandler leaf = new LeafFaceletHandler();
        if (template == null) {
            log.error("Missing template property for widget " + widget.getName() + " in layout "
                    + widget.getLayoutName());
            return leaf;
        }
        FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, tagConfig);
        String widgetId = widget.getId();
        TagAttributes attributes = helper.getTagAttributes(widgetId, widget);
        TagAttribute templateAttr = getTemplateAttribute(helper);
        if (templateAttr == null) {
            templateAttr = helper.createAttribute(TEMPLATE_PROPERTY_NAME, template);
        }
        attributes = FaceletHandlerHelper.addTagAttribute(attributes, templateAttr);
        String widgetTagConfigId = widget.getTagConfigId();
        FaceletHandler nextHandler = leaf;
        if (subHandlers != null) {
            nextHandler = new CompositeFaceletHandler(subHandlers);
        }

        TagConfig config = TagConfigFactory.createTagConfig(tagConfig, widgetTagConfigId, attributes, nextHandler);

        VariableMapper cvm = ctx.getVariableMapper();
        if (!(cvm instanceof BlockingVariableMapper)) {
            throw new IllegalArgumentException(
                    "Current context variable mapper should be an instance of MetaVariableMapper");
        }
        BlockingVariableMapper vm = (BlockingVariableMapper) cvm;
        fillVariablesForRendering(ctx, vm, widget, subHandlers, widgetTagConfigId, template);

        return new DecorateHandler(config);
    }

    /**
     * Computes variables for rendering, making available the field values in templates using the format "field_0",
     * "field_1", etc. and also the widget properties using the format "widgetProperty_thePropertyName".
     */
    protected void fillVariablesForRendering(FaceletContext ctx, BlockingVariableMapper vm, Widget widget,
            FaceletHandler[] subHandlers, String widgetTagConfigId, String template) {
        ExpressionFactory eFactory = ctx.getExpressionFactory();

        FieldDefinition[] fieldDefs = widget.getFieldDefinitions();
        // expose field variables
        FieldDefinition firstField = null;
        if (fieldDefs != null && fieldDefs.length > 0) {
            for (int i = 0; i < fieldDefs.length; i++) {
                if (i == 0) {
                    addFieldVariable(ctx, eFactory, vm, widget, fieldDefs[i], null);
                    firstField = fieldDefs[i];
                }
                addFieldVariable(ctx, eFactory, vm, widget, fieldDefs[i], Integer.valueOf(i));
            }
        } else if (getBindValueIfNoFieldValue(widget)) {
            // expose value as first parameter
            addFieldVariable(ctx, eFactory, vm, widget, null, null);
            addFieldVariable(ctx, eFactory, vm, widget, null, Integer.valueOf(0));
        }
        vm.addBlockedPattern(RenderVariables.widgetVariables.field.name() + "*");

        // add binding "fieldOrValue" available since 5.6, in case template
        // widget is always supposed to bind value when no field is defined
        String computedValue = ValueExpressionHelper.createExpressionString(widget.getValueName(), firstField);
        vm.setVariable(RenderVariables.widgetVariables.fieldOrValue.name(),
                eFactory.createValueExpression(ctx, computedValue, Object.class));
        vm.addBlockedPattern(RenderVariables.widgetVariables.fieldOrValue.name());

        // expose widget properties too
        Map<String, ValueExpression> mappedExpressions = new HashMap<String, ValueExpression>();
        for (Map.Entry<String, Serializable> prop : widget.getProperties().entrySet()) {
            String key = prop.getKey();
            String name = RenderVariables.widgetVariables.widgetProperty.name() + "_" + key;
            ValueExpression ve = eFactory.createValueExpression(prop.getValue(), Object.class);
            vm.setVariable(name, ve);
            mappedExpressions.put(key, ve);
        }
        vm.addBlockedPattern(RenderVariables.widgetVariables.widgetProperty.name() + "_*");
        vm.setVariable(RenderVariables.widgetVariables.widgetProperties.name(), new MapValueExpression(
                mappedExpressions));
        vm.addBlockedPattern(RenderVariables.widgetVariables.widgetProperties.name());
        // expose widget controls too
        for (Map.Entry<String, Serializable> ctrl : widget.getControls().entrySet()) {
            String key = ctrl.getKey();
            String name = RenderVariables.widgetVariables.widgetControl.name() + "_" + key;
            vm.setVariable(name, eFactory.createValueExpression(ctrl.getValue(), Object.class));
        }
        vm.addBlockedPattern(RenderVariables.widgetVariables.widgetControl.name() + "_*");
    }

    protected void addFieldVariable(FaceletContext ctx, ExpressionFactory eFactory, BlockingVariableMapper vm,
            Widget widget, FieldDefinition fieldDef, Integer index) {
        String computedName;
        if (index == null) {
            computedName = RenderVariables.widgetVariables.field.name();
        } else {
            computedName = RenderVariables.widgetVariables.field.name() + "_" + index;
        }
        String computedValue = ValueExpressionHelper.createExpressionString(widget.getValueName(), fieldDef);
        vm.setVariable(computedName, eFactory.createValueExpression(ctx, computedValue, Object.class));
    }

    /**
     * Returns the "template" property value, looking up on the widget definition definition first, and on the widget
     * type definition if not found.
     */
    protected String getTemplateValue(Widget widget) {
        return lookupProperty(TEMPLATE_PROPERTY_NAME, widget);
    }

    /**
     * Helper method to retrieve a property value, looking up on the widget definition first, and on the widget type
     * definition if not found.
     *
     * @since 7.2
     */
    protected String lookupProperty(String name, Widget widget) {
        // lookup in the widget configuration
        String val = (String) widget.getProperty(name);
        if (val == null) {
            // lookup in the widget type configuration
            val = getProperty(name);
        }
        return val;
    }

    /**
     * Returns the "bindValueIfNoField" property value, looking up on the widget type definition first, and on the
     * widget definition if not found.
     *
     * @since 5.6
     * @param widget
     * @return
     */
    protected boolean getBindValueIfNoFieldValue(Widget widget) {
        Object value = getProperty(BIND_VALUE_IF_NO_FIELD_PROPERTY_NAME);
        if (value == null) {
            value = widget.getProperty(BIND_VALUE_IF_NO_FIELD_PROPERTY_NAME);
        }
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return Boolean.TRUE.equals(value);
        }
        return Boolean.TRUE.equals(Boolean.valueOf(value.toString()));

    }

    /**
     * Returns the template attribute.
     */
    protected TagAttribute getTemplateAttribute(FaceletHandlerHelper helper) {
        // do not return anything as it will be computed from the widget
        // properties anyway.
        return null;
    }

}
