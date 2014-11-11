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

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;

/**
 * An {@code INSERT} statement.
 *
 * @author Florent Guillaume
 */
public class Insert implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final Dialect dialect;

    private final Table table;

    private final List<Column> columns;

    private String insertValues;

    public Insert(Table table) {
        this.table = table;
        dialect = table.getDialect();
        columns = new LinkedList<Column>();
    }

    public void addColumn(Column column) {
        columns.add(column);
    }

    public void setValues(String insertValues) {
        this.insertValues = insertValues;
    }

    /**
     * Gets the statement to insert a row, or copy it if {@link #setValues} has
     * been called.
     * <p>
     * Example: {@code INSERT INTO foo (a, b, c) SELECT ?, b, c FROM foo WHERE
     * id = ?}
     *
     * @return the SQL insert or copy statement
     */
    public String getStatement() {
        StringBuilder buf = new StringBuilder(128);
        buf.append("INSERT INTO ");
        buf.append(table.getQuotedName());
        buf.append(' ');

        List<String> columnNames = new LinkedList<String>();
        List<String> values = new LinkedList<String>();
        for (Column column : columns) {
            columnNames.add(column.getQuotedName());
            values.add(column.getFreeVariableSetter());
        }

        if (columnNames.isEmpty()) {
            buf.append(dialect.getNoColumnsInsertString());
        } else {
            buf.append('(');
            buf.append(StringUtils.join(columnNames, ", "));
            buf.append(") ");
            if (insertValues == null) {
                buf.append("VALUES (");
                buf.append(StringUtils.join(values, ", "));
                buf.append(')');
            } else {
                buf.append(insertValues);
            }
        }
        return buf.toString();
    }
}
