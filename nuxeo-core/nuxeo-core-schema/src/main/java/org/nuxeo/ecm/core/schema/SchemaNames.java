/*
 * (C) Copyright 2007-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.schema;

/**
 * The available registries of type-like information.
 */
public final class SchemaNames {

    /** A builtin data type. */
    public static final String BUILTIN = "@builtin";

    /** A registered schema. */
    public static final String SCHEMAS = "@schemas";

    /** A registered document type. */
    public static final String DOCTYPES = "@doctypes";

    /** A registered facet. */
    public static final String FACETS = "@facets";

    // Constant utility class.
    private SchemaNames() {
    }

}
