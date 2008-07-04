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

package org.nuxeo.ecm.core.storage.sql;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.exception.SQLExceptionConverter;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.db.Column;
import org.nuxeo.ecm.core.storage.sql.db.Database;
import org.nuxeo.ecm.core.storage.sql.db.Delete;
import org.nuxeo.ecm.core.storage.sql.db.Insert;
import org.nuxeo.ecm.core.storage.sql.db.Select;
import org.nuxeo.ecm.core.storage.sql.db.Table;
import org.nuxeo.ecm.core.storage.sql.db.Update;

/**
 * This singleton generates and holds the actual SQL DDL and DML statements for
 * the operations needed by the {@link Mapper}, given a {@link Model}.
 * <p>
 * It is specific to one SQL dialect.
 *
 * @author Florent Guillaume
 */
public class SQLInfo {

    private static final Log log = LogFactory.getLog(SQLInfo.class);

    private final Model model;

    private final Dialect dialect;

    private final SQLExceptionConverter sqlExceptionConverter;

    private final Database database;

    private final Map<String, List<String>> primaryColumnsMap;

    private final Map<String, List<String>> primaryKeysMap;

    private final Map<String, String> selectByIdSqlMap; // statement

    private final Map<String, String> selectCollectionByIdSqlMap; // statement

    private final Map<String, List<Column>> selectByIdColumnsMap; // without ids

    private final Map<String, String> selectByChildNameSqlMap;

    private final Map<String, List<Column>> selectByChildNameWhatColumnsMap;

    private final Map<String, List<Column>> selectByChildNameWhereColumnsMap;

    private final Map<String, String> identityFetchSqlMap; // statement

    private final Map<String, Column> identityFetchColumnMap;

    private final Map<String, String> insertSqlMap; // statement

    private final Map<String, List<Column>> insertColumnsMap;

    private final Map<String, String> collectionInsertSqlMap; // statement

    private final Map<String, List<Column>> collectionInsertColumnsMap;

    private final Map<String, String> updateByIdSqlMap; // statement

    private final Map<String, List<Column>> updateByIdColumnsMap;

    private final Map<String, String> deleteSqlMap; // statement

    private String selectChildrenSql;

    private List<Column> selectChildrenWhereColumns;

    private List<Column> selectChildrenWhatColumns;

    /**
     * Generates and holds the needed SQL statements given a {@link Model} and a
     * {@link Dialect}.
     *
     * @param model the model
     * @param dialect the SQL dialect
     * @throws StorageException
     */
    public SQLInfo(Model model, Dialect dialect) throws StorageException {
        this.model = model;
        this.dialect = dialect;
        sqlExceptionConverter = dialect.buildSQLExceptionConverter();

        database = new Database();

        primaryColumnsMap = new HashMap<String, List<String>>();
        primaryKeysMap = new HashMap<String, List<String>>();

        selectByIdSqlMap = new HashMap<String, String>();
        selectCollectionByIdSqlMap = new HashMap<String, String>();
        selectByIdColumnsMap = new HashMap<String, List<Column>>();
        identityFetchSqlMap = new HashMap<String, String>();
        identityFetchColumnMap = new HashMap<String, Column>();

        selectByChildNameSqlMap = new HashMap<String, String>();
        selectByChildNameWhatColumnsMap = new HashMap<String, List<Column>>();
        selectByChildNameWhereColumnsMap = new HashMap<String, List<Column>>();
        selectChildrenSql = null;
        selectChildrenWhereColumns = null;
        selectChildrenWhatColumns = null;

        insertSqlMap = new HashMap<String, String>();
        insertColumnsMap = new HashMap<String, List<Column>>();
        collectionInsertSqlMap = new HashMap<String, String>();
        collectionInsertColumnsMap = new HashMap<String, List<Column>>();

        updateByIdSqlMap = new HashMap<String, String>();
        updateByIdColumnsMap = new HashMap<String, List<Column>>();

        deleteSqlMap = new HashMap<String, String>();

        initSQL();
    }

    // ----- exceptions -----

    public SQLExceptionConverter getSqlExceptionConverter() {
        return sqlExceptionConverter;
    }

    // ----- create whole database -----

    public List<String> getDatabaseCreateSql() {
        List<String> sqls = new LinkedList<String>();
        for (Table table : database.getTables()) {
            sqls.add(table.getCreateSql(dialect));
        }
        return sqls;
    }

    // ----- primary keys -----

    public List<String> getPrimaryColumns(String typeName) {
        return primaryColumnsMap.get(typeName);
    }

    public List<String> getPrimaryKeys(String typeName) {
        return primaryKeysMap.get(typeName);
    }

    // ----- select -----

