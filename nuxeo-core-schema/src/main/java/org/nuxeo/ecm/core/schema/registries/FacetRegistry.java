/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.core.schema.registries;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for core facets
 *
 * @since 5.6
 */
public class FacetRegistry extends ContributionFragmentRegistry<CompositeType> {

    protected final Map<String, CompositeType> facets = new HashMap<String, CompositeType>();

    @Override
    public String getContributionId(CompositeType contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, CompositeType contrib,
            CompositeType newOrigContrib) {
        facets.put(id, contrib);
    }

    @Override
    public void contributionRemoved(String id, CompositeType origContrib) {
        facets.remove(id);
    }

    @Override
    public boolean isSupportingMerge() {
        return false;
    }

    @Override
    public CompositeType clone(CompositeType orig) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void merge(CompositeType src, CompositeType dst) {
        throw new UnsupportedOperationException();
    }

    // custom API

    public CompositeType getFacet(String name) {
        return facets.get(name);
    }

    public CompositeType[] getFacets() {
        return facets.values().toArray(new CompositeType[facets.size()]);
    }

    public void clear() {
        facets.clear();
        contribs.clear();
    }
}
