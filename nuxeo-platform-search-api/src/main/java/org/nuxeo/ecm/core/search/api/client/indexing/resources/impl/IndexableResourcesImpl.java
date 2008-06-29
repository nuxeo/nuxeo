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
 * $Id: IndexableResourcesImpl.java 28515 2008-01-06 20:37:29Z sfermigier $
 */

package org.nuxeo.ecm.core.search.api.client.indexing.resources.impl;

import java.util.List;

import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResource;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResources;

/**
 * Indexable resources impl.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class IndexableResourcesImpl implements
        IndexableResources {

    private static final long serialVersionUID = 3101060507626423127L;

    protected String id;

    protected List<IndexableResource> indexableResources;

    public IndexableResourcesImpl() {
    }

    public IndexableResourcesImpl(String id,
            List<IndexableResource> indexableResources) {
        this.id = id;
        this.indexableResources = indexableResources;
    }

    public String getId() {
        return id;
    }

    public List<IndexableResource> getIndexableResources() {
        return indexableResources;
    }

}
