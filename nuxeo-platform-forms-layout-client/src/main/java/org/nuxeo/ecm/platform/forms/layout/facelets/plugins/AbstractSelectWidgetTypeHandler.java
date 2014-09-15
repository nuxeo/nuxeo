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
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetSelectOptionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetSelectOptionsImpl;
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

    protected enum SelectPropertyMappings {
        selectOptions, var, itemLabel, itemValue, itemRendered, itemDisabled, itemEscaped, ordering, caseSensitive;
    }

    // ease up override of behaviour without impacting default options
    // management
    protected FaceletHandler getOptionFaceletHandler(FaceletContext ctx,
            FaceletHandlerHelper helper, Widget widget,
            WidgetSelectOption selectOption, FaceletHandler nextHandler) {
        return getBareOptionFaceletHandler(ctx, helper, widget, selectOption,
                nextHandler);
    }

    protected FaceletHandler getBareOptionFaceletHandler(FaceletContext ctx,
            FaceletHandlerHelper helper, Widget widget,
            WidgetSelectOption selectOption, FaceletHandler nextHandler) {
        String componentType;
        if (selectOption instanceof WidgetSelectOptions) {
            componentType = UISelectItems.COMPONENT_TYPE;
        } else {
            componentType = UISelectItem.COMPONENT_TYPE;
        }
        TagAttributes attrs = helper.getTagAttributes(selectOption);
        return helper.getHtmlComponentHandler(widget.getTagConfigId(), attrs,
                nextHandler, componentType, null);
    }

    /**
     * Adds a default disabled "select a value" option if widget is not
     * required.
     *
     * @since 5.9.6
     */
    protected FaceletHandler getFirstHandler(FaceletContext ctx,
            FaceletHandlerHelper helper, Widget widget,
            FaceletHandler nextHandler) {
        if (!widget.isRequired()) {
            String bundleName = ctx.getFacesContext().getApplication().getMessageBundle();
            String localizedExpression = String.format("#{%s['%s']}",
                    bundleName, "label.vocabulary.selectValue");
            WidgetSelectOption selectOption = new WidgetSelectOptionImpl("",
                    "", localizedExpression, "", Boolean.TRUE, Boolean.TRUE);
            return getBareOptionFaceletHandler(ctx, helper, widget,
                    selectOption, nextHandler);
        }
        return null;
    }

    /**
     * Computes select options from widget properties.
     *
     * @since 5.9.6
     */
    protected FaceletHandler getWidgetPropsHandler(FaceletContext ctx,
            FaceletHandlerHelper helper, Widget widget,
            FaceletHandler nextHandler) {
        if (widget.getProperties().containsKey(
                SelectPropertyMappings.selectOptions.name())) {
            WidgetSelectOption selectOption = new WidgetSelectOptionsImpl(
                    widget.getProperty(SelectPropertyMappings.selectOptions.name()),
                    (String) widget.getProperty(SelectPropertyMappings.var.name()),
                    (String) widget.getProperty(SelectPropertyMappings.itemLabel.name()),
                    (String) widget.getProperty(SelectPropertyMappings.itemValue.name()),
                    widget.getProperty(SelectPropertyMappings.itemDisabled.name()),
                    widget.getProperty(SelectPropertyMappings.itemRendered.name()));
            return getOptionFaceletHandler(ctx, helper, widget, selectOption,
                    nextHandler);
        }
        return null;
    }

    protected FaceletHandler getOptionsFaceletHandler(FaceletContext ctx,
            FaceletHandlerHelper helper, Widget widget,
            WidgetSelectOption[] selectOptions) {
        FaceletHandler leaf = new LeafFaceletHandler();
        List<FaceletHandler> selectItems = new ArrayList<FaceletHandler>();
        FaceletHandler firstItem = getFirstHandler(ctx, helper, widget, leaf);
        if (firstItem != null) {
            selectItems.add(firstItem);
        }
        FaceletHandler widgetPropsHandler = getWidgetPropsHandler(ctx, helper,
                widget, leaf);
        if (widgetPropsHandler != null) {
            selectItems.add(widgetPropsHandler);
        }
        if (selectOptions != null && selectOptions.length > 0) {
            for (WidgetSelectOption selectOption : selectOptions) {
                if (selectOption == null) {
                    continue;
                }
                FaceletHandler h = getBareOptionFaceletHandler(ctx, helper,
                        widget, selectOption, leaf);
                if (h != null) {
                    selectItems.add(h);
                }
            }
        }
        return new CompositeFaceletHandler(
                selectItems.toArray(new FaceletHandler[0]));
    }

    protected FaceletHandler getOptionsFaceletHandler(FaceletContext ctx,
            FaceletHandlerHelper helper, Widget widget) {
        return getOptionsFaceletHandler(ctx, helper, widget,
                widget.getSelectOptions());
    }

    protected FaceletHandler getFaceletHandler(FaceletContext ctx,
            TagConfig tagConfig, Widget widget, FaceletHandler[] subHandlers,
            String componentType) throws WidgetException {
        FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, tagConfig);
        String mode = widget.getMode();
        String widgetId = widget.getId();
        String widgetName = widget.getName();
        String widgetTagConfigId = widget.getTagConfigId();
        List<String> excludedProps = new ArrayList<>();
        for (SelectPropertyMappings mapping : SelectPropertyMappings.values()) {
            excludedProps.add(mapping.name());
        }
        TagAttributes attributes = helper.getTagAttributes(widget,
                excludedProps, true);
        if (!BuiltinWidgetModes.isLikePlainMode(mode)) {
            FaceletHandlerHelper.addTagAttribute(attributes,
                    helper.createAttribute("id", widgetId));
        }
        if (BuiltinWidgetModes.EDIT.equals(mode)) {
            FaceletHandler optionsHandler = getOptionsFaceletHandler(ctx,
                    helper, widget);
            FaceletHandler nextHandler = optionsHandler;
            if (subHandlers != null) {
                nextHandler = new CompositeFaceletHandler(new FaceletHandler[] {
                        optionsHandler,
                        new CompositeFaceletHandler(subHandlers) });
            }
            // maybe add convert handler for easier integration of select2
            // widgets handling multiple values
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
