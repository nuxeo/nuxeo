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

import java.util.ArrayList;
import java.util.List;

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
 * Template widget type
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
        if (templateAttr != null) {
            attributes = FaceletHandlerHelper.addTagAttribute(attributes,
                    templateAttr);
        }
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
        for (int i = 0; i < fieldDefs.length; i++) {
            String computedName = String.format("%s_%s",
                    RenderVariables.widgetVariables.field.name(), i);
            TagAttribute name = helper.createAttribute("name", computedName);
            String computedValue = ValueExpressionHelper.createExpressionString(
                    widget.getValueName(), fieldDefs[i]);
            TagAttribute value = helper.createAttribute("value", computedValue);
            TagConfig config = TagConfigFactory.createTagConfig(tagConfig,
                    FaceletHandlerHelper.getTagAttributes(name, value), leaf);
            paramHandlers.add(new ParamHandler(config));
        }
        return new CompositeFaceletHandler(
                paramHandlers.toArray(new ParamHandler[] {}));
    }

    /**
     * Returns the template value.
     */
    protected String getTemplateValue(Widget widget) {
        return (String) widget.getProperty(TEMPLATE_PROPERTY_NAME);
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
