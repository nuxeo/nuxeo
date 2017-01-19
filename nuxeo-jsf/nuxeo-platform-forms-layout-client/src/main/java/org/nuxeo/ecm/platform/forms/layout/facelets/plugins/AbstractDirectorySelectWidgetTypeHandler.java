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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttributes;

import org.nuxeo.ecm.platform.forms.layout.api.BuiltinWidgetModes;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOptions;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.ui.web.directory.UIDirectorySelectItem;
import org.nuxeo.ecm.platform.ui.web.directory.UIDirectorySelectItems;

/**
 * Helper class for options generation depending on the widget definition
 *
 * @since 5.4.2
 */
public abstract class AbstractDirectorySelectWidgetTypeHandler extends AbstractSelectWidgetTypeHandler {

    private static final long serialVersionUID = 1L;

    protected enum DirectoryPropertyMappings {
        directoryName, displayAll, displayObsoleteEntries, filter, localize, dbl10n;
    }

    @Override
    protected List<String> getExcludedProperties() {
        List<String> res = super.getExcludedProperties();
        for (DirectoryPropertyMappings mapping : DirectoryPropertyMappings.values()) {
            res.add(mapping.name());
        }
        return res;
    }

    protected String getOptionComponentType(WidgetSelectOption selectOption) {
        if (selectOption instanceof WidgetSelectOptions) {
            return UIDirectorySelectItems.COMPONENT_TYPE;
        } else {
            return UIDirectorySelectItem.COMPONENT_TYPE;
        }
    }

    // do not rely on selectOptions to be filled
    protected boolean shouldAddWidgetPropsHandler(Widget widget) {
        return true;
    }

    protected Map<String, Serializable> getOptionProperties(FaceletContext ctx, Widget widget,
            WidgetSelectOption selectOption) {
        Map<String, Serializable> props = super.getOptionProperties(ctx, widget, selectOption);
        for (DirectoryPropertyMappings mapping : DirectoryPropertyMappings.values()) {
            if (widget.getProperties().containsKey(mapping.name())) {
                props.put(mapping.name(), widget.getProperty(mapping.name()));
            }
        }
        // if selectOptions is filled on widget properties, force
        // displayAll value to false to ensure filtering of presented
        // items
        if (props.containsKey(SelectPropertyMappings.selectOptions.name())
                && !props.containsKey(DirectoryPropertyMappings.displayAll.name())) {
            props.put(DirectoryPropertyMappings.displayAll.name(), Boolean.FALSE);
        }
        return props;
    }

    /**
     * Get tag attributes for a specific mode.
     * @param widget The widget to generate tag attributes for.
     * @param mode The given mode like PLAIN, CSV.
     * @param helper An instance of FaceletHandlerHelper.
     * @param widgetId The widget id.
     * @return
     *
     * @since 9.3
     */
    protected TagAttributes getTagAttributesForMode(Widget widget, String mode, FaceletHandlerHelper helper,
            String widgetId) {
        TagAttributes result;
        if (BuiltinWidgetModes.isLikePlainMode(mode)) {
            // use attributes without id and with
            List<String> excludedProperties = new ArrayList<String>();
            // In case of plain mode css style attributes are to be excluded
            excludedProperties.add("styleClass");
            result = helper.getTagAttributes(widget, excludedProperties, true);
        } else {
            result = helper.getTagAttributes(widgetId, widget);
        }
        return result;
    }

}