    /**
     * Gets the SQL statement to select one row.
     *
     * @param typeName the type name.
     * @return the SQL statement.
     */
    public String getSelectByIdSql(String typeName) {
        return selectByIdSqlMap.get(typeName);
    }

    public String getSelectCollectionByIdSql(String typeName) {
        return selectCollectionByIdSqlMap.get(typeName);
    }

    // field names to bind
    public List<Column> getSelectByIdColumns(String typeName) {
        return selectByIdColumnsMap.get(typeName);
    }

    public String getSelectByChildNameSql() {
        return selectByChildNameSqlMap.get(model.HIER_TABLE_NAME);
    }

    public List<Column> getSelectByChildNameWhatColumns() {
        return selectByChildNameWhatColumnsMap.get(model.HIER_TABLE_NAME);
    }

    public List<Column> getSelectByChildNameWhereColumns() {
        return selectByChildNameWhereColumnsMap.get(model.HIER_TABLE_NAME);
    }


    public String getSelectChildrenSql() {
        return selectChildrenSql;
    }

    public List<Column> getSelectChildrenWhereColumns() {
        return selectChildrenWhereColumns;
    }

    public List<Column> getSelectChildrenWhatColumns() {
        return selectChildrenWhatColumns;
    }

    // ----- insert -----

    /**
     * Returns the SQL {@code INSERT} to add a row. The columns that represent
     * sequences that are implicitly auto-incremented aren't included.
     *
     * @param tableName the table name
     * @return the SQL {@code INSERT} statement
     */
    public String getInsertSql(String tableName) {
        return insertSqlMap.get(tableName);
    }

    /**
     * Returns the list of columns to use for an {@INSERT} statement
     * {@link #getInsertSql}.
     *
     * @param tableName the table name
     * @return the list of columns
     */
    public List<Column> getInsertColumns(String tableName) {
        return insertColumnsMap.get(tableName);
    }

    public String getCollectionInsertSql(String tableName) {
        return collectionInsertSqlMap.get(tableName);
    }

    public List<Column> getCollectionInsertColumns(String tableName) {
        return collectionInsertColumnsMap.get(tableName);
    }

    // ----- post insert fetch -----

    // statement to fetch all values auto-incremented by an insert
    public String getIdentityFetchSql(String tableName) {
        return identityFetchSqlMap.get(tableName);
    }

    public Column getIdentityFetchColumn(String tableName) {
        return identityFetchColumnMap.get(tableName);
    }

    // ----- update -----

    // XXX more fined grained SQL updating only changed columns
    public String getUpdateByIdSql(String tableName) {
        return updateByIdSqlMap.get(tableName);
    }

    public List<Column> getUpdateByIdColumns(String tableName) {
        return updateByIdColumnsMap.get(tableName);
    }

    // ----- delete -----

    /**
     * Returns the SQL {@code DELETE} to delete a row. The primary key columns
     * are free parameters.
     *
     * @param tableName the table name
     * @return the SQL {@code INSERT} statement
     */
    public String getDeleteSql(String tableName) {
        return deleteSqlMap.get(tableName);
    }

    // ----- prepare everything -----

    /**
     * Creates all the sql from the models.
     */
    protected void initSQL() {

        // structural tables
        initRepositorySQL();
        initHierarchySQL();

        for (String tableName : model.fragmentsKeysType.keySet()) {
            initSimpleFragmentSQL(tableName);
        }
        for (String tableName : model.collectionTables.keySet()) {
            initCollectionFragmentSQL(tableName);
        }

    }

    /**
     * Creates the SQL for the table holding global repository information. This
     * includes the id of the hierarchy root node.
     */
    protected void initRepositorySQL() {
        log.debug("Init repository information");
        TableMaker maker = new TableMaker(model.REPOINFO_TABLE_NAME, false);
        maker.newPrimaryKey();
        maker.newColumn(model.REPOINFO_ROOTID_KEY, Types.BIGINT);
        maker.postProcess();
    }

    /**
     * Creates the SQL for the table holding hierarchy information.
     */
    protected void initHierarchySQL() {
        log.debug("Init hierarchy information");

        TableMaker maker = new TableMaker(model.HIER_TABLE_NAME, false);
        maker.newPrimaryKey();
        maker.newColumn(model.HIER_PARENT_KEY, Types.BIGINT);
        maker.newColumn(model.HIER_CHILD_POS_KEY, Types.INTEGER);
        maker.newColumn(model.HIER_CHILD_NAME_KEY, Types.VARCHAR); // text?
        maker.postProcess();
        maker.postProcessSelectByChildName();
        maker.postProcessSelectChildren();
    }

