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
package org.nuxeo.ecm.platform.forms.layout.api.service;

import java.io.Serializable;
import java.util.List;

import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetType;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.converters.LayoutDefinitionConverter;
import org.nuxeo.ecm.platform.forms.layout.api.converters.WidgetDefinitionConverter;

/**
 * Layout store interface.
 * <p>
 * It manages registries of layout definitions, widget types, widget definitions and converters.
 *
 * @since 5.5
 */
public interface LayoutStore extends Serializable {

    /**
     * Returns categories of layout and widgets definitions and instances held by this service.
     */
    List<String> getCategories();

    /**
     * Returns the registered widget type for this type name.
     * <p>
     * If the no widget type is found with this name, return null.
     */
    WidgetType getWidgetType(String category, String typeName);

    /**
     * Returns the widget type definition with given name, or null if no widget type with this name is found.
     */
    WidgetTypeDefinition getWidgetTypeDefinition(String category, String typeName);

    /**
     * Returns the widget type definitions for all the registered widget types.
     */
    List<WidgetTypeDefinition> getWidgetTypeDefinitions(String category);

    /**
     * @since 6.0
     */
    LayoutTypeDefinition getLayoutTypeDefinition(String category, String typeName);

    /**
     * @since 6.0
     */
    List<LayoutTypeDefinition> getLayoutTypeDefinitions(String category);

    /**
     * Returns the registered layout definition for this name.
     * <p>
     * If the no definition is found with this name, return null.
     */
    LayoutDefinition getLayoutDefinition(String category, String layoutName);

    /**
     * Returns the names of all the registered layout definitions
     */
    List<String> getLayoutDefinitionNames(String category);

    /**
     * Returns the registered widget definition for this name.
     * <p>
     * If the no definition is found with this name, return null.
     */
    WidgetDefinition getWidgetDefinition(String category, String widgetName);

    List<LayoutDefinitionConverter> getLayoutConverters(String category);

    List<WidgetDefinitionConverter> getWidgetConverters(String category);

    // registry API

    void registerWidgetType(String category, WidgetTypeDefinition desc);

    void unregisterWidgetType(String category, WidgetTypeDefinition desc);

    /**
     * @since 6.0
     */
    void registerLayoutType(String category, LayoutTypeDefinition desc);

    /**
     * @since 6.0
     */
    void unregisterLayoutType(String category, LayoutTypeDefinition desc);

    void registerLayout(String category, LayoutDefinition layoutDef);

    void unregisterLayout(String category, LayoutDefinition layoutDef);

    void registerWidget(String category, WidgetDefinition widgetDef);

    void unregisterWidget(String category, WidgetDefinition widgetDef);

}
