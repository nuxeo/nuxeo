/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.types;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.service.TimestampedService;

/**
 * Service handling registered UI Types.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public interface TypeManager extends TimestampedService {

    /**
     * Gets the super type names for the given type.
     *
     * @return an array of supertypes or an empty array if no supertype exists. null is returned if no such type exists
     */
    String[] getSuperTypes(String typeName);

    /**
     * Returns all the registered {@code Type}s.
     */
    Collection<Type> getTypes();

    /**
     * Returns the {@code Type} instance for the given {@code typeName}.
     */
    Type getType(String typeName);

    /**
     * Returns {@code true} if {@code typeName} is a registered Type, {@code false} otherwise.
     */
    boolean hasType(String typeName);

    Collection<Type> getAllowedSubTypes(String typeName);

    /**
     * Returns the allowed sub types of the given {@code typeName}, filtered by a local UI types configuration retrieved
     * from the {@code currentDoc}, if any.
     *
     * @since 5.4.2
     */
    Collection<Type> getAllowedSubTypes(String typeName, DocumentModel currentDoc);

    /**
     * Returns recursively all the allowed sub types from the given {@code typeName}.
     *
     * @since 5.4.2
     */
    Collection<Type> findAllAllowedSubTypesFrom(String typeName);

    /**
     * Returns recursively all the allowed sub types from the given {@code typeName}, filtered by a local UI types
     * configuration retrieved from the {@code currentDoc}, if any.
     *
     * @since 5.4.2
     */
    Collection<Type> findAllAllowedSubTypesFrom(String typeName, DocumentModel currentDoc);

    /**
     * Returns the sub type of the given {@code typeName}, filtered by a local UI types configuration retrieved from the
     * {@code currentDoc}, if any, and organized by type categories.
     *
     * @since 5.4.2
     */
    Map<String, List<Type>> getTypeMapForDocumentType(String typeName, DocumentModel currentDoc);

    /**
     * Returns {@code true} if {@code typeName} is a sub type, allowed in creation mode, of {@code containerTypeName},
     * {@code false} otherwise.
     *
     * @since 5.4.2
     */
    boolean canCreate(String typeName, String containerTypeName);

    /**
     * Returns {@code true} if {@code typeName} is a sub type, allowed in creation, of {@code containerTypeName},
     * {@code false} otherwise.
     * <p>
     * It takes care of a local UI types configuration retrieved from the {@code currentDoc} to filter the sub types of
     * {@code typeName} before checking the creation mode.
     *
     * @since 5.4.2
     */
    boolean canCreate(String typeName, String containerTypeName, DocumentModel currentDoc);

    /**
     * Returns {@code true} if {@code typeName} is an allowed sub type of {@code containerTypeName}, {@code false}
     * otherwise.
     *
     * @since 5.4.2
     */
    boolean isAllowedSubType(String typeName, String containerTypeName);

    /**
     * Returns {@code true} if {@code typeName} is an allowed sub type of {@code containerTypeName}, filtered by a local
     * UI types configuration retrieved from the {@code currentDoc}, if any, {@code false} otherwise.
     *
     * @since 5.4.2
     */
    boolean isAllowedSubType(String typeName, String containerTypeName, DocumentModel currentDoc);

}
