/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.contentview.jsf.facelets.plugins;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.el.ValueExpression;
import javax.faces.component.html.HtmlSelectManyCheckbox;
import javax.faces.component.html.HtmlSelectManyListbox;
import javax.faces.component.html.HtmlSelectManyMenu;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.CompositeFaceletHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagAttributes;
import javax.faces.view.facelets.TagConfig;

import org.apache.commons.lang.ArrayUtils;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinWidgetModes;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.WidgetException;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetSelectOptionsImpl;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.forms.layout.facelets.LeafFaceletHandler;
import org.nuxeo.ecm.platform.forms.layout.facelets.plugins.AbstractSelectWidgetTypeHandler;

/**
 * @since 5.9.6
 */
public abstract class SelectAggregateWidgetTypeHandler extends
        AbstractSelectWidgetTypeHandler {

    private static final String LABELS = "labels";
    private static final long serialVersionUID = 1L;

    protected enum AggregatePropertyMappings {
        itemCount;
    }

    @Override
    protected FaceletHandler getOptionsFaceletHandler(FaceletContext ctx,
            FaceletHandlerHelper helper, Widget widget,
            WidgetSelectOption[] selectOptions) {
        FaceletHandler leaf = new LeafFaceletHandler();
        List<FaceletHandler> selectItems = new ArrayList<FaceletHandler>();
        FaceletHandler firstItem = getFirstHandler(ctx, helper, widget, leaf);
        final boolean hasOtherOptions = selectOptions != null && selectOptions.length > 0;
        if (firstItem != null) {
            selectItems.add(firstItem);
        }
        FaceletHandler widgetPropsHandler = getWidgetPropsHandler(ctx, helper,
                widget, leaf, hasOtherOptions);
        if (widgetPropsHandler != null) {
            selectItems.add(widgetPropsHandler);
        }
        return new CompositeFaceletHandler(
                selectItems.toArray(new FaceletHandler[0]));
    }

    @Override
    protected List<String> getExcludedProperties() {
        List<String> res = super.getExcludedProperties();
        for (AggregatePropertyMappings mapping : AggregatePropertyMappings.values()) {
            res.add(mapping.name());
        }
        return res;
    }

    protected Map<String, Serializable> getOptionProperties(FaceletContext ctx,
            Widget widget, WidgetSelectOption selectOption) {
        Map<String, Serializable> props = super.getOptionProperties(ctx,
                widget, selectOption);
        props.put(SelectPropertyMappings.itemLabelSuffix.name(),
                widget.getProperty(AggregatePropertyMappings.itemCount.name()));
        return props;
    }

    protected FaceletHandler getWidgetPropsHandler(FaceletContext ctx,
            FaceletHandlerHelper helper, Widget widget,
            FaceletHandler nextHandler, boolean hasOtherOptions) {

        if (!hasOtherOptions) {
            return super.getWidgetPropsHandler(ctx, helper, widget, nextHandler);
        }

        if (shouldAddWidgetPropsHandler(widget)) {
            WidgetSelectOption selectOption = new WidgetSelectOptionsImpl(
                    widget.getProperty(SelectPropertyMappings.selectOptions.name()),
                    (String) widget.getProperty(SelectPropertyMappings.var.name()),
                    "#{map[item.key]}",
                    (String) widget.getProperty(SelectPropertyMappings.itemValue.name()),
                    widget.getProperty(SelectPropertyMappings.itemDisabled.name()),
                    widget.getProperty(SelectPropertyMappings.itemRendered.name()));
            return getOptionFaceletHandler(ctx, helper, widget, selectOption,
                    nextHandler);
        }
        return null;
    }

    @Override
    protected FaceletHandler getFaceletHandler(FaceletContext ctx,
            TagConfig tagConfig, Widget widget, FaceletHandler[] subHandlers,
            String componentType, String rendererType) throws WidgetException {
        FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, tagConfig);
        String mode = widget.getMode();
        String widgetId = widget.getId();
        String widgetName = widget.getName();
        String widgetTagConfigId = widget.getTagConfigId();
        List<String> excludedProps = getExcludedProperties();
        TagAttributes attributes = helper.getTagAttributes(widget,
                excludedProps, true);
        // BBB for CSS style classes on directory select components
        if (widget.getProperty("cssStyle") != null) {
            attributes = FaceletHandlerHelper.addTagAttribute(
                    attributes,
                    helper.createAttribute("style",
                            (String) widget.getProperty("cssStyle")));
        }
        if (widget.getProperty("cssStyleClass") != null) {
            attributes = FaceletHandlerHelper.addTagAttribute(
                    attributes,
                    helper.createAttribute("styleClass",
                            (String) widget.getProperty("cssStyleClass")));
        }
        if (!BuiltinWidgetModes.isLikePlainMode(mode)) {
            attributes = FaceletHandlerHelper.addTagAttribute(attributes,
                    helper.createAttribute("id", widgetId));
        }
        if (BuiltinWidgetModes.EDIT.equals(mode)) {
            WidgetSelectOption[] selectOptions = widget.getSelectOptions();
            final boolean hasOtherOptions = selectOptions != null
                    && selectOptions.length > 0;

            FaceletHandler optionsHandler = getOptionsFaceletHandler(ctx,
                    helper, widget);

            FaceletHandler[] nextHandlers = new FaceletHandler[] {};
            nextHandlers = (FaceletHandler[]) ArrayUtils.add(nextHandlers,
                    optionsHandler);
            if (subHandlers != null) {
                nextHandlers = (FaceletHandler[]) ArrayUtils.addAll(
                        nextHandlers, subHandlers);
            }
            FaceletHandler leaf = getNextHandler(ctx, tagConfig, widget,
                    nextHandlers, helper, true);
            // maybe add convert handler for easier integration of select2
            // widgets handling multiple values
            if (HtmlSelectManyListbox.COMPONENT_TYPE.equals(componentType)
                    || HtmlSelectManyCheckbox.COMPONENT_TYPE.equals(componentType)
                    || HtmlSelectManyMenu.COMPONENT_TYPE.equals(componentType)) {
                // add hint for value conversion to collection
                attributes = FaceletHandlerHelper.addTagAttribute(attributes,
                        helper.createAttribute("collectionType",
                                ArrayList.class.getName()));
            }
            ComponentHandler input = helper.getHtmlComponentHandler(
                    widgetTagConfigId, attributes, leaf, componentType, rendererType);

            FaceletHandler faceletHandler = null;
            if (hasOtherOptions && optionsHandler != null) {
                List<String> blockedPatterns = new ArrayList<String>(1);
                blockedPatterns.add(LABELS);
                Map<String, String> labels = new HashMap<String, String>();
                for (WidgetSelectOption selectOption : selectOptions) {
                    if (selectOption == null) {
                        continue;
                    }
                    labels.put(
                            selectOption.getItemValue(),
                                    selectOption.getItemLabel());
                }
                Map<String, ValueExpression> variables = new HashMap<String, ValueExpression>();
                variables.put(LABELS, ctx.getExpressionFactory().createValueExpression(labels, HashMap.class));
                faceletHandler = helper.getAliasTagHandler(
                        widget.getTagConfigId(), variables , blockedPatterns,
                        input);
            }

            String msgId = helper.generateMessageId(widgetName);
            ComponentHandler message = helper.getMessageComponentHandler(
                    widgetTagConfigId, msgId, widgetId, null);
            FaceletHandler[] handlers = { faceletHandler != null ? faceletHandler: input, message };
            return new CompositeFaceletHandler(handlers);
        } else {
            // TODO
            return null;
        }
    }


}
