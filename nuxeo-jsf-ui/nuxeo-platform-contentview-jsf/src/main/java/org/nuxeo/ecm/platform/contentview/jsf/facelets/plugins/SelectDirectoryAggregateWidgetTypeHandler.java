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
import java.util.List;
import java.util.Map;

import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagConfig;

import org.nuxeo.ecm.platform.contentview.jsf.facelets.plugins.SelectAggregateWidgetTypeHandler.AggregatePropertyMappings;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;
import org.nuxeo.ecm.platform.forms.layout.facelets.plugins.AbstractDirectorySelectWidgetTypeHandler;

/**
 * @since 6.0
 */
public abstract class SelectDirectoryAggregateWidgetTypeHandler extends AbstractDirectorySelectWidgetTypeHandler {

    public SelectDirectoryAggregateWidgetTypeHandler(TagConfig config) {
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
        props.put(SelectPropertyMappings.itemLabelSuffix.name(),
                widget.getProperty(AggregatePropertyMappings.itemCount.name()));
        return props;
    }

}
