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
 * Representation of a schema diff, field by field.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public interface SchemaDiff extends Serializable {

    /**
     * Gets the schema diff.
     * 
     * @return the schema diff
     */
    Map<String, PropertyDiff> getSchemaDiff();

    /**
     * Gets the field count.
     * 
     * @return the field count
     */
    int getFieldCount();

    /**
     * Gets the field names as a list.
     * 
     * @return the field names
     */
    List<String> getFieldNames();

    /**
     * Gets the field diff.
     * 
     * @param field the field
     * @return the field diff
     */
    PropertyDiff getFieldDiff(String field);

    /**
     * Put field diff.
     * 
     * @param field the field
     * @param fieldDiff the field diff
     * @return the property diff
     */
    PropertyDiff putFieldDiff(String field, PropertyDiff fieldDiff);

}
