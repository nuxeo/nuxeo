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

package org.nuxeo.ecm.core.storage.sql.jdbc;

import java.io.Serializable;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FullTextUtils;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;
import org.nuxeo.ecm.core.query.sql.model.BooleanLiteral;
import org.nuxeo.ecm.core.query.sql.model.DateLiteral;
import org.nuxeo.ecm.core.query.sql.model.DefaultQueryVisitor;
import org.nuxeo.ecm.core.query.sql.model.DoubleLiteral;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.FromClause;
import org.nuxeo.ecm.core.query.sql.model.FromList;
import org.nuxeo.ecm.core.query.sql.model.Function;
import org.nuxeo.ecm.core.query.sql.model.IntegerLiteral;
import org.nuxeo.ecm.core.query.sql.model.Literal;
import org.nuxeo.ecm.core.query.sql.model.LiteralList;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operand;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.OrderByList;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.query.sql.model.SelectClause;
import org.nuxeo.ecm.core.query.sql.model.StringLiteral;
import org.nuxeo.ecm.core.query.sql.model.WhereClause;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.ColumnType;
import org.nuxeo.ecm.core.storage.sql.ColumnType.WrappedId;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.ModelProperty;
import org.nuxeo.ecm.core.storage.sql.Session.PathResolver;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo.ColumnMapMaker;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo.SQLInfoSelect;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Database;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Join;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Select;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.TableAlias;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect.FulltextMatchInfo;

/**
 * Transformer of NXQL queries into underlying SQL queries to the actual
 * database.
 * <p>
 * The examples below are using the NXQL statement syntax:
 *
 * <pre>
 * SELECT * FROM File
 * WHERE
 *   dc:title = 'abc'
 *   AND uid:uid = '123'
 *   AND dc:contributors = 'bob'    -- multi-valued
 * </pre>
 *
 * If there are no proxies (ecm:isProxy = 0) we get:
 *
 * <pre>
 * SELECT hierarchy.id
 *   FROM hierarchy
 *   LEFT JOIN dublincore ON hierarchy.id = dublincore.id
 *   LEFT JOIN uid ON hierarchy.id = uid.id
 * WHERE
 *   hierarchy.primarytype IN ('File', 'SubFile')
 *   AND dublincore.title = 'abc'
 *   AND uid.uid = '123'
 *   AND EXISTS (SELECT 1 FROM dc_contributors WHERE hierarchy.id = dc_contributors.id
 *               AND dc_contributors.item = 'bob')
 *   AND NX_ACCESS_ALLOWED(hierarchy.id, 'user1|user2', 'perm1|perm2')
 * </pre>
 *
 * The data tables (dublincore, uid) are joined using a LEFT JOIN, as the schema
 * may not be present on all documents but this shouldn't prevent the WHERE
 * clause from being evaluated.
 *
 * Complex properties are matched using an EXISTS and a subselect.
 *
 * When proxies are matched (ecm:isProxy = 1) there are two additional FULL
 * JOINs. Security checks, id, name, parents and path use the base hierarchy
 * (_H), but all other data use the joined hierarchy.
 *
 * <pre>
 * SELECT _H.id
 *   FROM hierarchy _H
 *   JOIN proxies ON _H.id = proxies.id                     -- proxy full join
 *   JOIN hierarchy ON hierarchy.id = proxies.targetid      -- proxy full join
 *   LEFT JOIN dublincore ON hierarchy.id = dublincore.id
 *   LEFT JOIN uid ON hierarchy.id = uid.id
 * WHERE
 *   hierarchy.primarytype IN ('File', 'SubFile')
 *   AND dublincore.title = 'abc'
 *   AND uid.uid = '123'
 *   AND EXISTS (SELECT 1 FROM dc_contributors WHERE hierarchy.id = dc_contributors.id
 *               AND dc_contributors.item = 'bob')
 *   AND NX_ACCESS_ALLOWED(_H.id, 'user1|user2', 'perm1|perm2') -- uses _H
 * </pre>
 *
 * When both normal documents and proxies are matched, we UNION ALL the two
 * queries. If an ORDER BY is requested, then columns from the inner SELECTs
 * have to be aliased so that an outer ORDER BY can user their names.
 *
 * @author Florent Guillaume
 */
public class NXQLQueryMaker implements QueryMaker {

    private static final Log log = LogFactory.getLog(NXQLQueryMaker.class);

    public static final String TYPE_DOCUMENT = "Document";

    public static final String TYPE_RELATION = "Relation";

    public static final String TYPE_TAGGING = "Tagging";

    public static final String RELATION_TABLE = "relation";

    public static final String ECM_TAG_STAR = NXQL.ECM_TAG + "/*";

    protected static final String TABLE_HIER_ALIAS = "_H";

    protected static final String TABLE_FRAG_ALIAS = "_F";

    protected static final String COL_ALIAS_PREFIX = "_C";

    protected static final String UNION_ALIAS = "_T";

    protected static final String WITH_ALIAS_PREFIX = "_W";

    protected static final String READ_ACL_ALIAS = "_RACL";

    protected static final String DATE_CAST = "DATE";

    /**
     * These mixins never match an instance mixin when used in a clause
     * ecm:mixinType = 'foo'
     */
    protected static final Set<String> MIXINS_NOT_PER_INSTANCE = new HashSet<String>(
            Arrays.asList(FacetNames.FOLDERISH, FacetNames.HIDDEN_IN_NAVIGATION));

    /*
     * Fields used by the search service.
     */

    protected SQLInfo sqlInfo;

    protected Database database;

    protected Dialect dialect;

    protected Model model;

    protected PathResolver pathResolver;

    /** Do we match only relations (and therefore no proxies). */
    protected boolean onlyRelations;

    protected final List<String> whatColumnNames = new LinkedList<String>();

    protected final List<String> orderByColumnNames = new LinkedList<String>();

    protected final Map<String, String> aliasesByName = new HashMap<String, String>();

    protected final List<String> aliases = new LinkedList<String>();

    protected boolean hasWildcardIndex;

    protected boolean orderByHasWildcardIndex;

    protected Boolean proxyClause;

    // The hierarchy table for the hierarchy/name, may be an alias table
    protected Table hierTable;

    // The hierarchy table of the data
    protected Table dataHierTable;

    protected List<Join> joins;

    protected List<String> whereClauses;

    protected List<Serializable> whereParams;

    // fragmentName or prefix/fragmentName -> fragment table to join
    protected Map<String, Table> propertyFragmentTables = new HashMap<String, Table>();

    protected int fragJoinCount = 0;

    @Override
    public String getName() {
        return NXQL.NXQL;
    }

    @Override
    public boolean accepts(String queryType) {
        return queryType.equals(NXQL.NXQL);
    }

    public enum DocKind {
        DIRECT, PROXY;
    }

