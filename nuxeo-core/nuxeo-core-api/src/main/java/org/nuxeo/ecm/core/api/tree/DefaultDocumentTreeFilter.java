/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.nuxeo.ecm.core.api.LifeCycleConstants;

/**
 * Default filter for tree elements.
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

    @Override
    public boolean accept(DocumentModel document) {
        String docType = document.getType();
        if (excludedTypes != null && excludedTypes.contains(docType)) {
            return false;
        }
        // exclude deleted documents from tree
        try {
            if (LifeCycleConstants.DELETED_STATE.equals(document.getCurrentLifeCycleState())) {
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

    @Override
    public List<String> getIncludedFacets() {
        return includedFacets;
    }

    @Override
    public void setIncludedFacets(List<String> includedFacets) {
        this.includedFacets = includedFacets;
    }

    @Override
    public List<String> getExcludedFacets() {
        return excludedFacets;
    }

    @Override
    public void setExcludedFacets(List<String> excludedFacets) {
        this.excludedFacets = excludedFacets;
    }

    @Override
    public List<String> getExcludedTypes() {
        return excludedTypes;
    }

    @Override
    public void setExcludedTypes(List<String> excludedTypes) {
        this.excludedTypes = excludedTypes;
    }

}
