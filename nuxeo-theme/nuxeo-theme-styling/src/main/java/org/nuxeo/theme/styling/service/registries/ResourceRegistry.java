/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.theme.styling.service.registries;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.runtime.model.ContributionFragmentRegistry;
import org.nuxeo.theme.resources.ResourceType;

/**
 * Registry for resource elements. Does not handle merge.
 *
 * @since 5.5
 */
public class ResourceRegistry extends
        ContributionFragmentRegistry<ResourceType> {

    protected Map<String, ResourceType> resources = new HashMap<String, ResourceType>();

    @Override
    public String getContributionId(ResourceType contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, ResourceType contrib,
            ResourceType newOrigContrib) {
        resources.put(id, contrib);
    }

    @Override
    public void contributionRemoved(String id, ResourceType origContrib) {
        resources.remove(id);
    }

    @Override
    public boolean isSupportingMerge() {
        return false;
    }

    @Override
    public ResourceType clone(ResourceType orig) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void merge(ResourceType src, ResourceType dst) {
        throw new UnsupportedOperationException();
    }

    public ResourceType getResource(String id) {
        return resources.get(id);
    }
}
