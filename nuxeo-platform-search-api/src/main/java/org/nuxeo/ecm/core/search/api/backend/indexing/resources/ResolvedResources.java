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
 * $Id: ResolvedResources.java 19476 2007-05-27 10:35:17Z sfermigier $
 */

package org.nuxeo.ecm.core.search.api.backend.indexing.resources;

import java.io.Serializable;
import java.util.List;

import org.nuxeo.ecm.core.api.security.ACP;

/**
 * Resolved resources are for one step related resources indexing.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface ResolvedResources extends Serializable {

    /**
     * Returns a common key for all the resources. The common key is intended to
     * be used for join type queries.
     *
     * @return a common key for all the resources.
     */
    String getId();

    /**
     * Returns the list of resolved resources.
     *
     * @return the list of resolved resources.
     */
    List<ResolvedResource> getIndexableResolvedResources();

    /**
     * Returns the list of all indexable data from all resolved resources.
     *
     * @return the list of all indexable data from all resolved resources.
     */
    List<ResolvedData> getMergedIndexableData();

    /**
     * Returns the indexable data that are common to all the indexable
     * resources.
     *
     * @return a list if indexable data.
     */
    List<ResolvedData> getCommonIndexableData();

    /**
     * Returns the ACP to apply on the whole resources.
     *
     * <p>
     * Will be useful for the backend to compute its security index.
     * </p>
     *
     * @return an Nuxeo core ACP object.
     */
    ACP getACP();

    /**
     * Returns an indexable resolved resource given its configuration name.
     *
     * @param name : the name of the resource.
     * @return an ResolvedResource instance if exists null if not.
     */
    ResolvedResource getIndexableResolvedResourceByConfName(String name);

    /**
     * Returns an indexable data given its name.
     *
     * @param name : the name of the indexable data
     * @param resourceName : the name of the resource which is supposed to hold
     *            the indexable data
     * @return an ResolvedData instance if exists null if not.
     */
    ResolvedData getIndexableDataByName(String resourceName, String name);

}
