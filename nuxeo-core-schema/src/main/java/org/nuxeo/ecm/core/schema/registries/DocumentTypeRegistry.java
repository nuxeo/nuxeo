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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for core document types
 *
 * @since 5.6
 */
public class DocumentTypeRegistry extends
        ContributionFragmentRegistry<DocumentType> {

    protected final Map<String, DocumentType> types = new HashMap<String, DocumentType>();

    protected final Map<String, Set<String>> inheritanceCache = new HashMap<String, Set<String>>();

    /** Facet -> docTypes having this facet. */
    protected Map<String, Set<String>> facetsCache;

    @Override
    public String getContributionId(DocumentType contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, DocumentType contrib,
            DocumentType newOrigContrib) {
        types.put(id, contrib);
        facetsCache = null;
    }

    @Override
    public void contributionRemoved(String id, DocumentType docType) {
        types.remove(id);
        if (docType != null) {
            removeFromFacetsCache(docType);
            removeFromInheritanceCache(docType);
        }
    }

    private void removeFromFacetsCache(DocumentType docType) {
        if (facetsCache == null) {
            return;
        }
        String name = docType.getName();
        for (String facet : docType.getFacets()) {
            Set<String> types = facetsCache.get(facet);
            types.remove(name);
            if (types.isEmpty()) {
                facetsCache.remove(facet); // Consistency
            }
        }
    }

    private void removeFromInheritanceCache(DocumentType docType) {
        String name = docType.getName();
        for (String type : inheritanceCache.keySet()) {
            Set<String> types = inheritanceCache.get(type);
            types.remove(name);
        }
        // The only case where an entry becomes empty.
        inheritanceCache.remove(name);
    }

    @Override
    public boolean isSupportingMerge() {
        return false;
    }

    @Override
    public DocumentType clone(DocumentType orig) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void merge(DocumentType src, DocumentType dst) {
        throw new UnsupportedOperationException();
    }

    // custom API

    public DocumentType getType(String name) {
        return types.get(name);
    }

    public DocumentType[] getDocumentTypes() {
        return types.values().toArray(new DocumentType[types.size()]);
    }

    public int size() {
        return types.size();
    }

    /**
     * Implementation details: there is a cache on each server for this.
     * <p>
     * Assumes that types never change in the lifespan of this server process
     * and that the Core server has finished loading its types.
     */
    public Set<String> getDocumentTypeNamesForFacet(String facet) {
        if (facetsCache == null) {
            initFacetsCache();
        }
        return facetsCache.get(facet);
    }

    protected void initFacetsCache() {
        if (facetsCache != null) {
            // another thread just did it
            return;
        }
        synchronized (this) {
            facetsCache = new HashMap<String, Set<String>>();
            for (DocumentType dt : getDocumentTypes()) {
                for (String facet : dt.getFacets()) {
                    Set<String> dts = facetsCache.get(facet);
                    if (dts == null) {
                        dts = new HashSet<String>();
                        facetsCache.put(facet, dts);
                    }
                    dts.add(dt.getName());
                }
            }
        }
    }

    /**
     * Implementation details: there is a cache on each server for this.
     * <p>
     * Assumes that types never change in the lifespan of this server process
     * and that the Core server has finished loading its types.
     */
    public Set<String> getDocumentTypeNamesExtending(String docTypeName) {
        Set<String> res = inheritanceCache.get(docTypeName);
        if (res != null) {
            return res;
        }
        synchronized (inheritanceCache) {
            // recheck in case another thread just did it
            res = inheritanceCache.get(docTypeName);
            if (res != null) {
                return res;
            }

            if (getType(docTypeName) == null) {
                return null;
            }
            res = new HashSet<String>();
            res.add(docTypeName);
            for (DocumentType dt : getDocumentTypes()) {
                Type parent = dt.getSuperType();
                if (parent == null) {
                    // Must be the root document
                    continue;
                }
                if (docTypeName.equals(parent.getName())) {
                    res.addAll(getDocumentTypeNamesExtending(dt.getName()));
                }
            }
            inheritanceCache.put(docTypeName, res);
            return res;
        }
    }

    public void clear() {
        types.clear();
        if (facetsCache != null) {
            facetsCache.clear();
        }
        contribs.clear();
    }
}
