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
     * Returns template to use in a given mode.
     */
    String getTemplate(String mode);

    /**
     * Returns templates by mode
     */
    Map<String, String> getTemplates();

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
}
