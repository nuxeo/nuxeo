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
 * $Id: LayoutRow.java 26053 2007-10-16 01:45:43Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.api;

import java.io.Serializable;
import java.util.Map;

/**
 * Layout row interface.
 * <p>
 * A layout row is a list {@link Widget} instances, built from a
 * {@link LayoutRowDefinition} in a given mode. It gives information about the
 * widgets presentation.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface LayoutRow extends Serializable {

    String getName();

    /**
     * Returns true if this row should be considered selected by default
     */
    boolean isSelectedByDefault();

    /**
     * Return true if this row should be considered always selected
     */
    boolean isAlwaysSelected();

    Widget[] getWidgets();

    int getSize();

    /**
     * Get properties to use in this mode.
     * <p>
     * The way that properties will be mapped to rendered components is managed
     * by the widget type.
     *
     * @since 5.3.1
     */
    Map<String, Serializable> getProperties();

    /**
     * Returns property with given name in this mode.
     *
     * @param name the property name.
     * @return the property value or null if not found.
     * @since 5.3.1
     */
    Serializable getProperty(String name);

}
