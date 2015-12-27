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

import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;

/**
 * Converter for a layout definition.
 *
 * @since 5.5
 */
public interface LayoutDefinitionConverter {

    /**
     * Returns the original layout definition, or a clone if it needs to be changed. Can also return null if layout
     * should be removed.
     */
    LayoutDefinition getLayoutDefinition(LayoutDefinition orig, LayoutConversionContext ctx);

}
