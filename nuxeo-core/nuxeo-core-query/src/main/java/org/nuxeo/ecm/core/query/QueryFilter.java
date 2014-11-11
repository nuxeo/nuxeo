/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.query;

import java.io.Serializable;
import java.security.Principal;
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
public class QueryFilter implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final QueryFilter EMPTY = new QueryFilter(null, null,
            new String[0], null,
            Collections.<SQLQuery.Transformer> emptyList(), 0, 0);

    /** The principal. Note that this MUST be {@link Serializable}. */
    protected final Principal principal;

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
    public QueryFilter(Principal principal, String[] principals,
            String[] permissions, FacetFilter facetFilter,
            Collection<SQLQuery.Transformer> queryTransformers, long limit,
            long offset) {
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

    public Principal getPrincipal() {
        return principal;
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

    public long getLimit() {
        return limit;
    }

    public long getOffset() {
        return offset;
    }

}
