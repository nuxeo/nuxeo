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
package org.nuxeo.ecm.platform.forms.layout.api.converters;

import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;

/**
 * Abstract implementation to ease up upgrade if interface changes.
 *
 * @since 5.5
 */
public abstract class AbstractWidgetDefinitionConverter implements WidgetDefinitionConverter {

    protected WidgetDefinition getClonedWidget(WidgetDefinition widget) {
        return widget.clone();
    }

}
