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
 * $Id: IndexableResource.java 22566 2007-07-15 22:37:42Z gracinet $
 */

package org.nuxeo.ecm.core.search.api.client.indexing.resources;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;

/**
 * Base interface for indexable resources.
 * <p>
 * An indexable resource is a data source where to get data to index. For
 * instance, a Nuxeo core document is an example of resource. SQL, JPA,
 * Hibernate resources are as candidates.
 * <p>
 * An indexable resource contains a set of indexable data.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface IndexableResource extends Serializable {

    /**
     * Returns the resource configuration name.
     *
     * @return the resource name.
     */
    String getName();

    /**
     * Computes the resource unique id. This can be costly, since
     * it may involve fetching data, computing hash codes, etc.
     *
     * @return the resource id
     */
    String computeId();

    /**
     * Returns this instance bound configuration.
     *
     * @return an IndexableResourceConf instance.
     */
    IndexableResourceConf getConfiguration();

    /**
     * Returns the value for a given indexable data name.
     *
     * @param indexableDataName the actual indexable data name resource side.
     *
     * @return a serializable object holding the value.
     * @throws IndexingException TODO
     */
    Serializable getValueFor(String indexableDataName)
            throws IndexingException;

    /**
     * Computes an {@link ACP} for the given resource.
     * <p>
     * The returned value applies to all the resources that may be
     * associated with the present one in an {@link IndexableResources}.
     * Confidence in the validity of said value must be as strong as it gets.
     * The counterpart is that the caller must interpret <code>null</code>
     * return values as a lack of info from <strong>this</resource> and
     * perform other computations.
     *
     * @return the ACP
     */
    ACP computeAcp();

}
