/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
     * Property representing a boolean value that indicates if only internal
     * relations must be kept when exporting/importing.
     * <p>
     * For instance, if a document tree is copied, we could chose to ignore
     * relations pointing to documents outside of the copied tree.
     * <p>
     * Default value: false.
     */
    public static final String IGNORE_EXTERNAL = "ignore-external";

    /**
     * Property representing a boolean value that indicates if relations
     * involving literals should be ignored when importing/exporting.
     * <p>
     * Default value: false.
     */
    public static final String IGNORE_LITERALS = "ignore-literals";

    /**
     * Property representing a boolean value that indicates if relations
     * involving resources that are not QName resources should be ignored when
     * importing/exporting.
     * <p>
     * Default value: false.
     */
    public static final String IGNORE_SIMPLE_RESOURCES = "ignore-simple-resources";

    /**
     * Property representing a list of predicates uris that should be filtered.
     * <p>
     * If list is not empty, relations using a predicate which is not on this
     * list will not be kept.
     */
    public static final String FILTER_PREDICATES = "filter-predicates";

    /**
     * Property representing a list of predicates uris that should be ignored.
     * <p>
     * If list is not empty, relations using a predicate which is on this list
     * will not be kept.
     */
    public static final String IGNORE_PREDICATES = "ignore-predicates";

    /**
     * Property representing a list of metadata uris that should be filtered.
     * <p>
     * If list is not empty, metadata (properties) for relations using a uri
     * which is not on this list will not be kept.
     */
    public static final String FILTER_METADATA = "filter-metatada";

    /**
     * Property representing a list of metadata uris that should be ignored.
     * <p>
     * If list is not empty, metadata (properties) for relations using a uri
     * which is on this list will not be kept.
     */
    public static final String IGNORE_METADATA = "ignore-metatada";

    /**
     * Property representing a boolean value that indicates if metadata should
     * not be kept when exporting/importing.
     * <p>
     * Default value: false.
     */
    public static final String IGNORE_ALL_METADATA = "ignore-all-metatada";

    /**
     * Property representing a list of metadata uris that should be updated.
     * <p>
     * If list is not empty, metadata (properties) for relations using a uri
     * which is on this list will be updated to match current date.
     * <p>
     * Current date is set as a literal as described in {@link RelationDate}
     */
    public static final String UPDATE_DATE_METADATA = "update-date-metatada";

    // Constant utility class.
    private IORelationAdapterProperties() {
    }

}