    @Override
    public Query buildQuery(SQLInfo sqlInfo, Model model,
            PathResolver pathResolver, String query, QueryFilter queryFilter,
            Object... params) throws StorageException {
        this.sqlInfo = sqlInfo;
        database = sqlInfo.database;
        dialect = sqlInfo.dialect;
        this.model = model;
        this.pathResolver = pathResolver;
        // transform the query according to the transformers defined by the
        // security policies
        SQLQuery sqlQuery = SQLQueryParser.parse(query);
        for (SQLQuery.Transformer transformer : queryFilter.getQueryTransformers()) {
            sqlQuery = transformer.transform(queryFilter.getPrincipal(),
                    sqlQuery);
        }

        // SELECT * -> SELECT ecm:uuid
        boolean selectStar = sqlQuery.select.isEmpty();
        if (selectStar) {
            sqlQuery.select.add(new Reference(NXQL.ECM_UUID));
        }

        /*
         * Find all relevant types and keys for the criteria.
         */

        QueryAnalyzer queryAnalyzer = newQueryAnalyzer(queryFilter.getFacetFilter());
        try {
            queryAnalyzer.visitQuery(sqlQuery);
        } catch (QueryCannotMatchException e) {
            // query cannot match
            return null;
        } catch (QueryMakerException e) {
            throw new StorageException(e.getMessage(), e);
        }

        /*
         * Find whether to check proxies, relations.
         */

        if (!model.getRepositoryDescriptor().proxiesEnabled) {
            if (proxyClause == Boolean.TRUE) {
                throw new StorageException(
                        "Proxies are disabled by configuration, a query with "
                                + NXQL.ECM_ISPROXY + " = 1 is disallowed");
            }
            proxyClause = Boolean.FALSE;
        }
        if (onlyRelations) {
            if (proxyClause == Boolean.TRUE) {
                // no proxies to relations, query cannot match
                return null;
            }
            proxyClause = Boolean.FALSE;
        }
        DocKind[] docKinds;
        if (proxyClause == Boolean.TRUE) {
            docKinds = new DocKind[] { DocKind.PROXY };
        } else if (proxyClause == Boolean.FALSE) {
            docKinds = new DocKind[] { DocKind.DIRECT };
        } else {
            docKinds = new DocKind[] { DocKind.DIRECT, DocKind.PROXY };
        }
        boolean doUnion = docKinds.length > 1;

        /*
         * DISTINCT check and add additional selected columns for ORDER BY.
         */

        Set<String> onlyOrderByColumnNames = new HashSet<String>(
                orderByColumnNames);
        onlyOrderByColumnNames.removeAll(whatColumnNames);
        boolean distinct = sqlQuery.select.isDistinct();
        if (selectStar && hasWildcardIndex) {
            distinct = true;
        }

        if (doUnion || distinct) {
            // if UNION, we need all the ORDER BY columns in the SELECT list
            // for aliasing
            if (distinct && !onlyOrderByColumnNames.isEmpty()) {
                // if DISTINCT, check that the ORDER BY columns are all in the
                // SELECT list
                if (!selectStar) {
                    throw new StorageException(
                            "For SELECT DISTINCT the ORDER BY columns must be in the SELECT list, missing: "
                                    + onlyOrderByColumnNames);
                }
                // for a SELECT *, we can add the needed columns if they
                // don't involve wildcard index array elements
                if (orderByHasWildcardIndex) {
                    throw new StorageException(
                            "For SELECT * the ORDER BY columns cannot use wildcard indexes");
                }
            }
            for (String name : onlyOrderByColumnNames) {
                sqlQuery.select.add(new Reference(name));
            }
        }

        /*
         * Build the FROM / JOIN criteria for each select.
         */

        String from;
        List<Column> whatColumns = null;
        List<String> whatKeys = null;
        Select select = null;
        String orderBy = null;
        List<String> statements = new ArrayList<String>(2);
        List<Serializable> selectParams = new LinkedList<Serializable>();
        List<String> withTables = new LinkedList<String>();
        List<Select> withSelects = new LinkedList<Select>();
        List<String> withSelectsStatements = new LinkedList<String>();
        List<Serializable> withParams = new LinkedList<Serializable>();
        Table hier = database.getTable(model.HIER_TABLE_NAME);

        for (DocKind docKind : docKinds) {

            // Quoted id in the hierarchy. This is the id returned by the query.
            String hierId;

            joins = new LinkedList<Join>();
            whereClauses = new LinkedList<String>();
            whereParams = new LinkedList<Serializable>();
            propertyFragmentTables = new HashMap<String, Table>();
            fragJoinCount = 0;

            switch (docKind) {
            case DIRECT:
                hierTable = hier;
                dataHierTable = hierTable;
                hierId = hierTable.getColumn(model.MAIN_KEY).getFullQuotedName();
                from = hierTable.getQuotedName();
                break;
            case PROXY:
                hierTable = new TableAlias(hier, TABLE_HIER_ALIAS);
                dataHierTable = hier;
                // TODO use dialect
                from = hier.getQuotedName() + " " + hierTable.getQuotedName();
                hierId = hierTable.getColumn(model.MAIN_KEY).getFullQuotedName();
                // proxies
                Table proxies = database.getTable(model.PROXY_TABLE_NAME);
                // join all that
                addJoin(Join.INNER, null, proxies, model.MAIN_KEY, hierTable,
                        model.MAIN_KEY, null, -1, null);
                addJoin(Join.INNER, null, dataHierTable, model.MAIN_KEY,
                        proxies, model.PROXY_TARGET_KEY, null, -1, null);
                break;
            default:
                throw new AssertionError(docKind);
            }
            fixInitialJoins();

            /*
             * Process WHAT to select.
             */

            WhereBuilder whereBuilder;
            try {
                whereBuilder = newWhereBuilder(docKind == DocKind.PROXY);
            } catch (QueryMakerException e) {
                throw new StorageException(e.getMessage(), e);
            }
            sqlQuery.select.accept(whereBuilder);
            whatColumns = whereBuilder.whatColumns;
            whatKeys = whereBuilder.whatKeys;

            // alias columns in all cases to simplify logic
            List<String> whatNames = new ArrayList<String>(1);
            List<Serializable> whatNamesParams = new ArrayList<Serializable>(1);
            String mainAlias = hierId;
            aliasesByName.clear();
            aliases.clear();
            for (int i = 0; i < whatColumns.size(); i++) {
                Column col = whatColumns.get(i);
                String key = whatKeys.get(i);
                String alias = dialect.openQuote() + COL_ALIAS_PREFIX
                        + (i+1) + dialect.closeQuote();
                aliasesByName.put(key, alias);
                aliases.add(alias);
                String whatName = getSelectColName(col);
                whatName += " AS " + alias;
                if (col.getTable().getRealTable() == hier
                        && col.getKey().equals(model.MAIN_KEY)) {
                    mainAlias = alias;
                }
                whatNames.add(whatName);
            }

            fixWhatColumns(whatColumns);

            /*
             * Process WHERE.
             */

            if (queryAnalyzer.wherePredicate != null) {
                queryAnalyzer.wherePredicate.accept(whereBuilder);
                // WHERE clause
                String where = whereBuilder.buf.toString();
                if (where.length() != 0) {
                    whereClauses.add(where);
                }
            }

            /*
             * Process ORDER BY.
             */

            boolean orderByScoreDesc = sqlQuery.orderBy == null
                    && whereBuilder.ftJoinNumber == 1 && !distinct;
            FulltextMatchInfo ftMatchInfo = whereBuilder.ftMatchInfo;

            // ORDER BY computed just once; may use just aliases
            if (orderBy == null) {
                if (sqlQuery.orderBy != null) {
                    whereBuilder.aliasOrderByColumns = doUnion;
                    whereBuilder.buf.setLength(0);
                    sqlQuery.orderBy.accept(whereBuilder);
                    // ends up in WhereBuilder#visitOrderByExpr
                    orderBy = whereBuilder.buf.toString();
                } else if (orderByScoreDesc) {
                    // add order by score desc
                    orderBy = ftMatchInfo.scoreAlias + " DESC";
                }
            }

            if (orderByScoreDesc) {
                // add score expression to selected columns
                String scoreExprSql = ftMatchInfo.scoreExpr + " AS "
                        + ftMatchInfo.scoreAlias;
                whatNames.add(scoreExprSql);
                if (ftMatchInfo.scoreExprParam != null) {
                    whatNamesParams.add(ftMatchInfo.scoreExprParam);
                }
            }

            String selectWhat = StringUtils.join(whatNames, ", ");
            if (!doUnion && distinct) {
                selectWhat = "DISTINCT " + selectWhat;
            }

            /*
             * Soft delete.
             */

            if (model.getRepositoryDescriptor().softDeleteEnabled) {
                whereClauses.add(hierTable.getColumn(model.MAIN_IS_DELETED_KEY).getFullQuotedName()
                        + " IS NULL");
            }

            /*
             * Security check.
             */

            String securityClause = null;
            List<Serializable> securityParams = new LinkedList<Serializable>();
            Join securityJoin = null;
            if (queryFilter.getPrincipals() != null) {
                Serializable principals = queryFilter.getPrincipals();
                Serializable permissions = queryFilter.getPermissions();
                if (!dialect.supportsArrays()) {
                    principals = StringUtils.join((String[]) principals, Dialect.ARRAY_SEP);
                    permissions = StringUtils.join((String[]) permissions, Dialect.ARRAY_SEP);
                }
                // when using WITH for the query, the main column is referenced
                // through an alias because of the subselect
                String id = dialect.supportsWith() ? mainAlias : hierId;
                if (dialect.supportsReadAcl()) {
                    /* optimized read acl */
                    String racl = dialect.openQuote() + READ_ACL_ALIAS
                            + dialect.closeQuote();
                    securityClause = dialect.getReadAclsCheckSql(racl
                            + ".acl_id");
                    securityParams.add(principals);
                    securityJoin = new Join(Join.INNER,
                            model.HIER_READ_ACL_TABLE_NAME, READ_ACL_ALIAS,
                            null, id, racl + ".id");
                } else {
                    securityClause = dialect.getSecurityCheckSql(id);
                    securityParams.add(principals);
                    securityParams.add(permissions);
                }
            }

            /*
             * Resulting select.
             */

            if (securityClause != null) {
                if (dialect.supportsWith()) {
                    // wrap security into a WITH
                    String withTable = dialect.openQuote() + WITH_ALIAS_PREFIX
                            + (statements.size() + 1) + dialect.closeQuote();
                    withTables.add(withTable);
                    Select withSelect = new Select(null);
                    withSelect.setWhat("*");
                    withSelect.setFrom(withTable
                            + (securityJoin == null ? ""
                                    : (" " + securityJoin.toSql(dialect))));
                    withSelect.setWhere(securityClause);
                    withSelects.add(withSelect);
                    withSelectsStatements.add(withSelect.getStatement());
                    withParams.addAll(securityParams);
                } else {
                    // add directly to main select
                    if (securityJoin != null) {
                        joins.add(securityJoin);
                    }
                    whereClauses.add(securityClause);
                    whereParams.addAll(securityParams);
                }
            }

            select = new Select(null);
            select.setWhat(selectWhat);
            selectParams.addAll(whatNamesParams);

            StringBuilder fromb = new StringBuilder(from);
            if (dialect.needsOracleJoins() && doUnion && securityJoin != null
                    && ftMatchInfo != null) {
                // NXP-5410 we must use Oracle joins
                // when there's union all + fulltext + security
                for (Join join : joins) {
                    if (!join.whereClauses.isEmpty()) {
                        // we cannot use Oracle join when there are join filters
                        throw new StorageException(
                                "Query too complex for Oracle (NXP-5410)");
                    }
                }
                // implicit joins for Oracle
                List<String> joinClauses = new LinkedList<String>();
                for (Join join : joins) {
                    fromb.append(", ");
                    fromb.append(join.getTable(dialect));
                    if (join.tableParam != null) {
                        selectParams.add(join.tableParam);
                    }
                    String joinClause = join.getClause(dialect);
                    if (join.kind == Join.LEFT) {
                        joinClause += "(+)"; // Oracle implicit LEFT JOIN syntax
                    }
                    if (!join.whereClauses.isEmpty()) {
                        joinClause += " AND "
                                + StringUtils.join(join.whereClauses, " AND ");
                        selectParams.addAll(join.whereParams);
                    }
                    joinClauses.add(joinClause);
                }
                whereClauses.addAll(0, joinClauses);
            } else {
                // else ANSI join
                Collections.sort(joins); // implicit JOINs last (PostgreSQL)
                for (Join join : joins) {
                    if (join.tableParam != null) {
                        selectParams.add(join.tableParam);
                    }
                    String joinClause = join.toSql(dialect);
                    // add JOIN filter for complex properties
                    if (!join.whereClauses.isEmpty()) {
                        joinClause += " AND "
                                + StringUtils.join(join.whereClauses, " AND ");
                        selectParams.addAll(join.whereParams);
                    }
                    fromb.append(joinClause);
                }
            }

            select.setFrom(fromb.toString());
            select.setWhere(StringUtils.join(whereClauses, " AND "));
            selectParams.addAll(whereParams);

            statements.add(select.getStatement());
        }

        /*
         * Create the whole select.
         */

        if (doUnion) {
            select = new Select(null);
            // use aliases for column names
            String selectWhat = StringUtils.join(aliases, ", ");
            if (distinct) {
                selectWhat = "DISTINCT " + selectWhat;
            }
            select.setWhat(selectWhat);
            // note that Derby has bizarre restrictions on parentheses placement
            // around UNION, see http://issues.apache.org/jira/browse/DERBY-2374
            String subselect;
            if (withSelects.isEmpty()) {
                subselect = StringUtils.join(statements, " UNION ALL ");
            } else {
                StringBuilder with = new StringBuilder("WITH ");
                for (int i = 0; i < statements.size(); i++) {
                    if (i > 0) {
                        with.append(", ");
                    }
                    with.append(withTables.get(i));
                    with.append(" AS (");
                    with.append(statements.get(i));
                    with.append(')');
                }
                with.append(' ');
                subselect = with.toString()
                        + StringUtils.join(withSelectsStatements, " UNION ALL ");
                selectParams.addAll(withParams);
            }
            String selectFrom = '(' + subselect + ')';
            if (dialect.needsAliasForDerivedTable()) {
                selectFrom += " AS " + dialect.openQuote() + UNION_ALIAS
                        + dialect.closeQuote();
            }
            select.setFrom(selectFrom);
        } else {
            // use last (and only) Select in above big loop
            if (!withSelects.isEmpty()) {
                select = new Select(null);
                String with = withTables.get(0) + " AS (" + statements.get(0)
                        + ')';
                select.setWith(with);
                Select withSelect = withSelects.get(0);
                select.setWhat(withSelect.getWhat());
                select.setFrom(withSelect.getFrom());
                select.setWhere(withSelect.getWhere());
                selectParams.addAll(withParams);
            }
        }

        select.setOrderBy(orderBy);
        fixSelect(select);

        Query q = new Query();
        ColumnMapMaker mapMaker = new ColumnMapMaker(whatColumns, whatKeys);
        q.selectInfo = new SQLInfoSelect(select.getStatement(), whatColumns,
                mapMaker, null, null);
        q.selectParams = selectParams;
        return q;
    }

