/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */

package org.nuxeo.ecm.core.api.tree;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Default filter for tree elements.
 * <p>
 * Filters using facets and types criteria. Also filters documents that are in the trash.
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
        // exclude trashed documents from tree
        if (document.isTrashed()) {
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
