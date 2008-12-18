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
 *     George Lefter
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
 * An {@code UPDATE} statement.
 *
 * @author George Lefter
 */
public class Update implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Dialect dialect;

    private Table table;

    private List<Column> columns;

    private String where;

    public Update(Dialect dialect) {
        this.dialect = dialect;
    }

    public void setTable(Table table) {
        this.table = table;
    }
    public void setColumns(List<Column> columns) {
        this.columns = new ArrayList<Column>(columns);
    }

    public void addColumn(Column column) {
        columns.add(column);
    }

    /**
     * Gets the insert statement, and fill in {@code keys} with free parameter
     * names.
     *
     * @return the SQL insert statement.
     */
    public String getStatement() {
        StringBuilder buf = new StringBuilder(128);
        buf.append("update ");
        buf.append(table.getQuotedName(dialect));
        buf.append(" set ");

        List<String> updatedColumns = new LinkedList<String>();
        for (Column column : columns) {
            if (column.isIdentity()) {
                // identity column is never inserted
                continue;
            }
            updatedColumns.add(column.getQuotedName(dialect) + " = ?");
        }
        buf.append(StringUtils.join(updatedColumns, ", "));

        if (where != null) {
            buf.append(" where ");
            buf.append(where);
        }

        return buf.toString();
    }

    public void setWhere(String whereString) {
        where = whereString;
    }

}