    // overridden by specialized query makers that need to tweak some joins
    protected void addJoin(int kind, String alias, Table table, String column,
            Table contextTable, String contextColumn, String name, int index,
            String primaryType) {
        Column column1 = contextTable.getColumn(contextColumn);
        Column column2 = table.getColumn(column);
        Join join = new Join(kind, table.getRealTable().getQuotedName(), alias,
                null, column1, column2);
        if (name != null) {
            String nameCol = table.getColumn(model.HIER_CHILD_NAME_KEY).getFullQuotedName();
            join.addWhereClause(nameCol + " = ?", name);
        }
        if (index != -1) {
            String posCol = table.getColumn(model.HIER_CHILD_POS_KEY).getFullQuotedName();
            join.addWhereClause(posCol + " = ?", Long.valueOf(index));
        }
        if (primaryType != null) {
            String typeCol = table.getColumn(model.MAIN_PRIMARY_TYPE_KEY).getFullQuotedName();
            join.addWhereClause(typeCol + " = ?", primaryType);
        }
        joins.add(join);
    }

    /**
     * Gets the table for the given fragmentName in the given contextKey, and
     * maybe adds a join if one is not already done.
     * <p>
     * LEFT JOIN fragmentName _F123 ON contextHier.id = _F123.id
     */
    protected Table getFragmentTable(Table contextHier, String contextKey,
            String fragmentName, int index, boolean skipJoin) {
        return getFragmentTable(Join.LEFT, contextHier, contextKey, fragmentName,
                model.MAIN_KEY, index, skipJoin, null);
    }

    /**
     * Adds a more general JOIN:
     * <p>
     * (LEFT) JOIN fragmentName _F123 ON contextTable.id = _F123.fragmentColumn
     */
    protected Table getFragmentTable(int joinKind, Table contextTable,
            String contextKey, String fragmentName, String fragmentColumn,
            int index, boolean skipJoin, String primaryType) {
        Table table = propertyFragmentTables.get(contextKey);
        if (table == null) {
            Table baseTable = database.getTable(fragmentName);
            String alias = TABLE_FRAG_ALIAS + ++fragJoinCount;
            table = new TableAlias(baseTable, alias);
            propertyFragmentTables.put(contextKey, table);
            if (!skipJoin) {
                addJoin(joinKind, alias, table, fragmentColumn, contextTable,
                        model.MAIN_KEY, null, index, primaryType);
            }
        }
        return table;
    }

    // overridden by specialized query makers that need to tweak some joins
    protected void fixInitialJoins() {
        // to be overridden
    }

    // overridden by specialized query makers that need to add COUNT
    protected String getSelectColName(Column col) {
        return col.getFullQuotedName();
    }

    // overridden by specialized query makers that need to add COUNT
    protected void fixWhatColumns(List<Column> whatColumns) {
        // to be overridden
    }

    // overridden by specialized query makers that need to add GROUP BY
    protected void fixSelect(Select select) {
        // to be overridden
    }

    protected static boolean findFulltextIndexOrField(Model model,
            String[] nameref) {
        boolean useIndex;
        String name = nameref[0];
        if (name.equals(NXQL.ECM_FULLTEXT)) {
            name = Model.FULLTEXT_DEFAULT_INDEX;
            useIndex = true;
        } else {
            // ecm:fulltext_indexname
            // ecm:fulltext.field
            char sep = name.charAt(NXQL.ECM_FULLTEXT.length());
            if (sep != '.' && sep != '_') {
                throw new QueryMakerException("Unknown field: " + name);
            }
            useIndex = sep == '_';
            name = name.substring(NXQL.ECM_FULLTEXT.length() + 1);
            if (useIndex) {
                if (!model.fulltextInfo.indexNames.contains(name)) {
                    throw new QueryMakerException("No such fulltext index: "
                            + name);
                }
            } else {
                // find if there's an index holding just that field
                String index = model.fulltextInfo.fieldToIndexName.get(name);
                if (index != null) {
                    name = index;
                    useIndex = true;
                }
            }
        }
        nameref[0] = name;
        return useIndex;
    }

    // digits or star or star followed by digits, for segments
    protected final static Pattern INDEX = Pattern.compile("\\d+|\\*|\\*\\d+");

    // wildcard index in xpath
    protected final static Pattern HAS_WILDCARD_INDEX = Pattern.compile(".*/(\\*|\\*\\d+)(/.*|$)");

    // digits or star or star followed by digits, then slash, for replaceAll
    protected final static Pattern INDEX_SLASH = Pattern.compile("/(?:\\d+|\\*|\\*\\d+)(/|$)");

    // non-canonical index syntax, for replaceAll
    protected final static Pattern NON_CANON_INDEX = Pattern.compile("[^/\\[\\]]+" // name
            + "\\[(\\d+|\\*|\\*\\d+)\\]" // index in brackets
    );

