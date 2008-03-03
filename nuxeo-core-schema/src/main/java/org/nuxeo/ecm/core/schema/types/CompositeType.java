/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.schema.types;

import java.util.Collection;

import org.nuxeo.ecm.core.schema.TypeRef;

/**
 * A composite type is an aggregation of several schemas.
 * <p>
 * Each schema defines its own namespace to avoid
 * field name collisions.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface CompositeType extends ComplexType {

    /**
     * Adds a schema.
     *
     * @param schema the schema name to add
     */
    void addSchema(String schema);

    /**
     * Adds a schema.
     *
     * @param schema the schema to add
     */
    void addSchema(Schema schema);

    /**
     * Gets the composite type schema given its name.
     *
     * @param name the schema name
     * @return the schema if any or null if none was found
     */
    Schema getSchema(String name);

    /**
     * Finds the composite type schema given the schema prefix.
     *
     * @param prefix the schema prefix
     * @return the schema if any or null if none was found
     */
    Schema getSchemaByPrefix(String prefix);

    /**
     * Checks if this composite type has any schema defined.
     *
     * @return true if this composite type has some schemas defined, false
     *         otherwise
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
     * Gets all the schemas (inherited ones too) of this composite type.
     *
     * @return the composite type schemas
     */
    Collection<Schema> getSchemas();

    TypeRef<? extends CompositeType> getRef();

}
