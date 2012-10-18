/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.schema;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nuxeo.ecm.core.schema.types.CompositeTypeImpl;
import org.nuxeo.ecm.core.schema.types.Schema;

/**
 * Implementation of a document type.
 */
public class DocumentTypeImpl extends CompositeTypeImpl implements DocumentType {

    private static final long serialVersionUID = 1L;

    protected Set<String> facets;

    protected PrefetchInfo prefetchInfo;

    /**
     * Constructs a document type. Schemas and facets must include those from
     * the super type.
     */
    public DocumentTypeImpl(String name, DocumentType superType,
            List<Schema> schemas, Collection<String> facets, PrefetchInfo prefetchInfo) {
        super(superType, SchemaNames.DOCTYPES, name, schemas);
        if (facets == null) {
            this.facets = Collections.emptySet();
        } else {
            this.facets = new HashSet<String>(facets);
        }
        this.prefetchInfo = prefetchInfo;
    }

    public DocumentTypeImpl(String name) {
        this(name, null, Collections.<Schema> emptyList(),
                Collections.<String> emptySet(), null);
    }

    public void setPrefetchInfo(PrefetchInfo prefetchInfo) {
        this.prefetchInfo = prefetchInfo;
    }

    @Override
    public PrefetchInfo getPrefetchInfo() {
        return prefetchInfo;
    }

    @Override
    public boolean isFile() {
        return !facets.contains(FacetNames.FOLDERISH);
    }

    @Override
    public boolean isFolder() {
        return facets.contains(FacetNames.FOLDERISH);
    }

    @Override
    public boolean isOrdered() {
        return facets.contains(FacetNames.ORDERABLE);
    }

    @Override
    public Set<String> getFacets() {
        return facets;
    }

    @Override
    public boolean hasFacet(String facetName) {
        return facets.contains(facetName);
    }

}