    /**
     * Canonicalizes a Nuxeo-xpath.
     * <p>
     * Replaces {@code a/foo[123]/b} with {@code a/123/b}
     * <p>
     * A star or a star followed by digits can be used instead of just the
     * digits as well.
     *
     * @param xpath the xpath
     * @return the canonicalized xpath.
     */
    public static String canonicalXPath(String xpath) {
        while (xpath.length() > 0 && xpath.charAt(0) == '/') {
            xpath = xpath.substring(1);
        }
        if (xpath.indexOf('[') == -1) {
            return xpath;
        } else {
            return NON_CANON_INDEX.matcher(xpath).replaceAll("$1");
        }
    }

    /**
     * Turns the xpath into one where all indices have been replaced by *.
     *
     * @param xpath the xpath
     * @return the simple xpath
     */
    public static String simpleXPath(String xpath) {
        xpath = canonicalXPath(xpath);
        return INDEX_SLASH.matcher(xpath).replaceAll("/*$1");
    }

    public boolean hasWildcardIndex(String xpath) {
        xpath = canonicalXPath(xpath);
        return HAS_WILDCARD_INDEX.matcher(xpath).matches();
    }

    protected QueryAnalyzer newQueryAnalyzer(FacetFilter facetFilter) {
        return new QueryAnalyzer(facetFilter);
    }

    protected static Set<String> getStringLiterals(LiteralList list)
            throws QueryMakerException {
        Set<String> set = new HashSet<String>();
        for (Literal literal : list) {
            if (!(literal instanceof StringLiteral)) {
                throw new QueryMakerException("requires string literals");
            }
            set.add(((StringLiteral) literal).value);
        }
        return set;
    }

    /**
     * Collects various info about the query AST, and rewrites the toplevel AND
     * {@link Predicate}s of the WHERE clause into a single
     * {@link MultiExpression} for easier analysis.
     */
    protected class QueryAnalyzer extends DefaultQueryVisitor {

        private static final long serialVersionUID = 1L;

        protected boolean inSelect;

        protected boolean inOrderBy;

        protected final LinkedList<Operand> toplevelOperands = new LinkedList<Operand>();

        protected MultiExpression wherePredicate;

        public QueryAnalyzer(FacetFilter facetFilter) {
            if (facetFilter != null) {
                addFacetFilterClauses(facetFilter);
            }
        }

        public void addFacetFilterClauses(FacetFilter facetFilter) {
            Expression expr;
            for (String mixin : facetFilter.required) {
                // every facet is required, not just any of them,
                // so do them one by one
                // expr = getMixinsMatchExpression(Collections.singleton(facet),
                // true);
                expr = new Expression(new Reference(NXQL.ECM_MIXINTYPE),
                        Operator.EQ, new StringLiteral(mixin));
                toplevelOperands.add(expr);
            }
            if (!facetFilter.excluded.isEmpty()) {
                // expr = getMixinsMatchExpression(facetFilter.excluded, false);
                LiteralList list = new LiteralList();
                for (String mixin : facetFilter.excluded) {
                    list.add(new StringLiteral(mixin));
                }
                expr = new Expression(new Reference(NXQL.ECM_MIXINTYPE),
                        Operator.NOTIN, list);
                toplevelOperands.add(expr);
            }
        }

        @Override
        public void visitQuery(SQLQuery node) {
            visitSelectClause(node.select);
            visitFromClause(node.from);
            visitWhereClause(node.where); // may be null
            if (node.orderBy != null) {
                visitOrderByClause(node.orderBy);
            }
        }

        @Override
        public void visitSelectClause(SelectClause node) {
            inSelect = true;
            super.visitSelectClause(node);
            inSelect = false;
        }

        /**
         * Finds all the types to take into account (all concrete types being a
         * subtype of the passed types) based on the FROM list.
         * <p>
         * Adds them as a ecm:primaryType match in the toplevel operands.
         */
        @Override
        public void visitFromClause(FromClause node) {
            onlyRelations = true;
            Set<String> fromTypes = new HashSet<String>();
            FromList elements = node.elements;
            for (int i = 0; i < elements.size(); i++) {
                String typeName = elements.get(i);
                if (TYPE_DOCUMENT.equalsIgnoreCase(typeName)) {
                    typeName = TYPE_DOCUMENT;
                }
                Set<String> subTypes = model.getDocumentSubTypes(typeName);
                if (subTypes == null) {
                    throw new QueryMakerException("Unknown type: " + typeName);
                }
                fromTypes.addAll(subTypes);
                boolean isRelation = false;
                do {
                    if (TYPE_RELATION.equals(typeName)) {
                        isRelation = true;
                        break;
                    }
                    typeName = model.getDocumentSuperType(typeName);
                } while (typeName != null);
                onlyRelations = onlyRelations && isRelation;
            }
            fromTypes.remove(model.ROOT_TYPE);
            LiteralList list = new LiteralList();
            for (String type : fromTypes) {
                list.add(new StringLiteral(type));
            }
            toplevelOperands.add(new Expression(new Reference(
                    NXQL.ECM_PRIMARYTYPE), Operator.IN, list));
        }

        @Override
        public void visitWhereClause(WhereClause node) {
            if (node != null) {
                analyzeToplevelOperands(node.predicate);
            }
            simplifyToplevelOperands();
            wherePredicate = new MultiExpression(Operator.AND, toplevelOperands);
            super.visitMultiExpression(wherePredicate);
        }

        /**
         * Process special toplevel ANDed operands: ecm:isProxy
         */
        protected void analyzeToplevelOperands(Operand node) {
            if (node instanceof Expression) {
                Expression expr = (Expression) node;
                Operator op = expr.operator;
                if (op == Operator.AND) {
                    analyzeToplevelOperands(expr.lvalue);
                    analyzeToplevelOperands(expr.rvalue);
                    return;
                }
                if (op == Operator.EQ || op == Operator.NOTEQ) {
                    // put reference on the left side
                    if (expr.rvalue instanceof Reference) {
                        expr = new Expression(expr.rvalue, op, expr.lvalue);
                    }
                    if (expr.lvalue instanceof Reference) {
                        String name = ((Reference) expr.lvalue).name;
                        if (NXQL.ECM_ISPROXY.equals(name)) {
                            analyzeToplevelIsProxy(expr);
                            return;
                        }
                    }
                }
            }
            toplevelOperands.add(node);
        }

        /**
         * Simplify ecm:primaryType positive references, and non-per-instance
         * mixin types.
         */
        protected void simplifyToplevelOperands() {
            Set<String> primaryTypes = null; // if defined, required
            for (Iterator<Operand> it = toplevelOperands.iterator(); it.hasNext();) {
                // whenever we don't know how to optimize the expression,
                // we just continue the loop
                Operand node = it.next();
                if (!(node instanceof Expression)) {
                    continue;
                }
                Expression expr = (Expression) node;
                if (!(expr.lvalue instanceof Reference)) {
                    continue;
                }
                String name = ((Reference) expr.lvalue).name;
                Operator op = expr.operator;
                Operand rvalue = expr.rvalue;
                if (NXQL.ECM_PRIMARYTYPE.equals(name)) {
                    if (op != Operator.EQ && op != Operator.IN) {
                        continue;
                    }
                    Set<String> set;
                    if (op == Operator.EQ) {
                        if (!(rvalue instanceof StringLiteral)) {
                            continue;
                        }
                        String primaryType = ((StringLiteral) rvalue).value;
                        set = new HashSet<String>(
                                Collections.singleton(primaryType));
                    } else { // Operator.IN
                        if (!(rvalue instanceof LiteralList)) {
                            continue;
                        }
                        set = getStringLiterals((LiteralList) rvalue);
                    }
                    if (primaryTypes == null) {
                        primaryTypes = set;
                    } else {
                        primaryTypes.retainAll(set);
                    }
                    it.remove(); // expression simplified into primaryTypes set
                } else if (NXQL.ECM_MIXINTYPE.equals(name)) {
                    if (op != Operator.EQ && op != Operator.NOTEQ) {
                        continue;
                    }
                    if (!(rvalue instanceof StringLiteral)) {
                        continue;
                    }
                    String mixin = ((StringLiteral) rvalue).value;
                    if (!MIXINS_NOT_PER_INSTANCE.contains(mixin)) {
                        // mixin per instance -> primary type checks not enough
                        continue;
                    }
                    Set<String> set = model.getMixinDocumentTypes(mixin);
                    if (primaryTypes == null) {
                        if (op == Operator.EQ) {
                            primaryTypes = new HashSet<String>(set); // copy
                        } else {
                            continue; // unknown positive, no optimization
                        }
                    } else {
                        if (op == Operator.EQ) {
                            primaryTypes.retainAll(set);
                        } else {
                            primaryTypes.removeAll(set);
                        }
                    }
                    it.remove(); // expression simplified into primaryTypes set
                }
            }
            // readd the simplified primary types constraints
            if (primaryTypes != null) {
                if (primaryTypes.isEmpty()) {
                    // TODO better removal
                    primaryTypes.add("__NOSUCHTYPE__");
                }
                Expression expr;
                if (primaryTypes.size() == 1) {
                    String pt = primaryTypes.iterator().next();
                    expr = new Expression(new Reference(NXQL.ECM_PRIMARYTYPE),
                            Operator.EQ, new StringLiteral(pt));
                } else { // primaryTypes.size() > 1
                    LiteralList list = new LiteralList();
                    for (String pt : primaryTypes) {
                        list.add(new StringLiteral(pt));
                    }
                    expr = new Expression(new Reference(NXQL.ECM_PRIMARYTYPE),
                            Operator.IN, list);
                }
                toplevelOperands.addFirst(expr);
            }
        }

