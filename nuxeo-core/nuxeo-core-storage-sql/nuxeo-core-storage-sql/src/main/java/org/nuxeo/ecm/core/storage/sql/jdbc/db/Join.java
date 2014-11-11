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
import java.util.LinkedList;
import java.util.List;

import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;

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
     * Parameter if table name is an expression that contains a "?", or
     * {@code null}.
     */
    public final String tableParam;

    /** Left part of equijoin. */
    public final String on1;

    /** Right part of equijoin. */
    public final String on2;

    /** Additional WHERE clauses. */
    public final List<String> whereClauses = new LinkedList<String>();

    /** Additional WHERE clauses parameters. */
    public final List<Serializable> whereParams = new LinkedList<Serializable>();

    public Join(int kind, String table, String tableAlias, String tableParam,
            String on1, String on2) {
        this.kind = kind;
        this.table = table;
        this.tableAlias = tableAlias;
        this.tableParam = tableParam;
        this.on1 = on1;
        this.on2 = on2;
    }

    public void addWhereClause(String whereClause, Serializable whereParam) {
        whereClauses.add(whereClause);
        whereParams.add(whereParam);
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

    public String getTable(Dialect dialect) {
        if (tableAlias == null) {
            return table;
        } else {
            return table + " " + dialect.openQuote() + tableAlias
                    + dialect.closeQuote();
        }
    }

    public String getClause() {
        return String.format("%s = %s", on1, on2);
    }

    /**
     * Does not return the WHERE clause.
     * <p>
     * {@inheritDoc}
     */
    public String toSql(Dialect dialect) {
        switch (kind) {
        case INNER:
            return String.format(" JOIN %s ON %s", getTable(dialect),
                    getClause());
        case LEFT:
            return String.format(" LEFT JOIN %s ON %s", getTable(dialect),
                    getClause());
        case RIGHT:
            return String.format(" RIGHT JOIN %s ON %s", getTable(dialect),
                    getClause());
        case IMPLICIT:
            return String.format(", %s", getTable(dialect));
        default:
            throw new AssertionError();
        }
    }

    @Override
    public String toString() {
        String k;
        switch (kind) {
        case INNER:
            k = "INNER";
            break;
        case LEFT:
            k = "LEFT";
            break;
        case RIGHT:
            k = "RIGHT";
            break;
        case IMPLICIT:
            k = "IMPLICIT";
            break;
        default:
            throw new AssertionError();
        }
        StringBuilder buf = new StringBuilder();
        buf.append("<");
        buf.append(k);
        buf.append(" JOIN ");
        buf.append(table);
        if (tableAlias != null) {
            buf.append(" ");
            buf.append(tableAlias);
        }
        buf.append(" ON ");
        buf.append(getClause());
        if (!whereClauses.isEmpty()) {
            buf.append(" WHERE ");
            buf.append(whereClauses);
            buf.append(" % ");
            buf.append(whereParams);
        }
        buf.append(">");
        return buf.toString();
    }

}
