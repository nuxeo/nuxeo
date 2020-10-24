/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.query;

import java.util.Collection;
import java.util.Collections;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;

/**
 * Filtering parameters that can be passed when executing a query.
 * <p>
 * This includes filtering on the BROWSE permission for the given principal, filtering on facets, and applying query
 * transformers.
 * <p>
 * You can also include a limit and offset, to get a subset of the total.
 *
 * @author Florent Guillaume
 */
public class QueryFilter implements org.nuxeo.ecm.core.api.query.QueryFilter<SQLQuery.Transformer> {

    public static final QueryFilter EMPTY = new QueryFilter(null, null, new String[0], null,
            Collections.<SQLQuery.Transformer> emptyList(), 0, 0);

    protected final NuxeoPrincipal principal;

    protected final String[] principals;

    protected final String[] permissions;

    protected final FacetFilter facetFilter;

    protected final Collection<SQLQuery.Transformer> queryTransformers;

    protected final long limit;

    protected final long offset;

    /**
     * Constructs a query filter.
     * <p>
     * Note that the principal MUST be {@link Serializable}.
     */
    public QueryFilter(NuxeoPrincipal principal, String[] principals, String[] permissions, FacetFilter facetFilter,
            Collection<SQLQuery.Transformer> queryTransformers, long limit, long offset) {
        this.principal = principal;
        this.principals = principals;
        this.permissions = permissions;
        this.facetFilter = facetFilter;
        this.queryTransformers = queryTransformers;
        this.limit = limit;
        this.offset = offset;
    }

    public static QueryFilter withoutLimitOffset(QueryFilter other) {
        return new QueryFilter( //
                other.principal, //
                other.principals, //
                other.permissions, //
                other.facetFilter, //
                other.queryTransformers, //
                0, 0);
    }

    @Override
    public NuxeoPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public String[] getPrincipals() {
        return principals;
    }

    @Override
    public String[] getPermissions() {
        return permissions;
    }

    @Override
    public FacetFilter getFacetFilter() {
        return facetFilter;
    }

    @Override
    public Collection<SQLQuery.Transformer> getQueryTransformers() {
        return queryTransformers;
    }

    @Override
    public long getLimit() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public String toString() {
        return String.format("QueryFilter(principal=%s, limit=%d, offset=%d)", principal, limit, offset);
    }
}
