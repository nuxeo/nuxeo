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

/**
 * Multiple select options top be held by the {@link WidgetDefinition} and {@link Widget} generated from the definition.
 *
 * @author Anahide Tchertchian
 * @since 5.4.2
 */
public interface WidgetSelectOptions extends WidgetSelectOption {

    /**
     * Returns a string used for ordering of options.
     * <p>
     * Sample possible values are 'id' and 'label'.
     */
    String getOrdering();

    /**
     * Returns true if ordering should be case sensitive?
     */
    Boolean getCaseSensitive();

}
