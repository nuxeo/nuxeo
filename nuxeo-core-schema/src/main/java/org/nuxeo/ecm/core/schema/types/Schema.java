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

import org.nuxeo.ecm.core.schema.TypeRef;


/**
 * A marker interface for schemas.
 * <p>
 * A schema is a complex type that can be used used to create composite types -
 * such as document types.
 * <p>
 * Schemas have no super types and must not be used as field types.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface Schema extends ComplexType {

    boolean isLazy();

    /**
     * Sets the lazy flag for the given schema. DocumentTypes that was already
     * initialized are not notified about schema change.
     *
     * @param isLazy
     */
    // TODO: impl a notification mechanism?
    void setLazy(boolean isLazy);

    TypeRef<Schema> getRef();

    /**
     * Gets the types declared by this schema.
     */
    Type[] getTypes();

    /**
     * Gets a schema local type given its name.
     *
     * @param typeName
     * @return the type or null if no such type
     */
    Type getType(String typeName);

    /**
     * Registers a new type in that schema context.
     *
     * @param type
     */
    void registerType(Type type);

}
