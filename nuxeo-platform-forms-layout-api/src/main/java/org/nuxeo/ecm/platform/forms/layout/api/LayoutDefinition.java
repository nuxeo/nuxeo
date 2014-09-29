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
     * Since 5.9.6, the layout type can hold templates and properties
     * configuration, so that layout does not need to define them again.
     *
     * @since 5.9.6
     */
    String getType();

    /**
     * @since 5.9.6
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
     * Returns the list of widget names to use at a given row.
     * <p>
     * For instance, this could describe a layout like: [['title'],
     * ['description'], ['creationDate', '', 'modificationDate'], ['subject']].
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
     * Useful for preview management where some configuration needs to be
     * changed: what's changed can be set as rendering information here to be
     * displayed.
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
     * Returns a clone instance of this layout definition.
     * <p>
     * Useful for conversion of layout definition during export.
     *
     * @since 5.5
     */
    LayoutDefinition clone();

    /**
     * Returns true if all widget references in this layout are empty
     *
     * @since 5.6
     */
    boolean isEmpty();

}
