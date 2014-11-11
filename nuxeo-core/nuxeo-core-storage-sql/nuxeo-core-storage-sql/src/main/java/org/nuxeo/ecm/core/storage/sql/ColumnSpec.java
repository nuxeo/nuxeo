/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

}