        protected void analyzeToplevelIsProxy(Expression expr) {
            if (!(expr.rvalue instanceof IntegerLiteral)) {
                throw new QueryMakerException(NXQL.ECM_ISPROXY
                        + " requires literal 0 or 1 as right argument");
            }
            long v = ((IntegerLiteral) expr.rvalue).value;
            if (v != 0 && v != 1) {
                throw new QueryMakerException(NXQL.ECM_ISPROXY
                        + " requires literal 0 or 1 as right argument");
            }
            boolean isEq = expr.operator == Operator.EQ;
            Boolean pr = Boolean.valueOf((v == 1) == isEq);
            if (proxyClause != null && proxyClause != pr) {
                throw new QueryCannotMatchException();
            }
            proxyClause = pr;
        }

        @Override
        public void visitReference(Reference node) {
            boolean hasTag = false;
            if (node.cast != null) {
                if (!DATE_CAST.equals(node.cast)) {
                    throw new QueryMakerException("Invalid cast: " + node);
                }
            }
            String name = node.name;
            if (NXQL.ECM_PATH.equals(name) || //
                    NXQL.ECM_ISPROXY.equals(name) || //
                    NXQL.ECM_MIXINTYPE.equals(name) || //
                    NXQL.ECM_ISVERSION.equals(name)) {
                if (inSelect) {
                    throw new QueryMakerException("Cannot select on column: "
                            + name);
                }
                if (inOrderBy) {
                    throw new QueryMakerException("Cannot order by column: "
                            + name);
                }
            } else if (NXQL.ECM_PRIMARYTYPE.equals(name) || //
                    NXQL.ECM_UUID.equals(name) || //
                    NXQL.ECM_NAME.equals(name) || //
                    NXQL.ECM_POS.equals(name) || //
                    NXQL.ECM_PARENTID.equals(name) || //
                    NXQL.ECM_LIFECYCLESTATE.equals(name) || //
                    NXQL.ECM_VERSIONLABEL.equals(name) || //
                    NXQL.ECM_LOCK.equals(name) || //
                    NXQL.ECM_LOCK_OWNER.equals(name) || //
                    NXQL.ECM_LOCK_CREATED.equals(name) || //
                    NXQL.ECM_FULLTEXT_JOBID.equals(name)) {
                // ok
            } else if (NXQL.ECM_TAG.equals(name)
                    || name.startsWith(ECM_TAG_STAR)) {
                hasTag = true;
            } else if (name.startsWith(NXQL.ECM_FULLTEXT)) {
                if (inSelect) {
                    throw new QueryMakerException("Cannot select on column: "
                            + name);
                }
                if (inOrderBy) {
                    throw new QueryMakerException("Cannot order by column: "
                            + name);
                }
                if (model.getRepositoryDescriptor().fulltextDisabled) {
                    throw new QueryMakerException(
                            "Fulltext disabled by configuration");
                }
                String[] nameref = new String[] { name };
                boolean useIndex = findFulltextIndexOrField(model, nameref);
                if (!useIndex) {
                    // LIKE on a field, continue analysing with that field
                    name = nameref[0];
                    checkProperty(name); // may throw
                }
                // else all is done in fulltext match info
            } else if (name.startsWith(NXQL.ECM_PREFIX)) {
                throw new QueryMakerException("Unknown field: " + name);
            } else {
                checkProperty(name); // may throw
            }

            if (inSelect) {
                whatColumnNames.add(name);
            } else if (inOrderBy) {
                orderByColumnNames.add(name);
            }
            if (hasWildcardIndex(name) || hasTag) {
                hasWildcardIndex = true;
                if (inOrderBy) {
                    orderByHasWildcardIndex = true;
                }
            }
        }

        /**
         * Checks that a property exists.
         *
         * @throws QueryMakerException if the property doesn't exist
         */
        protected void checkProperty(String xpath) {
            String simple = simpleXPath(xpath);
            ModelProperty prop = model.getPathPropertyInfo(simple);
            if (prop == null || prop == ModelProperty.NONE) {
                throw new QueryMakerException("No such property: " + xpath);
            }
        }

        @Override
        public void visitFunction(Function node) {
            throw new QueryMakerException("Function not supported: "
                    + node.toString());
        }

        @Override
        public void visitOrderByClause(OrderByClause node) {
            inOrderBy = true;
            super.visitOrderByClause(node);
            inOrderBy = false;
        }

    }

    /**
     * Info about a column and its property type.
     */
    protected static class ColumnInfo {

        public final Column column;

        public final boolean isArrayElement;

        public final boolean needsSubSelect;

        public ColumnInfo(Column column, boolean isArrayElement, boolean isArray) {
            this.column = column;
            this.isArrayElement = isArrayElement;
            this.needsSubSelect = !isArrayElement && isArray;
        }
    }

    protected WhereBuilder newWhereBuilder(boolean isProxies) {
        return new WhereBuilder(isProxies);
    }

    /**
     * Builds the database-level WHERE query from the AST.
     */
    protected class WhereBuilder extends DefaultQueryVisitor {

        private static final long serialVersionUID = 1L;

        public static final String PATH_SEP = "/";

        public final List<Column> whatColumns = new LinkedList<Column>();

        public final List<String> whatKeys = new LinkedList<String>();

        public final StringBuilder buf = new StringBuilder();

        // used to assign unique numbers to join aliases for complex property
        // wildcard indexes or tags
        protected int uniqueJoinIndex = 0;

        protected int hierJoinCount = 0;

        // path prefix -> hier table to join,
        protected Map<String, Table> propertyHierTables = new HashMap<String, Table>();

        protected final boolean isProxies;

        protected boolean aliasOrderByColumns;

        // internal fields

        protected boolean allowSubSelect;

        protected boolean inSelect;

        protected boolean inOrderBy;

        protected int ftJoinNumber;

        protected FulltextMatchInfo ftMatchInfo;

        // true when visiting the rvalue of an id expression
        protected boolean visitingId;

        public WhereBuilder(boolean isProxies) {
            this.isProxies = isProxies;
        }

        protected int getUniqueJoinIndex() {
            return ++uniqueJoinIndex;
        }

        protected Column getSpecialColumn(String name) {
            Table table = null;
            String fragmentName = null;
            String fragmentKey;
            if (NXQL.ECM_UUID.equals(name)) {
                table = hierTable;
                fragmentKey = model.MAIN_KEY;
            } else if (NXQL.ECM_NAME.equals(name)) {
                table = hierTable;
                fragmentKey = model.HIER_CHILD_NAME_KEY;
            } else if (NXQL.ECM_POS.equals(name)) {
                table = hierTable;
                fragmentKey = model.HIER_CHILD_POS_KEY;
            } else if (NXQL.ECM_PARENTID.equals(name)) {
                table = hierTable;
                fragmentKey = model.HIER_PARENT_KEY;
            } else if (NXQL.ECM_ISVERSION.equals(name)) {
                table = hierTable;
                fragmentKey = model.MAIN_IS_VERSION_KEY;
            } else if (NXQL.ECM_PRIMARYTYPE.equals(name)) {
                table = dataHierTable;
                fragmentKey = model.MAIN_PRIMARY_TYPE_KEY;
            } else if (NXQL.ECM_MIXINTYPE.equals(name)) {
                // toplevel ones have been extracted by the analyzer
                throw new QueryMakerException("Cannot use non-toplevel " + name
                        + " in query");
            } else if (NXQL.ECM_LIFECYCLESTATE.equals(name)) {
                fragmentName = model.MISC_TABLE_NAME;
                fragmentKey = model.MISC_LIFECYCLE_STATE_KEY;
            } else if (NXQL.ECM_VERSIONLABEL.equals(name)) {
                fragmentName = model.VERSION_TABLE_NAME;
                fragmentKey = model.VERSION_LABEL_KEY;
            } else if (NXQL.ECM_LOCK.equals(name)
                    || NXQL.ECM_LOCK_OWNER.equals(name)) {
                fragmentName = model.LOCK_TABLE_NAME;
                fragmentKey = model.LOCK_OWNER_KEY;
            } else if (NXQL.ECM_LOCK_CREATED.equals(name)) {
                fragmentName = model.LOCK_TABLE_NAME;
                fragmentKey = model.LOCK_CREATED_KEY;
            } else if (NXQL.ECM_FULLTEXT_JOBID.equals(name)) {
                fragmentName = model.FULLTEXT_TABLE_NAME;
                fragmentKey = model.FULLTEXT_JOBID_KEY;
            } else if (name.startsWith(NXQL.ECM_FULLTEXT)) {
                throw new QueryMakerException(NXQL.ECM_FULLTEXT
                        + " must be used as left-hand operand");
            } else if (NXQL.ECM_TAG.equals(name)
                    || name.startsWith(ECM_TAG_STAR)) {
                /*
                 * JOIN relation _F1 ON hierarchy.id = _F1.source
                 *
                 * JOIN hierarchy _F2 ON _F1.id = _F2.id AND _F2.primarytype =
                 * 'Tagging'
                 *
                 * and returns _F2.name
                 */
                String suffix;
                if (name.startsWith(ECM_TAG_STAR)) {
                    suffix = name.substring(ECM_TAG_STAR.length());
                    if (suffix.isEmpty()) {
                        // any
                        suffix = "/*-" + getUniqueJoinIndex();
                    } else {
                        // named
                        suffix = "/*" + suffix;
                     }
                } else {
                    suffix = "";
                }
                String relContextKey = "_tag_relation" + suffix;
                Table rel = getFragmentTable(Join.INNER, dataHierTable,
                        relContextKey, RELATION_TABLE, "source", -1, false,
                        null);
                fragmentName = model.HIER_TABLE_NAME;
                fragmentKey = model.HIER_CHILD_NAME_KEY;
                String hierContextKey = "_tag_hierarchy" + suffix;
                table = getFragmentTable(Join.INNER, rel, hierContextKey,
                        fragmentName, model.MAIN_KEY, -1, false, TYPE_TAGGING);
            } else {
                throw new QueryMakerException("No such property: " + name);
            }
            if (table == null) {
                table = getFragmentTable(dataHierTable, fragmentName,
                        fragmentName, -1, false);
            }
            return table.getColumn(fragmentKey);
        }

