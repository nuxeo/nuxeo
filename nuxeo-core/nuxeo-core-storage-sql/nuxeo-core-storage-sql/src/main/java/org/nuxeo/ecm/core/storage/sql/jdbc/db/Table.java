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
import java.util.Collection;
import java.util.List;

import org.nuxeo.ecm.core.storage.sql.ColumnType;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;

/**
 * A SQL table.
 */
public interface Table extends Serializable {

    enum IndexType {
        /** Fulltext index, may be on several columns. */
        FULLTEXT,
        /** Non primary index but the main one for this table. */
        MAIN_NON_PRIMARY
    }

    boolean isAlias();

    Table getRealTable();

    Dialect getDialect();

    String getKey();

    String getPhysicalName();

    String getQuotedName();

    String getQuotedSuffixedName(String suffix);

    Column getColumn(String name);

    Column getPrimaryColumn();

    Collection<Column> getColumns();

    /**
     * Adds a {@link Column} to the table.
     */
    Column addColumn(String name, ColumnType type, String key, Model model);

    /**
     * Adds an index on one or several columns.
     *
     * @param columnNames the column names
     */
    void addIndex(String... columnNames);

    /**
     * Adds an index of the given name and type on one or several columns.
     *
     * @param indexName the index name
     * @param indexType the index type
     * @param columnNames the column names
     */
    void addIndex(String indexName, IndexType indexType, String... columnNames);

    /**
     * Checks if the table has some fulltext indexes.
     *
     * @return {@code true} if the table has some fulltext indexes
     */
    boolean hasFulltextIndex();

    /**
     * Computes the SQL statement to create the table.
     *
     * @return the SQL create string.
     */
    String getCreateSql();

    /**
     * Computes the SQL statement to alter a table and add a column to it.
     *
     * @param column the column to add
     * @return the SQL alter table string
     */
    String getAddColumnSql(Column column);

    /**
     * Computes the SQL statements to finish creating the table, usually some
     * ALTER TABLE statements to add constraints or indexes.
     *
     * @return the SQL strings
     */
    List<String> getPostCreateSqls(Model model);

    /**
     * Computes the SQL statements to finish adding a column, usually some ALTER
     * TABLE statements to add constraints or indexes.
     *
     * @return the SQL strings
     */
    List<String> getPostAddSqls(Column column, Model model);

    /**
     * Computes the SQL statement to drop the table.
     * <p>
     * TODO drop constraints and indexes
     *
     * @return the SQL drop string.
     */
    String getDropSql();

}
