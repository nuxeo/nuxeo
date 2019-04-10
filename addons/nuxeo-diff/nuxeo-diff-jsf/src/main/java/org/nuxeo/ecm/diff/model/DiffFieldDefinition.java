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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.model;

import java.io.Serializable;
import java.util.List;

/**
 * Diff field definition interface.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public interface DiffFieldDefinition extends Serializable {

    /**
     * Optional category on the field: if this category is filled, the widget instance will be looked up with this
     * category in the store
     */
    String getCategory();

    /**
     * Gets the field schema.
     *
     * @return the field schema
     */
    String getSchema();

    /**
     * Gets the field name.
     *
     * @return the field name
     */
    String getName();

    /**
     * Checks if must display content diff links.
     *
     * @return true, if must display content diff links
     */
    boolean isDisplayContentDiffLinks();

    /**
     * Gets the field items.
     *
     * @return the field items
     */
    List<DiffFieldItemDefinition> getItems();

}