        /**
         * Finds info about column (special or not).
         */
        public ColumnInfo getColumnInfo(String name) {
            if (name.startsWith(NXQL.ECM_PREFIX)) {
                Column column = getSpecialColumn(name);
                return new ColumnInfo(column, false, false);
            } else {
                return getRegularColumnInfo(name);
            }
        }

        /**
         * Gets column information for a regular property.
         * <p>
         * Accumulates info about joins needed to get to this property.
         * <p>
         * IMPORTANT: THIS MUST NOT BE CALLED TWICE ON THE SAME PROPERTY as some
         * structures are updated (joins, counters).
         *
         * @throws QueryMakerException if the property doesn't exist
         */
        protected ColumnInfo getRegularColumnInfo(String xpath) {
            Table hier = dataHierTable;
            Table contextHier = hier;
            xpath = canonicalXPath(xpath);
            String[] segments = xpath.split("/");
            String simple = null; // simplified prefix to match model
            String contextKey = null; // prefix used as key for table to join
            String segment = null;
            ModelProperty prop = null;
            for (int i = 0; i < segments.length; i++) {
                segment = segments[i];
                simple = simple == null ? segment : simple + '/' + segment;
                String contextStart = contextKey == null ? ""
                        : contextKey + '/';
                String contextSuffix = "";
                int index = -1;
                boolean star = false;
                boolean isArrayElement = false;
                if (i < segments.length - 1) {
                    // check if we have a complex list index in the next
                    // position
                    String next = segments[i + 1];
                    if (INDEX.matcher(next).matches()) {
                        isArrayElement = true;
                        if (next.startsWith("*")) {
                            star = true;
                            next = next.substring(1);
                        }
                        if (!next.isEmpty()) {
                            index = Integer.parseInt(next);
                        }
                        // swallow next segment
                        i++;
                        simple += "/*";
                        if (star) {
                            if (index == -1) {
                                // any
                                contextSuffix = "/*-" + getUniqueJoinIndex();
                            } else {
                                // named
                                contextSuffix = "/*" + index;
                            }
                            index = -1;
                        } else {
                            contextSuffix = "/" + index;
                        }
                    }
                }

                prop = model.getPathPropertyInfo(simple);
                if (prop == null) {
                    throw new QueryMakerException("No such property: " + xpath);
                }
                if (i < segments.length - 1) {
                    // non-final segment
                    if (prop != ModelProperty.NONE) {
                        throw new QueryMakerException("No such property: "
                                + xpath);
                    }
                    contextKey = contextStart + segment + contextSuffix;
                    Table table = propertyHierTables.get(contextKey);
                    if (table == null) {
                        // none existing
                        // create new Join with hierarchy from previous
                        String alias = TABLE_HIER_ALIAS + ++hierJoinCount;
                        table = new TableAlias(hier, alias);
                        propertyHierTables.put(contextKey, table);
                        addJoin(Join.LEFT, alias, table, model.HIER_PARENT_KEY,
                                contextHier, model.MAIN_KEY, segment, index,
                                null);
                    }
                    contextHier = table;
                } else {
                    // last segment
                    if (prop == ModelProperty.NONE) {
                        throw new QueryMakerException("No such property: "
                                + xpath);
                    }
                    // use fragment name, not segment, for table context key
                    contextKey = contextStart + prop.fragmentName
                            + contextSuffix;
                    boolean skipJoin = !isArrayElement
                            && prop.propertyType.isArray();
                    Table table = getFragmentTable(contextHier, contextKey,
                            prop.fragmentName, index, skipJoin);
                    return new ColumnInfo(table.getColumn(prop.fragmentKey),
                            isArrayElement, prop.propertyType.isArray());
                }
            }
            throw new AssertionError("not reached");
        }

        @Override
        public void visitQuery(SQLQuery node) {
            super.visitQuery(node);
            // intentionally does not set limit or offset in the query
        }

        @Override
        public void visitSelectClause(SelectClause node) {
            inSelect = true;
            super.visitSelectClause(node);
            inSelect = false;
        }

        @Override
        public void visitMultiExpression(MultiExpression node) {
            buf.append('(');
            for (Iterator<Operand> it = node.values.iterator(); it.hasNext();) {
                it.next().accept(this);
                if (it.hasNext()) {
                    node.operator.accept(this);
                }
            }
            buf.append(')');
        }

        @Override
        public void visitExpression(Expression node) {
            buf.append('(');
            Reference ref = node.lvalue instanceof Reference ? (Reference) node.lvalue
                    : null;
            String name = ref != null ? ref.name : null;
            String cast = ref != null ? ref.cast : null;
            Operand rvalue = node.rvalue;
            if (DATE_CAST.equals(cast)) {
                checkDateLiteralForCast(rvalue, node);
            }
            Operator op = node.operator;
            if (op == Operator.STARTSWITH) {
                visitExpressionStartsWith(node);
            } else if (NXQL.ECM_PATH.equals(name)) {
                visitExpressionEcmPath(node);
            } else if (NXQL.ECM_ISPROXY.equals(name)) {
                visitExpressionIsProxy(node);
            } else if (NXQL.ECM_ISVERSION.equals(name)) {
                visitExpressionIsVersion(node);
            } else if (NXQL.ECM_MIXINTYPE.equals(name)) {
                visitExpressionMixinType(node);
            } else if (name != null && name.startsWith(NXQL.ECM_FULLTEXT)
                    && !NXQL.ECM_FULLTEXT_JOBID.equals(name)) {
                visitExpressionFulltext(node, name);
            } else if ((op == Operator.EQ || op == Operator.NOTEQ
                    || op == Operator.IN || op == Operator.NOTIN
                    || op == Operator.LIKE || op == Operator.NOTLIKE
                    || op == Operator.ILIKE || op == Operator.NOTILIKE)) {
                ColumnInfo info = getColumnInfo(name);
                // node.lvalue must not be accepted from now on
                if (info.needsSubSelect) {
                    // use EXISTS with subselect clause
                    boolean direct = op == Operator.EQ || op == Operator.IN
                            || op == Operator.LIKE || op == Operator.ILIKE;
                    Operator directOp = direct ? op
                            : (op == Operator.NOTEQ ? Operator.EQ
                                    : op == Operator.NOTIN ? Operator.IN
                                            : op == Operator.NOTLIKE ? Operator.LIKE
                                                    : Operator.ILIKE);
                    if (!direct) {
                        buf.append("NOT ");
                    }
                    generateExistsStart(buf, info.column.getTable());
                    allowSubSelect = true;
                    visitColumnExpression(info.column, directOp, rvalue, cast);
                    allowSubSelect = false;
                    generateExistsEnd(buf);
                } else {
                    // boolean literals have to be translated according the
                    // database dialect
                    if (info.column.getType() == ColumnType.BOOLEAN) {
                        rvalue = getBooleanLiteral(rvalue);
                    }
                    visitColumnExpression(info.column, op, rvalue, cast);
                }
            } else if (op == Operator.BETWEEN
                    || op == Operator.NOTBETWEEN) {
                LiteralList l = (LiteralList) rvalue;
                if (DATE_CAST.equals(cast)) {
                    checkDateLiteralForCast(l.get(0), node);
                    checkDateLiteralForCast(l.get(1), node);
                }
                node.lvalue.accept(this);
                buf.append(' ');
                op.accept(this);
                buf.append(' ');
                l.get(0).accept(this);
                buf.append(" AND ");
                l.get(1).accept(this);
            } else {
                super.visitExpression(node);
            }
            buf.append(')');
        }

