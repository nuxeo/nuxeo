/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.tree;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.tree.DocumentTreeFilter;

/**
 * Default filter for tree elements
 * <p>
 * Filters using facets and types criteria. Also filters documents that are in
 * the "deleted" life cycle state.
 *
 * @author Anahide Tchertchian
 */
public class DefaultDocumentTreeFilter implements DocumentTreeFilter {

    private static final long serialVersionUID = 1L;

    protected List<String> includedFacets;

    protected List<String> excludedFacets;

    protected List<String> excludedTypes;

    public boolean accept(DocumentModel document) {
        String docType = document.getType();
        if (excludedTypes.contains(docType)) {
            return false;
        }
        // exclude deleted documents from tree
        try {
            // FIXME: avoid harcoded reference
            if ("deleted".equals(document.getCurrentLifeCycleState())) {
                return false;
            }
        } catch (ClientException e) {
            return false;
        }
        // XXX AT: this could have not been copied from FacetFilter if fields
        // were not private there.
        if (excludedFacets != null) {
            for (String exc : excludedFacets) {
                if (document.hasFacet(exc)) {
                    return false;
                }
            }
        }
        if (includedFacets != null) {
            for (String req : includedFacets) {
                if (!document.hasFacet(req)) {
                    return false;
                }
            }
        }
        return true;
    }

    public List<String> getIncludedFacets() {
        return includedFacets;
    }

    public void setIncludedFacets(List<String> includedFacets) {
        this.includedFacets = includedFacets;
    }

    public List<String> getExcludedFacets() {
        return excludedFacets;
    }

    public void setExcludedFacets(List<String> excludedFacets) {
        this.excludedFacets = excludedFacets;
    }

    public List<String> getExcludedTypes() {
        return excludedTypes;
    }

    public void setExcludedTypes(List<String> excludedTypes) {
        this.excludedTypes = excludedTypes;
    }

}
