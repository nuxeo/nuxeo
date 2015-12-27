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

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagConfig;

import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOptions;
import org.nuxeo.ecm.platform.ui.web.directory.UIDirectorySelectItem;
import org.nuxeo.ecm.platform.ui.web.directory.UIDirectorySelectItems;

/**
 * Helper class for options generation depending on the widget definition
 *
 * @since 5.4.2
 */
public abstract class AbstractDirectorySelectWidgetTypeHandler extends AbstractSelectWidgetTypeHandler {

    protected enum DirectoryPropertyMappings {
        directoryName, displayAll, displayObsoleteEntries, filter, localize, dbl10n;
    }

    public AbstractDirectorySelectWidgetTypeHandler(TagConfig config) {
        super(config);
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

}
