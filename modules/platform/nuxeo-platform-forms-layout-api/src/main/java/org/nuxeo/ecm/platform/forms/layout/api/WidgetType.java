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
     * Returns the name of this widget type, used to identify it in the service.
     */
    String getName();

    /**
     * Return alias names for this widget type (useful for compatibility on old widget types).
     *
     * @since 6.0
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
