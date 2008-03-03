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
 * $Id: ResolvedData.java 24603 2007-09-05 17:14:43Z gracinet $
 */

package org.nuxeo.ecm.core.search.api.backend.indexing.resources;

import java.io.Serializable;
import java.util.Map;

/**
 * Resolved data.
 *
 * <p>
 * Holds the actual data to index along with its configuration. The backend will
 * used the associated data configuration to know what to do backend side.
 * </p>
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface ResolvedData extends Serializable {

    /**
     * Returns the name of the indexable data.
     *
     * @return the name of the indexable data.
     */
    String getName();

    /**
     * Returns the analyzer to apply to this data.
     *
     * @return the analyzer to apply to this data.
     */
    String getAnalyzerName();

    /**
     * Returns the target data type.
     *
     * @return the target data type.
     */
    String getTypeName();

    /**
     * Returns the actual indexable value.
     *
     * <p>
     * Important : we <strong>do not</strong> ensure the value here as a
     * serializable Object since this API is used at backend level. It will be
     * the responsability of the backend to ensure it if the target search
     * server is located on another node.
     * </p>
     *
     * @return the actual indexable value as a Java Object
     */
    Object getValue();

    /**
     * Is this indexable data aimed at being stored ?
     *
     * @return true if stored / false if unstored.
     */
    boolean isStored();

    /**
     * Is this indexable data aimed at being indexed ?
     *
     * @return true if indexed / false if unindexed
     */
    boolean isIndexed();

    /**
     * Returns whether and how a field should have term vectors. If empty map
     * then no vectorization.
     *
     * @return a map from term vector prop id to term vector prop value.
     */
    Map<String, String> getTermVector();

    /**
     * Does this data needs to be handled like a binary data ?
     *
     * @return true if binary / false if not.
     */
    boolean isBinary();

    /**
     * Is this data multivalued ?
     *
     * @return true if multivalued / false if not.
     */
    boolean isMultiple();

    /**
     * @see org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceDataConf#isSortable()
     */
    boolean isSortable();

    /**
     * @see org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceDataConf#getSortOption()
     */
    String getSortOption();

    /**
     * @see org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceDataConf#getProperties()
     */
    Map<String, Serializable> getProperties();

}
