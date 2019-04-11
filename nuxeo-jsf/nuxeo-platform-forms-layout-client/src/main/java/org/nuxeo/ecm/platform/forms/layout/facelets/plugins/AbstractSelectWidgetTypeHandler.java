/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.facelets.plugins;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlSelectManyCheckbox;
import javax.faces.component.html.HtmlSelectManyListbox;
import javax.faces.component.html.HtmlSelectManyMenu;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.CompositeFaceletHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagAttributes;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.apache.commons.lang3.ArrayUtils;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinWidgetModes;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOptions;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.WidgetException;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetSelectOptionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetSelectOptionsImpl;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.ui.web.component.UISelectItem;
import org.nuxeo.ecm.platform.ui.web.component.UISelectItems;
import org.nuxeo.ecm.platform.ui.web.tag.handler.LeafFaceletHandler;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;

/**
 * Helper class for options generation depending on the widget definition
 *
 * @since 5.4.2
 */
public abstract class AbstractSelectWidgetTypeHandler extends AbstractWidgetTypeHandler {

    protected enum SelectPropertyMappings {
        selectOptions, var, itemLabel, resolveItemLabelTwice, itemLabelPrefix, itemLabelPrefixSeparator,
        //
        itemLabelSuffix, itemLabelSuffixSeparator, itemValue,
        //
        itemRendered, itemDisabled, itemEscaped, ordering, caseSensitive,
        //
        displayIdAndLabel, displayIdAndLabelSeparator, notDisplayDefaultOption,
        //
        localize, dbl10n;
    }

    public AbstractSelectWidgetTypeHandler(TagConfig config) {
        super(config);
    }

    // ease up override of behavior without impacting default options
    // management
    protected Map<String, Serializable> getOptionProperties(FaceletContext ctx, Widget widget,
            WidgetSelectOption selectOption) {
        Map<String, Serializable> props = new HashMap<>();
        for (SelectPropertyMappings mapping : SelectPropertyMappings.values()) {
            if (widget.getProperties().containsKey(mapping.name())) {
                props.put(mapping.name(), widget.getProperty(mapping.name()));
            }
        }
        return props;
    }

    protected String getOptionComponentType(WidgetSelectOption selectOption) {
        if (selectOption instanceof WidgetSelectOptions) {
            return UISelectItems.COMPONENT_TYPE;
        } else {
            return UISelectItem.COMPONENT_TYPE;
        }
    }

    protected FaceletHandler getOptionFaceletHandler(FaceletContext ctx, FaceletHandlerHelper helper, Widget widget,
            WidgetSelectOption selectOption, FaceletHandler nextHandler) {
        String componentType = getOptionComponentType(selectOption);
        TagAttributes attrs = helper.getTagAttributes(selectOption, getOptionProperties(ctx, widget, selectOption));
        return helper.getHtmlComponentHandler(widget.getTagConfigId(), attrs, nextHandler, componentType, null);
    }

    // not impacted by custom behaviour by default
    protected FaceletHandler getBareOptionFaceletHandler(FaceletContext ctx, FaceletHandlerHelper helper,
            Widget widget, WidgetSelectOption selectOption, FaceletHandler nextHandler) {
        String componentType = getOptionComponentType(selectOption);
        TagAttributes attrs = helper.getTagAttributes(selectOption);
        return helper.getHtmlComponentHandler(widget.getTagConfigId(), attrs, nextHandler, componentType, null);
    }

    /**
     * Adds a default disabled "select a value" option if widget is not required.
     *
     * @since 6.0
     */
    protected FaceletHandler getFirstHandler(FaceletContext ctx, FaceletHandlerHelper helper, Widget widget,
            FaceletHandler nextHandler) {
        Object doNotDisplay = widget.getProperty(SelectPropertyMappings.notDisplayDefaultOption.name());
        if (doNotDisplay != null) {
            if (Boolean.TRUE.equals(doNotDisplay)) {
                return null;
            }
            if (doNotDisplay instanceof String) {
                Object res = ComponentTagUtils.resolveElExpression(ctx, (String) doNotDisplay);
                if ((res instanceof Boolean && Boolean.TRUE.equals(res))
                        || (res instanceof String && Boolean.parseBoolean((String) res))) {
                    return null;
                }
            }
        }
        String bundleName = ctx.getFacesContext().getApplication().getMessageBundle();
        String localizedExpression = "#{" + bundleName + "['label.vocabulary.selectValue']}";
        WidgetSelectOption selectOption = new WidgetSelectOptionImpl("", "", localizedExpression, "", Boolean.FALSE,
                Boolean.TRUE);
        return getBareOptionFaceletHandler(ctx, helper, widget, selectOption, nextHandler);
    }

    /**
     * Returns true if widget properties should generate a default tag handler for select options.
     * <p>
     * This default implementation requires the selectOptions widget property to be filled.
     *
     * @since 6.0
     */
    protected boolean shouldAddWidgetPropsHandler(Widget widget) {
        if (widget.getProperties().containsKey(SelectPropertyMappings.selectOptions.name())) {
            return true;
        }
        return false;
    }

