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
 * $Id: FieldDefinition.java 26053 2007-10-16 01:45:43Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.api;

import java.io.Serializable;

/**
 * Interface for field definition.
 * <p>
 * Will help to identify a document field.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface FieldDefinition extends Serializable {

    /**
     * Optional schema name.
     * <p>
     * If the field is used to identify a sub field within a complex type, no schema name is needed. Otherwise, the
     * field will not be resolved correctly if the property does not have a prefix.
     */
    String getSchemaName();

    /**
     * Returns the field name, following XPath conventions.
     * <p>
     * If the field is prefixed, it should contain the prefix followed by ':'. Examples: dc:title, dc:author/name
     */
    String getFieldName();

    /**
     * Returns the computed property name with schema and field information.
     */
    String getPropertyName();

    /**
     * Returns a clone instance of this field definition.
     * <p>
     * Useful for conversion of layout definition during export.
     *
     * @since 5.5
     */
    FieldDefinition clone();

}
