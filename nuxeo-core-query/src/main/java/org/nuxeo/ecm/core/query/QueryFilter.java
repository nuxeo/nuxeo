/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.query;

import java.util.Collection;
import java.util.Collections;

import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;

/**
 * Filtering parameters that can be passed when executing a
 * {@link FilterableQuery}.
 * <p>
 * This includes filtering on the BROWSE permission for the given principal,
 * filtering on facets, and applying query transformers.
 * <p>
 * You can also include a limit and offset, to get a subset of the total.
 *
 * @author Florent Guillaume
 */
public class QueryFilter {

    public static final QueryFilter EMPTY = new QueryFilter(new String[0],
            new String[0], null, Collections.<SQLQuery.Transformer> emptyList());

    protected final String[] principals;

    protected final String[] permissions;

    protected final FacetFilter facetFilter;

    protected final Collection<SQLQuery.Transformer> queryTransformers;

    public QueryFilter(String[] principals, String[] permissions,
            FacetFilter facetFilter,
            Collection<SQLQuery.Transformer> queryTransformers) {
        this.principals = principals;
        this.permissions = permissions;
        this.facetFilter = facetFilter;
        this.queryTransformers = queryTransformers;
    }

    public String[] getPrincipals() {
        return principals;
    }

    public String[] getPermissions() {
        return permissions;
    }

    public FacetFilter getFacetFilter() {
        return facetFilter;
    }

    public Collection<SQLQuery.Transformer> getQueryTransformers() {
        return queryTransformers;
    }

}
