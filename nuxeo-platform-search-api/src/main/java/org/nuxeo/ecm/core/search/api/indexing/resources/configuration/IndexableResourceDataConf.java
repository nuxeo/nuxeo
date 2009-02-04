/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: IndexableField.java 13119 2007-03-01 17:56:22Z janguenot $
 */

package org.nuxeo.ecm.core.search.api.indexing.resources.configuration;

import java.io.Serializable;
import java.util.Map;

/**
 * Indexable resource data configuration.
 * <p>
 * Holds the properties specifying the way data from a given resource will be
 * indexed. An indexable resource data configuration is bound to an
 * <code>IndexableResourceConf</code> instance.
 *
 * @see IndexableResourceConf
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface IndexableResourceDataConf extends Serializable {

    /**
     * Returns the name of the target indexable data once indexed.
     * <p>
     * The name here is not necessarily bound to the resource data key(s).This
     * is the name that aimed at being used at backend level for the target
     * index.
     *
     * @return the name of the target indexable data index in the backend
     */
    String getIndexingName();

    /**
     * Returns the target indexing field type.
     *
     * @return the field type
     */
    String getIndexingType();

    /**
     * Returns the analyzer that must be applied on the indexable data at
     * backend level.
     *
     * @return the analyzer name
     */
    String getIndexingAnalyzer();

    /**
     * Returns if whether or not the value will be stored.
     *
     * @return true if stored false if unstored.
     */
    boolean isStored();

    /**
     * Returns if whether or not the value will be indexed.
     *
     * @return true if indexed, false if unindexed.
     */
    boolean isIndexed();

    /**
     * Returns whether or not the value is a multiple one.
     *
     * @return true if multiple, false otherwise
     */
    boolean isMultiple();

    /**
     * Say if special care must be taken to make the field sortable.
     * <p>
     * Typically (e.g., with Lucene) a tokenized/stemmed/analyzed field would
     * not be sortable out of the box. The special treatment to restore
     * sortability being potentially very costly, it should be applied only to
     * fields for which isSortable() is true.
     * <p>
     * A false value doesn't forbid sortability for fields whose indexing
     * digestion doesn't technically prevent sortability.
     *
     * @return a boolean
     */
    boolean isSortable();

    /**
     * Get a string describing behaviour of this data with respect to sort queries,
     * e.g., case insensitivity.
     * <p>
     * <strong>Compatibility issues</strong>
     * This is new in 5.1.2. Customized implementations of this interface can
     * simply return null, the behavior will then be exactly the same as before.
     *
     * @return the sort option
     */
    String getSortOption();

    /**
     * Returns whether and how a field should have term vectors. If empty map
     * then no vectorization.
     *
     * @return a map from term vector prop id to term vector prop value.
     */
    Map<String, String> getTermVector();

    /**
     * Does this data needs to be handled like a binary data?
     *
     * @return true if binary / false if not.
     */
    boolean isBinary();

    /**
     * Returns a map from of properties bound to this field.
     *
     * @return a map from string to serializable.
     */
    Map<String, Serializable> getProperties();

}