        protected Operand getBooleanLiteral(Operand rvalue) {
            if (!(rvalue instanceof IntegerLiteral)) {
                throw new QueryMakerException(
                        "Boolean expressions require literal 0 or 1 as right argument");
            }
            long v = ((IntegerLiteral) rvalue).value;
            if (v != 0 && v != 1) {
                throw new QueryMakerException(
                        "Boolean expressions require literal 0 or 1 as right argument");
            }
            return new BooleanLiteral(v == 1);
        }

        protected void visitColumnExpression(Column column, Operator op,
                Operand rvalue, String cast) {
            if (op == Operator.ILIKE || op == Operator.NOTILIKE) {
                visitExpressionIlike(column, op, rvalue);
            } else {
                visitReference(column, cast);
                op.accept(this);
                boolean oldVisitingId = visitingId;
                visitingId = column.getType().isId();
                rvalue.accept(this);
                visitingId = oldVisitingId;
            }
        }

        /**
         * This operand is going to be used with a lvalue that has a DATE cast,
         * so if it's a date literal make sure it's not a TIMESTAMP.
         */
        protected void checkDateLiteralForCast(Operand value, Expression node) {
            if (value instanceof DateLiteral && !((DateLiteral) value).onlyDate) {
                throw new QueryMakerException(
                        "DATE() cast must be used with DATE literal, not TIMESTAMP: "
                                + node);
            }
        }

        protected void generateExistsStart(StringBuilder buf, Table table) {
            String tableName;
            if (table.isAlias()) {
                tableName = table.getRealTable().getQuotedName() + " "
                        + table.getQuotedName();
            } else {
                tableName = table.getQuotedName();
            }
            buf.append(String.format(
                    "EXISTS (SELECT 1 FROM %s WHERE %s = %s AND ",
                    tableName,
                    dataHierTable.getColumn(model.MAIN_KEY).getFullQuotedName(),
                    table.getColumn(model.MAIN_KEY).getFullQuotedName()));
        }

        protected void generateExistsEnd(StringBuilder buf) {
            buf.append(")");
        }

        protected void visitExpressionStartsWith(Expression node) {
            if (!(node.lvalue instanceof Reference)) {
                throw new QueryMakerException("Illegal left argument for "
                        + Operator.STARTSWITH + ": " + node.lvalue);
            }
            if (!(node.rvalue instanceof StringLiteral)) {
                throw new QueryMakerException(Operator.STARTSWITH
                        + " requires literal path as right argument");
            }
            String path = ((StringLiteral) node.rvalue).value;
            if (path.length() > 1 && path.endsWith(PATH_SEP)) {
                path = path.substring(0, path.length() - PATH_SEP.length());
            }
            String name = ((Reference) node.lvalue).name;
            if (NXQL.ECM_PATH.equals(name)) {
                visitExpressionStartsWithPath(path);
            } else {
                visitExpressionStartsWithNonPath(node, path);
            }
        }

        protected void visitExpressionStartsWithPath(String path) {
            // find the id from the path
            Serializable id;
            try {
                id = pathResolver.getIdForPath(path);
            } catch (StorageException e) {
                throw new QueryMakerException(e);
            }
            if (id == null) {
                // no such path, always return a false
                // TODO remove the expression more intelligently from the parse
                // tree
                buf.append("0=1");
            } else {
                buf.append(dialect.getInTreeSql(hierTable.getColumn(
                        model.MAIN_KEY).getFullQuotedName()));
                whereParams.add(id);
            }
        }

        protected void visitExpressionStartsWithNonPath(Expression node,
                String path) {
            String name = ((Reference) node.lvalue).name;
            ColumnInfo info = getColumnInfo(name);
            if (info.needsSubSelect) {
                // use EXISTS with subselect clause
                generateExistsStart(buf, info.column.getTable());
            }
            buf.append('(');
            visitReference(info.column);
            buf.append(" = ");
            visitStringLiteral(path);
            buf.append(" OR ");
            visitReference(info.column);
            buf.append(" LIKE ");
            // TODO escape % chars...
            visitStringLiteral(path + PATH_SEP + '%');
            buf.append(')');
            if (info.needsSubSelect) {
                generateExistsEnd(buf);
            }
        }

        protected void visitExpressionEcmPath(Expression node) {
            if (node.operator != Operator.EQ && node.operator != Operator.NOTEQ) {
                throw new QueryMakerException(NXQL.ECM_PATH
                        + " requires = or <> operator");
            }
            if (!(node.rvalue instanceof StringLiteral)) {
                throw new QueryMakerException(NXQL.ECM_PATH
                        + " requires literal path as right argument");
            }
            String path = ((StringLiteral) node.rvalue).value;
            if (path.length() > 1 && path.endsWith(PATH_SEP)) {
                path = path.substring(0, path.length() - PATH_SEP.length());
            }
            Serializable id;
            try {
                id = pathResolver.getIdForPath(path);
            } catch (StorageException e) {
                throw new QueryMakerException(e);
            }
            if (id == null) {
                // no such path, always return a false
                // TODO remove the expression more intelligently from the parse
                // tree
                buf.append("0=1");
            } else {
                visitReference(hierTable.getColumn(model.MAIN_KEY));
                visitOperator(node.operator);
                buf.append('?');
                whereParams.add(id);
            }
        }

        protected void visitExpressionIsProxy(Expression node) {
            if (node.operator != Operator.EQ && node.operator != Operator.NOTEQ) {
                throw new QueryMakerException(NXQL.ECM_ISPROXY
                        + " requires = or <> operator");
            }
            if (!(node.rvalue instanceof IntegerLiteral)) {
                throw new QueryMakerException(NXQL.ECM_ISPROXY
                        + " requires literal 0 or 1 as right argument");
            }
            long v = ((IntegerLiteral) node.rvalue).value;
            if (v != 0 && v != 1) {
                throw new QueryMakerException(NXQL.ECM_ISPROXY
                        + " requires literal 0 or 1 as right argument");
            }
            boolean bool = node.operator == Operator.EQ ^ v == 0;
            buf.append(isProxies == bool ? "1=1" : "0=1");
        }

        protected void visitExpressionIsVersion(Expression node) {
            if (node.operator != Operator.EQ && node.operator != Operator.NOTEQ) {
                throw new QueryMakerException(NXQL.ECM_ISVERSION
                        + " requires = or <> operator");
            }
            if (!(node.rvalue instanceof IntegerLiteral)) {
                throw new QueryMakerException(NXQL.ECM_ISVERSION
                        + " requires literal 0 or 1 as right argument");
            }
            long v = ((IntegerLiteral) node.rvalue).value;
            if (v != 0 && v != 1) {
                throw new QueryMakerException(NXQL.ECM_ISVERSION
                        + " requires literal 0 or 1 as right argument");
            }
            boolean bool = node.operator == Operator.EQ ^ v == 0;

            node.lvalue.accept(this);
            // buf.append(database.getTable(model.VERSION_TABLE_NAME).getColumn(
            // model.MAIN_KEY).getFullQuotedName());
            if (bool) {
                buf.append(" = " + dialect.toBooleanValueString(true));
            } else {
                buf.append(" IS NULL");
            }
        }

