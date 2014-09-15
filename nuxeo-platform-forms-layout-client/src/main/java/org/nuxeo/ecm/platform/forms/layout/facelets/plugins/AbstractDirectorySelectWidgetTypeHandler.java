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
import java.util.List;
import java.util.Map;

import javax.faces.view.facelets.FaceletContext;

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
public abstract class AbstractDirectorySelectWidgetTypeHandler extends
        AbstractSelectWidgetTypeHandler {

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

    protected Map<String, Serializable> getOptionProperties(FaceletContext ctx,
            Widget widget, WidgetSelectOption selectOption) {
        Map<String, Serializable> props = super.getOptionProperties(ctx,
                widget, selectOption);
        for (DirectoryPropertyMappings mapping : DirectoryPropertyMappings.values()) {
            if (widget.getProperties().containsKey(mapping.name())) {
                props.put(mapping.name(), widget.getProperty(mapping.name()));
            }
        }
        return props;
    }

}