    /**
     * Computes select options from widget properties.
     *
     * @since 6.0
     */
    protected FaceletHandler getWidgetPropsHandler(FaceletContext ctx, FaceletHandlerHelper helper, Widget widget,
            FaceletHandler nextHandler) {
        if (shouldAddWidgetPropsHandler(widget)) {
            WidgetSelectOptionsImpl selectOption = new WidgetSelectOptionsImpl(
                    widget.getProperty(SelectPropertyMappings.selectOptions.name()),
                    (String) widget.getProperty(SelectPropertyMappings.var.name()),
                    (String) widget.getProperty(SelectPropertyMappings.itemLabel.name()),
                    (String) widget.getProperty(SelectPropertyMappings.itemValue.name()),
                    widget.getProperty(SelectPropertyMappings.itemDisabled.name()),
                    widget.getProperty(SelectPropertyMappings.itemRendered.name()));
            return getOptionFaceletHandler(ctx, helper, widget, selectOption, nextHandler);
        }
        return null;
    }

    protected FaceletHandler getOptionsFaceletHandler(FaceletContext ctx, FaceletHandlerHelper helper, Widget widget,
            WidgetSelectOption[] selectOptions) {
        FaceletHandler leaf = new LeafFaceletHandler();
        List<FaceletHandler> selectItems = new ArrayList<>();
        FaceletHandler firstItem = getFirstHandler(ctx, helper, widget, leaf);
        if (firstItem != null) {
            selectItems.add(firstItem);
        }
        FaceletHandler widgetPropsHandler = getWidgetPropsHandler(ctx, helper, widget, leaf);
        if (widgetPropsHandler != null) {
            selectItems.add(widgetPropsHandler);
        }
        if (selectOptions != null && selectOptions.length > 0) {
            for (WidgetSelectOption selectOption : selectOptions) {
                if (selectOption == null) {
                    continue;
                }
                FaceletHandler h = getBareOptionFaceletHandler(ctx, helper, widget, selectOption, leaf);
                if (h != null) {
                    selectItems.add(h);
                }
            }
        }
        return new CompositeFaceletHandler(selectItems.toArray(new FaceletHandler[0]));
    }

    protected FaceletHandler getOptionsFaceletHandler(FaceletContext ctx, FaceletHandlerHelper helper, Widget widget) {
        return getOptionsFaceletHandler(ctx, helper, widget, widget.getSelectOptions());
    }

    /**
     * Returns properties useful for select items, not to be reported on the select component.
     */
    protected List<String> getExcludedProperties() {
        List<String> excludedProps = new ArrayList<>();
        // BBB
        excludedProps.add("cssStyle");
        excludedProps.add("cssStyleClass");
        for (SelectPropertyMappings mapping : SelectPropertyMappings.values()) {
            excludedProps.add(mapping.name());
        }
        return excludedProps;
    }

    protected void apply(FaceletContext ctx, UIComponent parent, Widget widget, String componentType)
            throws WidgetException, IOException {
        apply(ctx, parent, widget, componentType, null);
    }

    protected TagHandler getComponentFaceletHandler(FaceletContext ctx, FaceletHandlerHelper helper, Widget widget,
            TagHandler componentHandler) {
        return componentHandler;
    }

    protected void apply(FaceletContext ctx, UIComponent parent, Widget widget, String componentType,
            String rendererType) throws WidgetException, IOException {
        FaceletHandlerHelper helper = new FaceletHandlerHelper(tagConfig);
        String mode = widget.getMode();
        String widgetId = widget.getId();
        String widgetName = widget.getName();
        String widgetTagConfigId = widget.getTagConfigId();
        List<String> excludedProps = getExcludedProperties();
        TagAttributes attributes = helper.getTagAttributes(widget, excludedProps, true);
        // BBB for CSS style classes on directory select components
        if (widget.getProperty("cssStyle") != null) {
            attributes = FaceletHandlerHelper.addTagAttribute(attributes,
                    helper.createAttribute("style", (String) widget.getProperty("cssStyle")));
        }
        if (widget.getProperty("cssStyleClass") != null) {
            attributes = FaceletHandlerHelper.addTagAttribute(attributes,
                    helper.createAttribute("styleClass", (String) widget.getProperty("cssStyleClass")));
        }
        if (!BuiltinWidgetModes.isLikePlainMode(mode)) {
            attributes = FaceletHandlerHelper.addTagAttribute(attributes, helper.createAttribute("id", widgetId));
        }
        if (BuiltinWidgetModes.EDIT.equals(mode)) {
            FaceletHandler optionsHandler = getOptionsFaceletHandler(ctx, helper, widget);
            FaceletHandler[] nextHandlers = new FaceletHandler[] {};
            nextHandlers = ArrayUtils.add(nextHandlers, optionsHandler);
            FaceletHandler leaf = getNextHandler(ctx, tagConfig, widget, nextHandlers, helper, true, true);
            // maybe add convert handler for easier integration of select2
            // widgets handling multiple values
            if (HtmlSelectManyListbox.COMPONENT_TYPE.equals(componentType)
                    || HtmlSelectManyCheckbox.COMPONENT_TYPE.equals(componentType)
                    || HtmlSelectManyMenu.COMPONENT_TYPE.equals(componentType)) {
                // add hint for value conversion to collection
                attributes = FaceletHandlerHelper.addTagAttribute(attributes,
                        helper.createAttribute("collectionType", ArrayList.class.getName()));
            }

            ComponentHandler input = helper.getHtmlComponentHandler(widgetTagConfigId, attributes, leaf, componentType,
                    rendererType);
            String msgId = FaceletHandlerHelper.generateMessageId(ctx, widgetName);
            ComponentHandler message = helper.getMessageComponentHandler(widgetTagConfigId, msgId, widgetId, null);
            FaceletHandler[] handlers = { getComponentFaceletHandler(ctx, helper, widget, input), message };
            FaceletHandler h = new CompositeFaceletHandler(handlers);
            h.apply(ctx, parent);
        } else {
            // TODO
            return;
        }
    }
}