        /**
         * Include or exclude mixins.
         * <p>
         * include: primarytype IN (... types with Foo or Bar ...) OR mixintypes
         * LIKE '%Foo%' OR mixintypes LIKE '%Bar%'
         * <p>
         * exclude: primarytype IN (... types without Foo or Bar ...) AND
         * (mixintypes NOT LIKE '%Foo%' AND mixintypes NOT LIKE '%Bar%' OR
         * mixintypes IS NULL)
         */
        protected void visitExpressionMixinType(Expression node) {
            boolean include;
            Set<String> mixins;

            Expression expr = (Expression) node;
            Operator op = expr.operator;
            if (op == Operator.EQ || op == Operator.NOTEQ) {
                include = op == Operator.EQ;
                if (!(expr.rvalue instanceof StringLiteral)) {
                    throw new QueryMakerException(NXQL.ECM_MIXINTYPE
                            + " = requires literal string as right argument");
                }
                String value = ((StringLiteral) expr.rvalue).value;
                mixins = Collections.singleton(value);
            } else if (op == Operator.IN || op == Operator.NOTIN) {
                include = op == Operator.IN;
                if (!(expr.rvalue instanceof LiteralList)) {
                    throw new QueryMakerException(NXQL.ECM_MIXINTYPE
                            + " = requires string list as right argument");
                }
                mixins = getStringLiterals((LiteralList) expr.rvalue);
            } else {
                throw new QueryMakerException(NXQL.ECM_MIXINTYPE
                        + " unknown operator: " + op);
            }

            /*
             * Primary types
             */

            Set<String> types;
            if (include) {
                types = new HashSet<String>();
                for (String mixin : mixins) {
                    types.addAll(model.getMixinDocumentTypes(mixin));
                }
            } else {
                types = new HashSet<String>(model.getDocumentTypes());
                for (String mixin : mixins) {
                    types.removeAll(model.getMixinDocumentTypes(mixin));
                }
            }

            /*
             * Instance mixins
             */

            Set<String> instanceMixins = new HashSet<String>();
            for (String mixin : mixins) {
                if (!MIXINS_NOT_PER_INSTANCE.contains(mixin)) {
                    instanceMixins.add(mixin);
                }
            }

            /*
             * SQL generation
             */

            if (!types.isEmpty()) {
                Column col = dataHierTable.getColumn(model.MAIN_PRIMARY_TYPE_KEY);
                visitReference(col);
                buf.append(" IN ");
                buf.append('(');
                for (Iterator<String> it = types.iterator(); it.hasNext();) {
                    visitStringLiteral(it.next());
                    if (it.hasNext()) {
                        buf.append(", ");
                    }
                }
                buf.append(')');

                if (!instanceMixins.isEmpty()) {
                    buf.append(include ? " OR " : " AND ");
                }
            }

            if (!instanceMixins.isEmpty()) {
                buf.append('(');
                Column mixinsColumn = dataHierTable.getColumn(model.MAIN_MIXIN_TYPES_KEY);
                String[] returnParam = new String[1];
                for (Iterator<String> it = instanceMixins.iterator(); it.hasNext();) {
                    String mixin = it.next();
                    String sql = dialect.getMatchMixinType(mixinsColumn, mixin,
                            include, returnParam);
                    buf.append(sql);
                    if (returnParam[0] != null) {
                        whereParams.add(returnParam[0]);
                    }
                    if (it.hasNext()) {
                        buf.append(include ? " OR " : " AND ");
                    }
                }
                if (!include) {
                    buf.append(" OR ");
                    visitReference(mixinsColumn);
                    buf.append(" IS NULL");
                }
                buf.append(')');
            }

            if (types.isEmpty() && instanceMixins.isEmpty()) {
                buf.append(include ? "0=1" : "0=0");
            }
        }

        protected void visitExpressionFulltext(Expression node, String name) {
            String[] nameref = new String[] { name };
            boolean useIndex = findFulltextIndexOrField(model, nameref);
            name = nameref[0];
            if (node.operator != Operator.EQ && node.operator != Operator.LIKE) {
                throw new QueryMakerException(NXQL.ECM_FULLTEXT
                        + " requires = or LIKE operator");
            }
            if (!(node.rvalue instanceof StringLiteral)) {
                throw new QueryMakerException(NXQL.ECM_FULLTEXT
                        + " requires literal string as right argument");
            }
            if (useIndex) {
                // use actual fulltext query using a dedicated index
                String fulltextQuery = ((StringLiteral) node.rvalue).value;
                fulltextQuery = dialect.getDialectFulltextQuery(fulltextQuery);
                ftJoinNumber++;
                Column mainColumn = dataHierTable.getColumn(model.MAIN_KEY);
                FulltextMatchInfo info = dialect.getFulltextScoredMatchInfo(
                        fulltextQuery, name, ftJoinNumber, mainColumn, model,
                        database);
                ftMatchInfo = info; // used for order by if only one
                if (info.joins != null) {
                    joins.addAll(info.joins);
                }
                buf.append(info.whereExpr);
                if (info.whereExprParam != null) {
                    whereParams.add(info.whereExprParam);
                }
            } else {
                // single field matched with ILIKE
                log.warn("No fulltext index configured for field " + name
                        + ", falling back on LIKE query");
                String value = ((StringLiteral) node.rvalue).value;

                // fulltext translation into pseudo-LIKE syntax
                Set<String> words = FullTextUtils.parseFullText(value, false);
                if (words.isEmpty()) {
                    // only stop words or empty
                    value = "DONTMATCHANYTHINGFOREMPTYQUERY";
                } else {
                    value = "%"
                            + StringUtils.join(new ArrayList<String>(words),
                                    "%") + "%";
                }

                Reference ref = new Reference(name);
                if (dialect.supportsIlike()) {
                    visitReference(ref);
                    buf.append(" ILIKE ");
                    visitStringLiteral(value);
                } else {
                    buf.append("LOWER(");
                    visitReference(ref);
                    buf.append(") LIKE ");
                    visitStringLiteral(value);
                }
            }
        }

        protected void visitExpressionIlike(Column lvalue, Operator op,
                Operand rvalue) {
            if (dialect.supportsIlike()) {
                visitReference(lvalue);
                op.accept(this);
                rvalue.accept(this);
            } else {
                buf.append("LOWER(");
                visitReference(lvalue);
                buf.append(") ");
                if (op == Operator.NOTILIKE) {
                    buf.append("NOT ");
                }
                buf.append("LIKE");
                buf.append(" LOWER(");
                rvalue.accept(this);
                buf.append(")");
            }
        }

        @Override
        public void visitOperator(Operator node) {
            if (node != Operator.NOT) {
                buf.append(' ');
            }
            buf.append(node.toString());
            buf.append(' ');
        }

        @Override
        public void visitReference(Reference node) {
            String name = node.name;
            ColumnInfo info = getColumnInfo(name);
            if (info.needsSubSelect && !allowSubSelect) {
                String msg = inOrderBy ? "Cannot use collection %s in ORDER BY clause"
                        : "Can only use collection %s with =, <>, IN or NOT IN clause";
                throw new QueryMakerException(String.format(msg, name));
            }
            if (inSelect) {
                whatColumns.add(info.column);
                whatKeys.add(name);
            } else {
                visitReference(info.column, node.cast);
            }
        }

        protected void visitReference(Column column) {
            visitReference(column, null);
        }

        protected void visitReference(Column column, String cast) {
            if (DATE_CAST.equals(cast)
                    && column.getType() != ColumnType.TIMESTAMP) {
                throw new QueryMakerException("Cannot cast to " + cast + ": "
                        + column);
            }
            String qname = column.getFullQuotedName();
            // some databases (Derby) can't do comparisons on CLOB
            if (column.getJdbcType() == Types.CLOB) {
                String colFmt = dialect.getClobCast(inOrderBy);
                if (colFmt != null) {
                    qname = String.format(colFmt, qname, Integer.valueOf(255));
                }
            }
            if (cast != null) {
                // only DATE cast for now
                String fmt = dialect.getDateCast();
                buf.append(String.format(fmt, qname));
            } else {
                buf.append(qname);
            }
        }

        @Override
        public void visitLiteralList(LiteralList node) {
            buf.append('(');
            for (Iterator<Literal> it = node.iterator(); it.hasNext();) {
                it.next().accept(this);
                if (it.hasNext()) {
                    buf.append(", ");
                }
            }
            buf.append(')');
        }

        @Override
        public void visitDateLiteral(DateLiteral node) {
            buf.append('?');
            if (node.onlyDate) {
                whereParams.add(node.toSqlDate());
            } else {
                whereParams.add(node.toCalendar());
            }
        }

        @Override
        public void visitStringLiteral(StringLiteral node) {
            if (visitingId) {
                visitId(node.value);
            } else {
                visitStringLiteral(node.value);
            }
        }

        // wrap the string so that the mapper can detect it
        // and map to an actual database id
        protected void visitId(String string) {
            buf.append('?');
            whereParams.add(new WrappedId(string));
        }

        public void visitStringLiteral(String string) {
            buf.append('?');
            whereParams.add(string);
        }

        @Override
        public void visitDoubleLiteral(DoubleLiteral node) {
            buf.append('?');
            whereParams.add(Double.valueOf(node.value));
        }

        @Override
        public void visitIntegerLiteral(IntegerLiteral node) {
            buf.append('?');
            whereParams.add(Long.valueOf(node.value));
        }

        @Override
        public void visitBooleanLiteral(BooleanLiteral node) {
            buf.append('?');
            whereParams.add(Boolean.valueOf(node.value));
        }

        @Override
        public void visitOrderByList(OrderByList node) {
            inOrderBy = true;
            for (Iterator<OrderByExpr> it = node.iterator(); it.hasNext();) {
                it.next().accept(this);
                if (it.hasNext()) {
                    buf.append(", ");
                }
            }
            inOrderBy = false;
        }

        @Override
        public void visitOrderByExpr(OrderByExpr node) {
            int length = buf.length();
            // generates needed joins
            super.visitOrderByExpr(node); // visit reference
            if (aliasOrderByColumns) {
                // but don't use generated values
                // make the ORDER BY clause uses the aliases instead
                buf.setLength(length);
                buf.append(aliasesByName.get(node.reference.name));
            }
            if (node.isDescending) {
                buf.append(dialect.getDescending());
            }
        }

    }

}
