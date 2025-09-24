/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.directory.api;

/**
 * @since 2025.9
 */
public final class DirectoryConstants {

    // -------------------------------
    // System schema related constants
    // -------------------------------

    /**
     * The Nuxeo XPath property for the entry external id.
     */
    public static final String SYSTEM_ID_PROPERTY = "sys:id";

    /**
     * The Nuxeo Schema name for the external metadata.
     */
    public static final String SYSTEM_SCHEMA = "system";

    // ---------------
    // Directory Types
    // ---------------

    /**
     * The {@link org.nuxeo.ecm.directory.Directory#getTypes() directory type} for directory with external id support.
     */
    public static final String EXTERNAL_ID_TYPE = "external-id";

    /**
     * The {@link org.nuxeo.ecm.directory.Directory#getTypes() directory type} for system directory.
     */
    public static final String SYSTEM_DIRECTORY_TYPE = "system";

    // -----
    // Other
    // -----
    public static final String READONLY_ENTRY_FLAG = "READONLY_ENTRY";

    private DirectoryConstants() {
        // constants class
    }
}
