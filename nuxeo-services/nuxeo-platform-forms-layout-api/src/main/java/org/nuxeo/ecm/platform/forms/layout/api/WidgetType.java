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
 * $Id: WidgetType.java 26053 2007-10-16 01:45:43Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Interface for widget type.
 * <p>
 * A widget type is used to handle the rendering of a widget in a given mode.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface WidgetType extends Serializable {

    /**
     * Returns the name of this widget type, used to identify it in the
     * service.
     */
    String getName();

    /**
     * Return alias names for this widget type (useful for compatibility
     * on old widget types).
     *
     * @since 5.9.6
     */
    List<String> getAliases();

    /**
     * Returns the class defining this widget type behaviour.
     */
    Class<?> getWidgetTypeClass();

    /**
     * Returns properties.
     */
    Map<String, String> getProperties();

}
