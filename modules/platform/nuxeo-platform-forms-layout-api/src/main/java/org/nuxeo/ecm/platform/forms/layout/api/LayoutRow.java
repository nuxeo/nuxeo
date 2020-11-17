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
 * $Id: LayoutRow.java 26053 2007-10-16 01:45:43Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.api;

import java.io.Serializable;
import java.util.Map;

/**
 * Layout row interface.
 * <p>
 * A layout row is a list {@link Widget} instances, built from a {@link LayoutRowDefinition} in a given mode. It gives
 * information about the widgets presentation.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface LayoutRow extends Serializable {

    String getName();

    /**
     * Returns the unique identifier of this widget to be used in tag configuration.
     *
     * @since 5.4.2
     * @see Layout#getTagConfigId()
     */
    String getTagConfigId();

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
     * The way that properties will be mapped to rendered components is managed by the widget type.
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
