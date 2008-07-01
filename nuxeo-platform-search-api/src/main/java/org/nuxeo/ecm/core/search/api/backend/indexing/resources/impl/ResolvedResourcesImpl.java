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
 * $Id: ResolvedResourcesImpl.java 22088 2007-07-06 10:38:42Z janguenot $
 */

package org.nuxeo.ecm.core.search.api.backend.indexing.resources.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedData;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResource;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;

/**
 * Resolved resources.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class ResolvedResourcesImpl implements ResolvedResources {

    private static final long serialVersionUID = 6245974901650687007L;

    protected String id;

    protected final Map<String, ResolvedResource> resolvedResources = new HashMap<String, ResolvedResource>();

    protected final List<ResolvedData> commonData = new ArrayList<ResolvedData>();

    protected final List<ResolvedData> mergedData = new ArrayList<ResolvedData>();

    protected ACP acp;

    public ResolvedResourcesImpl() {
    }

    public ResolvedResourcesImpl(String id) {
        this.id = id;
    }

    public ResolvedResourcesImpl(String id,
            List<ResolvedResource> resolvedResources,
            List<ResolvedData> commonData, ACP acp) {
        this.id = id;

        if (resolvedResources != null) {
            for (ResolvedResource res : resolvedResources) {
                String name = res.getConfiguration().getName();
                this.resolvedResources.put(name, res);
            }
        }

        if (commonData != null) {
            this.commonData.addAll(commonData);
        }

        this.acp = acp;
    }

    public String getId() {
        return id;
    }

    public List<ResolvedResource> getIndexableResolvedResources() {
        return new ArrayList<ResolvedResource>(resolvedResources.values());
    }

    public List<ResolvedData> getMergedIndexableData() {

        // No resolved resources => no indexable data
        if (resolvedResources.isEmpty()) {
            return mergedData;
        }

        // We just add the result in a member of this instance so that the
        // computation will be done only once.
        if (mergedData.isEmpty()) {
            for (ResolvedResource resource : resolvedResources.values()) {
                List<ResolvedData> one = resource.getIndexableData();
                if (one != null) {
                    mergedData.addAll(one);
                }
            }
            // Add commons
            if (commonData != null) {
                mergedData.addAll(commonData);
            }
        }
        return mergedData;
    }

    public ACP getACP() {
        return acp;
    }

    public ResolvedData getIndexableDataByName(String resourceName, String name) {
        ResolvedData data = null;
        ResolvedResource resource = getIndexableResolvedResourceByConfName(resourceName);
        if (resource != null) {
            data = resource.getIndexableDataByName(name);
        }
        return data;
    }

    public ResolvedResource getIndexableResolvedResourceByConfName(String name) {
        // No resources
        if (resolvedResources.isEmpty()) {
            return null;
        }
        return resolvedResources.get(name);
    }

    public List<ResolvedData> getCommonIndexableData() {
        return commonData;
    }

}
