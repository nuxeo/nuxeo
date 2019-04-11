/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.forms.layout.demo.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.forms.layout.demo.descriptors.DemoWidgetTypeDescriptor;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Anahide Tchertchian
 */
public class LayoutDemoService extends DefaultComponent implements LayoutDemoManager {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(LayoutDemoService.class);

    public static final String WIDGET_TYPES_EP_NAME = "widgettypes";

    private final Map<String, DemoWidgetType> widgetTypeRegistry = new HashMap<>();

    private final Map<String, List<String>> widgetTypesByCategory = new HashMap<>();

    private final Map<String, String> widgetTypeByViewId = new HashMap<>();

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(WIDGET_TYPES_EP_NAME)) {
            registerWidgetType(contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(WIDGET_TYPES_EP_NAME)) {
            unregisterWidgetType(contribution);
        }
    }

    private void registerWidgetType(Object contribution) {
        DemoWidgetTypeDescriptor desc = (DemoWidgetTypeDescriptor) contribution;
        String name = desc.getName();
        if (widgetTypeRegistry.containsKey(name)) {
            log.error(String.format("Overriding definition for widget type %s", name));
            widgetTypeRegistry.remove(name);
        }
        String category = desc.getCategory();
        String wtCat = desc.getWidgetTypeCategory();
        if (wtCat == null) {
            wtCat = WebLayoutManager.JSF_CATEGORY;
        }
        String viewId = desc.getViewId();
        // TODO: query the layout service to get more information about this
        // widget type and use it in the demo
        DemoWidgetType widgetType = new DemoWidgetTypeImpl(desc.getWidgetTypeName(), desc.getLabel(), viewId, category,
                wtCat, desc.isPreviewEnabled(), desc.isPreviewHideViewMode(), desc.isPreviewHideEditMode(),
                desc.getFields(), desc.getDefaultProperties(), desc.getDemoLayouts());
        widgetTypeRegistry.put(name, widgetType);
        if (category != null) {
            List<String> byCat = widgetTypesByCategory.get(category);
            if (byCat != null && !byCat.contains(name)) {
                byCat.add(name);
            } else {
                byCat = new ArrayList<>();
                byCat.add(name);
            }
            widgetTypesByCategory.put(category, byCat);
        }
        if (widgetTypeByViewId.containsKey(viewId)) {
            String existingWidget = widgetTypeByViewId.get(viewId);
            if (!name.equals(existingWidget)) {
                log.warn(String.format("Changing view id '%s' from widget '%s' to widget '%s'", viewId, existingWidget,
                        name));
            }
        }
        widgetTypeByViewId.put(viewId, name);
        log.info("Registered widget type: " + name);
    }

    private void unregisterWidgetType(Object contribution) {
        DemoWidgetTypeDescriptor desc = (DemoWidgetTypeDescriptor) contribution;
        String name = desc.getName();
        if (widgetTypeRegistry.containsKey(name)) {
            widgetTypeRegistry.remove(name);
            log.debug("Unregistered widget type: " + name);
        }
        String category = desc.getCategory();
        if (category != null) {
            List<String> byCat = widgetTypesByCategory.get(category);
            if (byCat != null && byCat.contains(name)) {
                byCat.remove(name);
                widgetTypesByCategory.put(category, byCat);
            }
        }
    }

    @Override
    public DemoWidgetType getWidgetType(String widgetTypeName) {
        return widgetTypeRegistry.get(widgetTypeName);
    }

    @Override
    public DemoWidgetType getWidgetTypeByViewId(String viewId) {
        if (!widgetTypeByViewId.containsKey(viewId)) {
            return null;
        }
        String name = widgetTypeByViewId.get(viewId);
        return widgetTypeRegistry.get(name);
    }

    @Override
    public List<DemoWidgetType> getWidgetTypes(String category) {
        List<String> wTypes = widgetTypesByCategory.get(category);
        List<DemoWidgetType> res = new ArrayList<>();
        if (wTypes != null) {
            for (String wType : wTypes) {
                res.add(widgetTypeRegistry.get(wType));
            }
        }
        return res;
    }

}
