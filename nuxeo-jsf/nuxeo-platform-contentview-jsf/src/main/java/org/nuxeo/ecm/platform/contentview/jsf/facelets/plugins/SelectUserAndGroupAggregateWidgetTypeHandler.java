/*
 * (C) Copyright 2019 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.platform.contentview.jsf.facelets.plugins;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttributes;
import javax.faces.view.facelets.TagConfig;

import org.nuxeo.ecm.platform.contentview.jsf.facelets.plugins.SelectAggregateWidgetTypeHandler.AggregatePropertyMappings;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinWidgetModes;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.forms.layout.facelets.plugins.AbstractSelectWidgetTypeHandler;
import org.nuxeo.ecm.platform.ui.web.directory.UIUserAndGroupSelectItems;

/**
 * Helper class for options generation depending on the widget definition
 *
 * @since 11.1
 */
public abstract class SelectUserAndGroupAggregateWidgetTypeHandler extends AbstractSelectWidgetTypeHandler {

    public SelectUserAndGroupAggregateWidgetTypeHandler(TagConfig config) {
        super(config);
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
        for (UIUserAndGroupSelectItems.UserAndGroupPropertyKeys mapping : UIUserAndGroupSelectItems.UserAndGroupPropertyKeys.values()) {
            if (widget.getProperties().containsKey(mapping.name())) {
                props.put(mapping.name(), widget.getProperty(mapping.name()));
            }
        }
        props.put(SelectPropertyMappings.itemLabelSuffix.name(),
                widget.getProperty(AggregatePropertyMappings.itemCount.name()));
        return props;
    }

    @Override
    protected String getOptionComponentType(WidgetSelectOption selectOption) {
        return UIUserAndGroupSelectItems.COMPONENT_TYPE;
    }

    // do not rely on selectOptions to be filled
    @Override
    protected boolean shouldAddWidgetPropsHandler(Widget widget) {
        return true;
    }

    /**
     * Get tag attributes for a specific mode.
     *
     * @param widget The widget to generate tag attributes for.
     * @param mode The given mode like PLAIN, CSV.
     * @param helper An instance of FaceletHandlerHelper.
     * @param widgetId The widget id.
     * @return
     * @since 11.1
     */
    protected TagAttributes getTagAttributesForMode(Widget widget, String mode, FaceletHandlerHelper helper,
            String widgetId) {
        TagAttributes result;
        if (BuiltinWidgetModes.isLikePlainMode(mode)) {
            // use attributes without id and with
            List<String> excludedProperties = new ArrayList<>();
            // In case of plain mode css style attributes are to be excluded
            excludedProperties.add("styleClass");
            result = helper.getTagAttributes(widget, excludedProperties, true);
        } else {
            result = helper.getTagAttributes(widgetId, widget);
        }
        return result;
    }

}
