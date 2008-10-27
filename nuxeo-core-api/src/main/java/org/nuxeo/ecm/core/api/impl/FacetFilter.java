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
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;

/**
 * A filter based on facets.
 *
 * @author Georges Racinet
 * @author Florent Guillaume
 */
public class FacetFilter implements Filter {

    private static final long serialVersionUID = 666516084564501480L;

    public final List<String> required;

    public final List<String> excluded;

    /**
     * Generic constructor.
     *
     * @param required : list of facets the models must have to pass the filter
     * @param excluded : list of facets the models must not have to pass the
     *            filter
     */
    public FacetFilter(List<String> required, List<String> excluded) {
        this.required = required;
        this.excluded = excluded;
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
            required = Collections.singletonList(facet);
            excluded = null;
        } else {
            required = null;
            excluded = Collections.singletonList(facet);
        }
    }

    public boolean accept(DocumentModel docModel) {
        if (excluded != null) { // assume that excluded will be shorter
            for (String exc : excluded) {
                if (docModel.hasFacet(exc)) {
                    return false;
                }
            }
        }
        if (required != null) {
            for (String req : required) {
                if (!docModel.hasFacet(req)) {
                    return false;
                }
            }
        }
        return true;
    }

}
