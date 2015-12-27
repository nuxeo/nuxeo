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
package org.nuxeo.ecm.platform.forms.layout.api;

import java.io.Serializable;

/**
 * Reference for a widget within a layout row.
 *
 * @since 5.5
 */
public interface WidgetReference extends Serializable {

    /**
     * Optional category on the widget: if this category is filled, the widget instance will be looked up with this
     * category in the store
     */
    String getCategory();

    /**
     * Widget name. If a widget with this name is present in the layout definition, it references this widget. Else, if
     * a category is filled, the widget is looked up in the store with this category. If no category is filled, the
     * widget is looked up in the same store than the one of the layout.
     */
    String getName();

    /**
     * @since 5.5
     */
    WidgetReference clone();

}
