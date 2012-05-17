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
import javax.faces.FacesException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.WidgetException;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.forms.layout.facelets.LeafFaceletHandler;
import org.nuxeo.ecm.platform.forms.layout.facelets.RenderVariables;
import org.nuxeo.ecm.platform.forms.layout.facelets.TagConfigFactory;
import org.nuxeo.ecm.platform.forms.layout.facelets.ValueExpressionHelper;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.runtime.api.Framework;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletHandler;
import com.sun.facelets.tag.CompositeFaceletHandler;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagAttributes;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.ui.DecorateHandler;

/**
 * Template widget type.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class TemplateWidgetTypeHandler extends AbstractWidgetTypeHandler {

    private static final Log log = LogFactory.getLog(TemplateWidgetTypeHandler.class);

    private static final long serialVersionUID = 6886289896957398368L;

    public static final String TEMPLATE_PROPERTY_NAME = "template";

    @Override
    public FaceletHandler getFaceletHandler(FaceletContext ctx,
            TagConfig tagConfig, Widget widget, FaceletHandler[] subHandlers)
            throws WidgetException {
        String template = getTemplateValue(widget);
        FaceletHandler leaf = new LeafFaceletHandler();
        if (template == null) {
            log.error("Missing template property for widget "
                    + widget.getName() + " in layout " + widget.getLayoutName());
            return leaf;
        }
        FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, tagConfig);
        String widgetId = widget.getId();
        TagAttributes attributes = helper.getTagAttributes(widgetId, widget);
        TagAttribute templateAttr = getTemplateAttribute(helper);
        if (templateAttr == null) {
            templateAttr = helper.createAttribute(TEMPLATE_PROPERTY_NAME,
                    template);
        }
        attributes = FaceletHandlerHelper.addTagAttribute(attributes,
                templateAttr);
        String widgetTagConfigId = widget.getTagConfigId();
        FaceletHandler nextHandler = leaf;
        if (subHandlers != null) {
            nextHandler = new CompositeFaceletHandler(subHandlers);
        }

        TagConfig config = TagConfigFactory.createTagConfig(tagConfig,
                widgetTagConfigId, attributes, nextHandler);

        Map<String, ValueExpression> variables = getVariablesForRendering(ctx,
                helper, widget, subHandlers, widgetTagConfigId, template);
        DecorateHandler includeHandler = new DecorateHandler(config);
        FaceletHandler handler = helper.getAliasTagHandler(widgetTagConfigId,
                variables, includeHandler);
        return handler;
    }

    /**
     * Computes variables for rendering, making available the field values in
     * templates using the format "field_0", "field_1", etc. and also the
     * widget properties using the format "widgetProperty_thePropertyName".
     */
    protected Map<String, ValueExpression> getVariablesForRendering(
            FaceletContext ctx, FaceletHandlerHelper helper, Widget widget,
            FaceletHandler[] subHandlers, String widgetTagConfigId,
            String template) {
        Map<String, ValueExpression> variables = new HashMap<String, ValueExpression>();
        ExpressionFactory eFactory = ctx.getExpressionFactory();

        FieldDefinition[] fieldDefs = widget.getFieldDefinitions();
        // expose field variables
        if (fieldDefs != null && fieldDefs.length > 0) {
            for (int i = 0; i < fieldDefs.length; i++) {
                if (i == 0) {
                    addFieldVariable(variables, ctx, widget, fieldDefs[i], null);
                }
                addFieldVariable(variables, ctx, widget, fieldDefs[i],
                        Integer.valueOf(i));
            }
        } else {
            // expose value as first parameter
            addFieldVariable(variables, ctx, widget, null, null);
            addFieldVariable(variables, ctx, widget, null, Integer.valueOf(0));
        }

        // expose widget properties too
        WebLayoutManager layoutService;
        try {
            layoutService = Framework.getService(WebLayoutManager.class);
        } catch (Exception e) {
            throw new FacesException(e);
        }
        if (layoutService == null) {
            throw new FacesException("Layout service not found");
        }
        for (Map.Entry<String, Serializable> prop : widget.getProperties().entrySet()) {
            String key = prop.getKey();
            String name = String.format("%s_%s",
                    RenderVariables.widgetVariables.widgetProperty.name(), key);
            String value;
            Serializable valueInstance = prop.getValue();
            if (!layoutService.referencePropertyAsExpression(key,
                    valueInstance, widget.getType(), widget.getMode(), template)) {
                // FIXME: this will not be updated correctly using ajax
                value = (String) valueInstance;
            } else {
                // create a reference so that it's a real expression and it's
                // not kept (cached) in a component value on ajax refresh
                value = String.format("#{%s.properties.%s}",
                        RenderVariables.widgetVariables.widget.name(), key);
            }
            variables.put(name,
                    eFactory.createValueExpression(ctx, value, Object.class));
        }
        return variables;
    }

    protected void addFieldVariable(Map<String, ValueExpression> variables,
            FaceletContext ctx, Widget widget, FieldDefinition fieldDef,
            Integer index) {
        String computedName;
        if (index == null) {
            computedName = String.format("%s",
                    RenderVariables.widgetVariables.field.name());
        } else {
            computedName = String.format("%s_%s",
                    RenderVariables.widgetVariables.field.name(), index);
        }
        String computedValue = ValueExpressionHelper.createExpressionString(
                widget.getValueName(), fieldDef);

        ExpressionFactory eFactory = ctx.getExpressionFactory();
        variables.put(
                computedName,
                eFactory.createValueExpression(ctx, computedValue, Object.class));
    }

    /**
     * Returns the template value.
     */
    protected String getTemplateValue(Widget widget) {
        // lookup in the widget type configuration
        String template = getProperty(TEMPLATE_PROPERTY_NAME);
        if (template == null) {
            // lookup in the widget configuration
            template = (String) widget.getProperty(TEMPLATE_PROPERTY_NAME);
        }
        return template;
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
