/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.schema;

import org.nuxeo.ecm.core.schema.types.CompositeType;

import java.util.Set;

/**
 * Document types are composite types made of several schemas.
 * <p>
 * Sample document types are Workspace, Section, Domain,... The list of
 * builtin document type is visible at NXCore/OSGI-INF/CoreExtensions.xml.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface DocumentType extends CompositeType {

    /**
     * Sets the prefetch info.
     * <p>
     * The prefetch info describes which fields (or entire schemas)
     * should be prefetched when instantiating a document.
     * <p>
     * This is primarily intended to optimize document initialization time.
     * <p>
     * The prefetch info is a string array of length multiple of 2 containing a sequence of pairs
     * composed by the schema name and the field name. If the field name is null then the entire schema
     * should be prefetched. Example: <code>"common", null, "dublincore", "title"</code>
     *
     * @param prefetchInfo
     */
    void setPrefetchInfo(PrefetchInfo prefetchInfo);

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
     *
     * @param facets
     */
    void setDeclaredFacets(String[] facets);

    /**
     * Adds specified schemas to the document type.
     *
     * @param schemas
     */
    void addSchemas(String[] schemas);

    @Override
    TypeRef<DocumentType> getRef();

    /**
     * Sets the names of the allowed children doc types.
     * <p>
     * Children types are document types allowed for the children of
     * a document of the current type.
     * <p>
     * Type names may include '*' for all types.
     *
     * @param subTypes null if no children types have been defined
     * (i.e. this type cannot have children), else an array with children types
     */
    // TODO: exclusion filters
    void setChildrenTypes(String[] subTypes);

    /**
     * Gets the type names that can be used for children docs.
     * <p>
     * Returned types may include special group of types like '*'.
     *
     * @return
     */
    String[] getChildrenTypes();

    /**
     * Gets the resolved children doc types.
     * <p>
     * Special group of types like '*' and exclusion filters if any are applied and the
     * set of actual children types is computed and resolved into real doc types.
     *
     * @return null if no children types was defined
     * (i.e. this type cannot have children) or an array with children types
     */
    DocumentType[] getResolvedChildrenTypes();

    boolean isChildTypeAllowed(String name);

    /**
     * Returns {@code true} if this document type has the given
     * {@code facetName} facet, {@code false otherwise}.
     *
     * @since 5.7
     */
    boolean hasFacet(String facetName);

}
