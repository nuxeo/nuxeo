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

import java.io.Serializable;

/**
 * The identification of a {@link Row} (table name and id) without the row
 * content itself.
 * <p>
 * This class is sometimes used as a marker for an "absent" row in the database,
 * when mixed with actual {@link Row}s.
 */
public class RowId implements Serializable {

    private static final long serialVersionUID = 1L;

    public final String tableName;

    public Serializable id;

    public RowId(RowId rowId) {
        tableName = rowId.tableName;
        id = rowId.id;
    }

    public RowId(String tableName, Serializable id) {
        this.tableName = tableName;
        this.id = id;
    }

    @Override
    public int hashCode() {
        int result = 31 + (id == null ? 0 : id.hashCode());
        return 31 * result + tableName.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof RowId) {
            return equals((RowId) other);
        }
        return false;
    }

    private boolean equals(RowId other) {
        if (other == this) {
            return true;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return tableName.equals(other.tableName);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + tableName + ", " + id + ')';
    }

}
