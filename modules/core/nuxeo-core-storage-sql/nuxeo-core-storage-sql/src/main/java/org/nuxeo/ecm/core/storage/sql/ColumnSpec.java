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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

/**
 * Abstract representation of the database-level column types.
 */
public enum ColumnSpec {

    // ----- schema-based columns -----
    STRING(), // may be VARCHAR or CLOB depending on length
    BOOLEAN(), //
    LONG(), //
    DOUBLE(), //
    TIMESTAMP(), //
    BLOBID(), // attached files
    BLOB, // byte array, for key/value store column
    ARRAY_STRING(), // may be VARCHAR or CLOB array depending on length
    ARRAY_BOOLEAN(), //
    ARRAY_LONG(), //
    ARRAY_DOUBLE(), //
    ARRAY_TIMESTAMP(), //
    ARRAY_BLOBID(), // attached files array
    ARRAY_INTEGER(),
    // ----- system columns -----
    NODEID, // node id primary generated key
    NODEIDFK, // fk to main node id, not nullable (frag id)
    NODEIDFKNP, // fk to main node id, not nullable, not primary
    NODEIDFKMUL, // fk to main node id, not nullable, non-unique
    NODEIDFKNULL, // fk to main node id, nullable
    NODEIDPK, // node id primary key, but not a fk (locks)
    NODEVAL, // same type as node id, not a fk (versionable, cluster...)
    NODEARRAY, // array of node if supported
    SYSNAME, // system names (type names etc)
    SYSNAMEARRAY, // system names array (mixins), string if not suppported
    TINYINT, // cluster inval kind
    INTEGER, // complex prop order, ordered doc
    AUTOINC, // auto-incremented integer (identity, serial, etc.)
    FTINDEXED, // summary ft column being indexed
    FTSTORED, // individual ft column
    CLUSTERNODE, // cluster node id
    CLUSTERFRAGS; // list of fragments impacted, for clustering

    /**
     * Checks if this spec holds a Nuxeo unique id (usually UUID).
     */
    public boolean isId() {
        switch (this) {
        case NODEID:
        case NODEIDFK:
        case NODEIDFKNP:
        case NODEIDFKMUL:
        case NODEIDFKNULL:
        case NODEIDPK:
        case NODEVAL:
            return true;
        default:
            return false;
        }
    }

}