    /**
     * Creates the SQL for one fragment.
     */
    protected void initSimpleFragmentSQL(String tableName) {
        TableMaker maker = new TableMaker(tableName, false);
        if (tableName.equals(model.MAIN_TABLE_NAME)) {
            maker.newId(); // this is how a new doc id is generated
        } else {
            maker.newPrimaryKey();
        }

        Map<String, PropertyType> fragmentKeysType = model.fragmentsKeysType.get(tableName);
        for (Entry<String, PropertyType> entry : fragmentKeysType.entrySet()) {
            String key = entry.getKey();
            PropertyType type = entry.getValue();
            maker.newPrimitiveField(key, type);
        }

        maker.postProcess();

    }

    protected void initCollectionFragmentSQL(String tableName) {
        PropertyType type = model.collectionTables.get(tableName);
        TableMaker maker = new TableMaker(tableName, true);
        maker.newPrimaryKey();

        maker.newColumn(model.COLL_TABLE_POS_KEY, Types.INTEGER);
        maker.newPrimitiveField(model.COLL_TABLE_VALUE_KEY,
                type.getArrayBaseType());
        maker.postProcess();
    }

    // ----- prepare one table -----

    protected class TableMaker {

        private final String tableName;

        private final Table table;

        private final boolean isCollection;

        protected TableMaker(String tableName, boolean isCollection) {
            this.tableName = tableName;
            this.isCollection = isCollection;
            table = new Table(tableName);
            database.addTable(table);
        }

        protected void newId() {
            Column column = newColumn(model.MAIN_KEY, Types.BIGINT);
            column.setIdentity(true);
            column.setPrimary(true);
        }

        protected void newPrimaryKey() {
            Column column = newColumn(model.MAIN_KEY, Types.BIGINT);
            column.setPrimary(true);
        }

        protected void newPrimitiveField(String key, PropertyType type) {
            int sqlType;
            switch (type) {
            case STRING:
                sqlType = Types.CLOB; // or VARCHAR for system tables?
                break;
            case LONG:
                sqlType = Types.INTEGER;
                break;
            case DATETIME:
                sqlType = Types.TIMESTAMP;
                break;
            default:
                throw new RuntimeException("Bad type: " + type);
            }
            newColumn(key, sqlType);
            // XXX apply defaults
        }

        protected Column newColumn(String key, int sqlType) {
            String columnName = key;
            Column column = new Column(columnName, sqlType, key);
            table.addColumn(column);
            return column;
        }

        // ----------------------- post processing -----------------------

        /**
         * Precompute what we can from the information available.
         */
        protected void postProcess() {
            postProcessPrimary();
            postProcessSelectById(isCollection);
            if (isCollection) {
                postProcessCollectionInsert();
            } else {
                postProcessInsert();
            }
            postProcessIdentityFetch();
            postProcessUpdateById();
            postProcessDelete();
        }

        protected void postProcessPrimary() {
            List<String> primaryColumns = new LinkedList<String>();
            List<String> primaryKeys = new LinkedList<String>();
            for (Column column : table.getColumns()) {
                if (column.isPrimary()) {
                    primaryColumns.add(column.getName());
                    primaryKeys.add(column.getKey());
                }
            }
            primaryColumnsMap.put(tableName, primaryColumns);
            primaryKeysMap.put(tableName, primaryKeys);
        }

        protected void postProcessSelectById(boolean isCollection) {
            List<Column> selectByIdColumns = new LinkedList<Column>();
            List<String> whats = new LinkedList<String>();
            List<String> wheres = new LinkedList<String>();
            Column posColumn = null;
            for (Column column : table.getColumns()) {
                if (column.isPrimary()) {
                    wheres.add(column.getQuotedName(dialect) + " = ?");
                } else {
                    whats.add(column.getQuotedName(dialect));
                    selectByIdColumns.add(column);
                    if (column.getName().equals(model.COLL_TABLE_POS_KEY)) {
                        posColumn = column;
                    }
                }
            }
            Select select = new Select(dialect);
            select.setWhat(StringUtils.join(whats, ", "));
            select.setFrom(table.getQuotedName(dialect));
            select.setWhere(StringUtils.join(wheres, " AND "));
            if (isCollection) {
                select.setOrderBy(posColumn.getQuotedName(dialect));
                selectCollectionByIdSqlMap.put(tableName, select.getStatement());
                selectByIdColumnsMap.put(tableName, selectByIdColumns);
            } else {
                selectByIdSqlMap.put(tableName, select.getStatement());
                selectByIdColumnsMap.put(tableName, selectByIdColumns);
            }
        }

