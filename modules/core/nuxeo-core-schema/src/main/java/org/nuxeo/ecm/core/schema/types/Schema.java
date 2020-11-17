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
package org.nuxeo.ecm.core.schema.types;

/**
 * A marker interface for schemas.
 * <p>
 * A schema is a complex type that can be used used to create composite types - such as document types.
 * <p>
 * Schemas have no super types and must not be used as field types.
 */
public interface Schema extends ComplexType {

    /**
     * Gets the types declared by this schema.
     */
    Type[] getTypes();

    /**
     * Gets a schema local type given its name.
     *
     * @return the type or null if no such type
     */
    Type getType(String typeName);

    /**
     * Registers a new type in that schema context.
     */
    void registerType(Type type);

    /**
     * @return true if the schema's fields are writable even for Version document.
     * @since 8.4
     */
    default boolean isVersionWritabe() {
        return false;
    }

}
