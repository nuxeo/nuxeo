/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
import java.util.List;
import java.util.Map;

/**
 * Widget type definition
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public interface WidgetTypeDefinition extends Serializable {

    String getName();

    /**
     * Return alias names for this widget type definition (useful for compatibility on old widget type names).
     *
     * @since 6.0
     */
    List<String> getAliases();

    String getHandlerClassName();

    Map<String, String> getProperties();

    WidgetTypeConfiguration getConfiguration();

}
