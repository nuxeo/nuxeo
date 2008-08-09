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
import java.util.Arrays;
import java.util.Collection;
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
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.IdGenPolicy;
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

    private static final String ORDER_DESC = "DESC";

    private static final String ORDER_ASC = "ASC";

    private final Map<String, String> selectByIdSqlMap; // statement

    private final Map<String, List<Column>> selectByIdColumnsMap; // without ids

    private final Map<String, String> identityFetchSqlMap; // statement

    private final Map<String, Column> identityFetchColumnMap;

    private final Map<String, String> insertSqlMap; // statement

    private final Map<String, List<Column>> insertColumnsMap;

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

    private String selectChildrenIdsAndTypesSql;

    private String selectComplexChildrenIdsAndTypesSql;

    private List<Column> selectChildrenIdsAndTypesWhatColumns;

    private String copyHierSqlExplicitName;

    private String copyHierSqlCreateVersion;

    private String copyHierSql;

    private List<Column> copyHierColumnsExplicitName;

    private List<Column> copyHierColumnsCreateVersion;

    private List<Column> copyHierColumns;

    private Column copyHierWhereColumn;

    private final Map<String, String> copySqlMap;

    private final Map<String, Column> copyIdColumnMap;

    private String selectVersionIdByLabelSql;

    private final List<Column> selectVersionIdByLabelWhereColumns;

    private Column selectVersionIdByLabelWhatColumn;

    protected SQLInfoSelect selectVersionsByLabel;

    protected SQLInfoSelect selectVersionsByVersionable;

    protected SQLInfoSelect selectVersionsByVersionableLastFirst;

    protected SQLInfoSelect selectProxiesByVersionable;

    protected SQLInfoSelect selectProxiesByTarget;

    protected SQLInfoSelect selectChildrenByIsProperty;

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

        selectChildrenIdsAndTypesSql = null;
        selectChildrenIdsAndTypesWhatColumns = null;
        selectComplexChildrenIdsAndTypesSql = null;

        insertSqlMap = new HashMap<String, String>();
        insertColumnsMap = new HashMap<String, List<Column>>();

        updateByIdSqlMap = new HashMap<String, String>();
        updateByIdColumnsMap = new HashMap<String, List<Column>>();

        deleteSqlMap = new HashMap<String, String>();

        copyHierSqlExplicitName = null;
        copyHierSqlCreateVersion = null;
        copyHierSql = null;
        copyHierColumnsExplicitName = null;
        copyHierColumnsCreateVersion = null;
        copyHierColumns = null;
        copyHierWhereColumn = null;
        copySqlMap = new HashMap<String, String>();
        copyIdColumnMap = new HashMap<String, Column>();

        selectVersionIdByLabelSql = null;
        selectVersionIdByLabelWhereColumns = new ArrayList<Column>(2);
        selectVersionIdByLabelWhatColumn = null;

        initSQL();
    }

    // ----- exceptions -----

    public SQLExceptionConverter getSqlExceptionConverter() {
        return sqlExceptionConverter;
    }

    // ----- create whole database -----

    public Database getDatabase() {
        return database;
    }

    public String getTableCreateSql(String tableName) {
        return database.getTable(tableName).getCreateSql(dialect);
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

    public String getSelectChildrenIdsAndTypesSql(boolean onlyComplex) {
        return onlyComplex ? selectComplexChildrenIdsAndTypesSql
                : selectChildrenIdsAndTypesSql;
    }

    public List<Column> getSelectChildrenIdsAndTypesWhatColumns() {
        return selectChildrenIdsAndTypesWhatColumns;
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

    // ----- copy -----

    public String getCopyHierSql(boolean explicitName, boolean createVersion) {
        assert !(explicitName && createVersion);
        return explicitName ? copyHierSqlExplicitName
                : createVersion ? copyHierSqlCreateVersion : copyHierSql;
    }

    public List<Column> getCopyHierColumns(boolean explicitName,
            boolean createVersion) {
        assert !(explicitName && createVersion);
        return explicitName ? copyHierColumnsExplicitName
                : createVersion ? copyHierColumnsCreateVersion
                        : copyHierColumns;
    }

    public Column getCopyHierWhereColumn() {
        return copyHierWhereColumn;
    }

    public String getCopySql(String tableName) {
        return copySqlMap.get(tableName);
    }

    public Column getCopyIdColumn(String tableName) {
        return copyIdColumnMap.get(tableName);
    }

    public String getVersionIdByLabelSql() {
        return selectVersionIdByLabelSql;
    }

    public List<Column> getVersionIdByLabelWhereColumns() {
        return selectVersionIdByLabelWhereColumns;
    }

    public Column getVersionIdByLabelWhatColumn() {
        return selectVersionIdByLabelWhatColumn;
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
            if (tableName.equals(model.MAIN_TABLE_NAME) &&
                    !model.separateMainTable) {
                // merged into already-generated hierarchy
                continue;
            }
            initFragmentSQL(tableName);
        }
    }

    /**
     * Creates the SQL for the table holding global repository information. This
     * includes the id of the hierarchy root node.
     */
    protected void initRepositorySQL() {
        log.debug("Init repository information");
        TableMaker maker = new TableMaker(model.REPOINFO_TABLE_NAME);
        maker.newColumn(model.REPOINFO_REPOID_KEY, PropertyType.LONG,
                Types.INTEGER);
        maker.newPrimaryKey(); // foreign key to main id
        maker.postProcessRepository();
    }

    /**
     * Creates the SQL for the table holding hierarchy information.
     */
    protected void initHierarchySQL() {
        log.debug("Init hierarchy information");

        TableMaker maker = new TableMaker(model.hierFragmentName);
        if (model.separateMainTable) {
            maker.newPrimaryKey();
        } else {
            maker.newId(); // global primary key / generation
        }
        maker.newMainKey(model.HIER_PARENT_KEY);
        maker.newColumn(model.HIER_CHILD_POS_KEY, PropertyType.LONG,
                Types.INTEGER);
        maker.newColumn(model.HIER_CHILD_NAME_KEY, PropertyType.STRING,
                Types.VARCHAR); // text?
        maker.newColumn(model.HIER_CHILD_ISPROPERTY_KEY, PropertyType.BOOLEAN,
                Types.BIT); // not null
        if (!model.separateMainTable) {
            Map<String, PropertyType> fragmentKeysType = model.fragmentsKeysType.get(model.MAIN_TABLE_NAME);
            for (Entry<String, PropertyType> entry : fragmentKeysType.entrySet()) {
                maker.newPrimitiveField(entry.getKey(), entry.getValue());
            }
        }
        maker.postProcess();
        maker.postProcessHierarchy();
        if (!model.separateMainTable) {
            maker.postProcessIdGeneration();
        }
    }

    /**
     * Creates the SQL for one fragment (simple or collection).
     */
    protected void initFragmentSQL(String tableName) {
        TableMaker maker = new TableMaker(tableName);
        boolean isMain = tableName.equals(model.mainFragmentName);

        if (isMain) {
            maker.newId(); // global primary key / generation
        } else {
            maker.newPrimaryKey();
        }

        Map<String, PropertyType> fragmentKeysType = model.fragmentsKeysType.get(tableName);
        for (Entry<String, PropertyType> entry : fragmentKeysType.entrySet()) {
            maker.newPrimitiveField(entry.getKey(), entry.getValue());
        }

        maker.postProcess();
        if (isMain) {
            maker.postProcessIdGeneration();
        }
        if (tableName.equals(model.VERSION_TABLE_NAME)) {
            selectVersionsByLabel = new SQLInfoSelect(maker,
                    model.VERSION_VERSIONABLE_KEY, model.VERSION_LABEL_KEY);
            selectVersionsByVersionable = new SQLInfoSelect(maker,
                    model.VERSION_VERSIONABLE_KEY);
            String[] createdDesc = new String[] { model.VERSION_CREATED_KEY,
                    ORDER_DESC };
            selectVersionsByVersionableLastFirst = new SQLInfoSelect(maker,
                    createdDesc, model.VERSION_VERSIONABLE_KEY);
        }
        if (tableName.equals(model.PROXY_TABLE_NAME)) {
            maker.postProcessSelectProxies();
            selectProxiesByVersionable = new SQLInfoSelect(maker,
                    model.PROXY_VERSIONABLE_KEY);
            selectProxiesByTarget = new SQLInfoSelect(maker,
                    model.PROXY_TARGET_KEY);
        }
    }

    // ----- prepare one table -----

    protected class TableMaker {

        private final Dialect dialect;

        private final String tableName;

        private final Table table;

        private final List<String> orderBy;

        protected TableMaker(String tableName) {
            this.dialect = SQLInfo.this.dialect;
            this.tableName = tableName;
            table = new Table(tableName);
            database.addTable(table);
            orderBy = model.collectionOrderBy.get(tableName);
        }

        protected Column newMainKey(String name) {
            Column column;
            switch (model.idGenPolicy) {
            case APP_UUID:
                column = newColumn(name, PropertyType.STRING, Types.VARCHAR);
                column.setLength(36);
                break;
            case DB_IDENTITY:
                column = newColumn(name, PropertyType.LONG, Types.BIGINT);
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
            if (tableName.equals(model.VERSION_TABLE_NAME) &&
                    key.equals(model.VERSION_VERSIONABLE_KEY)) {
                newMainKey(key); // not a foreign key though
                return;
            }
            int sqlType;
            switch (type) {
            case STRING:
                // hack, make this more configurable
                if (tableName.equals(model.VERSION_TABLE_NAME) &&
                        key.equals(model.VERSION_LABEL_KEY)) {
                    sqlType = Types.VARCHAR;
                } else {
                    sqlType = Types.CLOB; // or VARCHAR for system tables?
                }
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
                // TODO depends on repository conf for blob storage, also
                // depends on Column implementation
                sqlType = Types.VARCHAR;
                break;
            default:
                throw new RuntimeException("Bad type: " + type);
            }
            newColumn(key, type, sqlType);
            // XXX apply defaults
        }

        protected Column newColumn(String key, PropertyType type, int sqlType) {
            String columnName = key;
            Column column = new Column(columnName, type, sqlType, key, model);
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
         * schema table, or a collection table.
         */
        protected void postProcess() {
            postProcessSelectById();
            postProcessInsert();
            postProcessUpdateById();
            postProcessDelete();
            postProcessCopy();
        }

        /**
         * Additional SQL for the main table.
         */
        protected void postProcessIdGeneration() {
            switch (model.idGenPolicy) {
            case APP_UUID:
                break;
            case DB_IDENTITY:
                postProcessIdentityFetch();
                break;
            default:
                throw new AssertionError(model.idGenPolicy);
            }
        }

        /**
         * Additional SQL for the hierarchy table.
         */
        protected void postProcessHierarchy() {
            postProcessSelectByChildNameAll();
            postProcessSelectByChildNamePropertiesFlag();
            postProcessSelectChildrenIdsAndTypes();
            postProcessCopyHier();

            selectChildrenByIsProperty = new SQLInfoSelect(this,
                    model.HIER_PARENT_KEY, model.HIER_CHILD_ISPROPERTY_KEY);
        }

        protected void postProcessSelectById() {
            List<Column> selectByIdColumns = new LinkedList<Column>();
            List<String> whats = new LinkedList<String>();
            List<String> wheres = new LinkedList<String>();
            for (Column column : table.getColumns()) {
                String qname = column.getQuotedName(dialect);
                if (column.isPrimary()) {
                    wheres.add(qname + " = ?");
                } else {
                    whats.add(qname);
                    selectByIdColumns.add(column);
                }
            }
            Select select = new Select(dialect);
            select.setWhat(StringUtils.join(whats, ", "));
            select.setFrom(table.getQuotedName(dialect));
            select.setWhere(StringUtils.join(wheres, " AND "));
            if (orderBy != null) {
                List<String> order = new LinkedList<String>();
                for (String name : orderBy) {
                    order.add(table.getColumn(name).getQuotedName(dialect));
                }
                select.setOrderBy(StringUtils.join(order, ", "));
            }
            selectByIdSqlMap.put(tableName, select.getStatement());
            selectByIdColumnsMap.put(tableName, selectByIdColumns);
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

        // children ids and types
        protected void postProcessSelectChildrenIdsAndTypes() {
            assert !model.separateMainTable; // otherwise join needed
            ArrayList<Column> whatColumns = new ArrayList<Column>(2);
            ArrayList<String> whats = new ArrayList<String>(2);
            Column column = table.getColumn(model.MAIN_KEY);
            whatColumns.add(column);
            whats.add(column.getQuotedName(dialect));
            column = table.getColumn(model.MAIN_PRIMARY_TYPE_KEY);
            whatColumns.add(column);
            whats.add(column.getQuotedName(dialect));
            Select select = new Select(dialect);
            select.setWhat(StringUtils.join(whats, ", "));
            select.setFrom(table.getQuotedName(dialect));
            String where = table.getColumn(model.HIER_PARENT_KEY).getQuotedName(
                    dialect) +
                    " = ?";
            select.setWhere(where);
            selectChildrenIdsAndTypesSql = select.getStatement();
            selectChildrenIdsAndTypesWhatColumns = whatColumns;
            // now only complex properties
            where += " AND " +
                    table.getColumn(model.HIER_CHILD_ISPROPERTY_KEY).getQuotedName(
                            dialect) + " = " +
                    dialect.toBooleanValueString(true);
            select.setWhere(where);
            selectComplexChildrenIdsAndTypesSql = select.getStatement();
        }

        // TODO optimize multiple inserts into one statement for collections
        protected void postProcessInsert() {
            // insert (implicitly auto-generated sequences not included)
            Collection<Column> columns = table.getColumns();
            List<Column> insertColumns = new ArrayList<Column>(columns.size());
            Insert insert = new Insert(dialect);
            insert.setTable(table);
            for (Column column : columns) {
                if (column.isIdentity()) {
                    // identity column is never inserted
                    continue;
                }
                insertColumns.add(column);
                insert.addColumn(column);
            }
            insertSqlMap.put(tableName, insert.getStatement());
            insertColumnsMap.put(tableName, insertColumns);
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

        // copy, with or without explicit name
        protected void postProcessCopyHier() {
            Collection<Column> columns = table.getColumns();
            List<String> selectWhats = new ArrayList<String>(columns.size());
            List<String> selectWhatsExplicitName = new ArrayList<String>(
                    columns.size());
            List<String> selectWhatsCreateVersion = new ArrayList<String>(
                    columns.size());
            copyHierColumns = new ArrayList<Column>(2);
            copyHierColumnsExplicitName = new ArrayList<Column>(3);
            copyHierColumnsCreateVersion = new ArrayList<Column>(3);
            Insert insert = new Insert(dialect);
            insert.setTable(table);
            for (Column column : columns) {
                if (column.isIdentity()) {
                    // identity column is never copied
                    continue;
                }
                insert.addColumn(column);
                String quotedName = column.getQuotedName(dialect);
                String key = column.getName();
                if (key.equals(model.MAIN_KEY) ||
                        key.equals(model.HIER_PARENT_KEY)) {
                    // explicit id/parent value (id if not identity column)
                    selectWhats.add("?");
                    copyHierColumns.add(column);
                    selectWhatsExplicitName.add("?");
                    copyHierColumnsExplicitName.add(column);
                    selectWhatsCreateVersion.add("?");
                    copyHierColumnsCreateVersion.add(column);
                } else if (key.equals(model.HIER_CHILD_NAME_KEY)) {
                    selectWhats.add(quotedName);
                    // exlicit name value if requested
                    selectWhatsExplicitName.add("?");
                    copyHierColumnsExplicitName.add(column);
                    // version creation copies name
                    selectWhatsCreateVersion.add(quotedName);
                } else if (key.equals(model.MAIN_BASE_VERSION_KEY) ||
                        key.equals(model.MAIN_CHECKED_IN_KEY)) {
                    selectWhats.add(quotedName);
                    selectWhatsExplicitName.add(quotedName);
                    // version creation sets those null
                    selectWhatsCreateVersion.add("?");
                    copyHierColumnsCreateVersion.add(column);
                } else {
                    // otherwise copy value
                    selectWhats.add(quotedName);
                    selectWhatsExplicitName.add(quotedName);
                    selectWhatsCreateVersion.add(quotedName);
                }
            }
            copyHierWhereColumn = table.getColumn(model.MAIN_KEY);
            Select select = new Select(dialect);
            select.setFrom(table.getQuotedName(dialect));
            select.setWhere(copyHierWhereColumn.getQuotedName(dialect) + " = ?");
            // without explicit name nor version creation (normal)
            select.setWhat(StringUtils.join(selectWhats, ", "));
            insert.setValues(select.getStatement());
            copyHierSql = insert.getStatement();
            // with explicit name
            select.setWhat(StringUtils.join(selectWhatsExplicitName, ", "));
            insert.setValues(select.getStatement());
            copyHierSqlExplicitName = insert.getStatement();
            // with version creation
            select.setWhat(StringUtils.join(selectWhatsCreateVersion, ", "));
            insert.setValues(select.getStatement());
            copyHierSqlCreateVersion = insert.getStatement();
        }

        // copy of a fragment
        protected void postProcessCopy() {
            String tableName = table.getName();
            Collection<Column> columns = table.getColumns();
            List<String> selectWhats = new ArrayList<String>(columns.size());
            Column copyIdColumn = table.getColumn(model.MAIN_KEY);
            Insert insert = new Insert(dialect);
            insert.setTable(table);
            for (Column column : columns) {
                if (column.isIdentity()) {
                    // identity column is never copied
                    continue;
                }
                insert.addColumn(column);
                if (column == copyIdColumn) {
                    // explicit value
                    selectWhats.add("?");
                } else {
                    // otherwise copy value
                    selectWhats.add(column.getQuotedName(dialect));
                }
            }
            Select select = new Select(dialect);
            select.setWhat(StringUtils.join(selectWhats, ", "));
            select.setFrom(table.getQuotedName(dialect));
            select.setWhere(copyIdColumn.getQuotedName(dialect) + " = ?");
            insert.setValues(select.getStatement());
            copySqlMap.put(tableName, insert.getStatement());
            copyIdColumnMap.put(tableName, copyIdColumn);
        }

        public void postProcessSelectProxies() {
            /*
             * normal/proxy: SELECT ... FROM proxies WHERE versionable = ?
             *
             * version: SELECT ... FROM proxies WHERE target = ?
             *
             * with parent: SELECT ... FROM proxies, hierarchy WHERE proxies.id
             * = hierarchy.id AND hierarchy.parentid = ? AND
             * proxies.versionable/target = ?
             */
            List<Column> whatColumns = new ArrayList<Column>(2);
            List<String> whats = new ArrayList<String>(2);
            Column whereColumn = table.getColumn(model.PROXY_VERSIONABLE_KEY);
            for (Column column : table.getColumns()) {
                if (column != whereColumn) {
                    whatColumns.add(column);
                    whats.add(column.getQuotedName(dialect));
                }
            }
            Select select = new Select(dialect);
            select.setWhat(StringUtils.join(whats, ", "));
            select.setFrom(table.getQuotedName(dialect));
            select.setWhere(whereColumn.getQuotedName(dialect) + " = ?");
            //selectProxiesSql = select.getStatement();
            //selectProxiesWhereColumn = whereColumn;
            //selectProxiesWhatColumns = whatColumns;
        }

    }

    protected static class SQLInfoSelect {

        public final String sql;

        public final List<Column> whatColumns;

        public final List<Column> whereColumns;

        public SQLInfoSelect(TableMaker maker, String... freeColumns) {
            this(maker, new String[0], freeColumns);
        }

        public SQLInfoSelect(TableMaker maker, String[] orderBys,
                String... freeColumns) {
            List<String> freeColumnsList = Arrays.asList(freeColumns);
            whatColumns = new ArrayList<Column>();
            whereColumns = new ArrayList<Column>();
            List<String> whats = new ArrayList<String>();
            List<String> wheres = new ArrayList<String>();
            for (Column column : maker.table.getColumns()) {
                String qname = column.getQuotedName(maker.dialect);
                if (freeColumnsList.contains(column.getName())) {
                    whereColumns.add(column);
                    wheres.add(qname + " = ?");
                } else {
                    whatColumns.add(column);
                    whats.add(qname);
                }
            }
            Select select = new Select(maker.dialect);
            select.setWhat(StringUtils.join(whats, ", "));
            select.setFrom(maker.table.getQuotedName(maker.dialect));
            select.setWhere(StringUtils.join(wheres, " AND "));
            List<String> orders = new LinkedList<String>();
            for (int i = 0; i < orderBys.length; i++) {
                String name = orderBys[i++];
                String ascdesc = orderBys[i].equals(ORDER_DESC) ? " " +
                        ORDER_DESC : "";
                orders.add(maker.table.getColumn(name).getQuotedName(
                        maker.dialect) +
                        ascdesc);
            }
            select.setOrderBy(StringUtils.join(orders, ", "));
            sql = select.getStatement();
        }
    }

}
