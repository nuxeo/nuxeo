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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.ecm.core.storage.sql.ColumnType;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;

/**
 * An alias for an existing table. The returned columns are wrapped.
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

    @Override
    public boolean isAlias() {
        return true;
    }

    @Override
    public Table getRealTable() {
        return table;
    }

    @Override
    public Dialect getDialect() {
        return dialect;
    }

    @Override
    public String getKey() {
        return table.getKey();
    }

    @Override
    public String getPhysicalName() {
        return alias;
    }

    @Override
    public String getQuotedName() {
        return dialect.openQuote() + alias + dialect.closeQuote();
    }

    @Override
    public String getQuotedSuffixedName(String suffix) {
        return dialect.openQuote() + alias + suffix + dialect.closeQuote();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Table(");
        buf.append(table.getPhysicalName());
        buf.append(" AS ");
        buf.append(alias);
        buf.append(')');
        return buf.toString();
    }

    @Override
    public Column getColumn(String name) {
        return new Column(table.getColumn(name), this);
    }

    @Override
    public Column getPrimaryColumn() {
        return new Column(table.getPrimaryColumn(), this);
    }

    // probably never used
    @Override
    public Collection<Column> getColumns() {
        Collection<Column> columns = table.getColumns();
        List<Column> result = new ArrayList<Column>(columns.size());
        for (Column column : columns) {
            result.add(new Column(column, this));
        }
        return result;
    }

    @Override
    public Column addColumn(String name, ColumnType type, String key,
            Model model) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addIndex(String... columnNames) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addFulltextIndex(String indexName, String... columnNames) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasFulltextIndex() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCreateSql() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getAddColumnSql(Column column) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getPostCreateSqls(Model model) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getPostAddSqls(Column column, Model model) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDropSql() {
        throw new UnsupportedOperationException();
    }

}
