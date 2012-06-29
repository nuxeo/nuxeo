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

import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Registry for core facets
 *
 * @since 5.6
 */
public class FacetRegistry extends SimpleContributionRegistry<CompositeType> {

    @Override
    public String getContributionId(CompositeType contrib) {
        return contrib.getName();
    }

    // custom API

    public CompositeType getFacet(String name) {
        return getCurrentContribution(name);
    }

    public CompositeType[] getFacets() {
        return currentContribs.values().toArray(
                new CompositeType[currentContribs.size()]);
    }

    public void clear() {
        currentContribs.clear();
        contribs.clear();
    }
}
