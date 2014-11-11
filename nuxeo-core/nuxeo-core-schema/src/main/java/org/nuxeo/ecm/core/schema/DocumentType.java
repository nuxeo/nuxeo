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

import org.nuxeo.ecm.core.schema.types.CompositeType;

import java.util.Set;

/**
 * Document types are composite types made of several schemas.
 * <p>
 * Sample document types are Workspace, Section, Domain,... The list of builtin
 * document type is visible at NXCore/OSGI-INF/CoreExtensions.xml.
 */
public interface DocumentType extends CompositeType {

    /**
     * Gets the prefetch info, or null if no prefetch is defined.
     * <p>
     * If the prefetch info is not null, the caller should use it when
     * instantiating a document to preload the fields defined by the prefetch
     * info.
     * <p>
     * If no prefetch is specified by the document type, the caller is free to
     * use a default prefetch info or no prefetch at all.
     *
     * @return the prefetch info or null
     */
    PrefetchInfo getPrefetchInfo();

    /**
     * Tests whether this type describes a document (not a folder!) or not.
     *
     * @return true if the type describes a document folder, otherwise returns
     *         false
     */
    boolean isFile();

    /**
     * Tests whether this type describes a folder or not.
     *
     * @return true if the type describes a folder, otherwise returns false
     */
    boolean isFolder();

    /**
     * Tests whether this type describe an ordered folder or not.
     *
     * @return true if the type describes an ordered folder, otherwise returns
     *         false
     */
    boolean isOrdered();

    /**
     * Gets all the facets of this document type.
     * <p>
     * Facets inherited from parents are taken into account.
     *
     * @return the facets
     */
    Set<String> getFacets();

    /**
     * Returns {@code true} if this document type has the given
     * {@code facetName} facet, {@code false otherwise}.
     *
     * @since 5.7
     */
    boolean hasFacet(String facetName);

}
