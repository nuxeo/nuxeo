/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
