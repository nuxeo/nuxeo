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
import java.util.Set;

import org.antlr.runtime.tree.Tree;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDecimalDefinitionImpl;
import org.apache.chemistry.opencmis.server.support.query.AbstractPredicateWalker;
import org.apache.chemistry.opencmis.server.support.query.AbstractQueryConditionProcessor;
import org.apache.chemistry.opencmis.server.support.query.CmisQueryWalker;
import org.apache.chemistry.opencmis.server.support.query.CmisSelector;
import org.apache.chemistry.opencmis.server.support.query.ColumnReference;
import org.apache.chemistry.opencmis.server.support.query.FunctionReference;
import org.apache.chemistry.opencmis.server.support.query.FunctionReference.CmisQlFunction;
import org.apache.chemistry.opencmis.server.support.query.QueryObject;
import org.apache.chemistry.opencmis.server.support.query.QueryObject.SortSpec;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.TypeConstants;
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

    public static class Join {
        /** INNER / LEFT / RIGHT */
        String kind;

        /** Table name. */
        String table;

        /** Correlation name (qualifier), or null. */
        String corr;

        String on1;

        String on2;
    }

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

        boolean addSystemColumns = true; // TODO

        hierTable = database.getTable(model.HIER_TABLE_NAME);

        query = new QueryObject(service.repository.getTypeManager(), null);
        CmisQueryWalker walker;
        try {
            walker = AbstractQueryConditionProcessor.getWalker(statement);
            walker.query(query);
        } catch (Exception e) {
            throw new QueryMakerException("Cannot parse query: "
                    + e.getMessage(), e);
        }
        String err = walker.getErrorMessageString();
        if (err != null) {
            throw new QueryMakerException("Cannot parse query: " + err);
        }

        Tree whereNode = query.getWhereTree() == null ? null
                : query.getWhereTree().getChild(0);
        if (whereNode != null) {
            AnalyzingWalker analyzer = new AnalyzingWalker();
            analyzer.walkPredicate(whereNode);
        }

        // now resolve column selectors to actual database columns
        for (CmisSelector sel : query.getSelectReferences()) {
            recordSelectSelector(sel);
        }
        for (CmisSelector sel : query.getWhereReferences()) {
            recordSelector(sel, false);
        }
        for (SortSpec spec : query.getOrderBys()) {
            recordSelector(spec.getSelector(), true);
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

        Join jj = new Join();
        Entry<String, String> ee = query.getTypes().entrySet().iterator().next();
        jj.table = ee.getValue();
        jj.corr = ee.getKey();
        if (jj.table.equals(jj.corr)) {
            jj.corr = null;
        }
        List<Join> walker_from_joins = Collections.singletonList(jj);

        StringBuilder from = new StringBuilder();
        List<Serializable> fromParams = new LinkedList<Serializable>();
        int njoin = 0;
        for (Join j : walker_from_joins) {
            njoin++;
            boolean firstJoin = njoin == 1;

            // table this join is about

            String qual = j.corr;
            Table qualHierTable = getTable(hierTable, qual);
            Table table = null;
            if (firstJoin) {
                // start with hier
                table = qualHierTable;
            } else {
                // do requested join
                if (j.kind.equals("LEFT") || j.kind.equals("RIGHT")) {
                    from.append(" ");
                    from.append(j.kind);
                }
                from.append(" JOIN ");
                // find which table in on1/on2 refers to current qualifier
                Set<String> fqns = null; // columnsPerQual.get(qual);
                for (String fqn : Arrays.asList(j.on1, j.on2)) {
                    if (!fqns.contains(fqn)) {
                        continue;
                    }
                    Column col = null; // columns.get(fqn);
                    table = col.getTable();
                }
                if (table == null) {
                    throw new StorageException(
                            "Bad query, qualifier not found: " + qual);
                }
            }
            String tableMainId = table.getColumn(model.MAIN_KEY).getFullQuotedName();

            // join requested table

            String name;
            if (table.isAlias()) {
                name = table.getRealTable().getQuotedName() + " "
                        + table.getQuotedName();
            } else {
                name = table.getQuotedName();
            }
            from.append(name);

            if (!firstJoin) {
                // emit actual join requested
                from.append(" ON ");
                from.append(j.on1);
                from.append(" = ");
                from.append(j.on2);
            }

            // join other fragments for qualifier

            for (Table t : allTables.get(qual).values()) {
                if (t.getName().equals(table.getName())) {
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

            // restrict to relevant primary types TODO XXX XXXXXXXXXXX

            String nuxeoType;
            boolean skipFolderish;
            if (j.table.equalsIgnoreCase(BaseTypeId.CMIS_DOCUMENT.value())) {
                nuxeoType = TypeConstants.DOCUMENT;
                skipFolderish = true;
            } else if (j.table.equalsIgnoreCase(BaseTypeId.CMIS_FOLDER.value())) {
                nuxeoType = "Folder"; // TODO extract constant
                skipFolderish = false;
            } else {
                nuxeoType = j.table;
                skipFolderish = false;
            }
            Set<String> subTypes = model.getDocumentSubTypes(nuxeoType);
            if (subTypes == null) {
                throw new QueryMakerException("Unknown type: " + j.table);
            }
            List<String> types = new ArrayList<String>();
            List<String> qms = new ArrayList<String>();
            for (String type : subTypes) {
                Set<String> facets = model.getDocumentTypeFacets(type);
                if (skipFolderish && facets.contains(FacetNames.FOLDERISH)) {
                    continue;
                }
                if (skipHidden
                        && facets.contains(FacetNames.HIDDEN_IN_NAVIGATION)) {
                    continue;
                }
                if (type.equals(model.ROOT_TYPE)) {
                    continue;
                }
                types.add(type);
                qms.add("?");
            }
            if (types.isEmpty()) {
                types.add("__NOSUCHTYPE__");
                qms.add("?");
            }
            whereClauses.add(String.format(
                    "%s IN (%s)",
                    qualHierTable.getColumn(model.MAIN_PRIMARY_TYPE_KEY).getFullQuotedName(),
                    StringUtils.join(qms, ", ")));
            for (String type : types) {
                whereParams.add(type);
            }

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
                    // if (walker_from_joins.size() == 1) {
                    if (1 == 1) {
                        readAclTable = model.HIER_READ_ACL_TABLE_NAME;
                        readAclIdCol = model.HIER_READ_ACL_TABLE_NAME + '.'
                                + model.HIER_READ_ACL_ID;
                        readAclAclIdCol = model.HIER_READ_ACL_TABLE_NAME + '.'
                                + model.HIER_READ_ACL_ACL_ID;
                    } else {
                        String alias = "nxr" + njoin;
                        readAclTable = model.HIER_READ_ACL_TABLE_NAME + " "
                                + alias; // TODO dialect
                        readAclIdCol = alias + '.' + model.HIER_READ_ACL_ID;
                        readAclAclIdCol = alias + '.'
                                + model.HIER_READ_ACL_ACL_ID;
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
                virtualColumns);
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
            String fragment = table.getName();
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
            Column column = getColumn(col);
            if (column != null) {
                col.setInfo(column);
                recordColumnFragment(qual, column);
                String sql = column.getFullQuotedName();
                SqlColumn c = new SqlColumn(sql, column, key);
                realColumns.add(c);
                if (typeInfo != null) {
                    TypeDefinition type = col.getTypeDefinition();
                    PropertyDefinition<?> pd = type.getPropertyDefinitions().get(
                            col.getPropertyId());
                    if (pd.getCardinality() == Cardinality.MULTI) {
                        throw new QueryMakerException(
                                "Cannot SELECT on multi-value column: "
                                        + col.getPropertyQueryName());
                    }
                    typeInfo.put(key, pd);
                }
            } else {
                virtualColumns.put(key, col);
                virtualColumnNames.add(key);
            }
        }
    }

    /**
     * Records a WHERE / ORDER BY selector, and associates it to a database
     * column.
     */
    protected void recordSelector(CmisSelector sel, boolean inOrderBy) {
        if (sel instanceof FunctionReference) {
            FunctionReference fr = (FunctionReference) sel;
            if (!inOrderBy) {
                throw new QueryMakerException(
                        "Cannot use function in WHERE clause: "
                                + fr.getFunction());
            }
            // ORDER BY SCORE, nothing further to record
            if (fulltextMatchInfo == null) {
                throw new QueryMakerException(
                        "Cannot use ORDER BY SCORE without CONTAINS");
            }
            return;
        }
        ColumnReference col = (ColumnReference) sel;
        TypeDefinition type = col.getTypeDefinition();
        PropertyDefinition<?> pd = type.getPropertyDefinitions().get(
                col.getPropertyId());
        boolean multi = pd.getCardinality() == Cardinality.MULTI;

        // fetch column and associate it to the selector
        Column column = getColumn(col);
        if (column == null) {
            throw new QueryMakerException("Cannot use column in WHERE clause: "
                    + col.getPropertyQueryName());
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
        String fragment = table.getRealTable().getName();
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
        String qual = col.getTypeQueryName();
        String id = col.getPropertyQueryName();
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
        return "_" + qual + "_" + table.getName();
    }

    /**
     * Map maker that can deal with aliased column names and computed values.
     */
    // static to avoid keeping the whole QueryMaker in the returned object
    public static class CMISQLMapMaker implements MapMaker {

        protected List<SqlColumn> realColumns;

        protected Map<String, ColumnReference> virtualColumns;

        public CMISQLMapMaker(List<SqlColumn> realColumns,
                Map<String, ColumnReference> virtualColumns) {
            this.realColumns = realColumns;
            this.virtualColumns = virtualColumns;
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
            Map<String, DocumentModel> docs = null;
            for (Entry<String, ColumnReference> vc : virtualColumns.entrySet()) {
                String key = vc.getKey();
                ColumnReference col = vc.getValue();
                String qual = col.getTypeQueryName();
                if (docs == null) {
                    docs = new HashMap<String, DocumentModel>(2);
                }
                DocumentModel doc = docs.get(qual);
                if (doc == null) {
                    // find main id for this qualifier in the result set
                    // (main id always included in joins)
                    // SqlColumn idsc = null; //
                    // getSpecialColumn(PropertyIds.OBJECT_ID,
                    // qual);
                    String id = null; // (String) map.get(getColumnName(idsc));
                    try {
                        // reentrant call to the same session, but the MapMaker
                        // is only called from the IterableQueryResult in
                        // queryAndFetch which manipulates no session state
                        // doc = conn.session.getDocument(new IdRef(id));
                        doc = null;
                        throw new ClientException();
                    } catch (ClientException e) {
                        log.error("Cannot get document: " + id, e);
                    }
                    docs.put(qual, doc);
                }
                Serializable v;
                if (doc == null) {
                    // could not fetch
                    v = null;
                } else {
                    // TODO avoid fecthing doc and using a NuxeoProperty to
                    // compute things like cmis:baseTypeId
                    // PropertyDefinition pd =
                    // SimpleType.PROPS_MAP.get(vc.name);
                    // Property p = NuxeoProperty.construct(vc.name, pd,
                    // new DocHolder(doc));
                    // v = p.getValue();
                    v = null;
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
        public boolean walkContains(Tree opNode, Tree qualNode, Tree queryNode) {
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
            return false;
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
        public boolean walkNot(Tree opNode, Tree node) {
            whereBuf.append("NOT ");
            walkPredicate(node);
            return false;
        }

        @Override
        public boolean walkAnd(Tree opNode, Tree leftNode, Tree rightNode) {
            whereBuf.append("(");
            walkPredicate(leftNode);
            whereBuf.append(" AND ");
            walkPredicate(rightNode);
            whereBuf.append(")");
            return false;
        }

        @Override
        public boolean walkOr(Tree opNode, Tree leftNode, Tree rightNode) {
            whereBuf.append("(");
            walkPredicate(leftNode);
            whereBuf.append(" OR ");
            walkPredicate(rightNode);
            whereBuf.append(")");
            return false;
        }

        @Override
        public boolean walkEquals(Tree opNode, Tree leftNode, Tree rightNode) {
            walkExpr(leftNode);
            whereBuf.append(" = ");
            walkExpr(rightNode);
            return false;
        }

        @Override
        public boolean walkNotEquals(Tree opNode, Tree leftNode, Tree rightNode) {
            walkExpr(leftNode);
            whereBuf.append(" <> ");
            walkExpr(rightNode);
            return false;
        }

        @Override
        public boolean walkGreaterThan(Tree opNode, Tree leftNode,
                Tree rightNode) {
            walkExpr(leftNode);
            whereBuf.append(" > ");
            walkExpr(rightNode);
            return false;
        }

        @Override
        public boolean walkGreaterOrEquals(Tree opNode, Tree leftNode,
                Tree rightNode) {
            walkExpr(leftNode);
            whereBuf.append(" >= ");
            walkExpr(rightNode);
            return false;
        }

        @Override
        public boolean walkLessThan(Tree opNode, Tree leftNode, Tree rightNode) {
            walkExpr(leftNode);
            whereBuf.append(" < ");
            walkExpr(rightNode);
            return false;
        }

        @Override
        public boolean walkLessOrEquals(Tree opNode, Tree leftNode,
                Tree rightNode) {
            walkExpr(leftNode);
            whereBuf.append(" <= ");
            walkExpr(rightNode);
            return false;
        }

        @Override
        public boolean walkIn(Tree opNode, Tree colNode, Tree listNode) {
            walkExpr(colNode);
            whereBuf.append(" IN ");
            walkExpr(listNode);
            return false;
        }

        @Override
        public boolean walkNotIn(Tree opNode, Tree colNode, Tree listNode) {
            walkExpr(colNode);
            whereBuf.append(" NOT IN ");
            walkExpr(listNode);
            return false;
        }

        @Override
        public boolean walkInAny(Tree opNode, Tree colNode, Tree listNode) {
            walkAny(colNode, "IN", listNode);
            return false;
        }

        @Override
        public boolean walkNotInAny(Tree opNode, Tree colNode, Tree listNode) {
            walkAny(colNode, "NOT IN", listNode);
            return false;
        }

        @Override
        public boolean walkEqAny(Tree opNode, Tree literalNode, Tree colNode) {
            // note that argument order is reversed
            walkAny(colNode, "=", literalNode);
            return false;
        }

        protected void walkAny(Tree colNode, String op, Tree exprNode) {
            int token = ((Tree) colNode).getTokenStartIndex();
            ColumnReference col = (ColumnReference) query.getColumnReference(Integer.valueOf(token));
            TypeDefinition type = col.getTypeDefinition();
            PropertyDefinition<?> pd = type.getPropertyDefinitions().get(
                    col.getPropertyId());
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
        public boolean walkIsNull(Tree opNode, Tree colNode) {
            walkExpr(colNode);
            whereBuf.append(" IS NULL");
            return false;
        }

        @Override
        public boolean walkIsNotNull(Tree opNode, Tree colNode) {
            walkExpr(colNode);
            whereBuf.append(" IS NOT NULL");
            return false;
        }

        @Override
        public boolean walkLike(Tree opNode, Tree colNode, Tree stringNode) {
            walkExpr(colNode);
            whereBuf.append(" LIKE ");
            walkExpr(stringNode);
            return false;
        }

        @Override
        public boolean walkNotLike(Tree opNode, Tree colNode, Tree stringNode) {
            walkExpr(colNode);
            whereBuf.append(" NOT LIKE ");
            walkExpr(stringNode);
            return false;
        }

        @Override
        public boolean walkContains(Tree opNode, Tree qualNode, Tree queryNode) {
            if (fulltextMatchInfo.joins != null) {
                ftJoins.addAll(fulltextMatchInfo.joins);
            }
            whereBuf.append(fulltextMatchInfo.whereExpr);
            if (fulltextMatchInfo.whereExprParam != null) {
                whereBufParams.add(fulltextMatchInfo.whereExprParam);
            }
            return false;
        }

        @Override
        public boolean walkInFolder(Tree opNode, Tree qualNode, Tree paramNode) {
            String qual = qualNode == null ? null : qualNode.getText();
            // this is from the hierarchy table which is always present
            Column column = getSystemColumn(qual, PropertyIds.PARENT_ID);
            whereBuf.append(column.getFullQuotedName());
            whereBuf.append(" = ?");
            String id = (String) super.walkString(paramNode);
            whereBufParams.add(id);
            return false;
        }

        @Override
        public boolean walkInTree(Tree opNode, Tree qualNode, Tree paramNode) {
            String qual = qualNode == null ? null : qualNode.getText();
            Column column = getSystemColumn(qual, PropertyIds.OBJECT_ID);
            String sql = dialect.getInTreeSql(column.getFullQuotedName());
            String id = (String) super.walkString(paramNode);
            whereBuf.append(sql);
            whereBufParams.add(id);
            return false;
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
