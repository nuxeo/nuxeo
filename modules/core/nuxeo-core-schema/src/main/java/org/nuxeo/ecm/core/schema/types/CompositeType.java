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

import java.util.Collection;

/**
 * A composite type is an aggregation of several schemas.
 * <p>
 * Each schema defines its own namespace to avoid field name collisions.
 */
public interface CompositeType extends ComplexType {

    /**
     * Gets the composite type schema given its name.
     *
     * @param name the schema name
     * @return the schema if any or null if none was found
     */
    Schema getSchema(String name);

    /**
     * Checks if this composite type has any schema defined.
     *
     * @return true if this composite type has some schemas defined, false otherwise
     */
    boolean hasSchemas();

    /**
     * Checks if this composite type has the given schema.
     *
     * @param name the schema name
     * @return true if the composite type has this schema, false otherwise
     */
    boolean hasSchema(String name);

    /**
     * Gets the schema names of this type.
     *
     * @return the schema names
     */
    String[] getSchemaNames();

    /**
     * Gets all the schemas (including inherited schemas) of this composite type.
     *
     * @return the composite type schemas
     */
    Collection<Schema> getSchemas();

}
