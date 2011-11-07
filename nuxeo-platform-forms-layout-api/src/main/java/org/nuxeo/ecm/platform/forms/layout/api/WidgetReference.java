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
package org.nuxeo.ecm.platform.forms.layout.api;

import java.io.Serializable;

/**
 * Reference for a widget within a layout row.
 *
 * @since 5.5
 */
public interface WidgetReference extends Serializable {

    /**
     * Optional category on the widget: if this category is filled, the widget
     * instance will be looked up with this category in the store
     */
    String getCategory();

    /**
     * Widget name. If a widget with this name is present in the layout
     * definition, it references this widget. Else, if a category is filled,
     * the widget is looked up in the store with this category. If no category
     * is filled, the widget is looked up in the same store than the one of the
     * layout.
     */
    String getName();

    /**
     * @since 5.5
     */
    WidgetReference clone();

}
