/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: LayoutDefinition.java 26053 2007-10-16 01:45:43Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Layout definition interface.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface LayoutDefinition extends Serializable {

    /**
     * Returns the layout name used to identify it within the layout service.
     */
    String getName();

    /**
     * @since 5.5
     */
    void setName(String name);

    /**
     * Return the layout type, or null if not defined.
     * <p>
     * Since 6.0, the layout type can hold templates and properties configuration, so that layout does not need to
     * define them again.
     *
     * @since 6.0
     */
    String getType();

    /**
     * @since 6.0
     */
    String getTypeCategory();

    /**
     * Returns template to use in a given mode.
     */
    String getTemplate(String mode);

    /**
     * Returns templates by mode
     */
    Map<String, String> getTemplates();

    /**
     * @since 5.5
     */
    void setTemplates(Map<String, String> templates);

    /**
     * Returns the widget definition with given name.
     * <p>
     * Returns null if a widget with this name is not found within the layout.
     */
    WidgetDefinition getWidgetDefinition(String name);

    /**
     * Returns the map of widgets defined inside this layout.
     *
     * @since 8.1
     */
    Map<String, WidgetDefinition> getWidgetDefinitions();

    /**
     * Returns the list of widget names to use at a given row.
     * <p>
     * For instance, this could describe a layout like: [['title'], ['description'], ['creationDate', '',
     * 'modificationDate'], ['subject']].
     */
    LayoutRowDefinition[] getRows();

    /**
     * @since 5.5
     */
    void setRows(LayoutRowDefinition[] rows);

    /**
     * Returns the maximum number of columns.
     */
    int getColumns();

    /**
     * Returns a map of properties to use in a given mode.
     */
    Map<String, Serializable> getProperties(String layoutMode);

    /**
     * Returns a map of properties by mode.
     */
    Map<String, Map<String, Serializable>> getProperties();

    /**
     * @since 5.5
     */
    void setProperties(Map<String, Map<String, Serializable>> properties);

    /**
     * Returns the map of rendering information per mode.
     * <p>
     * Useful for preview management where some configuration needs to be changed: what's changed can be set as
     * rendering information here to be displayed.
     *
     * @since 5.5
     */
    Map<String, List<RenderingInfo>> getRenderingInfos();

    /**
     * Returns the list of rendering information for given mode.
     *
     * @since 5.5
     */
    List<RenderingInfo> getRenderingInfos(String mode);

    /**
     * @since 5.5
     */
    void setRenderingInfos(Map<String, List<RenderingInfo>> renderingInfos);

    /**
     * Return alias names for this layout definition (useful for compatibility on old layout names).
     *
     * @since 6.0
     */
    List<String> getAliases();

    /**
     * Returns true if all widget references in this layout are empty
     *
     * @since 5.6
     */
    boolean isEmpty();

    /**
     * @since 6.0
     */
    boolean isDynamic();

    /**
     * Returns a clone instance of this layout definition.
     * <p>
     * Useful for conversion of layout definition during export.
     *
     * @since 5.5
     */
    LayoutDefinition clone();

}
