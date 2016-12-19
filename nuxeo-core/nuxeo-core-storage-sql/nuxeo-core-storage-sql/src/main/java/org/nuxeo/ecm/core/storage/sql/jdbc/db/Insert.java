/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

    private Column idColumn;

    private String insertValues;

    public Insert(Table table) {
        this.table = table;
        dialect = table.getDialect();
        columns = new LinkedList<>();
    }

    public void addColumn(Column column) {
        columns.add(column);
    }

    public void addIdentityColumn(Column idColumn) {
        this.idColumn = idColumn;
    }

    public void setValues(String insertValues) {
        this.insertValues = insertValues;
    }

    /**
     * Gets the statement to insert a row, or copy it if {@link #setValues} has been called.
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

        List<String> columnNames = new LinkedList<>();
        List<String> values = new LinkedList<>();
        for (Column column : columns) {
            columnNames.add(column.getQuotedName());
            values.add(column.getFreeVariableSetter());
        }

        if (columnNames.isEmpty()) {
            buf.append(dialect.getNoColumnsInsertString(idColumn));
        } else {
            buf.append('(');
            buf.append(String.join(", ", columnNames));
            buf.append(") ");
            if (insertValues == null) {
                buf.append("VALUES (");
                buf.append(String.join(", ", values));
                buf.append(')');
            } else {
                buf.append(insertValues);
            }
        }
        return buf.toString();
    }
}
