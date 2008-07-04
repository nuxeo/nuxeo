/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: Insert.java 18035 2007-2008-05-01 03:34:19Z fguillaume $
 */

package org.nuxeo.ecm.core.storage.sql.db;

import java.io.Serializable;
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

    private Dialect dialect;

    private Table table;

    private List<Column> columns;

    public Insert(Dialect dialect) {
        this.dialect = dialect;
        columns = new LinkedList<Column>();
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public void addColumn(Column column) {
        columns.add(column);
    }

    /**
     * Get the insert statement, and fill in {@code keys} with free parameter
     * names.
     *
     * @param icolumns a returned list of columns bound to free parameters.
     * @return the SQL insert statement.
     */
    public String getStatement(List<Column> icolumns) {
        StringBuilder buf = new StringBuilder(128);
        buf.append("INSERT INTO ");
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
            // String value;
            // Sequence sequence = column.getSequence();
            // if (sequence != null && canInsertIncrementSequences) {
            // value = sequence.getAutoIncrementInsertDefaultSql(dialect);
            // if (value == null) {
            // // no value needed, skip this column
            // continue;
            // }
            // } else {
            // value = "?";
            icolumns.add(column);
            // }
            columnNames.add(column.getQuotedName(dialect));
            values.add("?");
        }

        if (columnNames.isEmpty()) {
            buf.append(dialect.getNoColumnsInsertString());
        } else {
            buf.append('(');
            buf.append(StringUtils.join(columnNames, ", "));
            buf.append(") VALUES (");
            buf.append(StringUtils.join(values, ", "));
            buf.append(')');
        }
        return buf.toString();
    }
}
