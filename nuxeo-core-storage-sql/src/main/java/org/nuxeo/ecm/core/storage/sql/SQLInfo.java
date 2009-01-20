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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.exception.SQLExceptionConverter;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.db.Column;
import org.nuxeo.ecm.core.storage.sql.db.Database;
import org.nuxeo.ecm.core.storage.sql.db.Delete;
import org.nuxeo.ecm.core.storage.sql.db.Dialect;
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

    protected final Dialect dialect;

    private final SQLExceptionConverter sqlExceptionConverter;

    public final Database database;

    private String selectRootIdSql;

    private Column selectRootIdWhatColumn;

    private static final String ORDER_DESC = "DESC";

    private static final String ORDER_ASC = "ASC";

    private final Map<String, String> identityFetchSqlMap; // statement

    private final Map<String, Column> identityFetchColumnMap;

    private final Map<String, String> insertSqlMap; // statement

    private final Map<String, List<Column>> insertColumnsMap;

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

    private final String selectVersionIdByLabelSql;

    private final List<Column> selectVersionIdByLabelWhereColumns;

    private final Column selectVersionIdByLabelWhatColumn;

    protected final Map<String, SQLInfoSelect> selectFragmentById;

    protected SQLInfoSelect selectVersionsByLabel;

    protected SQLInfoSelect selectVersionsByVersionable;

    protected SQLInfoSelect selectVersionsByVersionableLastFirst;

    protected SQLInfoSelect selectProxiesByVersionable;

    protected SQLInfoSelect selectProxiesByTarget;

    protected SQLInfoSelect selectChildrenByIsProperty;

    protected SQLInfoSelect selectProxiesByVersionableAndParent;

    protected SQLInfoSelect selectProxiesByTargetAndParent;

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

        database = new Database(dialect);

        selectRootIdSql = null;
        selectRootIdWhatColumn = null;

        selectFragmentById = new HashMap<String, SQLInfoSelect>();
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

    public SQLExceptionConverter getSqlExceptionConverter() {
        return sqlExceptionConverter;
    }

    public Database getDatabase() {
        return database;
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

    // TODO these two methods are redundant with one another

    public SQLInfoSelect getUpdateById(String tableName, Collection<String> keys) {
        Table table = database.getTable(tableName);
        List<String> values = new LinkedList<String>();
        List<Column> columns = new LinkedList<Column>();
        Column mainColumn = table.getColumn(model.MAIN_KEY);
        for (String key : keys) {
            Column column = table.getColumn(key);
            values.add(column.getQuotedName() + " = " +
                    column.getFreeVariableSetter());
            columns.add(column);
        }
        columns.add(mainColumn);
        Update update = new Update(table);
        update.setNewValues(StringUtils.join(values, ", "));
        update.setWhere(mainColumn.getQuotedName() + " = ?");
        return new SQLInfoSelect(update.getStatement(), columns, null, null);
    }

    public Update getUpdateByIdForKeys(String tableName, Set<String> keys) {
        Table table = database.getTable(tableName);
        List<String> values = new ArrayList<String>(keys.size());
        for (String key : keys) {
            Column column = table.getColumn(key);
            values.add(column.getQuotedName() + " = " +
                    column.getFreeVariableSetter());
        }
        Update update = new Update(table);
        update.setNewValues(StringUtils.join(values, ", "));
        update.setWhere(table.getColumn(model.MAIN_KEY).getQuotedName() +
                " = ?");
        return update;
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
        initHierarchySQL();
        initRepositorySQL();

        for (String tableName : model.getFragmentNames()) {
            if (tableName.equals(model.HIER_TABLE_NAME)) {
                continue;
            }
            if (tableName.equals(model.MAIN_TABLE_NAME) &&
                    !model.separateMainTable) {
                // merged into already-generated hierarchy
                continue;
            }
            initFragmentSQL(tableName);
        }

        /*
         * versions
         */
        Table table = database.getTable(model.VERSION_TABLE_NAME);
        selectVersionsByLabel = makeSelect(table,
                model.VERSION_VERSIONABLE_KEY, model.VERSION_LABEL_KEY);
        table.addIndex(model.VERSION_VERSIONABLE_KEY);
        // don't index versionable+label, a simple label scan will suffice
        selectVersionsByVersionable = makeSelect(table, new String[] {
                model.VERSION_CREATED_KEY, ORDER_ASC },
                model.VERSION_VERSIONABLE_KEY);
        selectVersionsByVersionableLastFirst = makeSelect(table, new String[] {
                model.VERSION_CREATED_KEY, ORDER_DESC },
                model.VERSION_VERSIONABLE_KEY);

        /*
         * proxies
         */
        table = database.getTable(model.PROXY_TABLE_NAME);
        Table hierTable = database.getTable(model.hierTableName);
        selectProxiesByVersionable = makeSelect(table,
                model.PROXY_VERSIONABLE_KEY);
        table.addIndex(model.PROXY_VERSIONABLE_KEY);
        selectProxiesByTarget = makeSelect(table, model.PROXY_TARGET_KEY);
        table.addIndex(model.PROXY_TARGET_KEY);
        selectProxiesByVersionableAndParent = makeSelect(table,
                new String[] { model.PROXY_VERSIONABLE_KEY }, hierTable,
                new String[] { model.HIER_PARENT_KEY });

        selectProxiesByTargetAndParent = makeSelect(table,
                new String[] { model.PROXY_TARGET_KEY }, hierTable,
                new String[] { model.HIER_PARENT_KEY });
    }

    /**
     * Creates the SQL for the table holding global repository information. This
     * includes the id of the hierarchy root node.
     */
    protected void initRepositorySQL() {
        TableMaker maker = new TableMaker(model.REPOINFO_TABLE_NAME);
        maker.newPrimaryKey(); // foreign key to main id
        maker.newColumn(model.REPOINFO_REPONAME_KEY, PropertyType.STRING,
                Types.VARCHAR);
        maker.postProcessRepository();
    }

    /**
     * Creates the SQL for the table holding hierarchy information.
     */
    protected void initHierarchySQL() {
        TableMaker maker = new TableMaker(model.hierTableName);
        if (model.separateMainTable) {
            maker.newPrimaryKey();
        } else {
            maker.newId(); // global primary key / generation
        }
        maker.newMainKeyReference(model.HIER_PARENT_KEY, true);
        maker.newColumn(model.HIER_CHILD_POS_KEY, PropertyType.LONG,
                Types.INTEGER);
        maker.newColumn(model.HIER_CHILD_NAME_KEY, PropertyType.STRING,
                Types.VARCHAR); // text?
        maker.newColumn(model.HIER_CHILD_ISPROPERTY_KEY, PropertyType.BOOLEAN,
                Types.BIT); // not null
        if (!model.separateMainTable) {
            maker.newFragmentFields();
        }
        maker.postProcess();
        maker.postProcessHierarchy();
        if (!model.separateMainTable) {
            maker.postProcessIdGeneration();
        }

        maker.table.addIndex(model.HIER_PARENT_KEY);
        maker.table.addIndex(model.HIER_PARENT_KEY, model.HIER_CHILD_NAME_KEY);
        // don't index parent+name+isprop, a simple isprop scan will suffice
    }

    /**
     * Creates the SQL for one fragment (simple or collection).
     */
    protected void initFragmentSQL(String tableName) {
        TableMaker maker = new TableMaker(tableName);
        boolean isMain = tableName.equals(model.mainTableName);

        if (isMain) {
            maker.newId(); // global primary key / generation
        } else {
            if (model.isCollectionFragment(tableName)) {
                maker.newMainKeyReference(model.MAIN_KEY, false);
                maker.table.addIndex(model.MAIN_KEY);
            } else {
                maker.newPrimaryKey();
            }
        }

        maker.newFragmentFields();

        maker.postProcess();
        if (isMain) {
            maker.postProcessIdGeneration();
        }
    }

    // ----- prepare one table -----

    protected class TableMaker {

        private final String tableName;

        private final Table table;

        private final String orderBy;

        protected TableMaker(String tableName) {
            this.tableName = tableName;
            table = database.addTable(tableName);
            orderBy = model.getCollectionOrderBy(tableName);
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

        protected Column newMainKeyReference(String name, boolean nullable) {
            Column column = newMainKey(name);
            column.setReferences(database.getTable(model.mainTableName),
                    model.MAIN_KEY);
            column.setNullable(nullable);
            return column;
        }

        protected void newPrimaryKey() {
            Column column = newMainKeyReference(model.MAIN_KEY, false);
            column.setPrimary(true);
        }

        protected void newId() {
            Column column = newMainKey(model.MAIN_KEY);
            switch (model.idGenPolicy) {
            case APP_UUID:
                break;
            case DB_IDENTITY:
                column.setIdentity(true);
                break;
            default:
                throw new AssertionError(model.idGenPolicy);
            }
            column.setNullable(false);
            column.setPrimary(true);
        }

        protected void newFragmentFields() {
            Map<String, PropertyType> keysType = model.getFragmentKeysType(tableName);
            for (Entry<String, PropertyType> entry : keysType.entrySet()) {
                newPrimitiveField(entry.getKey(), entry.getValue());
            }
        }

        protected void newPrimitiveField(String key, PropertyType type) {
            // TODO find a way to put these exceptions in model
            if (tableName.equals(model.VERSION_TABLE_NAME) &&
                    key.equals(model.VERSION_VERSIONABLE_KEY)) {
                newMainKeyReference(key, true);
                return;
            }
            // TODO XXX also MAIN_BASE_VERSION_KEY is main key
            if (tableName.equals(model.PROXY_TABLE_NAME)) {
                if (key.equals(model.PROXY_TARGET_KEY)) {
                    newMainKeyReference(key, true);
                    return;
                }
                if (key.equals(model.PROXY_VERSIONABLE_KEY)) {
                    newMainKey(key); // not a foreign key
                    return;
                }
            }
            int sqlType;
            switch (type) {
            case STRING:
                // hack, make this more configurable
                if (tableName.equals(model.VERSION_TABLE_NAME) &&
                        key.equals(model.VERSION_LABEL_KEY)) {
                    // these are columns that need to be searchable, as some
                    // databases (Derby) don't allow matches on CLOB columns
                    sqlType = Types.VARCHAR;
                } else if (tableName.equals(model.mainTableName) ||
                        tableName.equals(model.ACL_TABLE_NAME) ||
                        tableName.equals(model.MISC_TABLE_NAME)) {
                    // or VARCHAR for system tables // TODO size?
                    sqlType = Types.VARCHAR;
                } else if (tableName.equals(model.FULLTEXT_TABLE_NAME)) {
                    sqlType = Column.ExtendedTypes.FULLTEXT;
                } else {
                    sqlType = Types.CLOB;
                }
                break;
            case BOOLEAN:
                sqlType = Types.BIT; // many databases don't know BOOLEAN
                // turned into SMALLINT by Derby
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
            Column column = newColumn(key, type, sqlType);
            if (type == PropertyType.BINARY) {
                // log them, will be useful for GC of binaries
                SQLInfo.log.info("Binary column: " + column.getFullQuotedName());
            }
            // XXX apply defaults
        }

        protected Column newColumn(String key, PropertyType type, int sqlType) {
            String columnName = key;
            Column column = table.addColumn(columnName, type, sqlType, key,
                    model);
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
                String key = column.getKey();
                String qname = column.getQuotedName();
                if (key.equals(model.MAIN_KEY)) {
                    what = qname;
                    selectRootIdWhatColumn = column;
                } else if (key.equals(model.REPOINFO_REPONAME_KEY)) {
                    where = qname + " = ?";
                } else {
                    throw new AssertionError(column);
                }
            }
            Select select = new Select(table);
            select.setWhat(what);
            select.setFrom(table.getQuotedName());
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

            selectChildrenByIsProperty = makeSelect(table,
                    model.HIER_PARENT_KEY, model.HIER_CHILD_ISPROPERTY_KEY);
        }

        protected void postProcessSelectById() {
            String[] orderBys = orderBy == null ? new String[0] : new String[] {
                    orderBy, ORDER_ASC };
            SQLInfoSelect select = makeSelect(table, orderBys, model.MAIN_KEY);
            selectFragmentById.put(tableName, select);
        }

        protected void postProcessSelectByChildNameAll() {
            List<Column> whatColumns = new ArrayList<Column>(3);
            List<String> whats = new ArrayList<String>(3);
            List<Column> whereColumns = new ArrayList<Column>(2);
            List<String> wheres = new ArrayList<String>(2);
            for (Column column : table.getColumns()) {
                String key = column.getKey();
                String qname = column.getQuotedName();
                if (key.equals(model.HIER_PARENT_KEY) ||
                        key.equals(model.HIER_CHILD_NAME_KEY)) {
                    wheres.add(qname + " = ?");
                    whereColumns.add(column);
                } else {
                    whats.add(qname);
                    whatColumns.add(column);
                }
            }
            Select select = new Select(table);
            select.setWhat(StringUtils.join(whats, ", "));
            select.setFrom(table.getQuotedName());
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
                String key = column.getKey();
                String qname = column.getQuotedName();
                if (key.equals(model.HIER_PARENT_KEY) ||
                        key.equals(model.HIER_CHILD_NAME_KEY)) {
                    wheresRegular.add(qname + " = ?");
                    wheresProperties.add(qname + " = ?");
                    whereColumns.add(column);
                } else if (key.equals(model.HIER_CHILD_ISPROPERTY_KEY)) {
                    wheresRegular.add(qname + " = " +
                            dialect.toBooleanValueString(false));
                    wheresProperties.add(qname + " = " +
                            dialect.toBooleanValueString(true));
                } else {
                    whats.add(qname);
                    whatColumns.add(column);
                }
            }
            Select select = new Select(table);
            select.setWhat(StringUtils.join(whats, ", "));
            select.setFrom(table.getQuotedName());
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
            List<Column> whatColumns = new ArrayList<Column>(2);
            List<String> whats = new ArrayList<String>(2);
            Column column = table.getColumn(model.MAIN_KEY);
            whatColumns.add(column);
            whats.add(column.getQuotedName());
            column = table.getColumn(model.MAIN_PRIMARY_TYPE_KEY);
            whatColumns.add(column);
            whats.add(column.getQuotedName());
            Select select = new Select(table);
            select.setWhat(StringUtils.join(whats, ", "));
            select.setFrom(table.getQuotedName());
            String where = table.getColumn(model.HIER_PARENT_KEY).getQuotedName() +
                    " = ?";
            select.setWhere(where);
            selectChildrenIdsAndTypesSql = select.getStatement();
            selectChildrenIdsAndTypesWhatColumns = whatColumns;
            // now only complex properties
            where += " AND " +
                    table.getColumn(model.HIER_CHILD_ISPROPERTY_KEY).getQuotedName() +
                    " = " + dialect.toBooleanValueString(true);
            select.setWhere(where);
            selectComplexChildrenIdsAndTypesSql = select.getStatement();
        }

        // TODO optimize multiple inserts into one statement for collections
        protected void postProcessInsert() {
            // insert (implicitly auto-generated sequences not included)
            Collection<Column> columns = table.getColumns();
            List<Column> insertColumns = new ArrayList<Column>(columns.size());
            Insert insert = new Insert(table);
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
                    sql = dialect.getIdentitySelectString(tableName,
                            column.getPhysicalName(), column.getSqlType());
                    identityColumn = column;
                    break; // only one identity per table
                }
            }
            identityFetchSqlMap.put(tableName, sql);
            identityFetchColumnMap.put(tableName, identityColumn);
        }

        protected void postProcessDelete() {
            Delete delete = new Delete(table);
            List<String> wheres = new LinkedList<String>();
            for (Column column : table.getColumns()) {
                if (column.getKey().equals(model.MAIN_KEY)) {
                    wheres.add(column.getQuotedName() + " = ?");
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
            Insert insert = new Insert(table);
            for (Column column : columns) {
                if (column.isIdentity()) {
                    // identity column is never copied
                    continue;
                }
                insert.addColumn(column);
                String quotedName = column.getQuotedName();
                String key = column.getKey();
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
            Select select = new Select(table);
            select.setFrom(table.getQuotedName());
            select.setWhere(copyHierWhereColumn.getQuotedName() + " = ?");
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
        // INSERT INTO foo (id, x, y) SELECT ?, x, y FROM foo WHERE id = ?
        protected void postProcessCopy() {
            Collection<Column> columns = table.getColumns();
            List<String> selectWhats = new ArrayList<String>(columns.size());
            Column copyIdColumn = table.getColumn(model.MAIN_KEY);
            Insert insert = new Insert(table);
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
                    selectWhats.add(column.getQuotedName());
                }
            }
            Select select = new Select(table);
            select.setWhat(StringUtils.join(selectWhats, ", "));
            select.setFrom(table.getQuotedName());
            select.setWhere(copyIdColumn.getQuotedName() + " = ?");
            insert.setValues(select.getStatement());
            copySqlMap.put(tableName, insert.getStatement());
            copyIdColumnMap.put(tableName, copyIdColumn);
        }

    }

    public static class SQLInfoSelect {

        public final String sql;

        public final List<Column> whatColumns;

        public final List<Column> whereColumns;

        public final List<Column> opaqueColumns;

        public SQLInfoSelect(String sql, List<Column> whatColumns,
                List<Column> whereColumns, List<Column> opaqueColumns) {
            this.sql = sql;
            this.whatColumns = new ArrayList<Column>(whatColumns);
            this.whereColumns = whereColumns == null ? null
                    : new ArrayList<Column>(whereColumns);
            this.opaqueColumns = opaqueColumns == null ? null
                    : new ArrayList<Column>(opaqueColumns);
        }
    }

    /**
     * Basic SELECT x, y, z FROM table WHERE a = ? AND b = ?
     */
    public SQLInfoSelect makeSelect(Table table, String... freeColumns) {
        String[] orderBys = new String[0];
        return makeSelect(table, orderBys, freeColumns);
    }

    /**
     * Basic SELECT with optional ORDER BY x, y DESC
     */
    public SQLInfoSelect makeSelect(Table table, String[] orderBys,
            String... freeColumns) {
        List<String> freeColumnsList = Arrays.asList(freeColumns);
        List<Column> whatColumns = new LinkedList<Column>();
        List<Column> whereColumns = new LinkedList<Column>();
        List<Column> opaqueColumns = new LinkedList<Column>();
        List<String> whats = new LinkedList<String>();
        List<String> wheres = new LinkedList<String>();
        for (Column column : table.getColumns()) {
            String qname = column.getQuotedName();
            if (freeColumnsList.contains(column.getKey())) {
                whereColumns.add(column);
                wheres.add(qname + " = ?");
            } else if (column.isOpaque()) {
                opaqueColumns.add(column);
            } else {
                whatColumns.add(column);
                whats.add(qname);
            }
        }
        if (whats.isEmpty()) {
            // only opaque columns, don't generate an illegal SELECT
            whats.add(table.getColumn(model.MAIN_KEY).getQuotedName());
        }
        Select select = new Select(table);
        select.setWhat(StringUtils.join(whats, ", "));
        select.setFrom(table.getQuotedName());
        select.setWhere(StringUtils.join(wheres, " AND "));
        List<String> orders = new LinkedList<String>();
        for (int i = 0; i < orderBys.length; i++) {
            String name = orderBys[i++];
            String ascdesc = orderBys[i].equals(ORDER_DESC) ? " " + ORDER_DESC
                    : "";
            orders.add(table.getColumn(name).getQuotedName() + ascdesc);
        }
        select.setOrderBy(StringUtils.join(orders, ", "));
        return new SQLInfoSelect(select.getStatement(), whatColumns,
                whereColumns, opaqueColumns.isEmpty() ? null : opaqueColumns);
    }

    /**
     * Joining SELECT T.x, T.y, T.z FROM T, U WHERE T.id = U.id AND T.a = ? and
     * U.b = ?
     */
    public SQLInfoSelect makeSelect(Table table, String[] freeColumns,
            Table joinTable, String[] joinCriteria) {
        List<String> freeColumnsList = Arrays.asList(freeColumns);
        List<Column> whatColumns = new LinkedList<Column>();
        List<Column> whereColumns = new LinkedList<Column>();
        List<Column> opaqueColumns = new LinkedList<Column>();
        List<String> whats = new LinkedList<String>();
        List<String> wheres = new LinkedList<String>();
        String join = table.getColumn(model.MAIN_KEY).getFullQuotedName() +
                " = " + joinTable.getColumn(model.MAIN_KEY).getFullQuotedName();
        wheres.add(join);
        for (Column column : table.getColumns()) {
            String qname = column.getFullQuotedName();
            if (freeColumnsList.contains(column.getKey())) {
                whereColumns.add(column);
                wheres.add(qname + " = ?");
            } else if (column.isOpaque()) {
                opaqueColumns.add(column);
            } else {
                whatColumns.add(column);
                whats.add(qname);
            }
        }
        if (whats.isEmpty()) {
            // only opaque columns, don't generate an illegal SELECT
            whats.add(table.getColumn(model.MAIN_KEY).getQuotedName());
        }
        for (String name : joinCriteria) {
            Column column = joinTable.getColumn(name);
            whereColumns.add(column);
            wheres.add(column.getFullQuotedName() + " = ?");
        }
        Select select = new Select(table);
        select.setWhat(StringUtils.join(whats, ", "));
        select.setFrom(table.getQuotedName() + ", " + joinTable.getQuotedName());
        select.setWhere(StringUtils.join(wheres, " AND "));
        return new SQLInfoSelect(select.getStatement(), whatColumns,
                whereColumns, opaqueColumns.isEmpty() ? null : opaqueColumns);
    }

    /**
     * Class to hold info about a stored procedure and how to create it.
     */
    public static class StoredProcedureInfo {

        /**
         * If {@code null}, use {@link #checkDropStatement} to check if drop or
         * not.
         */
        public final Boolean dropFlag;

        /** If this statement's execution returns something, then do the drop. */
        public final String checkDropStatement;

        public final String dropStatement;

        public final String createStatement;

        public StoredProcedureInfo(Boolean dropFlag, String checkDropStatement,
                String dropStatement, String createStatement) {
            this.dropFlag = dropFlag;
            this.checkDropStatement = checkDropStatement;
            this.dropStatement = dropStatement;
            this.createStatement = createStatement;
        }
    }

    public class DerbyStoredProcedureInfoMaker {

        public final String idType;

        public final String methodSuffix;

        public final String className = "org.nuxeo.ecm.core.storage.sql.db.DerbyFunctions";

        public DerbyStoredProcedureInfoMaker() {
            switch (model.idGenPolicy) {
            case APP_UUID:
                idType = "VARCHAR(36)";
                methodSuffix = "String";
                break;
            case DB_IDENTITY:
                idType = "INTEGER";
                methodSuffix = "Long";
                break;
            default:
                throw new AssertionError(model.idGenPolicy);
            }
        }

        public StoredProcedureInfo makeInTree() {
            return makeFunction("NX_IN_TREE",
                    "(ID %s, BASEID %<s) RETURNS SMALLINT", "isInTree" +
                            methodSuffix, "READS SQL DATA");
        }

        public StoredProcedureInfo makeParseFullText() {
            return makeFunction(
                    "NX_PARSE_FULLTEXT",
                    "(S1 VARCHAR(10000), S2 VARCHAR(10000)) RETURNS VARCHAR(10000)",
                    "parseFullText", "");
        }

        public StoredProcedureInfo makeContainsFullText() {
            return makeFunction(
                    "NX_CONTAINS",
                    "(FT VARCHAR(10000), QUERY VARCHAR(10000)) RETURNS SMALLINT",
                    "matchesFullTextDerby", "");
        }

        public StoredProcedureInfo makeFTInsertTrigger() {
            Table ft = database.getTable(model.FULLTEXT_TABLE_NAME);
            Column ftft = ft.getColumn(model.FULLTEXT_FULLTEXT_KEY);
            Column ftst = ft.getColumn(model.FULLTEXT_SIMPLETEXT_KEY);
            Column ftbt = ft.getColumn(model.FULLTEXT_BINARYTEXT_KEY);
            Column ftid = ft.getColumn(model.MAIN_KEY);
            return makeTrigger(
                    "NX_TRIG_FT_INSERT", //
                    String.format(
                            "AFTER INSERT ON %1$s "//
                                    + "REFERENCING NEW AS NEW " //
                                    + "FOR EACH ROW "//
                                    + "UPDATE %1$s " //
                                    + "SET %2$s = NX_PARSE_FULLTEXT(CAST(%3$s AS VARCHAR(10000)), CAST(%4$s AS VARCHAR(10000))) " //
                                    + "WHERE %5$s = NEW.%5$s", //
                            ft.getQuotedName(), // 1 table "FULLTEXT"
                            ftft.getQuotedName(), // 2 column "TEXT"
                            ftst.getQuotedName(), // 3 column "SIMPLETEXT"
                            ftbt.getQuotedName(), // 4 column "BINARYTEXT"
                            ftid.getQuotedName() // 5 column "ID"
                    ));
        }

        public StoredProcedureInfo makeFTUpdateTrigger() {
            Table ft = database.getTable(model.FULLTEXT_TABLE_NAME);
            Column ftft = ft.getColumn(model.FULLTEXT_FULLTEXT_KEY);
            Column ftst = ft.getColumn(model.FULLTEXT_SIMPLETEXT_KEY);
            Column ftbt = ft.getColumn(model.FULLTEXT_BINARYTEXT_KEY);
            Column ftid = ft.getColumn(model.MAIN_KEY);
            return makeTrigger(
                    "NX_TRIG_FT_UPDATE", //
                    String.format(
                            "AFTER UPDATE OF %3$s, %4$s ON %1$s "//
                                    + "REFERENCING NEW AS NEW " //
                                    + "FOR EACH ROW "//
                                    + "UPDATE %1$s " //
                                    + "SET %2$s = NX_PARSE_FULLTEXT(CAST(%3$s AS VARCHAR(10000)), CAST(%4$s AS VARCHAR(10000))) " //
                                    + "WHERE %5$s = NEW.%5$s", //
                            ft.getQuotedName(), // 1 table "FULLTEXT"
                            ftft.getQuotedName(), // 2 column "TEXT"
                            ftst.getQuotedName(), // 3 column "SIMPLETEXT"
                            ftbt.getQuotedName(), // 4 column "BINARYTEXT"
                            ftid.getQuotedName() // 5 column "ID"
                    ));
        }

        public StoredProcedureInfo makeAccessAllowed() {
            return makeFunction(
                    "NX_ACCESS_ALLOWED",
                    "(ID %s, PRINCIPALS VARCHAR(10000), PERMISSIONS VARCHAR(10000)) RETURNS SMALLINT",
                    "isAccessAllowed" + methodSuffix, "READS SQL DATA");
        }

        protected StoredProcedureInfo makeFunction(String functionName,
                String proto, String methodName, String info) {
            proto = String.format(proto, idType);
            return new StoredProcedureInfo(
                    null, // do a drop check
                    String.format(
                            "SELECT ALIAS FROM SYS.SYSALIASES WHERE ALIAS = '%s' AND ALIASTYPE = 'F'",
                            functionName), //
                    String.format("DROP FUNCTION %s", functionName), //
                    String.format("CREATE FUNCTION %s%s " //
                            + "LANGUAGE JAVA " //
                            + "PARAMETER STYLE JAVA " //
                            + "EXTERNAL NAME '%s.%s' " //
                            + "%s", //
                            functionName, proto, //
                            className, methodName, info));
        }

        public StoredProcedureInfo makeTrigger(String triggerName, String body) {
            return new StoredProcedureInfo(
                    null, // do a drop check
                    String.format(
                            "SELECT TRIGGERNAME FROM SYS.SYSTRIGGERS WHERE TRIGGERNAME = '%s'",
                            triggerName), //
                    String.format("DROP TRIGGER %s", triggerName), //
                    String.format("CREATE TRIGGER %s %s", triggerName, body));

        }
    }

    public class H2StoredProcedureInfoMaker {

        public final String methodSuffix;

        public static final String h2Functions = "org.nuxeo.ecm.core.storage.sql.db.H2Functions";

        public static final String h2Fulltext = "org.nuxeo.ecm.core.storage.sql.db.H2Fulltext";

        public H2StoredProcedureInfoMaker() {
            switch (model.idGenPolicy) {
            case APP_UUID:
                methodSuffix = "String";
                break;
            case DB_IDENTITY:
                methodSuffix = "Long";
                break;
            default:
                throw new AssertionError(model.idGenPolicy);
            }
        }

        public StoredProcedureInfo makeInTree() {
            return makeFunction("NX_IN_TREE", "isInTree" + methodSuffix);
        }

        public StoredProcedureInfo makeAccessAllowed() {
            return makeFunction("NX_ACCESS_ALLOWED", "isAccessAllowed" +
                    methodSuffix);
        }

        public StoredProcedureInfo makeFTInit() {
            return new StoredProcedureInfo(Boolean.FALSE, null, null,
                    String.format(
                            "CREATE ALIAS IF NOT EXISTS NXFT_INIT FOR \"%s.init\"; "
                                    + "CALL NXFT_INIT()", h2Fulltext));
        }

        public StoredProcedureInfo makeFTIndex() {
            Table ft = database.getTable(model.FULLTEXT_TABLE_NAME);
            Column ftst = ft.getColumn(model.FULLTEXT_SIMPLETEXT_KEY);
            Column ftbt = ft.getColumn(model.FULLTEXT_BINARYTEXT_KEY);
            return new StoredProcedureInfo(
                    Boolean.FALSE,
                    null,
                    null, //
                    String.format(
                            "CALL NXFT_CREATE_INDEX('PUBLIC', '%s', ('%s', '%s'), '%s')", //
                            ft.getName(), ftst.getPhysicalName(),
                            ftbt.getPhysicalName(),
                            dialect.getFulltextAnalyzer()));
        }

        protected StoredProcedureInfo makeFunction(String functionName,
                String methodName) {
            return new StoredProcedureInfo(Boolean.TRUE, null, //
                    String.format("DROP ALIAS IF EXISTS %s", functionName), //
                    String.format("CREATE ALIAS %s FOR \"%s.%s\"",
                            functionName, h2Functions, methodName));
        }
    }

    public class PostgreSQLstoredProcedureInfoMaker {

        public final String idType;

        public PostgreSQLstoredProcedureInfoMaker() {
            switch (model.idGenPolicy) {
            case APP_UUID:
                idType = "varchar(36)";
                break;
            case DB_IDENTITY:
                idType = "integer";
                break;
            default:
                throw new AssertionError(model.idGenPolicy);
            }
        }

        public StoredProcedureInfo makeInTree() {
            return new StoredProcedureInfo(
                    Boolean.FALSE, // no drop needed
                    null,
                    null, //
                    String.format(
                            "CREATE OR REPLACE FUNCTION NX_IN_TREE(id %s, baseid %<s) " //
                                    + "RETURNS boolean " //
                                    + "AS $$ " //
                                    + "DECLARE" //
                                    + "  curid %<s := id; " //
                                    + "BEGIN" //
                                    + "  IF baseid IS NULL OR id IS NULL OR baseid = id THEN" //
                                    + "    RETURN false;" //
                                    + "  END IF;" //
                                    + "  LOOP" //
                                    + "    SELECT parentid INTO curid FROM hierarchy WHERE hierarchy.id = curid;" //
                                    + "    IF curid IS NULL THEN" //
                                    + "      RETURN false; " //
                                    + "    ELSIF curid = baseid THEN" //
                                    + "      RETURN true;" //
                                    + "    END IF;" //
                                    + "  END LOOP;" //
                                    + "END " //
                                    + "$$ " //
                                    + "LANGUAGE plpgsql " //
                                    + "STABLE " //
                            , idType));
        }

        public StoredProcedureInfo makeAccessAllowed() {
            return new StoredProcedureInfo(
                    Boolean.FALSE, // no drop needed
                    null, //
                    null, //
                    String.format(
                            "CREATE OR REPLACE FUNCTION NX_ACCESS_ALLOWED" //
                                    + "(id %s, users varchar[], permissions varchar[]) " //
                                    + "RETURNS boolean " //
                                    + "AS $$ " //
                                    + "DECLARE" //
                                    + "  curid %<s := id;" //
                                    + "  newid %<s;" //
                                    + "  r record;" //
                                    + "  first boolean := true;" //
                                    + "BEGIN" //
                                    + "  WHILE curid IS NOT NULL LOOP" //
                                    + "    FOR r in SELECT acls.grant, acls.permission, acls.user FROM acls WHERE acls.id = curid ORDER BY acls.pos LOOP"
                                    + "      IF r.permission = ANY(permissions) AND r.user = ANY(users) THEN" //
                                    + "        RETURN r.grant;" //
                                    + "      END IF;" //
                                    + "    END LOOP;" //
                                    + "    SELECT parentid INTO newid FROM hierarchy WHERE hierarchy.id = curid;" //
                                    + "    IF first AND newid IS NULL THEN" //
                                    + "      SELECT versionableid INTO newid FROM versions WHERE versions.id = curid;" //
                                    + "    END IF;" //
                                    + "    first := false;" //
                                    + "    curid := newid;" //
                                    + "  END LOOP;" //
                                    + "  RETURN false; " //
                                    + "END " //
                                    + "$$ " //
                                    + "LANGUAGE plpgsql " //
                                    + "STABLE " //
                            , idType));
        }

        public StoredProcedureInfo makeToTSVector() {
            String tsconfig = dialect.getFulltextAnalyzer();
            return new StoredProcedureInfo( //
                    Boolean.FALSE, // no drop needed
                    null, //
                    null, //
                    String.format(
                            "CREATE OR REPLACE FUNCTION NX_TO_TSVECTOR(string VARCHAR) " //
                                    + "RETURNS TSVECTOR " //
                                    + "AS $$" //
                                    + "  SELECT TO_TSVECTOR('%s', $1) " //
                                    + "$$ " //
                                    + "LANGUAGE sql " //
                                    + "STABLE " //
                            , tsconfig));
        }

        public StoredProcedureInfo makeContainsFullText() {
            String tsconfig = dialect.getFulltextAnalyzer();
            return new StoredProcedureInfo( //
                    Boolean.FALSE, // no drop needed
                    null, //
                    null, //
                    String.format(
                            "CREATE OR REPLACE FUNCTION NX_CONTAINS(ft TSVECTOR, query VARCHAR) " //
                                    + "RETURNS boolean " //
                                    + "AS $$" //
                                    + "  SELECT $1 @@ TO_TSQUERY('%s', $2) " //
                                    + "$$ " //
                                    + "LANGUAGE sql " //
                                    + "STABLE " //
                            , tsconfig));
        }

        public StoredProcedureInfo makeFTTrigger() {
            Table ft = database.getTable(model.FULLTEXT_TABLE_NAME);
            String qname = ft.getQuotedName();
            return new StoredProcedureInfo(
                    Boolean.TRUE, // do a drop
                    null, //
                    String.format(
                            "DROP TRIGGER IF EXISTS NX_TRIG_FT_UPDATE ON %s",
                            qname),
                    String.format(
                            "CREATE TRIGGER NX_TRIG_FT_UPDATE " //
                                    + "BEFORE INSERT OR UPDATE ON %s "
                                    + "FOR EACH ROW EXECUTE PROCEDURE NX_UPDATE_FULLTEXT()" //
                            , qname));
        }

        public StoredProcedureInfo makeConsolidateFullText() {
            Table ft = database.getTable(model.FULLTEXT_TABLE_NAME);
            Column ftft = ft.getColumn(model.FULLTEXT_FULLTEXT_KEY);
            Column ftst = ft.getColumn(model.FULLTEXT_SIMPLETEXT_KEY);
            Column ftbt = ft.getColumn(model.FULLTEXT_BINARYTEXT_KEY);
            return new StoredProcedureInfo(Boolean.FALSE, // no drop needed
                    null, //
                    null, //
                    String.format(
                            "CREATE OR REPLACE FUNCTION NX_UPDATE_FULLTEXT() " //
                                    + "RETURNS trigger " //
                                    + "AS $$ " //
                                    + "BEGIN" //
                                    + "  NEW.%s := NEW.%s || NEW.%s;" //
                                    + "  RETURN NEW; " //
                                    + "END " //
                                    + "$$ " //
                                    + "LANGUAGE plpgsql " //
                                    + "VOLATILE " //
                            , ftft.getQuotedName(), ftst.getQuotedName(),
                            ftbt.getQuotedName()));
        }

    }

    /**
     * Gets the statements creating the appropriate stored procedures.
     */
    public Collection<StoredProcedureInfo> getStoredProceduresSqls() {
        List<StoredProcedureInfo> spis = new LinkedList<StoredProcedureInfo>();
        String databaseName = dialect.getDatabaseName();
        if ("Apache Derby".equals(databaseName)) {
            DerbyStoredProcedureInfoMaker maker = new DerbyStoredProcedureInfoMaker();
            spis.add(maker.makeInTree());
            spis.add(maker.makeAccessAllowed());
            spis.add(maker.makeParseFullText());
            spis.add(maker.makeContainsFullText());
        } else if ("H2".equals(databaseName)) {
            H2StoredProcedureInfoMaker maker = new H2StoredProcedureInfoMaker();
            spis.add(maker.makeInTree());
            spis.add(maker.makeAccessAllowed());
        } else if ("PostgreSQL".equals(databaseName)) {
            PostgreSQLstoredProcedureInfoMaker maker = new PostgreSQLstoredProcedureInfoMaker();
            spis.add(maker.makeInTree());
            spis.add(maker.makeAccessAllowed());
            spis.add(maker.makeToTSVector());
            spis.add(maker.makeContainsFullText());
        }
        return spis;
    }

    /**
     * Gets the statements creating the appropriate triggers.
     */
    public Collection<StoredProcedureInfo> getTriggersSqls() {
        List<StoredProcedureInfo> spis = new LinkedList<StoredProcedureInfo>();
        String databaseName = dialect.getDatabaseName();
        if ("Apache Derby".equals(databaseName)) {
            DerbyStoredProcedureInfoMaker maker = new DerbyStoredProcedureInfoMaker();
            spis.add(maker.makeFTInsertTrigger());
            spis.add(maker.makeFTUpdateTrigger());
        } else if ("H2".equals(databaseName)) {
            H2StoredProcedureInfoMaker maker = new H2StoredProcedureInfoMaker();
            spis.add(maker.makeFTInit());
            spis.add(maker.makeFTIndex());
        } else if ("PostgreSQL".equals(databaseName)) {
            PostgreSQLstoredProcedureInfoMaker maker = new PostgreSQLstoredProcedureInfoMaker();
            spis.add(maker.makeConsolidateFullText());
            spis.add(maker.makeFTTrigger());
        }
        return spis;
    }

}
