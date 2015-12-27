/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
