/*
 * (C) Copyright 2008-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.core.opencmis.impl.server;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.antlr.runtime.tree.Tree;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDecimalDefinitionImpl;
import org.apache.chemistry.opencmis.server.support.query.AbstractPredicateWalker;
import org.apache.chemistry.opencmis.server.support.query.CmisQueryWalker;
import org.apache.chemistry.opencmis.server.support.query.CmisSelector;
import org.apache.chemistry.opencmis.server.support.query.ColumnReference;
import org.apache.chemistry.opencmis.server.support.query.FunctionReference;
import org.apache.chemistry.opencmis.server.support.query.FunctionReference.CmisQlFunction;
import org.apache.chemistry.opencmis.server.support.query.QueryObject;
import org.apache.chemistry.opencmis.server.support.query.QueryObject.JoinSpec;
import org.apache.chemistry.opencmis.server.support.query.QueryObject.SortSpec;
import org.apache.chemistry.opencmis.server.support.query.QueryUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.opencmis.impl.util.TypeManagerImpl;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.ModelProperty;
import org.nuxeo.ecm.core.storage.sql.Session.PathResolver;
import org.nuxeo.ecm.core.storage.sql.jdbc.QueryMaker;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo.MapMaker;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo.SQLInfoSelect;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Database;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Select;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.TableAlias;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect.FulltextMatchInfo;

/**
 * Transformer of CMISQL queries into real SQL queries for the actual database.
 */
public class CMISQLQueryMaker implements QueryMaker {

    private static final Log log = LogFactory.getLog(CMISQLQueryMaker.class);

    public static final String TYPE = "CMISQL";

    public static final String CMIS_PREFIX = "cmis:";

    public static final String DC_FRAGMENT_NAME = "dublincore";

    public static final String DC_TITLE_KEY = "title";

    public static final String DC_CREATOR_KEY = "creator";

    public static final String DC_CREATED_KEY = "created";

    public static final String DC_MODIFIED_KEY = "modified";

    protected Database database;

    protected Dialect dialect;

    protected Model model;

    protected Table hierTable;

    // ----- filled during walks of the clauses -----

    protected QueryObject query;

    public FulltextMatchInfo fulltextMatchInfo;

    /** Map of qualifier -> fragment -> table */
    protected Map<String, Map<String, Table>> allTables = new HashMap<String, Map<String, Table>>();

    /** The columns we'll actually request from the database. */
    protected List<SqlColumn> realColumns = new LinkedList<SqlColumn>();

    /** Parameters for above (for SCORE expressions on some databases) */
    protected List<String> realColumnsParams = new LinkedList<String>();

    /** The non-real-columns we'll return as well. */
    protected Map<String, ColumnReference> virtualColumns = new HashMap<String, ColumnReference>();

    /** Type info returned to caller. */
    Map<String, PropertyDefinition<?>> typeInfo;

    /** used for diagnostic when using DISTINCT */
    List<String> virtualColumnNames = new LinkedList<String>();

    /**
     * Column corresponding to a returned value computed from an actual SQL
     * expression.
     */
    public static class SqlColumn {

        /** Column name or expression passed to SQL statement. */
        public final String sql;

        /** Column used to get the value from the result set. */
        public final Column column;

        /** Key for the value returned to the caller. */
        public final String key;

        public SqlColumn(String sql, Column column, String key) {
            this.sql = sql;
            this.column = column;
            this.key = key;
        }
    }

    @Override
    public String getName() {
        return TYPE;
    }

