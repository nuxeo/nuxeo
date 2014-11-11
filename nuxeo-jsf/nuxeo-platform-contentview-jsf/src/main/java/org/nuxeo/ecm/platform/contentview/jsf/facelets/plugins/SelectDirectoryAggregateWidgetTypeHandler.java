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
import java.util.List;
import java.util.Map;

import javax.faces.view.facelets.FaceletContext;

import org.nuxeo.ecm.platform.contentview.jsf.facelets.plugins.SelectAggregateWidgetTypeHandler.AggregatePropertyMappings;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;
import org.nuxeo.ecm.platform.forms.layout.facelets.plugins.AbstractDirectorySelectWidgetTypeHandler;

/**
 * @since 6.0
 */
public abstract class SelectDirectoryAggregateWidgetTypeHandler extends
        AbstractDirectorySelectWidgetTypeHandler {

    private static final long serialVersionUID = 1L;

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

}
