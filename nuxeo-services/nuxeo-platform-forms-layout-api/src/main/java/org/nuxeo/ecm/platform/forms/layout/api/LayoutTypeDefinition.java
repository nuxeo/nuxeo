/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 * @since 6.0
 */
public interface LayoutTypeDefinition extends Serializable {

    String getName();

    /**
     * Return alias names for this layout type definition (useful for compatibility on old layout type names).
     */
    List<String> getAliases();

    /**
     * Returns templates by mode
     */
    Map<String, String> getTemplates();

    String getTemplate(String mode);

    LayoutTypeConfiguration getConfiguration();

}