    @Override
    public boolean accepts(String queryType) {
        return queryType.equals(TYPE);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The optional parameters must be passed: {@code params[0]} is the
     * {@link NuxeoCmisService}, optional {@code params[1]} is a type info map.
     */
    @Override
    public Query buildQuery(SQLInfo sqlInfo, Model model,
            PathResolver pathResolver, String statement,
            QueryFilter queryFilter, Object... params) throws StorageException {
        database = sqlInfo.database;
        dialect = sqlInfo.dialect;
        this.model = model;
        NuxeoCmisService service = (NuxeoCmisService) params[0];
        typeInfo = params.length > 1 ? (Map<String, PropertyDefinition<?>>) params[1]
                : null;
        TypeManagerImpl typeManager = service.repository.getTypeManager();

        boolean addSystemColumns = true; // TODO

        hierTable = database.getTable(model.HIER_TABLE_NAME);

        query = new QueryObject(typeManager);
        CmisQueryWalker walker;
        try {
            walker = QueryUtil.getWalker(statement);
            walker.query(query, new AnalyzingWalker());
        } catch (Exception e) {
            throw new QueryMakerException("Cannot parse query: "
                    + e.getMessage(), e);
        }
        String err = walker.getErrorMessageString();
        if (err != null) {
            throw new QueryMakerException("Cannot parse query: " + err);
        }

        // now resolve column selectors to actual database columns
        for (CmisSelector sel : query.getSelectReferences()) {
            recordSelectSelector(sel);
        }
        for (CmisSelector sel : query.getJoinReferences()) {
            recordSelector(sel, JOIN);
        }
        for (CmisSelector sel : query.getWhereReferences()) {
            recordSelector(sel, WHERE);
        }
        for (SortSpec spec : query.getOrderBys()) {
            recordSelector(spec.getSelector(), ORDER_BY);
        }

        boolean distinct = false; // TODO extension
        boolean skipHidden = true; // ignore hidden and trashed documents
        addSystemColumns(addSystemColumns, distinct, skipHidden);

        /*
         * Find info about fragments needed.
         */

        List<String> whereClauses = new LinkedList<String>();
        List<Serializable> whereParams = new LinkedList<Serializable>();

        /*
         * Walk joins.
         */

        List<JoinSpec> joins = query.getJoins();
        StringBuilder from = new StringBuilder();
        List<Serializable> fromParams = new LinkedList<Serializable>();
        for (int njoin = -1; njoin < joins.size(); njoin++) {
            boolean firstTable = njoin == -1;
            JoinSpec join;
            String alias;

            if (firstTable) {
                join = null;
                alias = query.getMainTypeAlias();
            } else {
                join = joins.get(njoin);
                alias = join.alias;
            }

            String typeQueryName = query.getTypeQueryName(alias);
            String qual = alias;
            if (qual.equals(typeQueryName)) {
                qual = null;
            }
            Table qualHierTable;
            qualHierTable = getTable(hierTable, qual);

            // table this join is about
            Table table;
            if (firstTable) {
                table = qualHierTable;
            } else {
                // find which table in onLeft/onRight refers to current
                // qualifier
                table = null;
                for (ColumnReference col : Arrays.asList(join.onLeft,
                        join.onRight)) {
                    if (alias.equals(col.getTypeQueryName())) {
                        table = ((Column) col.getInfo()).getTable();
                        break;
                    }
                }
                if (table == null) {
                    throw new StorageException(
                            "Bad query, qualifier not found: " + qual);
                }
                // do requested join
                if (join.kind.equals("LEFT") || join.kind.equals("RIGHT")) {
                    from.append(" ");
                    from.append(join.kind);
                }
                from.append(" JOIN ");
            }

            // join requested table

            String name;
            if (table.isAlias()) {
                name = table.getRealTable().getQuotedName() + " "
                        + table.getQuotedName();
            } else {
                name = table.getQuotedName();
            }
            from.append(name);

            if (!firstTable) {
                // emit actual join requested
                from.append(" ON ");
                from.append(((Column) join.onLeft.getInfo()).getFullQuotedName());
                from.append(" = ");
                from.append(((Column) join.onRight.getInfo()).getFullQuotedName());
            }

            // join other fragments for qualifier

            String tableMainId = table.getColumn(model.MAIN_KEY).getFullQuotedName();

            for (Table t : allTables.get(qual).values()) {
                if (t.getKey().equals(table.getKey())) {
                    // this one was the first, already done
                    continue;
                }
                String n;
                if (t.isAlias()) {
                    n = t.getRealTable().getQuotedName() + " "
                            + t.getQuotedName();
                } else {
                    n = t.getQuotedName();
                }
                from.append(" LEFT JOIN ");
                from.append(n);
                from.append(" ON ");
                from.append(t.getColumn(model.MAIN_KEY).getFullQuotedName());
                from.append(" = ");
                from.append(tableMainId);
            }

            // restrict to relevant primary types

            List<String> types = new ArrayList<String>();
            TypeDefinition td = query.getTypeDefinitionFromQueryName(typeQueryName);
            if (td.getParentTypeId() != null) {
                // don't add abstract root types
                types.add(td.getId());
            }
            LinkedList<TypeDefinitionContainer> typesTodo = new LinkedList<TypeDefinitionContainer>();
            typesTodo.addAll(typeManager.getTypeDescendants(td.getId(), -1,
                    Boolean.TRUE));
            // recurse to get all subtypes
            TypeDefinitionContainer tc;
            while ((tc = typesTodo.poll()) != null) {
                types.add(tc.getTypeDefinition().getId());
                typesTodo.addAll(tc.getChildren());
            }
            if (types.isEmpty()) {
                // shoudn't happen
                types = Collections.singletonList("__NOSUCHTYPE__");
            }
            StringBuilder qms = new StringBuilder();
            for (int i = 0; i < types.size(); i++) {
                if (i != 0) {
                    qms.append(", ");
                }
                qms.append("?");
            }

            whereClauses.add(String.format(
                    "%s IN (%s)",
                    qualHierTable.getColumn(model.MAIN_PRIMARY_TYPE_KEY).getFullQuotedName(),
                    qms));
            whereParams.addAll(types);

            // lifecycle not deleted filter

            if (skipHidden) {
                Table misc = getTable(database.getTable(model.MISC_TABLE_NAME),
                        qual);
                Column lscol = misc.getColumn(model.MISC_LIFECYCLE_STATE_KEY);
                whereClauses.add(String.format("%s <> ?",
                        lscol.getFullQuotedName()));
                whereParams.add(LifeCycleConstants.DELETED_STATE);
            }

            // security check

            if (queryFilter != null && queryFilter.getPrincipals() != null) {
                Serializable principals;
                Serializable permissions;
                if (dialect.supportsArrays()) {
                    principals = queryFilter.getPrincipals();
                    permissions = queryFilter.getPermissions();
                } else {
                    principals = StringUtils.join(queryFilter.getPrincipals(),
                            '|');
                    permissions = StringUtils.join(
                            queryFilter.getPermissions(), '|');
                }
                if (dialect.supportsReadAcl()) {
                    /* optimized read acl */
                    String readAclTable;
                    String readAclIdCol;
                    String readAclAclIdCol;
                    if (joins.size() == 0) {
                        readAclTable = model.HIER_READ_ACL_TABLE_NAME;
                        readAclIdCol = model.HIER_READ_ACL_TABLE_NAME + '.'
                                + model.HIER_READ_ACL_ID;
                        readAclAclIdCol = model.HIER_READ_ACL_TABLE_NAME + '.'
                                + model.HIER_READ_ACL_ACL_ID;
                    } else {
                        String al = "nxr" + (njoin + 1);
                        readAclTable = model.HIER_READ_ACL_TABLE_NAME + " "
                                + al; // TODO dialect
                        readAclIdCol = al + '.' + model.HIER_READ_ACL_ID;
                        readAclAclIdCol = al + '.' + model.HIER_READ_ACL_ACL_ID;
                    }
                    whereClauses.add(dialect.getReadAclsCheckSql(readAclAclIdCol));
                    whereParams.add(principals);
                    from.append(String.format(" JOIN %s ON %s = %s",
                            readAclTable, tableMainId, readAclIdCol));
                } else {
                    whereClauses.add(dialect.getSecurityCheckSql(tableMainId));
                    whereParams.add(principals);
                    whereParams.add(permissions);
                }
            }
        }

        /*
         * WHERE clause.
         */

        Tree whereNode = walker.getWherePredicateTree();
        if (whereNode != null) {
            GeneratingWalker generator = new GeneratingWalker();
            generator.walkPredicate(whereNode);
            whereClauses.add(generator.whereBuf.toString());
            whereParams.addAll(generator.whereBufParams);

            // add JOINs for the external fulltext matches
            Collections.sort(generator.ftJoins); // implicit JOINs last
                                                 // (PostgreSQL)
            for (org.nuxeo.ecm.core.storage.sql.jdbc.db.Join join : generator.ftJoins) {
                from.append(join.toString());
                if (join.tableParam != null) {
                    fromParams.add(join.tableParam);
                }
            }
        }

        /*
         * SELECT clause.
         */

        List<String> selectWhat = new ArrayList<String>();
        List<Serializable> selectParams = new ArrayList<Serializable>(1);

        for (SqlColumn rc : realColumns) {
            selectWhat.add(rc.sql);
        }
        selectParams.addAll(realColumnsParams);

        CMISQLMapMaker mapMaker = new CMISQLMapMaker(realColumns,
                virtualColumns, service);
        String what = StringUtils.join(selectWhat, ", ");
        if (distinct) {
            what = "DISTINCT " + what;
        }

        /*
         * ORDER BY clause.
         */

        List<String> orderbys = new LinkedList<String>();
        for (SortSpec spec : query.getOrderBys()) {
            String orderby;
            CmisSelector sel = spec.getSelector();
            if (sel instanceof ColumnReference) {
                Column column = (Column) sel.getInfo();
                orderby = column.getFullQuotedName();
            } else {
                orderby = fulltextMatchInfo.scoreAlias;
            }
            if (!spec.ascending) {
                orderby += " DESC";
            }
            orderbys.add(orderby);
        }

        /*
         * Create the whole select.
         */

        Select select = new Select(null);
        select.setWhat(what);
        select.setFrom(from.toString());
        // TODO(fromParams); // TODO add before whereParams
        select.setWhere(StringUtils.join(whereClauses, " AND "));
        select.setOrderBy(StringUtils.join(orderbys, ", "));

        Query q = new Query();
        q.selectInfo = new SQLInfoSelect(select.getStatement(), mapMaker);
        q.selectParams = selectParams;
        q.selectParams.addAll(fromParams);
        q.selectParams.addAll(whereParams);
        return q;
    }

    // add main id to all qualifiers if
    // - we have no DISTINCT (in which case more columns don't matter), or
    // - we have virtual columns, or
    // - system columns are requested
    // check no added columns would bias the DISTINCT
    protected void addSystemColumns(boolean addSystemColumns, boolean distinct,
            boolean skipHidden) {

        List<CmisSelector> addedSystemColumns = new ArrayList<CmisSelector>(2);

        for (String qual : allTables.keySet()) {
            TypeDefinition type = getTypeForQualifier(qual);

            // additional references to cmis:objectId and cmis:objectTypeId
            for (String propertyId : Arrays.asList(PropertyIds.OBJECT_ID,
                    PropertyIds.OBJECT_TYPE_ID)) {
                ColumnReference col = new ColumnReference(qual, propertyId);
                col.setTypeDefinition(propertyId, type);
                String key = getColumnKey(col);
                boolean add = true;
                for (SqlColumn rc : realColumns) {
                    if (rc.key.equals(key)) {
                        add = false;
                        break;
                    }
                }
                if (add) {
                    addedSystemColumns.add(col);
                }
            }
            if (skipHidden) {
                // add lifecycle state column
                Table table = getTable(
                        database.getTable(model.MISC_TABLE_NAME), qual);
                Column column = table.getColumn(model.MISC_LIFECYCLE_STATE_KEY);
                recordColumnFragment(qual, column);
            }
        }

        // additional system columns to select on
        if (!distinct) {
            for (CmisSelector col : addedSystemColumns) {
                recordSelectSelector(col);
            }
        } else {
            if (!addedSystemColumns.isEmpty()) {
                if (!virtualColumnNames.isEmpty()) {
                    throw new QueryMakerException(
                            "Cannot use DISTINCT with virtual columns: "
                                    + StringUtils.join(virtualColumnNames, ", "));
                }
                if (addSystemColumns) {
                    throw new QueryMakerException(
                            "Cannot use DISTINCT without explicit "
                                    + PropertyIds.OBJECT_ID);
                }
                // don't add system columns as it would prevent DISTINCT from
                // working
            }
        }

        // per qualifier, include hier in fragments
        for (String qual : allTables.keySet()) {
            Table table = getTable(hierTable, qual);
            String fragment = table.getKey();
            Map<String, Table> tablesByFragment = allTables.get(qual);
            if (!tablesByFragment.containsKey(fragment)) {
                tablesByFragment.put(fragment, table);
            }
        }
    }

    /**
     * Records a SELECT selector, and associates it to a database column.
     */
    protected void recordSelectSelector(CmisSelector sel) {
        if (sel instanceof FunctionReference) {
            FunctionReference fr = (FunctionReference) sel;
            if (fr.getFunction() != CmisQlFunction.SCORE) {
                throw new CmisRuntimeException("Unknown function: "
                        + fr.getFunction());
            }
            String key = fr.getAliasName();
            if (key == null) {
                key = "SEARCH_SCORE"; // default, from spec
            }
            SqlColumn c = new SqlColumn(fulltextMatchInfo.scoreExpr,
                    fulltextMatchInfo.scoreCol, key);
            realColumns.add(c);
            if (fulltextMatchInfo.scoreExprParam != null) {
                realColumnsParams.add(fulltextMatchInfo.scoreExprParam);
            }
            if (typeInfo != null) {
                PropertyDecimalDefinitionImpl pd = new PropertyDecimalDefinitionImpl();
                pd.setId(key);
                pd.setQueryName(key);
                pd.setCardinality(Cardinality.SINGLE);
                pd.setDisplayName("Score");
                pd.setLocalName("score");
                typeInfo.put(key, pd);
            }
        } else { // sel instanceof ColumnReference
            ColumnReference col = (ColumnReference) sel;
            String qual = col.getTypeQueryName();

            if (col.getPropertyQueryName().equals("*")) {
                TypeDefinition type = getTypeForQualifier(qual);
                for (PropertyDefinition<?> pd : type.getPropertyDefinitions().values()) {
                    if (pd.getCardinality() == Cardinality.SINGLE
                            && Boolean.TRUE.equals(pd.isQueryable())) {
                        String id = pd.getId();
                        ColumnReference c = new ColumnReference(qual, id);
                        c.setTypeDefinition(id, type);
                        recordSelectSelector(c);
                    }
                }
                return;
            }

            String key = getColumnKey(col);
            PropertyDefinition<?> pd = col.getPropertyDefinition();
            Column column = getColumn(col);
            if (column != null && pd.getCardinality() == Cardinality.SINGLE) {
                col.setInfo(column);
                recordColumnFragment(qual, column);
                String sql = column.getFullQuotedName();
                SqlColumn c = new SqlColumn(sql, column, key);
                realColumns.add(c);
            } else {
                virtualColumns.put(key, col);
                virtualColumnNames.add(key);
            }
            if (typeInfo != null) {
                typeInfo.put(key, pd);
            }
        }
    }

    protected static final String JOIN = "JOIN";

    protected static final String WHERE = "WHERE";

    protected static final String ORDER_BY = "ORDER BY";

    /**
     * Records a JOIN / WHERE / ORDER BY selector, and associates it to a
     * database column.
     */
    protected void recordSelector(CmisSelector sel, String clauseType) {
        if (sel instanceof FunctionReference) {
            FunctionReference fr = (FunctionReference) sel;
            if (clauseType != ORDER_BY) { // == ok
                throw new QueryMakerException("Cannot use function in "
                        + clauseType + " clause: " + fr.getFunction());
            }
            // ORDER BY SCORE, nothing further to record
            if (fulltextMatchInfo == null) {
                throw new QueryMakerException(
                        "Cannot use ORDER BY SCORE without CONTAINS");
            }
            return;
        }
        ColumnReference col = (ColumnReference) sel;
        PropertyDefinition<?> pd = col.getPropertyDefinition();
        boolean multi = pd.getCardinality() == Cardinality.MULTI;

        // fetch column and associate it to the selector
        Column column = getColumn(col);
        if (column == null) {
            throw new QueryMakerException("Cannot use column in " + clauseType
                    + " clause: " + col.getPropertyQueryName());
        }
        col.setInfo(column);
        String qual = col.getTypeQueryName();

        // record as a needed fragment
        if (!multi) {
            recordColumnFragment(qual, column);
        }
    }

    /**
     * Records a database column's fragment (to know what to join).
     */
    protected void recordColumnFragment(String qual, Column column) {
        Table table = column.getTable();
        String fragment = table.getKey();
        Map<String, Table> tablesByFragment = allTables.get(qual);
        if (tablesByFragment == null) {
            tablesByFragment = new HashMap<String, Table>();
            allTables.put(qual, tablesByFragment);
        }
        tablesByFragment.put(fragment, table);
    }

    /**
     * Finds a database column from a CMIS reference.
     */
    protected Column getColumn(ColumnReference col) {
        String qual = col.getTypeQueryName();
        String id = col.getPropertyId();
        Column column;
        if (id.startsWith(CMIS_PREFIX)) {
            column = getSystemColumn(qual, id);
        } else {
            ModelProperty propertyInfo = model.getPropertyInfo(id);
            boolean multi = propertyInfo.propertyType.isArray();
            Table table = database.getTable(propertyInfo.fragmentName);
            String key = multi ? model.COLL_TABLE_VALUE_KEY
                    : propertyInfo.fragmentKey;
            column = getTable(table, qual).getColumn(key);
        }
        return column;
    }

    protected Column getSystemColumn(String qual, String id) {
        Column column = getSystemColumn(id);
        if (column != null && qual != null) {
            // alias table according to qualifier
            Table table = column.getTable();
            column = getTable(table, qual).getColumn(column.getKey());
            // TODO ensure key == name, or add getName()
        }
        return column;
    }

    protected Column getSystemColumn(String id) {
        if (id.equals(PropertyIds.OBJECT_ID)) {
            return hierTable.getColumn(model.MAIN_KEY);
        }
        if (id.equals(PropertyIds.PARENT_ID)) {
            return hierTable.getColumn(model.HIER_PARENT_KEY);
        }
        if (id.equals(PropertyIds.OBJECT_TYPE_ID)) {
            // joinedHierTable
            return hierTable.getColumn(model.MAIN_PRIMARY_TYPE_KEY);
        }
        if (id.equals(PropertyIds.VERSION_LABEL)) {
            return database.getTable(model.VERSION_TABLE_NAME).getColumn(
                    model.VERSION_LABEL_KEY);
        }
        if (id.equals(PropertyIds.NAME)) {
            return database.getTable(DC_FRAGMENT_NAME).getColumn(DC_TITLE_KEY);
        }
        if (id.equals(PropertyIds.CREATED_BY)) {
            return database.getTable(DC_FRAGMENT_NAME).getColumn(DC_CREATOR_KEY);
        }
        if (id.equals(PropertyIds.CREATION_DATE)) {
            return database.getTable(DC_FRAGMENT_NAME).getColumn(DC_CREATED_KEY);
        }
        if (id.equals(PropertyIds.LAST_MODIFICATION_DATE)) {
            return database.getTable(DC_FRAGMENT_NAME).getColumn(
                    DC_MODIFIED_KEY);
        }
        return null;
    }

    /** Get key to use in data returned to high-level caller. */
    protected static String getColumnKey(ColumnReference col) {
        String alias = col.getAliasName();
        if (alias != null) {
            return alias;
        }
        return getPropertyKey(col.getTypeQueryName(),
                col.getPropertyQueryName());
    }

    protected static String getPropertyKey(String qual, String id) {
        if (qual == null) {
            return id;
        }
        return qual + '.' + id;
    }

    protected TypeDefinition getTypeForQualifier(String qual) {
        if (qual == null) {
            for (Entry<String, String> en : query.getTypes().entrySet()) {
                if (en.getKey().equals(en.getValue())) {
                    qual = en.getValue();
                    break;
                }
            }
        }
        return query.getTypeDefinitionFromQueryName(query.getTypeQueryName(qual));
    }

    protected Table getTable(Table table, String qual) {
        if (qual == null) {
            return table;
        } else {
            return new TableAlias(table, getTableAlias(table, qual));
        }
    }

    protected String getTableAlias(Table table, String qual) {
        return "_" + qual + "_" + table.getPhysicalName();
    }

    /**
     * Map maker that can deal with aliased column names and computed values.
     */
    // static to avoid keeping the whole QueryMaker in the returned object
    public static class CMISQLMapMaker implements MapMaker {

        protected List<SqlColumn> realColumns;

        protected Map<String, ColumnReference> virtualColumns;

        protected NuxeoCmisService service;

        public CMISQLMapMaker(List<SqlColumn> realColumns,
                Map<String, ColumnReference> virtualColumns,
                NuxeoCmisService service) {
            this.realColumns = realColumns;
            this.virtualColumns = virtualColumns;
            this.service = service;
        }

        @Override
        public Map<String, Serializable> makeMap(ResultSet rs)
                throws SQLException {
            Map<String, Serializable> map = new HashMap<String, Serializable>();

            // get values from result set
            int i = 1;
            for (SqlColumn rc : realColumns) {
                Serializable value = rc.column.getFromResultSet(rs, i++);
                // type conversion to CMIS values
                if (value instanceof Long) {
                    value = BigInteger.valueOf(((Long) value).longValue());
                } else if (value instanceof Integer) {
                    value = BigInteger.valueOf(((Integer) value).intValue());
                } else if (value instanceof Double) {
                    value = BigDecimal.valueOf(((Double) value).doubleValue());
                }
                map.put(rc.key, value);
            }

            // virtual values
            // map to store actual data for each qualifier
            Map<String, NuxeoObjectData> datas = null;
            for (Entry<String, ColumnReference> vc : virtualColumns.entrySet()) {
                String key = vc.getKey();
                ColumnReference col = vc.getValue();
                String qual = col.getTypeQueryName();
                if (datas == null) {
                    datas = new HashMap<String, NuxeoObjectData>(2);
                }
                NuxeoObjectData data = datas.get(qual);
                if (data == null) {
                    // find main id for this qualifier in the result set
                    // (main id always included in joins)
                    // TODO check what happens if cmis:objectId is aliased
                    String id = (String) map.get(getPropertyKey(qual,
                            PropertyIds.OBJECT_ID));
                    try {
                        // reentrant call to the same session, but the MapMaker
                        // is only called from the IterableQueryResult in
                        // queryAndFetch which manipulates no session state
                        // TODO constructing the DocumentModel (in
                        // NuxeoObjectData) is expensive, try to get value
                        // directly
                        data = (NuxeoObjectData) service.getObject(
                                service.getNuxeoRepository().getId(), id, null,
                                null, null, null, null, null, null);
                    } catch (CmisRuntimeException e) {
                        log.error("Cannot get document: " + id, e);
                    }
                    datas.put(qual, data);
                }
                Serializable v;
                if (data == null) {
                    // could not fetch
                    v = null;
                } else {
                    NuxeoPropertyDataBase<?> pd = (NuxeoPropertyDataBase<?>) data.getProperty(key);
                    if (pd == null) {
                        v = null;
                    } else {
                        if (pd.getPropertyDefinition().getCardinality() == Cardinality.SINGLE) {
                            v = (Serializable) pd.getFirstValue();
                        } else {
                            v = (Serializable) pd.getValues();
                        }
                    }
                }
                map.put(key, v);
            }

            return map;
        }
    }

    /**
     * Walker of the WHERE clause to gather fulltext info.
     */
    public class AnalyzingWalker extends AbstractPredicateWalker {

        public boolean hasContains;

        @Override
        public Boolean walkContains(Tree opNode, Tree qualNode, Tree queryNode) {
            if (hasContains) {
                throw new QueryMakerException(
                        "At most one CONTAINS() is allowed");
            }
            hasContains = true;

            String qual = qualNode == null ? null : qualNode.getText();
            String statement = (String) super.walkString(queryNode);
            Column column = getSystemColumn(qual, PropertyIds.OBJECT_ID);
            fulltextMatchInfo = dialect.getFulltextScoredMatchInfo(statement,
                    Model.FULLTEXT_DEFAULT_INDEX, 1, column, model, database);
            return null;
        }
    }

    /**
     * Walker of the WHERE clause that generates final SQL.
     */
    public class GeneratingWalker extends AbstractPredicateWalker {

        public StringBuilder whereBuf = new StringBuilder();

        public LinkedList<Serializable> whereBufParams = new LinkedList<Serializable>();

        /** joins added by fulltext match */
        public final List<org.nuxeo.ecm.core.storage.sql.jdbc.db.Join> ftJoins = new LinkedList<org.nuxeo.ecm.core.storage.sql.jdbc.db.Join>();

        @Override
        public Boolean walkNot(Tree opNode, Tree node) {
            whereBuf.append("NOT ");
            walkPredicate(node);
            return null;
        }

        @Override
        public Boolean walkAnd(Tree opNode, Tree leftNode, Tree rightNode) {
            whereBuf.append("(");
            walkPredicate(leftNode);
            whereBuf.append(" AND ");
            walkPredicate(rightNode);
            whereBuf.append(")");
            return null;
        }

        @Override
        public Boolean walkOr(Tree opNode, Tree leftNode, Tree rightNode) {
            whereBuf.append("(");
            walkPredicate(leftNode);
            whereBuf.append(" OR ");
            walkPredicate(rightNode);
            whereBuf.append(")");
            return null;
        }

        @Override
        public Boolean walkEquals(Tree opNode, Tree leftNode, Tree rightNode) {
            walkExpr(leftNode);
            whereBuf.append(" = ");
            walkExpr(rightNode);
            return null;
        }

        @Override
        public Boolean walkNotEquals(Tree opNode, Tree leftNode, Tree rightNode) {
            walkExpr(leftNode);
            whereBuf.append(" <> ");
            walkExpr(rightNode);
            return null;
        }

        @Override
        public Boolean walkGreaterThan(Tree opNode, Tree leftNode,
                Tree rightNode) {
            walkExpr(leftNode);
            whereBuf.append(" > ");
            walkExpr(rightNode);
            return null;
        }

        @Override
        public Boolean walkGreaterOrEquals(Tree opNode, Tree leftNode,
                Tree rightNode) {
            walkExpr(leftNode);
            whereBuf.append(" >= ");
            walkExpr(rightNode);
            return null;
        }

        @Override
        public Boolean walkLessThan(Tree opNode, Tree leftNode, Tree rightNode) {
            walkExpr(leftNode);
            whereBuf.append(" < ");
            walkExpr(rightNode);
            return null;
        }

        @Override
        public Boolean walkLessOrEquals(Tree opNode, Tree leftNode,
                Tree rightNode) {
            walkExpr(leftNode);
            whereBuf.append(" <= ");
            walkExpr(rightNode);
            return null;
        }

        @Override
        public Boolean walkIn(Tree opNode, Tree colNode, Tree listNode) {
            walkExpr(colNode);
            whereBuf.append(" IN ");
            walkExpr(listNode);
            return null;
        }

        @Override
        public Boolean walkNotIn(Tree opNode, Tree colNode, Tree listNode) {
            walkExpr(colNode);
            whereBuf.append(" NOT IN ");
            walkExpr(listNode);
            return null;
        }

        @Override
        public Boolean walkInAny(Tree opNode, Tree colNode, Tree listNode) {
            walkAny(colNode, "IN", listNode);
            return null;
        }

        @Override
        public Boolean walkNotInAny(Tree opNode, Tree colNode, Tree listNode) {
            walkAny(colNode, "NOT IN", listNode);
            return null;
        }

        @Override
        public Boolean walkEqAny(Tree opNode, Tree literalNode, Tree colNode) {
            // note that argument order is reversed
            walkAny(colNode, "=", literalNode);
            return null;
        }

        protected void walkAny(Tree colNode, String op, Tree exprNode) {
            int token = ((Tree) colNode).getTokenStartIndex();
            ColumnReference col = (ColumnReference) query.getColumnReference(Integer.valueOf(token));
            PropertyDefinition<?> pd = col.getPropertyDefinition();
            if (pd.getCardinality() != Cardinality.MULTI) {
                throw new QueryMakerException("Cannot use " + op
                        + " ANY with single-valued property: "
                        + col.getPropertyQueryName());
            }
            Column column = (Column) col.getInfo();
            String qual = col.getTypeQueryName();
            Column hierMainColumn = getTable(hierTable, qual).getColumn(
                    model.MAIN_KEY);
            Column multiMainColumn = column.getTable().getColumn(model.MAIN_KEY);

            whereBuf.append("EXISTS (SELECT 1 FROM ");
            whereBuf.append(column.getTable().getQuotedName());
            whereBuf.append(" WHERE ");
            whereBuf.append(hierMainColumn.getFullQuotedName());
            whereBuf.append(" = ");
            whereBuf.append(multiMainColumn.getFullQuotedName());
            whereBuf.append(" AND ");
            whereBuf.append(column.getFullQuotedName());
            whereBuf.append(" ");
            whereBuf.append(op);
            whereBuf.append(" ");
            walkExpr(exprNode);
            whereBuf.append(")");
        }

        @Override
        public Boolean walkIsNull(Tree opNode, Tree colNode) {
            walkExpr(colNode);
            whereBuf.append(" IS NULL");
            return null;
        }

        @Override
        public Boolean walkIsNotNull(Tree opNode, Tree colNode) {
            walkExpr(colNode);
            whereBuf.append(" IS NOT NULL");
            return null;
        }

        @Override
        public Boolean walkLike(Tree opNode, Tree colNode, Tree stringNode) {
            walkExpr(colNode);
            whereBuf.append(" LIKE ");
            walkExpr(stringNode);
            return null;
        }

        @Override
        public Boolean walkNotLike(Tree opNode, Tree colNode, Tree stringNode) {
            walkExpr(colNode);
            whereBuf.append(" NOT LIKE ");
            walkExpr(stringNode);
            return null;
        }

        @Override
        public Boolean walkContains(Tree opNode, Tree qualNode, Tree queryNode) {
            if (fulltextMatchInfo.joins != null) {
                ftJoins.addAll(fulltextMatchInfo.joins);
            }
            whereBuf.append(fulltextMatchInfo.whereExpr);
            if (fulltextMatchInfo.whereExprParam != null) {
                whereBufParams.add(fulltextMatchInfo.whereExprParam);
            }
            return null;
        }

        @Override
        public Boolean walkInFolder(Tree opNode, Tree qualNode, Tree paramNode) {
            String qual = qualNode == null ? null : qualNode.getText();
            // this is from the hierarchy table which is always present
            Column column = getSystemColumn(qual, PropertyIds.PARENT_ID);
            whereBuf.append(column.getFullQuotedName());
            whereBuf.append(" = ?");
            String id = (String) super.walkString(paramNode);
            whereBufParams.add(id);
            return null;
        }

        @Override
        public Boolean walkInTree(Tree opNode, Tree qualNode, Tree paramNode) {
            String qual = qualNode == null ? null : qualNode.getText();
            Column column = getSystemColumn(qual, PropertyIds.OBJECT_ID);
            String sql = dialect.getInTreeSql(column.getFullQuotedName());
            String id = (String) super.walkString(paramNode);
            whereBuf.append(sql);
            whereBufParams.add(id);
            return null;
        }

        @Override
        public Object walkList(Tree node) {
            whereBuf.append("(");
            for (int i = 0; i < node.getChildCount(); i++) {
                if (i != 0) {
                    whereBuf.append(", ");
                }
                Tree child = node.getChild(i);
                walkExpr(child);
            }
            whereBuf.append(")");
            return null;
        }

        @Override
        public Object walkBoolean(Tree node) {
            Serializable value = (Serializable) super.walkBoolean(node);
            whereBuf.append("?");
            whereBufParams.add(value);
            return null;
        }

        @Override
        public Object walkNumber(Tree node) {
            Serializable value = (Serializable) super.walkNumber(node);
            whereBuf.append("?");
            whereBufParams.add(value);
            return null;
        }

        @Override
        public Object walkString(Tree node) {
            Serializable value = (Serializable) super.walkString(node);
            whereBuf.append("?");
            whereBufParams.add(value);
            return null;
        }

        @Override
        public Object walkTimestamp(Tree node) {
            Serializable value = (Serializable) super.walkTimestamp(node);
            whereBuf.append("?");
            whereBufParams.add(value);
            return null;
        }

        @Override
        public Object walkCol(Tree node) {
            int token = ((Tree) node).getTokenStartIndex();
            CmisSelector sel = query.getColumnReference(Integer.valueOf(token));
            if (sel instanceof ColumnReference) {
                Column column = (Column) sel.getInfo();
                whereBuf.append(column.getFullQuotedName());
            } else {
                throw new QueryMakerException(
                        "Cannot use column in WHERE clause: " + sel.getName());
            }
            return null;
        }
    }
}
