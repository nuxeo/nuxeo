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
 * Representation of a document diff, schema by schema and field by field.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public interface DocumentDiff extends Serializable {

    /**
     * Gets the doc diff.
     * 
     * @return the doc diff
     */
    Map<String, SchemaDiff> getDocDiff();

    /**
     * Gets the schema count.
     * 
     * @return the schema count
     */
    int getSchemaCount();

    /**
     * Checks if the doc diff is empty.
     * 
     * @return true, if is empty
     */
    boolean isDocDiffEmpty();

    /**
     * Gets the schema names as a list.
     * 
     * @return the schema names
     */
    List<String> getSchemaNames();

    /**
     * Gets the schema diff.
     * 
     * @param schema the schema
     * @return the schema diff
     */
    SchemaDiff getSchemaDiff(String schema);

    /**
     * Inits schema diff.
     * 
     * @param schema the schema
     * @return the map
     */
    SchemaDiff initSchemaDiff(String schema);

}
