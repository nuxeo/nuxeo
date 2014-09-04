/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.forms.layout.facelets.plugins;

import java.util.ArrayList;
import java.util.List;

import javax.faces.component.html.HtmlSelectManyListbox;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.CompositeFaceletHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagAttributes;
import javax.faces.view.facelets.TagConfig;

import org.nuxeo.ecm.platform.forms.layout.api.BuiltinWidgetModes;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOptions;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.WidgetException;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.forms.layout.facelets.LeafFaceletHandler;
import org.nuxeo.ecm.platform.ui.web.component.UISelectItem;
import org.nuxeo.ecm.platform.ui.web.component.UISelectItems;

/**
 * Helper class for options generation depending on the widget definition
 *
 * @since 5.4.2
 */
public abstract class AbstractSelectWidgetTypeHandler extends
        AbstractWidgetTypeHandler {

    private static final long serialVersionUID = 1L;

    protected FaceletHandler getOptionsFaceletHandler(
            FaceletHandlerHelper helper, Widget widget) {
        FaceletHandler leaf = new LeafFaceletHandler();
        WidgetSelectOption[] selectOptions = widget.getSelectOptions();
        List<FaceletHandler> selectItems = new ArrayList<FaceletHandler>();
        // TODO: maybe check other widget properties to know if a default
        // disabled option should be added for instance
        String widgetTagConfigId = widget.getTagConfigId();
        if (selectOptions != null && selectOptions.length > 0) {
            for (WidgetSelectOption selectOption : selectOptions) {
                TagAttributes attrs = helper.getTagAttributes(selectOption);
                if (selectOption instanceof WidgetSelectOptions) {
                    selectItems.add(helper.getHtmlComponentHandler(
                            widgetTagConfigId, attrs, leaf,
                            UISelectItems.COMPONENT_TYPE, null));
                } else if (selectOption != null) {
                    selectItems.add(helper.getHtmlComponentHandler(
                            widgetTagConfigId, attrs, leaf,
                            UISelectItem.COMPONENT_TYPE, null));
                }
            }
        }
        return new CompositeFaceletHandler(
                selectItems.toArray(new FaceletHandler[0]));
    }

    protected FaceletHandler getFaceletHandler(FaceletContext ctx,
            TagConfig tagConfig, Widget widget, FaceletHandler[] subHandlers,
            String componentType) throws WidgetException {
        FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, tagConfig);
        String mode = widget.getMode();
        String widgetId = widget.getId();
        String widgetName = widget.getName();
        String widgetTagConfigId = widget.getTagConfigId();
        TagAttributes attributes;
        if (BuiltinWidgetModes.isLikePlainMode(mode)) {
            // use attributes without id
            attributes = helper.getTagAttributes(widget);
        } else {
            attributes = helper.getTagAttributes(widgetId, widget);
        }
        if (BuiltinWidgetModes.EDIT.equals(mode)) {
            FaceletHandler optionsHandler = getOptionsFaceletHandler(helper,
                    widget);
            FaceletHandler nextHandler = optionsHandler;
            if (subHandlers != null) {
                nextHandler = new CompositeFaceletHandler(new FaceletHandler[] {
                        optionsHandler,
                        new CompositeFaceletHandler(subHandlers) });
            }
            ComponentHandler input = helper.getHtmlComponentHandler(
                    widgetTagConfigId, attributes, nextHandler, componentType,
                    null);
            String msgId = helper.generateMessageId(widgetName);
            ComponentHandler message = helper.getMessageComponentHandler(
                    widgetTagConfigId, msgId, widgetId, null);
            FaceletHandler[] handlers = { input, message };
            return new CompositeFaceletHandler(handlers);
        } else {
            // TODO
            return null;
        }
    }

}
