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
 * $Id: IORelationAdapterProperties.java 25081 2007-09-18 14:57:22Z atchertchian $
 */

package org.nuxeo.ecm.platform.relations.io;

import org.nuxeo.ecm.platform.relations.api.impl.RelationDate;

/**
 * Map of property names used by the relation adapter.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class IORelationAdapterProperties {

    /**
     * Property representing the graph name where relations should be extracted.
     */
    public static final String GRAPH = "graph";

    /**
     * Property representing the graph name where relations should be imported.
     * <p>
     * Defaults to the graph property value.
     */
    public static final String IMPORT_GRAPH = "import-graph";

    /**
     * Property representing a boolean value that indicates if only internal relations must be kept when
     * exporting/importing.
     * <p>
     * For instance, if a document tree is copied, we could chose to ignore relations pointing to documents outside of
     * the copied tree.
     * <p>
     * Default value: false.
     */
    public static final String IGNORE_EXTERNAL = "ignore-external";

    /**
     * Property representing a boolean value that indicates if relations involving literals should be ignored when
     * importing/exporting.
     * <p>
     * Default value: false.
     */
    public static final String IGNORE_LITERALS = "ignore-literals";

    /**
     * Property representing a boolean value that indicates if relations involving resources that are not QName
     * resources should be ignored when importing/exporting.
     * <p>
     * Default value: false.
     */
    public static final String IGNORE_SIMPLE_RESOURCES = "ignore-simple-resources";

    /**
     * Property representing a list of predicates uris that should be filtered.
     * <p>
     * If list is not empty, relations using a predicate which is not on this list will not be kept.
     */
    public static final String FILTER_PREDICATES = "filter-predicates";

    /**
     * Property representing a list of predicates uris that should be ignored.
     * <p>
     * If list is not empty, relations using a predicate which is on this list will not be kept.
     */
    public static final String IGNORE_PREDICATES = "ignore-predicates";

    /**
     * Property representing a list of metadata uris that should be filtered.
     * <p>
     * If list is not empty, metadata (properties) for relations using a uri which is not on this list will not be kept.
     */
    public static final String FILTER_METADATA = "filter-metatada";

    /**
     * Property representing a list of metadata uris that should be ignored.
     * <p>
     * If list is not empty, metadata (properties) for relations using a uri which is on this list will not be kept.
     */
    public static final String IGNORE_METADATA = "ignore-metatada";

    /**
     * Property representing a boolean value that indicates if metadata should not be kept when exporting/importing.
     * <p>
     * Default value: false.
     */
    public static final String IGNORE_ALL_METADATA = "ignore-all-metatada";

    /**
     * Property representing a list of metadata uris that should be updated.
     * <p>
     * If list is not empty, metadata (properties) for relations using a uri which is on this list will be updated to
     * match current date.
     * <p>
     * Current date is set as a literal as described in {@link RelationDate}
     */
    public static final String UPDATE_DATE_METADATA = "update-date-metatada";

    // Constant utility class.
    private IORelationAdapterProperties() {
    }

}
