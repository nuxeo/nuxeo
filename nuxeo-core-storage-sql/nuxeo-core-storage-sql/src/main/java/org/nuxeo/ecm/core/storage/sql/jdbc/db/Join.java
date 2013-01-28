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
    public Column column1;

    /** Right part of equijoin. */
    public Column column2;

    /** Left part of equijoin. */
    public String on1;

    /** Right part of equijoin. */
    public String on2;

    /** Additional WHERE clauses. */
    public final List<String> whereClauses = new LinkedList<String>();

    /** Additional WHERE clauses parameters. */
    public final List<Serializable> whereParams = new LinkedList<Serializable>();

    private Join(int kind, String table, String tableAlias, String tableParam) {
        this.kind = kind;
        this.table = table;
        this.tableAlias = tableAlias;
        this.tableParam = tableParam;
    }

    public Join(int kind, String table, String tableAlias, String tableParam,
            Column column1, Column column2) {
        this(kind, table, tableAlias, tableParam);
        this.column1 = column1;
        this.column2 = column2;
    }

    public Join(int kind, String table, String tableAlias, String tableParam,
            String on1, String on2) {
        this(kind, table, tableAlias, tableParam);
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

    public String getClause(Dialect dialect) {
        if (on1 == null && on2 == null) {
            on1 = column1.getFullQuotedName();
            on2 = column2.getFullQuotedName();
            boolean isid1 = column1.getType().isId();
            boolean isid2 = column2.getType().isId();
            if (dialect != null && isid1 != isid2) {
                // temporary fix cast uuid to varchar because relation table
                // has varchar source and target field
                if (isid1) {
                    on1 = dialect.castIdToVarchar(on1);
                } else {
                    on2 = dialect.castIdToVarchar(on2);
                }
            }
        }
        return on1 + " = " + on2;
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
                    getClause(dialect));
        case LEFT:
            return String.format(" LEFT JOIN %s ON %s", getTable(dialect),
                    getClause(dialect));
        case RIGHT:
            return String.format(" RIGHT JOIN %s ON %s", getTable(dialect),
                    getClause(dialect));
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
        buf.append(getClause(null));
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
