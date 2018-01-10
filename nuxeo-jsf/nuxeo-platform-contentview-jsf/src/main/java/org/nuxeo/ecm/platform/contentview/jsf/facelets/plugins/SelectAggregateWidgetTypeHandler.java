/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.contentview.jsf.facelets.plugins;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.el.ValueExpression;
import javax.faces.view.facelets.CompositeFaceletHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetSelectOptionsImpl;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.forms.layout.facelets.plugins.AbstractSelectWidgetTypeHandler;
import org.nuxeo.ecm.platform.ui.web.tag.handler.LeafFaceletHandler;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;

/**
 * @since 6.0
 */
public abstract class SelectAggregateWidgetTypeHandler extends AbstractSelectWidgetTypeHandler {

    private static final String LABELS = "optionLabels";

    protected enum AggregatePropertyMappings {
        itemCount;
    }

    public SelectAggregateWidgetTypeHandler(TagConfig config) {
        super(config);
    }

    @Override
    protected FaceletHandler getOptionsFaceletHandler(FaceletContext ctx, FaceletHandlerHelper helper, Widget widget,
            WidgetSelectOption[] selectOptions) {
        FaceletHandler leaf = new LeafFaceletHandler();
        List<FaceletHandler> selectItems = new ArrayList<FaceletHandler>();
        FaceletHandler firstItem = getFirstHandler(ctx, helper, widget, leaf);
        final boolean hasOtherOptions = selectOptions != null && selectOptions.length > 0;
        if (firstItem != null) {
            selectItems.add(firstItem);
        }
        FaceletHandler widgetPropsHandler = getWidgetPropsHandler(ctx, helper, widget, leaf, hasOtherOptions);
        if (widgetPropsHandler != null) {
            selectItems.add(widgetPropsHandler);
        }
        return new CompositeFaceletHandler(selectItems.toArray(new FaceletHandler[0]));
    }

    @Override
    protected List<String> getExcludedProperties() {
        List<String> res = super.getExcludedProperties();
        for (AggregatePropertyMappings mapping : AggregatePropertyMappings.values()) {
            res.add(mapping.name());
        }
        return res;
    }

    @Override
    protected Map<String, Serializable> getOptionProperties(FaceletContext ctx, Widget widget,
            WidgetSelectOption selectOption) {
        Map<String, Serializable> props = super.getOptionProperties(ctx, widget, selectOption);
        props.put(SelectPropertyMappings.itemLabelSuffix.name(),
                widget.getProperty(AggregatePropertyMappings.itemCount.name()));
        return props;
    }

    // redefined to merge selectOptions property and options put on widget
    // definition
    protected FaceletHandler getWidgetPropsHandler(FaceletContext ctx, FaceletHandlerHelper helper, Widget widget,
            FaceletHandler nextHandler, boolean hasOtherOptions) {

        if (!hasOtherOptions) {
            return super.getWidgetPropsHandler(ctx, helper, widget, nextHandler);
        }

        if (shouldAddWidgetPropsHandler(widget)) {
            final String itemValue = ComponentTagUtils.getBareValueName(
                    (String) widget.getProperty(SelectPropertyMappings.itemValue.name()));
            final String label = new StringBuilder().append("#{")
                                                    .append(LABELS)
                                                    .append("[")
                                                    .append(itemValue)
                                                    .append("]}")
                                                    .toString();
            WidgetSelectOption selectOption = new WidgetSelectOptionsImpl(
                    widget.getProperty(SelectPropertyMappings.selectOptions.name()),
                    (String) widget.getProperty(SelectPropertyMappings.var.name()), label,
                    (String) widget.getProperty(SelectPropertyMappings.itemValue.name()),
                    widget.getProperty(SelectPropertyMappings.itemDisabled.name()),
                    widget.getProperty(SelectPropertyMappings.itemRendered.name()));
            return getOptionFaceletHandler(ctx, helper, widget, selectOption, nextHandler);
        }
        return null;
    }

    @Override
    protected TagHandler getComponentFaceletHandler(FaceletContext ctx, FaceletHandlerHelper helper, Widget widget,
            TagHandler componentHandler) {
        WidgetSelectOption[] selectOptions = widget.getSelectOptions();
        if (selectOptions != null && selectOptions.length != 0) {
            List<String> blockedPatterns = new ArrayList<String>(1);
            blockedPatterns.add(LABELS);
            Map<String, String> labels = new HashMap<String, String>();
            for (WidgetSelectOption selectOption : selectOptions) {
                if (selectOption == null) {
                    continue;
                }
                Map<String, String> l10n_labels = selectOption.getItemLabels();
                boolean done = false;
                if (l10n_labels != null && !l10n_labels.isEmpty()) {
                    Locale locale = ctx.getFacesContext().getViewRoot().getLocale();
                    if (l10n_labels.containsKey(locale.getLanguage()) || l10n_labels.containsKey("en")) {
                        if (l10n_labels.containsKey(locale.getLanguage())) {
                            labels.put(selectOption.getItemValue(), l10n_labels.get(locale.getLanguage()));
                        } else {
                            labels.put(selectOption.getItemValue(), l10n_labels.get("en"));
                        }
                        done = true;
                    }
                }
                if (!done) {
                    labels.put(selectOption.getItemValue(), selectOption.getItemLabel());
                }
            }
            Map<String, ValueExpression> variables = new HashMap<String, ValueExpression>();
            variables.put(LABELS, ctx.getExpressionFactory().createValueExpression(labels, HashMap.class));
            return helper.getAliasTagHandler(widget.getTagConfigId(), variables, blockedPatterns, componentHandler);
        } else {
            return componentHandler;
        }
    }

}
