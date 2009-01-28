/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Georges Racinet
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.api.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Collection;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;

/**
 * A filter based on facets.
 *
 * @author Georges Racinet
 * @author Florent Guillaume
 */
public class FacetFilter implements Filter {

    private static final long serialVersionUID = 1L;

    public static final FacetFilter ALLOW = new FacetFilter((List<String>) null,
            (List<String>) null);

    /** Set of required facets. Never {@code null}. */
    public final Set<String> required;

    /** Set of excluded facets. Never {@code null}. */
    public final Set<String> excluded;

    public final Boolean shortcut;

    /**
     * Generic constructor.
     *
     * @param required : list of facets the models must have to pass the filter
     * @param excluded : list of facets the models must not have to pass the
     *            filter
     */
    public FacetFilter(List<String> required, List<String> excluded) {
        if (required == null) {
            this.required = Collections.emptySet();
        } else {
            this.required = new HashSet<String>(required);
        }
        if (excluded == null) {
            this.excluded = Collections.emptySet();
        } else {
            this.excluded = new HashSet<String>(excluded);
        }
        shortcut = findShortcut();
    }

    /**
     * Simpler constructor to filter on a single facet.
     *
     * @param facet the facet to filter on
     * @param isRequired if true, accepted models must have the facet; if false,
     *            accepted models must not have the facet
     */
    public FacetFilter(String facet, boolean isRequired) {
        if (isRequired) {
            required = Collections.singleton(facet);
            excluded = Collections.emptySet();
        } else {
            required = Collections.emptySet();
            excluded = Collections.singleton(facet);
        }
        shortcut = null;
    }

    /**
     * Constructor that ANDs two filters.
     *
     * @param filter1 the first filter
     * @param filter2 the second filter
     */
    public FacetFilter(FacetFilter filter1, FacetFilter filter2) {
        if (filter1.required.isEmpty() && filter2.required.isEmpty()) {
            required = Collections.emptySet();
        } else {
            required = new HashSet<String>(filter1.required);
            required.addAll(filter2.required);
        }
        if (filter1.excluded.isEmpty() && filter2.excluded.isEmpty()) {
            excluded = Collections.emptySet();
        } else {
            excluded = new HashSet<String>(filter1.excluded);
            excluded.addAll(filter2.excluded);
        }
        shortcut = findShortcut();
    }

    protected Boolean findShortcut() {
        if (required.isEmpty() && excluded.isEmpty()) {
            // no condition, always matches
            return Boolean.TRUE;
        }
        Collection<String> intersection = new HashSet<String>(required);
        intersection.retainAll(excluded);
        if (!intersection.isEmpty()) {
            // non-empty intersection, filter can never match
            return Boolean.FALSE;
        }
        return null;
    }

    public boolean accept(DocumentModel docModel) {
        if (shortcut != null) {
            return shortcut;
        }
        for (String exc : excluded) {
            if (docModel.hasFacet(exc)) {
                return false;
            }
        }
        for (String req : required) {
            if (!docModel.hasFacet(req)) {
                return false;
            }
        }
        return true;
    }

}
