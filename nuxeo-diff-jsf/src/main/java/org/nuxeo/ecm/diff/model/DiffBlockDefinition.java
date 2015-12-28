/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     ataillefer
 */
package org.nuxeo.ecm.diff.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Diff block definition interface.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public interface DiffBlockDefinition extends Serializable {

    /**
     * Gets the diff block definition name.
     */
    String getName();

    /**
     * Returns the template to use in a given mode.
     */
    String getTemplate(String mode);

    /**
     * Returns a map of templates by mode.
     */
    Map<String, String> getTemplates();

    /**
     * Returns the list of field definitions.
     */
    List<DiffFieldDefinition> getFields();

    /**
     * Returns a map of properties to use in a given mode.
     */
    Map<String, Serializable> getProperties(String layoutMode);

    /**
     * Returns a map of properties by mode.
     */
    Map<String, Map<String, Serializable>> getProperties();
}
