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
 * $Id$
 */

package org.nuxeo.ecm.core.search.api.indexing.resources.configuration;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * Indexable resource configuration.
 *
 * <p>
 * Base interface for all indexable resource candidates.
 * </p>
 *
 * @see org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResource
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface IndexableResourceConf extends Serializable {

    /**
     * Returns the name of the indexable resource c onfiguration.
     *
     * @return the name of the indexable resource configuration.
     */
    String getName();

    /**
     * Returns the resource prefix.
     *
     * @return the resource prefix.
     */
    String getPrefix();

    /**
     * Returns the map from field name to indexable schema field configuration.
     *
     * @return the map from field name to indexable field configuration.
     */
    Map<String, IndexableResourceDataConf> getIndexableFields();

    /**
     * All schema fields should be indexed ?
     *
     * @return true if all fields should be indexed.
     */
    boolean areAllFieldsIndexable();

    /**
     * Returns the list of field that should be excluded.
     *
     * @return a list of schema field names.
     */
    Set<String> getExcludedFields();

    /**
     * Returns the indexable resource type.
     *
     * <p>
     * The type will be use for further indexing resource introspection if
     * needed.
     * </p>
     *
     * @return a string identifier.
     */
    String getType();

}
