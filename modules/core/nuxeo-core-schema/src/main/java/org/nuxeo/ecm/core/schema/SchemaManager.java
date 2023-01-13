/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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

import java.util.Set;
import java.util.regex.Pattern;

import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;

/**
 * The Schema Manager manages core document types, schemas, facets and field types.
 */
public interface SchemaManager extends TypeProvider, PropertyCharacteristicHandler {

    static final Pattern PATH_INDEX_PATTERN = Pattern.compile("/-?\\d+/");

    /**
     * Remove prefix if any and replace the index of complex properties of the given path.
     * <p>
     * i.e. files:files/1/file -&gt; files\/*\/file
     *
     * @param path the path
     * @return a normalize path
     * @since 2021.32
     */
    static String normalizePath(String path) {
        // remove prefix if it exists
        String ret = path.substring(path.lastIndexOf(':') + 1);
        // remove /item used in list property item
        if (ret.endsWith("/item")) {
            ret = ret.substring(0, ret.length() - 5);
        }
        if (ret.contains("/")) {
            // we're only interested in sth/index/sth because we can't add info on sth/* property
            ret = PATH_INDEX_PATTERN.matcher(ret).replaceAll("/*/");
        }
        return ret;
    }

    /**
     * Returns the field with given xpath, or null if not found.
     */
    Field getField(String xpath);

    /**
     * Returns the field with given parent field and sub name, or null if not found.
     *
     * @since 7.2
     */
    Field getField(Field field, String subFieldName);

    Schema getSchemaFromPrefix(String schemaPrefix);

    /**
     * Finds within the schemas the first matching schema having a property with the same name as the first path segment
     * of the xpath. The xpath can be prefixed or unprefixed.
     *
     * @param xpath the prefixed or unprefixed xpath
     * @param schemas the schemas to be searched
     * @return the first schema containing a property matching the xpath
     * @since 2023
     */
    String getXPathSchemaName(String xpath, Set<String> schemas);

    /**
     * @deprecated since 11.1, seems unused
     */
    @Deprecated(since = "11.1")
    Schema getSchemaFromURI(String schemaURI);

    /**
     * Returns the names of all document types that have given facet.
     *
     * @return null or the names as a guaranteed non-empty set.
     */
    Set<String> getDocumentTypeNamesForFacet(String facet);

    /**
     * Return the names of all document types extending the given one, which is included.
     *
     * @return null or the set of names.
     */
    Set<String> getDocumentTypeNamesExtending(String docType);

    int getDocumentTypesCount();

    /**
     * Returns true if {@code docType} is or extends {@code superType}, false otherwise.
     *
     * @since 5.9.4
     */
    boolean hasSuperType(String docType, String superType);

    /**
     * Returns the types of the children that can be created inside a given {@code type} type.
     *
     * @since 8.4
     */
    Set<String> getAllowedSubTypes(String type);

    /**
     * @return the deprecated properties handler
     * @since 9.2
     * @deprecated since 11.1, use {@link PropertyCharacteristicHandler} methods instead
     */
    @Deprecated(since = "11.1")
    PropertyDeprecationHandler getDeprecatedProperties();

    /**
     * @return the removed properties handler
     * @since 9.2
     * @deprecated since 11.1, use {@link PropertyCharacteristicHandler} methods instead
     */
    @Deprecated(since = "11.1")
    PropertyDeprecationHandler getRemovedProperties();

    /**
     * Whether or not to ignore any previous values when setting complex properties.
     *
     * @return {@code true} if setting a complex property ignores any previous values
     * @since 9.3
     */
    boolean getClearComplexPropertyBeforeSet();

    /**
     * Whether we allow to write the dublincore schema on a version.
     *
     * @return {@code true} if write to the dublincore schema of a version is allowed
     * @since 10.3
     */
    boolean getAllowVersionWriteForDublinCore();

}
