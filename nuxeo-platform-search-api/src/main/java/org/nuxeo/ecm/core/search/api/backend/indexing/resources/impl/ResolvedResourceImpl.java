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
 * $Id: ResolvedResourceImpl.java 28989 2008-01-12 23:08:51Z sfermigier $
 */

package org.nuxeo.ecm.core.search.api.backend.indexing.resources.impl;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedData;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResource;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResource;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;

/**
 * Resolved resource impl.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class ResolvedResourceImpl implements ResolvedResource {

    private static final long serialVersionUID = -8173983658947817695L;

    protected String id;

    protected IndexableResource resourceProxy;

    protected List<ResolvedData> data;

    public ResolvedResourceImpl() {
    }

    public ResolvedResourceImpl(String id) {
        this.id = id;
    }

    public ResolvedResourceImpl(String id, IndexableResource resourceProxy,
            List<ResolvedData> data) {
        this.id = id;
        this.resourceProxy = resourceProxy;
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public List<ResolvedData> getIndexableData() {
        return data;
    }

    public IndexableResource getIndexableResource() {
        return resourceProxy;
    }

    public ResolvedData getIndexableDataByName(String name) {

        ResolvedData res = null;

        // No data
        if (data != null) {
            // :XXX: optimize
            for (ResolvedData one : data) {
                if (one.getName().equals(name)) {
                    res = one;
                    break;
                }
            }
        }
        return res;
    }

    public IndexableResourceConf getConfiguration() {
        IndexableResourceConf conf = null;
        if (resourceProxy != null) {
            conf = resourceProxy.getConfiguration();
        }
        return conf;
    }

    public void addIndexableData(ResolvedData one) {
        if (data == null) {
            data = new ArrayList<ResolvedData>();
        }
        data.add(one);
    }

    public void setIndexableResource(IndexableResource resource) {
        resourceProxy = resource;
    }

    @Override
    public String toString() {
        String className = getClass().getSimpleName();
        if (resourceProxy == null) {
            return String.format("%s (uninitialized)", className);
        }
        return String.format("%s (for '%s')", getClass().getSimpleName(),
                getConfiguration().getName());
    }

}
