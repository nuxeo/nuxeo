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
package org.nuxeo.ecm.core.storage.sql.jdbc.db;

import java.io.Serializable;

/**
 * A SQL JOIN.
 */
public class Join implements Serializable, Comparable<Join> {

    private static final long serialVersionUID = 1L;

    public static final int INNER = 1;

    public static final int LEFT = 2;

    public static final int RIGHT = 3;

    public static final int IMPLICIT = 4;

    /** INNER / LEFT / RIGHT / IMPLICIT */
    public final int kind;

    /** Table name. */
    public final String table;

    /** Table alias, or {@code null}. */
    public final String tableAlias;

    /**
     * Parameter if table name is an expression that contains a "?", or {@code
     * null}.
     */
    public final String tableParam;

    /** Left part of equijoin. */
    public final String on1;

    /** Right part of equijoin. */
    public final String on2;

    public Join(int kind, String table, String tableAlias, String tableParam,
            String on1, String on2) {
        this.kind = kind;
        this.table = table;
        this.tableAlias = tableAlias;
        this.tableParam = tableParam;
        this.on1 = on1;
        this.on2 = on2;
    }

    // make sure IMPLICIT joins are last
    @Override
    public int compareTo(Join other) {
        if (kind == IMPLICIT && other.kind == IMPLICIT) {
            return 0;
        }
        if (kind == IMPLICIT) {
            return 1;
        }
        if (other.kind == IMPLICIT) {
            return -1;
        }
        return 0;
    }

    public String getTable() {
        if (tableAlias == null) {
            return table;
        } else {
            return table + " " + tableAlias;
        }
    }

    public String getClause() {
        return String.format("%s = %s", on1, on2);
    }

    @Override
    public String toString() {
        switch (kind) {
        case INNER:
            return String.format(" JOIN %s ON %s", getTable(), getClause());
        case LEFT:
            return String.format(" LEFT JOIN %s ON %s", getTable(), getClause());
        case RIGHT:
            return String.format(" RIGHT JOIN %s ON %s", getTable(),
                    getClause());
        case IMPLICIT:
            return String.format(", %s", getTable());
        default:
            throw new AssertionError();
        }
    }

}
