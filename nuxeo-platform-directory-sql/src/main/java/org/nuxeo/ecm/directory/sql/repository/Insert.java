/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 *
 * $Id: Insert.java 18035 2007-05-01 03:34:19Z fguillaume $
 */

package org.nuxeo.ecm.directory.sql.repository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.nuxeo.common.utils.StringUtils;

/**
 * An {@code INSERT} statement.
 *
 * @author Florent Guillaume
 */
public class Insert implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Dialect dialect;

    private Table table;

    private final List<Column> columns;

    public Insert(Dialect dialect) {
        this.dialect = dialect;
        columns = new ArrayList<Column>();
    }

    public Insert(Dialect dialect, Table table, List<Column> columns) {
        this.dialect = dialect;
        this.table = table;
        this.columns = new ArrayList<Column>(columns);
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public void addColumn(Column column) {
        columns.add(column);
    }

    /**
     * Gets the insert statement, and fills in {@code keys} with free parameter
     * names.
     *
     * @return the SQL insert statement.
     */
    public String getStatement() {
        StringBuilder buf = new StringBuilder(128);
        buf.append("insert into ");
        buf.append(table.getQuotedName(dialect));
        buf.append(' ');

        List<String> columnNames = new LinkedList<String>();
        List<String> values = new LinkedList<String>();
        // boolean canInsertIncrementSequences =
        // Sequence.canInsertIncrementSequences(dialect);
        for (Column column : columns) {
            if (column.isIdentity()) {
                // identity column is never inserted
                continue;
            }
            columnNames.add(column.getQuotedName(dialect));
            values.add("?");
        }

        if (columnNames.isEmpty()) {
            buf.append(dialect.getNoColumnsInsertString());
        } else {
            buf.append('(');
            buf.append(StringUtils.join(columnNames, ", "));
            buf.append(") values (");
            buf.append(StringUtils.join(values, ", "));
            buf.append(')');
        }
        return buf.toString();
    }

}
