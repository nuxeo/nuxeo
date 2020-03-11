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
package org.nuxeo.ecm.platform.forms.layout.core.service;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetReference;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetType;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutManager;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Anahide Tchertchian
 * @since 5.5
 */
public abstract class AbstractLayoutManager extends DefaultComponent implements LayoutManager {

    private static final long serialVersionUID = 1L;

    @Override
    public abstract String getDefaultStoreCategory();

    protected String getStoreCategory(String cat) {
        if (StringUtils.isBlank(cat)) {
            return getDefaultStoreCategory();
        }
        return cat;
    }

    protected LayoutStore getLayoutStore() {
        return Framework.getService(LayoutStore.class);
    }

    protected WidgetDefinition lookupWidget(LayoutDefinition layoutDef, WidgetReference widgetRef) {
        String widgetName = widgetRef.getName();
        WidgetDefinition wDef = null;
        if (layoutDef != null) {
            wDef = layoutDef.getWidgetDefinition(widgetName);
            if (wDef != null && wDef.getType() == null) {
                // consider it's a reference for a global widget
                wDef = null;
            }
        }
        if (wDef == null) {
            // try in global registry
            wDef = lookupWidget(widgetRef);
        }
        return wDef;
    }

    protected WidgetDefinition lookupWidget(WidgetReference widgetRef) {
        String widgetName = widgetRef.getName();
        String cat = widgetRef.getCategory();
        WidgetDefinition wDef;
        if (StringUtils.isBlank(cat)) {
            wDef = getWidgetDefinition(widgetName);
        } else {
            wDef = getLayoutStore().getWidgetDefinition(cat, widgetName);
        }
        if (wDef != null) {
            wDef.setGlobal(true);
        }
        return wDef;
    }

    @Override
    public WidgetType getWidgetType(String typeName) {
        return getLayoutStore().getWidgetType(getDefaultStoreCategory(), typeName);
    }

    @Override
    public WidgetTypeDefinition getWidgetTypeDefinition(String typeName) {
        return getLayoutStore().getWidgetTypeDefinition(getDefaultStoreCategory(), typeName);
    }

    @Override
    public List<WidgetTypeDefinition> getWidgetTypeDefinitions() {
        return getLayoutStore().getWidgetTypeDefinitions(getDefaultStoreCategory());
    }

    @Override
    public LayoutTypeDefinition getLayoutTypeDefinition(String typeName) {
        return getLayoutStore().getLayoutTypeDefinition(getDefaultStoreCategory(), typeName);
    }

    @Override
    public List<LayoutTypeDefinition> getLayoutTypeDefinitions() {
        return getLayoutStore().getLayoutTypeDefinitions(getDefaultStoreCategory());
    }

    @Override
    public LayoutDefinition getLayoutDefinition(String layoutName) {
        return getLayoutStore().getLayoutDefinition(getDefaultStoreCategory(), layoutName);
    }

    @Override
    public List<String> getLayoutDefinitionNames() {
        return getLayoutStore().getLayoutDefinitionNames(getDefaultStoreCategory());
    }

    @Override
    public WidgetDefinition getWidgetDefinition(String widgetName) {
        return getLayoutStore().getWidgetDefinition(getDefaultStoreCategory(), widgetName);
    }

    // registry helpers

    protected void registerWidgetType(WidgetTypeDefinition desc) {
        getLayoutStore().registerWidgetType(getDefaultStoreCategory(), desc);
    }

    protected void unregisterWidgetType(WidgetTypeDefinition desc) {
        getLayoutStore().unregisterWidgetType(getDefaultStoreCategory(), desc);
    }

    protected void registerLayoutType(LayoutTypeDefinition desc) {
        getLayoutStore().registerLayoutType(getDefaultStoreCategory(), desc);
    }

    protected void unregisterLayoutType(LayoutTypeDefinition desc) {
        getLayoutStore().unregisterLayoutType(getDefaultStoreCategory(), desc);
    }

    protected void registerLayout(LayoutDefinition layoutDef) {
        getLayoutStore().registerLayout(getDefaultStoreCategory(), layoutDef);
    }

    protected void unregisterLayout(LayoutDefinition layoutDef) {
        getLayoutStore().unregisterLayout(getDefaultStoreCategory(), layoutDef);
    }

    protected void registerWidget(WidgetDefinition widgetDef) {
        getLayoutStore().registerWidget(getDefaultStoreCategory(), widgetDef);
    }

    protected void unregisterWidget(WidgetDefinition widgetDef) {
        getLayoutStore().unregisterWidget(getDefaultStoreCategory(), widgetDef);
    }

}
