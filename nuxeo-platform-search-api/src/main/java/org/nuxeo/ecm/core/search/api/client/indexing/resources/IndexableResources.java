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

package org.nuxeo.ecm.core.search.api.client.indexing.resources;

import java.io.Serializable;
import java.util.List;

/**
 * Indexable resources.
 * <p>
 * Aggregates a set of indexable resources with a shared key.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface IndexableResources extends Serializable {

    /**
     * Returns a shared identifier for all resources.
     *
     * @return a shared identifier for all resources.
     */
    String getId();

    /**
     * Returns a set of indexable resources.
     *
     * @return an array of indexable resource instances.
     */
    List<IndexableResource> getIndexableResources();

}
