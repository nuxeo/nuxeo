/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: LayoutManager.java 26053 2007-10-16 01:45:43Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.api.service;

import java.io.Serializable;
import java.util.List;

import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetType;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;

/**
 * Layout manager interface.
 * <p>
 * It manages access to layout definitions, widget definitions and widget types
 * for a given category.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface LayoutManager extends Serializable {

    /**
     * Return the default category used for storage
     *
     * @since 5.5
     */
    String getDefaultStoreCategory();

    /**
     * Returns the registered widget type for this type name.
     * <p>
     * If the no widget type is found with this name, return null.
     */
    WidgetType getWidgetType(String typeName);

    /**
     * Returns the widget type definition with given name, or null if no widget
     * type with this name is found.
     *
     * @since 5.4
     */
    WidgetTypeDefinition getWidgetTypeDefinition(String typeName);

    /**
     * Returns the widget type definitions for all the registered widget types.
     *
     * @since 5.4
     */
    List<WidgetTypeDefinition> getWidgetTypeDefinitions();

    /**
     * Returns the layout type definition with given name, or null if no layout
     * type with this name is found.
     *
     * @since 6.0
     */
    LayoutTypeDefinition getLayoutTypeDefinition(String typeName);

    /**
     * Returns the layout type definitions for all the registered layout types.
     *
     * @since 6.0
     */
    List<LayoutTypeDefinition> getLayoutTypeDefinitions();

    /**
     * Returns the registered layout definition for this name.
     * <p>
     * If the no definition is found with this name, return null.
     */
    LayoutDefinition getLayoutDefinition(String layoutName);

    /**
     * Returns the names of all the registered layout definitions
     *
     * @since 5.5
     */
    List<String> getLayoutDefinitionNames();

    /**
     * Returns the registered widget definition for this name.
     * <p>
     * If the no definition is found with this name, return null.
     *
     * @since 5.1.7, 5.2.0
     */
    WidgetDefinition getWidgetDefinition(String widgetName);

}
