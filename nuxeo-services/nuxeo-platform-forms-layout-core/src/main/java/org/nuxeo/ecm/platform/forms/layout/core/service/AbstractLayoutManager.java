/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.forms.layout.core.service;

import java.util.List;

import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
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
public abstract class AbstractLayoutManager extends DefaultComponent implements
        LayoutManager {

    private static final long serialVersionUID = 1L;

    public abstract String getDefaultStoreCategory();

    protected LayoutStore getLayoutStore() {
        LayoutStore lm = null;
        try {
            lm = Framework.getLocalService(LayoutStore.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (lm == null) {
            throw new RuntimeException("Missing service for LayoutStore");
        }
        return lm;
    }

    @Override
    public WidgetType getWidgetType(String typeName) {
        return getLayoutStore().getWidgetType(getDefaultStoreCategory(),
                typeName);
    }

    @Override
    public WidgetTypeDefinition getWidgetTypeDefinition(String typeName) {
        return getLayoutStore().getWidgetTypeDefinition(
                getDefaultStoreCategory(), typeName);
    }

    @Override
    public List<WidgetTypeDefinition> getWidgetTypeDefinitions() {
        return getLayoutStore().getWidgetTypeDefinitions(
                getDefaultStoreCategory());
    }

    @Override
    public LayoutTypeDefinition getLayoutTypeDefinition(String typeName) {
        return getLayoutStore().getLayoutTypeDefinition(
                getDefaultStoreCategory(), typeName);
    }

    @Override
    public List<LayoutTypeDefinition> getLayoutTypeDefinitions() {
        return getLayoutStore().getLayoutTypeDefinitions(
                getDefaultStoreCategory());
    }

    @Override
    public LayoutDefinition getLayoutDefinition(String layoutName) {
        return getLayoutStore().getLayoutDefinition(getDefaultStoreCategory(),
                layoutName);
    }

    @Override
    public List<String> getLayoutDefinitionNames() {
        return getLayoutStore().getLayoutDefinitionNames(
                getDefaultStoreCategory());
    }

    @Override
    public WidgetDefinition getWidgetDefinition(String widgetName) {
        return getLayoutStore().getWidgetDefinition(getDefaultStoreCategory(),
                widgetName);
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
