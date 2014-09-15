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
import java.util.HashMap;
import java.util.Map;

import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagAttributes;

import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOptions;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.ui.web.directory.UIDirectorySelectItem;
import org.nuxeo.ecm.platform.ui.web.directory.UIDirectorySelectItems;

/**
 * @since 5.9.6
 */
public abstract class SelectDirectoryAggregateWidgetTypeHandler extends
        SelectAggregateWidgetTypeHandler {

    private static final long serialVersionUID = 1L;

    protected enum DirectoryAggregatePropertyMappings {
        directoryName;
    }

    @Override
    protected FaceletHandler getOptionFaceletHandler(FaceletContext ctx,
            FaceletHandlerHelper helper, Widget widget,
            WidgetSelectOption selectOption, FaceletHandler nextHandler) {
        String componentType;
        if (selectOption instanceof WidgetSelectOptions) {
            componentType = UIDirectorySelectItems.COMPONENT_TYPE;
        } else {
            componentType = UIDirectorySelectItem.COMPONENT_TYPE;
        }
        Map<String, Serializable> additionalProps = new HashMap<>();
        additionalProps.put(
                SelectPropertyMappings.itemLabel.name(),
                selectOption.getItemLabel()
                        + (String) widget.getProperty(AggregatePropertyMappings.itemCount.name()));
        additionalProps.put(
                DirectoryAggregatePropertyMappings.directoryName.name(),
                widget.getProperty(DirectoryAggregatePropertyMappings.directoryName.name()));
        TagAttributes attrs = helper.getTagAttributes(selectOption,
                additionalProps);
        return helper.getHtmlComponentHandler(widget.getTagConfigId(), attrs,
                nextHandler, componentType, null);
    }

}
