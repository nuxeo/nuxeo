/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.facelets.dev;

import java.io.IOException;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributes;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.forms.layout.facelets.plugins.AbstractWidgetTypeHandler;
import org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory;

import com.sun.faces.facelets.tag.ui.DecorateHandler;

/**
 * Dev tag handler for widgets, retrieving the template to use on the widget
 * properties (preferably on the widget type properties) using key
 * {@link AbstractWidgetTypeHandler#DEV_TEMPLATE_PROPERTY_NAME}.
 * <p>
 * When the property
 * {@link AbstractWidgetTypeHandler#DISABLE_DEV_PROPERTY_NAME} is not defined,
 * a default template is used for the widget dev mode. If this property
 * (resolved on the widget or the widget type properties) resolves to false,
 * this handler is skipped.
 *
 * @since 6.0
 */
public class WidgetTypeDevTagHandler extends TagHandler {

    protected final String DEFAULT_TEMPLATE = "/widgets/dev/widget_dev_template.xhtml";

    protected final TagConfig config;

    protected final TagAttribute widget;

    protected final TagAttribute template;

    public WidgetTypeDevTagHandler(TagConfig config) {
        super(config);
        this.config = config;
        this.widget = getRequiredAttribute("widget");
        this.template = getAttribute("template");
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException, FacesException, FaceletException, ELException {
        Widget widgetInstance = (Widget) widget.getObject(ctx, Widget.class);
        FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, config);
        String templateValue = template != null ? template.getValue(ctx) : null;
        if (StringUtils.isBlank(templateValue)) {
            templateValue = DEFAULT_TEMPLATE;
        }
        TagAttribute templateAttr = helper.createAttribute("template",
                templateValue);
        TagAttributes attributes = FaceletHandlerHelper.getTagAttributes(templateAttr);
        String widgetTagConfigId = widgetInstance.getTagConfigId();
        TagConfig config = TagConfigFactory.createTagConfig(this.config,
                widgetTagConfigId, attributes, nextHandler);
        DecorateHandler includeHandler = new DecorateHandler(config);
        includeHandler.apply(ctx, parent);
    }
}
