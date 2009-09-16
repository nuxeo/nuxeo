/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.core.storage.sql.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.db.dialect.Dialect;

/**
 * An alias for an existing table. The returned columns are wrapped.
 *
 * @author Florent Guillaume
 */
public class TableAlias implements Table {

    private static final long serialVersionUID = 1L;

    /** The table this is an alias of. */
    protected final Table table;

    /** The name (alias) used to refer to this table. */
    protected final String alias;

    protected final Dialect dialect;

    /**
     * Creates a table as an alias for another one.
     */
    public TableAlias(Table table, String alias) {
        this.table = table;
        this.alias = alias;
        dialect = table.getDialect();
    }

    public boolean isAlias() {
        return true;
    }

    public Table getRealTable() {
        return table;
    }

    public Dialect getDialect() {
        return dialect;
    }

    public String getName() {
        return alias;
    }

    public String getQuotedName() {
        return dialect.openQuote() + alias + dialect.closeQuote();
    }

    public String getQuotedSuffixedName(String suffix) {
        return dialect.openQuote() + alias + suffix + dialect.closeQuote();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Table(");
        buf.append(table.getName());
        buf.append(" AS ");
        buf.append(alias);
        buf.append(')');
        return buf.toString();
    }

    public Column getColumn(String name) {
        return new Column(table.getColumn(name), this);
    }

    // probably never used
    public Collection<Column> getColumns() {
        Collection<Column> columns = table.getColumns();
        List<Column> result = new ArrayList<Column>(columns.size());
        for (Column column : columns) {
            result.add(new Column(column, this));
        }
        return result;
    }

    public Column addColumn(String name, ColumnType type, String key, Model model) {
        throw new UnsupportedOperationException();
    }

    public void addIndex(String... columnNames) {
        throw new UnsupportedOperationException();
    }

    public void addFulltextIndex(String indexName, String... columnNames) {
        throw new UnsupportedOperationException();
    }

    public boolean hasFulltextIndex() {
        throw new UnsupportedOperationException();
    }

    public String getCreateSql() {
        throw new UnsupportedOperationException();
    }

    public String getAddColumnSql(Column column) {
        throw new UnsupportedOperationException();
    }

    public List<String> getPostCreateSqls() {
        throw new UnsupportedOperationException();
    }

    public String getDropSql() {
        throw new UnsupportedOperationException();
    }

}
