/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.schema;

import org.nuxeo.ecm.core.schema.types.CompositeType;

import java.util.Collection;
import java.util.Set;

/**
 * Document types are composite types made of several schemas.
 * <p>
 * Sample document types are Workspace, Section, Domain,... The list of builtin document type is visible at
 * NXCore/OSGI-INF/CoreExtensions.xml.
 */
public interface DocumentType extends CompositeType {

    /**
     * Gets the prefetch info, or null if no prefetch is defined.
     * <p>
     * If the prefetch info is not null, the caller should use it when instantiating a document to preload the fields
     * defined by the prefetch info.
     * <p>
     * If no prefetch is specified by the document type, the caller is free to use a default prefetch info or no
     * prefetch at all.
     *
     * @return the prefetch info or null
     */
    PrefetchInfo getPrefetchInfo();

    /**
     * Tests whether this type describes a document (not a folder!) or not.
     *
     * @return true if the type describes a document folder, otherwise returns false
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
     * @return true if the type describes an ordered folder, otherwise returns false
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
     * Returns {@code true} if this document type has the given {@code facetName} facet, {@code false otherwise}.
     *
     * @since 5.7
     */
    boolean hasFacet(String facetName);

    /**
     * Returns the types of the children that can be created inside this type.
     *
     * @since 8.4
     */
    Set<String> getSubtypes();

    /**
     * Sets the types of the children that can be created inside the this type.
     *
     * @since 8.4
     */
    void setSubtypes(Collection<String> subtypes);

    /**
     * Returns {@code true} if the given {@code subtype} type was explicitly allowed to be created inside this type.
     *
     * @since 8.4
     */
    boolean hasSubtype(String subtype);

    /**
     * Returns the types of the children that cannot be created inside this type.
     *
     * @since 8.4
     */
    Set<String> getForbiddenSubtypes();

    /**
     * Sets the types of the children that cannot be created inside the this type.
     *
     * @since 8.4
     */
    void setForbiddenSubtypes(Collection<String> subtypes);

    /**
     * Returns {@code true} if the given {@code subtype} type was forbidden from being created inside this type.
     *
     * @since 8.4
     */
    boolean hasForbiddenSubtype(String subtype);

    /**
     * Returns the list of types that can effectively be created inside this type.
     * Allowed types results from the exclusion of the forbidden subtypes from the subtypes.
     *
     * @since 8.4
     */
    Set<String> getAllowedSubtypes();

    /**
     * Returns {@code true} if the given {@code subtype} type can effectively be created inside this type.
     *
     * @since 8.4
     */
    boolean hasAllowedSubtype(String subtype);

}
