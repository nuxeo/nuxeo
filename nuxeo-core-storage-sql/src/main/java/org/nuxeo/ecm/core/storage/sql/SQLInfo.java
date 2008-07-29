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

    private String selectRootIdSql;

    private Column selectRootIdWhatColumn;

    private final Map<String, String> selectByIdSqlMap; // statement

    private final Map<String, String> selectCollectionByIdSqlMap; // statement

    private final Map<String, List<Column>> selectByIdColumnsMap; // without ids

    private final Map<String, String> identityFetchSqlMap; // statement

    private final Map<String, Column> identityFetchColumnMap;

    private final Map<String, String> insertSqlMap; // statement

    private final Map<String, List<Column>> insertColumnsMap;

    private final Map<String, String> collectionInsertSqlMap; // statement

    private final Map<String, List<Column>> collectionInsertColumnsMap;

    private final Map<String, String> updateByIdSqlMap; // statement

    private final Map<String, List<Column>> updateByIdColumnsMap;

    private final Map<String, String> deleteSqlMap; // statement

    private String selectByChildNameAllSql;

    private String selectByChildNameRegularSql;

    private String selectByChildNamePropertiesSql;

    private List<Column> selectByChildNameAllWhatColumns;

    private List<Column> selectByChildNameRegularWhatColumns;

    private List<Column> selectByChildNamePropertiesWhatColumns;

    private List<Column> selectByChildNameAllWhereColumns;

    private List<Column> selectByChildNameRegularWhereColumns;

    private List<Column> selectByChildNamePropertiesWhereColumns;

    private String selectChildrenAllSql;

    private String selectChildrenRegularSql;

    private String selectChildrenPropertiesSql;

    private List<Column> selectChildrenAllWhatColumns;

    private List<Column> selectChildrenRegularWhatColumns;

    private List<Column> selectChildrenPropertiesWhatColumns;

    private List<Column> selectChildrenAllWhereColumns;

    private List<Column> selectChildrenRegularWhereColumns;

    private List<Column> selectChildrenPropertiesWhereColumns;

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

        selectRootIdSql = null;
        selectRootIdWhatColumn = null;

        selectByIdSqlMap = new HashMap<String, String>();
        selectCollectionByIdSqlMap = new HashMap<String, String>();
        selectByIdColumnsMap = new HashMap<String, List<Column>>();
        identityFetchSqlMap = new HashMap<String, String>();
        identityFetchColumnMap = new HashMap<String, Column>();

        selectByChildNameAllSql = null;
        selectByChildNameAllWhatColumns = null;
        selectByChildNameAllWhereColumns = null;
        selectByChildNameRegularSql = null;
        selectByChildNameRegularWhatColumns = null;
        selectByChildNameRegularWhereColumns = null;
        selectByChildNamePropertiesSql = null;
        selectByChildNamePropertiesWhatColumns = null;
        selectByChildNamePropertiesWhereColumns = null;

        selectChildrenAllSql = null;
        selectChildrenAllWhatColumns = null;
        selectChildrenAllWhereColumns = null;
        selectChildrenRegularSql = null;
        selectChildrenRegularWhatColumns = null;
        selectChildrenRegularWhereColumns = null;
        selectChildrenPropertiesSql = null;
        selectChildrenPropertiesWhatColumns = null;
        selectChildrenPropertiesWhereColumns = null;

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

    // ----- select -----

    public String getSelectRootIdSql() {
        return selectRootIdSql;
    }

    public Column getSelectRootIdWhatColumn() {
        return selectRootIdWhatColumn;
    }

    public String getInsertRootIdSql() {
        return insertSqlMap.get(model.REPOINFO_TABLE_NAME);
    }

    public List<Column> getInsertRootIdColumns() {
        return insertColumnsMap.get(model.REPOINFO_TABLE_NAME);
    }

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

    public String getSelectByChildNameSql(Boolean complexProp) {
        if (complexProp == null) {
            return selectByChildNameAllSql;
        } else if (complexProp.booleanValue()) {
            return selectByChildNamePropertiesSql;
        } else {
            return selectByChildNameRegularSql;
        }
    }

    public List<Column> getSelectByChildNameWhatColumns(Boolean complexProp) {
        if (complexProp == null) {
            return selectByChildNameAllWhatColumns;
        } else if (complexProp.booleanValue()) {
            return selectByChildNamePropertiesWhatColumns;
        } else {
            return selectByChildNameRegularWhatColumns;
        }
    }

    public List<Column> getSelectByChildNameWhereColumns(Boolean complexProp) {
        if (complexProp == null) {
            return selectByChildNameAllWhereColumns;
        } else if (complexProp.booleanValue()) {
            return selectByChildNamePropertiesWhereColumns;
        } else {
            return selectByChildNameRegularWhereColumns;
        }
    }

    public String getSelectChildrenSql(Boolean complexProp) {
        if (complexProp == null) {
            return selectChildrenAllSql;
        } else if (complexProp.booleanValue()) {
            return selectChildrenPropertiesSql;
        } else {
            return selectChildrenRegularSql;
        }
    }

    public List<Column> getSelectChildrenWhatColumns(Boolean complexProp) {
        if (complexProp == null) {
            return selectChildrenAllWhatColumns;
        } else if (complexProp.booleanValue()) {
            return selectChildrenPropertiesWhatColumns;
        } else {
            return selectChildrenRegularWhatColumns;
        }
    }

    public List<Column> getSelectChildrenWhereColumns(Boolean complexProp) {
        if (complexProp == null) {
            return selectChildrenAllWhereColumns;
        } else if (complexProp.booleanValue()) {
            return selectChildrenPropertiesWhereColumns;
        } else {
            return selectChildrenRegularWhereColumns;
        }
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
        maker.newColumn(model.REPOINFO_REPOID_KEY, Types.INTEGER);
        maker.newPrimaryKey(); // foreign key to main id
        maker.postProcessRepository();
    }

    /**
     * Creates the SQL for the table holding hierarchy information.
     */
    protected void initHierarchySQL() {
        log.debug("Init hierarchy information");

        TableMaker maker = new TableMaker(model.HIER_TABLE_NAME, false);
        maker.newPrimaryKey();
        maker.newMainKey(model.HIER_PARENT_KEY);
        maker.newColumn(model.HIER_CHILD_POS_KEY, Types.INTEGER);
        maker.newColumn(model.HIER_CHILD_NAME_KEY, Types.VARCHAR); // text?
        maker.newColumn(model.HIER_CHILD_ISPROPERTY_KEY, Types.BIT); // not null
        maker.postProcess();
        maker.postProcessSelectByChildNameAll();
        maker.postProcessSelectByChildNamePropertiesFlag();
        maker.postProcessSelectChildrenAll();
        maker.postProcessSelectChildrenPropertiesFlag();
    }

    /**
     * Creates the SQL for one fragment.
     */
    protected void initSimpleFragmentSQL(String tableName) {
        TableMaker maker = new TableMaker(tableName, false);
        if (tableName.equals(model.MAIN_TABLE_NAME)) {
            maker.newId(); // global primary key / generation
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

        protected Column newMainKey(String name) {
            Column column;
            switch (model.idGenPolicy) {
            case APP_UUID:
                column = newColumn(name, Types.VARCHAR);
                column.setLength(36);
                break;
            case DB_IDENTITY:
                column = newColumn(name, Types.BIGINT);
                break;
            default:
                throw new AssertionError(model.idGenPolicy);
            }
            return column;
        }

        protected void newPrimaryKey() {
            Column column = newMainKey(model.MAIN_KEY);
            column.setPrimary(true);
        }

        protected void newId() {
            Column column = newMainKey(model.MAIN_KEY);
            column.setPrimary(true);
            switch (model.idGenPolicy) {
            case APP_UUID:
                break;
            case DB_IDENTITY:
                column.setIdentity(true);
                break;
            default:
                throw new AssertionError(model.idGenPolicy);
            }
        }

        protected void newPrimitiveField(String key, PropertyType type) {
            int sqlType;
            switch (type) {
            case STRING:
                sqlType = Types.CLOB; // or VARCHAR for system tables?
                break;
            case BOOLEAN:
                sqlType = Types.BIT;
                break;
            case LONG:
                sqlType = Types.INTEGER;
                break;
            case DOUBLE:
                sqlType = Types.DOUBLE;
                break;
            case DATETIME:
                sqlType = Types.TIMESTAMP;
                break;
            case BINARY:
                sqlType = Types.BLOB;
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

        protected void postProcessRepository() {
            postProcessRootIdSelect();
            postProcessInsert();
        }

        protected void postProcessRootIdSelect() {
            String what = null;
            String where = null;
            for (Column column : table.getColumns()) {
                String name = column.getName();
                String qname = column.getQuotedName(dialect);
                if (name.equals(model.MAIN_KEY)) {
                    what = qname;
                    selectRootIdWhatColumn = column;
                } else if (name.equals(model.REPOINFO_REPOID_KEY)) {
                    where = qname + " = ?";
                } else {
                    throw new AssertionError(column);
                }
            }
            Select select = new Select(dialect);
            select.setWhat(what);
            select.setFrom(table.getQuotedName(dialect));
            select.setWhere(where);
            selectRootIdSql = select.getStatement();
        }

        /**
         * Precompute what we can from the information available for a regular
         * schema table.
         */
        protected void postProcess() {
            postProcessSelectById(isCollection);
            if (isCollection) {
                postProcessCollectionInsert();
            } else {
                postProcessInsert();
            }
            switch (model.idGenPolicy) {
            case APP_UUID:
                break;
            case DB_IDENTITY:
                postProcessIdentityFetch();
                break;
            default:
                throw new AssertionError(model.idGenPolicy);

            }
            postProcessUpdateById();
            postProcessDelete();
        }

        protected void postProcessSelectById(boolean isCollection) {
            List<Column> selectByIdColumns = new LinkedList<Column>();
            List<String> whats = new LinkedList<String>();
            List<String> wheres = new LinkedList<String>();
            Column posColumn = null;
            for (Column column : table.getColumns()) {
                String qname = column.getQuotedName(dialect);
                if (column.isPrimary()) {
                    wheres.add(qname + " = ?");
                } else {
                    whats.add(qname);
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

        protected void postProcessSelectByChildNameAll() {
            List<Column> whatColumns = new ArrayList<Column>(3);
            List<String> whats = new ArrayList<String>(3);
            List<Column> whereColumns = new ArrayList<Column>(2);
            List<String> wheres = new ArrayList<String>(2);
            for (Column column : table.getColumns()) {
                String name = column.getName();
                String qname = column.getQuotedName(dialect);
                if (name.equals(model.HIER_PARENT_KEY) ||
                        name.equals(model.HIER_CHILD_NAME_KEY)) {
                    wheres.add(qname + " = ?");
                    whereColumns.add(column);
                } else {
                    whats.add(qname);
                    whatColumns.add(column);
                }
            }
            Select select = new Select(dialect);
            select.setWhat(StringUtils.join(whats, ", "));
            select.setFrom(table.getQuotedName(dialect));
            select.setWhere(StringUtils.join(wheres, " AND "));
            selectByChildNameAllSql = select.getStatement();
            selectByChildNameAllWhatColumns = whatColumns;
            selectByChildNameAllWhereColumns = whereColumns;
        }

        protected void postProcessSelectByChildNamePropertiesFlag() {
            List<Column> whatColumns = new ArrayList<Column>(3);
            List<String> whats = new ArrayList<String>(3);
            List<Column> whereColumns = new ArrayList<Column>(2);
            List<String> wheresProperties = new ArrayList<String>(2);
            List<String> wheresRegular = new ArrayList<String>(2);
            for (Column column : table.getColumns()) {
                String name = column.getName();
                String qname = column.getQuotedName(dialect);
                if (name.equals(model.HIER_PARENT_KEY) ||
                        name.equals(model.HIER_CHILD_NAME_KEY)) {
                    wheresRegular.add(qname + " = ?");
                    wheresProperties.add(qname + " = ?");
                    whereColumns.add(column);
                } else if (name.equals(model.HIER_CHILD_ISPROPERTY_KEY)) {
                    wheresRegular.add(qname + " = " +
                            dialect.toBooleanValueString(false));
                    wheresProperties.add(qname + " = " +
                            dialect.toBooleanValueString(true));
                } else {
                    whats.add(qname);
                    whatColumns.add(column);
                }
            }
            Select select = new Select(dialect);
            select.setWhat(StringUtils.join(whats, ", "));
            select.setFrom(table.getQuotedName(dialect));
            // regular children
            select.setWhere(StringUtils.join(wheresRegular, " AND "));
            selectByChildNameRegularSql = select.getStatement();
            selectByChildNameRegularWhatColumns = whatColumns;
            selectByChildNameRegularWhereColumns = whereColumns;
            // complex properties
            select.setWhere(StringUtils.join(wheresProperties, " AND "));
            selectByChildNamePropertiesSql = select.getStatement();
            selectByChildNamePropertiesWhatColumns = whatColumns;
            selectByChildNamePropertiesWhereColumns = whereColumns;
        }

        protected void postProcessSelectChildrenAll() {
            List<Column> whatColumns = new ArrayList<Column>(4);
            List<String> whats = new ArrayList<String>(4);
            List<Column> whereColumns = null;
            String where = null;
            for (Column column : table.getColumns()) {
                String name = column.getName();
                String qname = column.getQuotedName(dialect);
                if (name.equals(model.HIER_PARENT_KEY)) {
                    where = qname + " = ?";
                    whereColumns = Collections.singletonList(column);
                } else {
                    whats.add(qname);
                    whatColumns.add(column);
                }
            }
            Select select = new Select(dialect);
            select.setWhat(StringUtils.join(whats, ", "));
            select.setFrom(table.getQuotedName(dialect));
            select.setWhere(where);
            selectChildrenAllSql = select.getStatement();
            selectChildrenAllWhatColumns = whatColumns;
            selectChildrenAllWhereColumns = whereColumns;

        }

        protected void postProcessSelectChildrenPropertiesFlag() {
            List<Column> whatColumns = new ArrayList<Column>(3);
            List<String> whats = new ArrayList<String>(3);
            List<Column> whereColumns = null;
            List<String> wheresProperties = new ArrayList<String>(2);
            List<String> wheresRegular = new ArrayList<String>(2);
            for (Column column : table.getColumns()) {
                String name = column.getName();
                String qname = column.getQuotedName(dialect);
                if (name.equals(model.HIER_PARENT_KEY)) {
                    wheresProperties.add(qname + " = ?");
                    wheresRegular.add(qname + " = ?");
                    whereColumns = Collections.singletonList(column);
                } else if (name.equals(model.HIER_CHILD_ISPROPERTY_KEY)) {
                    wheresRegular.add(qname + " = " +
                            dialect.toBooleanValueString(false));
                    wheresProperties.add(qname + " = " +
                            dialect.toBooleanValueString(true));
                } else {
                    whats.add(qname);
                    whatColumns.add(column);
                }
            }
            Select select = new Select(dialect);
            select.setWhat(StringUtils.join(whats, ", "));
            select.setFrom(table.getQuotedName(dialect));
            // regular children
            select.setWhere(StringUtils.join(wheresRegular, " AND "));
            selectChildrenRegularSql = select.getStatement();
            selectChildrenRegularWhatColumns = whatColumns;
            selectChildrenRegularWhereColumns = whereColumns;
            // complex properties
            select.setWhere(StringUtils.join(wheresProperties, " AND "));
            selectChildrenPropertiesSql = select.getStatement();
            selectChildrenPropertiesWhatColumns = whatColumns;
            selectChildrenPropertiesWhereColumns = whereColumns;
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