        // only called for hierarchy entity, so tableName is fixed
        protected void postProcessSelectByChildName() {
            List<Column> whatColumns = new LinkedList<Column>();
            List<Column> whereColumns = new ArrayList<Column>(2);
            List<String> whats = new LinkedList<String>();
            List<String> wheres = new LinkedList<String>();
            for (Column column : table.getColumns()) {
                String name = column.getName();
                if (name.equals(model.HIER_PARENT_KEY) ||
                        name.equals(model.HIER_CHILD_NAME_KEY)) {
                    wheres.add(column.getQuotedName(dialect) + " = ?");
                    whereColumns.add(column);
                } else {
                    whats.add(column.getQuotedName(dialect));
                    whatColumns.add(column);
                }
            }
            Select select = new Select(dialect);
            select.setWhat(StringUtils.join(whats, ", "));
            select.setFrom(table.getQuotedName(dialect));
            select.setWhere(StringUtils.join(wheres, " AND "));
            selectByChildNameSqlMap.put(tableName, select.getStatement());
            selectByChildNameWhatColumnsMap.put(tableName, whatColumns);
            selectByChildNameWhereColumnsMap.put(tableName, whereColumns);
        }

        protected void postProcessSelectChildren() {
            List<Column> whatColumns = new LinkedList<Column>();
            List<String> whats = new LinkedList<String>();
            List<Column> whereColumns = null;
            String where = null;
            for (Column column : table.getColumns()) {
                String name = column.getName();
                if (name.equals(model.HIER_PARENT_KEY)) {
                    where = column.getQuotedName(dialect) + " = ?";
                    whereColumns = Collections.singletonList(column);
                } else {
                    whats.add(column.getQuotedName(dialect));
                    whatColumns.add(column);
                }
            }
            Select select = new Select(dialect);
            select.setWhat(StringUtils.join(whats, ", "));
            select.setFrom(table.getQuotedName(dialect));
            select.setWhere(where);
            selectChildrenSql = select.getStatement();
            selectChildrenWhatColumns = whatColumns;
            selectChildrenWhereColumns = whereColumns;
        }

        protected void postProcessInsert() {
            // insert (implicitly auto-generated sequences not included)
            List<Column> insertColumns = new LinkedList<Column>();
            Insert insert = new Insert(dialect);
            insert.setTable(table);
            for (Column column : table.getColumns()) {
                insert.addColumn(column);
            }
            insertSqlMap.put(tableName, insert.getStatement(insertColumns));
            insertColumnsMap.put(tableName, insertColumns);
        }

        // identical to above for now
        // TODO optimize multiple inserts into one statement
        protected void postProcessCollectionInsert() {
            List<Column> insertColumns = new LinkedList<Column>();
            Insert insert = new Insert(dialect);
            insert.setTable(table);
            for (Column column : table.getColumns()) {
                insert.addColumn(column);
            }
            collectionInsertSqlMap.put(tableName,
                    insert.getStatement(insertColumns));
            collectionInsertColumnsMap.put(tableName, insertColumns);
        }

        protected void postProcessIdentityFetch() {
            // post-insert select of identity value
            String sql = null;
            Column identityColumn = null;
            for (Column column : table.getColumns()) {
                if (column.isIdentity()) {
                    sql = dialect.getIdentitySelectString(table.getName(),
                            column.getName(), column.getSqlType());
                    identityColumn = column;
                    break; // only one identity per table
                }
            }
            identityFetchSqlMap.put(tableName, sql);
            identityFetchColumnMap.put(tableName, identityColumn);
        }

        protected void postProcessUpdateById() {
            List<String> newValues = new LinkedList<String>();
            List<Column> updateByIdColumns = new LinkedList<Column>();
            List<String> wheres = new LinkedList<String>();
            List<Column> whereColumns = new LinkedList<Column>();
            for (Column column : table.getColumns()) {
                if (column.isPrimary()) {
                    wheres.add(column.getQuotedName(dialect) + " = ?");
                    whereColumns.add(column);
                } else {
                    newValues.add(column.getQuotedName(dialect) + " = ?");
                    updateByIdColumns.add(column);
                }
            }
            updateByIdColumns.addAll(whereColumns);
            Update update = new Update(dialect);
            update.setTable(table.getQuotedName(dialect));
            update.setNewValues(StringUtils.join(newValues, ", "));
            update.setWhere(StringUtils.join(wheres, " AND "));
            updateByIdSqlMap.put(tableName, update.getStatement());
            updateByIdColumnsMap.put(tableName, updateByIdColumns);
        }

        protected void postProcessDelete() {
            Delete delete = new Delete(dialect);
            delete.setTable(table);
            List<String> wheres = new LinkedList<String>();
            for (Column column : table.getColumns()) {
                if (column.isPrimary()) {
                    wheres.add(column.getQuotedName(dialect) + " = ?");
                }
            }
            delete.setWhere(StringUtils.join(wheres, " AND "));
            deleteSqlMap.put(tableName, delete.getStatement());
        }
    }

}
