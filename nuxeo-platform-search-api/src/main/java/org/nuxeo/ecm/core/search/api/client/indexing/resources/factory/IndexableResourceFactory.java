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
 *     anguenot
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.api.client.indexing.resources.factory;

import java.io.Serializable;

import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResource;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResource;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;

/**
 * Generic indexable resource factory interface.
 *
 * <p>
 * API to generate indexable resource instances and resolve them given
 * configuration and target objects on which indexable resources apply.
 * </p>
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface IndexableResourceFactory extends Serializable {

    /**
     * Returns an empty indexable resource instance.
     * <p>
     * Useful for complete computations of indexable resources outside of the
     * factory.
     *
     * @return an empty indexable resource instance.
     */
    IndexableResource createEmptyIndexableResource();

    /**
     * Returns an indexable resource instance given a target object needed by
     * the resource along with its configuration.
     * <p>
     * For instance it could be a document model or log entry id or still a
     * relation.
     *
     * @param conf the bound indexable resource configuration.
     * @param sid optional Nuxeo Core session id. (XXX should be removed from
     *            the signature)
     * @param targetResource the target object on which the indexable resource
     *            applies.
     * @return an indexable resource instance.
     */
    IndexableResource createIndexableResourceFrom(Serializable targetResource,
            IndexableResourceConf conf, String sid);

    /**
     * Resolves an indexable resource instance.
     * <p>
     * Note the indexable resource instance contains the configuration and the
     * target object on which it applies.
     *
     * @param resource the indexable resource instance.
     *
     * @return a resolved indexable resource instance.
     * @throws IndexingException
     */
    ResolvedResource resolveResourceFor(IndexableResource resource)
            throws IndexingException;

    /**
     * Resolves and returns an indexable resource instance.
     *
     * @param targetResource the target object on which the indexable resource
     *            applies.
     * @param conf the indexable resource configuration
     * @param sid optional Nuxeo Core session id. (XXX should be removed from
     *            the signature)
     * @return a resolved indexable resource instance.
     * @throws IndexingException
     */
    ResolvedResource resolveResourceFor(Serializable targetResource,
            IndexableResourceConf conf, String sid) throws IndexingException;

    /**
     * Resolves and returns an indexable resource instance.
     *
     * @param targetResource the target object on which the indexable resource
     *            applies.
     * @param conf the indexable resource configuration
     * @param sid optional Nuxeo Core session id. (XXX should be removed from
     *            the signature)
     * @return a resolved indexable resource instance.
     * @throws IndexingException
     */
    ResolvedResource createResolvedResourceFor(Serializable targetResource,
            IndexableResourceConf conf, String sid) throws IndexingException;

    /**
     * Resolves an indexable resources and returns an aggregated resolved
     * resources instances.
     * <p>
     * The idea here is to simplify the generations of aggregated resources when
     * only one indexable resource is involved.
     *
     * @param resource an indexable resource instance.
     * @return a resolved indexable resource instance.
     */
    ResolvedResources resolveResourcesFor(IndexableResource resource);

    /**
     * Resolves an indexable resources and returns an aggregated resolved
     * resources instances.
     * <p>
     * The idea here is to simplify the generations of aggregated resources when
     * only one indexable resource is involved.
     *
     * @param targetResource the target object on which the indexable resource
     *            applies.
     * @param conf the indexable resource configuration
     * @param sid optional Nuxeo Core session id. (XXX should be removed from
     *            the signature)
     * @return a resolved indexable resource instance.
     * @throws IndexingException
     */
    ResolvedResources createResolvedResourcesFor(Serializable targetResource,
            IndexableResourceConf conf, String sid) throws IndexingException;

}
