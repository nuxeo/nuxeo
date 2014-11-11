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
 * $Id: IndexableResourceModel.java 13313 2007-03-06 14:33:04Z janguenot $
 */

package org.nuxeo.ecm.core.search.api.backend.indexing.resources;

import java.io.Serializable;
import java.util.List;

import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResource;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;

/**
 * Resolved resource.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface ResolvedResource extends Serializable {

    /**
     * Returns the key value for this resource.
     *
     * @return the key value for this resource.
     */
    String getId();

    /**
     * Returns the indexable bound resource.
     *
     * @return the indexable bound resource
     */
    IndexableResource getIndexableResource();

    /**
     * Set the indexable resource for this resolved.
     *
     * @param iResource : an indexable resource.
     */
    void setIndexableResource(IndexableResource iResource);

    /**
     * Returns the list of indexable data.
     *
     * @return the list of indexable data.
     */
    List<ResolvedData> getIndexableData();

    /**
     * Adds a resolved data
     *
     * @param data : a resolved data instance.
     */
    void addIndexableData(ResolvedData data);

    /**
     * Returns an indexable data given its name.
     *
     * @param name : name of the indexable data
     * @return an ResolvedData instance if exists null if not.
     */
    ResolvedData getIndexableDataByName(String name);

    /**
     * Returns the associated configuration.
     *
     * @return an IndexableResourceConf instance.
     */
    IndexableResourceConf getConfiguration();

}
