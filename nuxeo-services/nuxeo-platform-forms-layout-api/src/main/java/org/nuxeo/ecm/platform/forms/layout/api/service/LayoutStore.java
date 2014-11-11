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
 * It manages registries of layout definitions, widget types, widget
 * definitions and converters.
 *
 * @since 5.5
 */
public interface LayoutStore extends Serializable {

    /**
     * Returns categories of layout and widgets definitions and instances held
     * by this service.
     */
    List<String> getCategories();

    /**
     * Returns the registered widget type for this type name.
     * <p>
     * If the no widget type is found with this name, return null.
     */
    WidgetType getWidgetType(String category, String typeName);

    /**
     * Returns the widget type definition with given name, or null if no widget
     * type with this name is found.
     */
    WidgetTypeDefinition getWidgetTypeDefinition(String category,
            String typeName);

    /**
     * Returns the widget type definitions for all the registered widget types.
     */
    List<WidgetTypeDefinition> getWidgetTypeDefinitions(String category);

    /**
     * @since 5.9.6
     */
    LayoutTypeDefinition getLayoutTypeDefinition(String category,
            String typeName);

    /**
     * @since 5.9.6
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
     * @since 5.9.6
     */
    void registerLayoutType(String category, LayoutTypeDefinition desc);

    /**
     * @since 5.9.6
     */
    void unregisterLayoutType(String category, LayoutTypeDefinition desc);

    void registerLayout(String category, LayoutDefinition layoutDef);

    void unregisterLayout(String category, LayoutDefinition layoutDef);

    void registerWidget(String category, WidgetDefinition widgetDef);

    void unregisterWidget(String category, WidgetDefinition widgetDef);

}
