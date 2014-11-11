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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletHandler;
import com.sun.facelets.tag.CompositeFaceletHandler;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagAttributes;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.ui.DecorateHandler;
import com.sun.facelets.tag.ui.ParamHandler;

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
        TagConfig config = TagConfigFactory.createTagConfig(tagConfig,
                attributes, getNextHandler(ctx, tagConfig, helper, widget));
        return new DecorateHandler(config);
    }

    /**
     * Computes the next handler, adding param handlers.
     * <p>
     * Makes available the field values in templates using the format field_0,
     * field_1, etc.
     */
    protected FaceletHandler getNextHandler(FaceletContext ctx,
            TagConfig tagConfig, FaceletHandlerHelper helper, Widget widget)
            throws WidgetException {
        FaceletHandler leaf = new LeafFaceletHandler();
        FieldDefinition[] fieldDefs = widget.getFieldDefinitions();
        if (fieldDefs == null) {
            return leaf;
        }
        List<ParamHandler> paramHandlers = new ArrayList<ParamHandler>();
        // expose field variables
        for (Integer i = 0; i < fieldDefs.length; i++) {
            if (i == 0) {
                paramHandlers.add(getFieldParamHandler(ctx, tagConfig, helper,
                        leaf, widget, fieldDefs[i], null));
            }
            paramHandlers.add(getFieldParamHandler(ctx, tagConfig, helper,
                    leaf, widget, fieldDefs[i], i));
        }
        // expose widget properties too
        for (Map.Entry<String, Serializable> prop : widget.getProperties().entrySet()) {
            String key = prop.getKey();
            TagAttribute name = helper.createAttribute("name", String.format(
                    "%s_%s",
                    RenderVariables.widgetVariables.widgetProperty.name(), key));
            TagAttribute value;
            Serializable valueInstance = prop.getValue();
            if (!helper.shouldCreateReferenceAttribute(key, valueInstance)) {
                // FIXME: this will not be updated correctly using ajax
                value = helper.createAttribute("value", (String) valueInstance);
            } else {
                // create a reference so that it's a real expression and it's
                // not kept (cached) in a component value on ajax refresh
                value = helper.createAttribute("value", String.format(
                        "#{%s.properties.%s}",
                        RenderVariables.widgetVariables.widget.name(), key));
            }
            TagConfig config = TagConfigFactory.createTagConfig(tagConfig,
                    FaceletHandlerHelper.getTagAttributes(name, value), leaf);
            paramHandlers.add(new ParamHandler(config));
        }

        return new CompositeFaceletHandler(
                paramHandlers.toArray(new ParamHandler[] {}));
    }

    protected ParamHandler getFieldParamHandler(FaceletContext ctx,
            TagConfig tagConfig, FaceletHandlerHelper helper,
            FaceletHandler leaf, Widget widget, FieldDefinition fieldDef,
            Integer index) {
        String computedName;
        if (index == null) {
            computedName = String.format("%s",
                    RenderVariables.widgetVariables.field.name());
        } else {
            computedName = String.format("%s_%s",
                    RenderVariables.widgetVariables.field.name(), index);
        }
        TagAttribute name = helper.createAttribute("name", computedName);
        String computedValue = ValueExpressionHelper.createExpressionString(
                widget.getValueName(), fieldDef);
        TagAttribute value = helper.createAttribute("value", computedValue);
        TagConfig config = TagConfigFactory.createTagConfig(tagConfig,
                FaceletHandlerHelper.getTagAttributes(name, value), leaf);
        return new ParamHandler(config);
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
