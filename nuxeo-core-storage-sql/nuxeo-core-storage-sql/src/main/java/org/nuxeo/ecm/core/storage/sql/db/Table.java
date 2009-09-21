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
 */

package org.nuxeo.ecm.core.storage.sql.db;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.PropertyType;
import org.nuxeo.ecm.core.storage.sql.db.dialect.Dialect;

/**
 * A SQL table.
 *
 * @author Florent Guillaume
 */
public interface Table extends Serializable {

    boolean isAlias();

    Table getRealTable();

    Dialect getDialect();

    String getName();

    String getQuotedName();

    String getQuotedSuffixedName(String suffix);

    Column getColumn(String name);

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
     * Adds a named fulltext index on one or several columns.
     *
     * @param indexName the index name
     * @param columnNames the column names
     */
    void addFulltextIndex(String indexName, String... columnNames);

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
     * Computes the SQL statement to finish creating the table, usually some
     * ALTER TABLE statements to add constraints.
     *
     * @return the SQL strings
     */
    List<String> getPostCreateSqls();

    /**
     * Computes the SQL statement to drop the table.
     * <p>
     * TODO drop constraints and indexes
     *
     * @return the SQL drop string.
     */
    String getDropSql();

}
