/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FullTextUtils;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.query.QueryParseException;
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
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect.ArraySubQuery;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect.FulltextMatchInfo;

/**
 * Transformer of NXQL queries into underlying SQL queries to the actual database.
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
 * The data tables (dublincore, uid) are joined using a LEFT JOIN, as the schema may not be present on all documents but
 * this shouldn't prevent the WHERE clause from being evaluated. Complex properties are matched using an EXISTS and a
 * subselect. When proxies are matched (ecm:isProxy = 1) there are two additional FULL JOINs. Security checks, id, name,
 * parents and path use the base hierarchy (_H), but all other data use the joined hierarchy.
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
 * When both normal documents and proxies are matched, we UNION ALL the two queries. If an ORDER BY is requested, then
 * columns from the inner SELECTs have to be aliased so that an outer ORDER BY can user their names.
 *
 * @author Florent Guillaume
 */
public class NXQLQueryMaker implements QueryMaker {

    private static final Log log = LogFactory.getLog(NXQLQueryMaker.class);

    public static final String TYPE_DOCUMENT = "Document";

    public static final String TYPE_RELATION = "Relation";

    public static final String TYPE_TAGGING = "Tagging";

    public static final String RELATION_TABLE = "relation";

    public static final String ECM_SIMPLE_ACP_PRINCIPAL = NXQL.ECM_ACL + "/*/" + NXQL.ECM_ACL_PRINCIPAL;

    public static final String ECM_SIMPLE_ACP_PERMISSION = NXQL.ECM_ACL + "/*/" + NXQL.ECM_ACL_PERMISSION;

    public static final String ECM_SIMPLE_ACP_GRANT = NXQL.ECM_ACL + "/*/" + NXQL.ECM_ACL_GRANT;

    public static final String ECM_SIMPLE_ACP_NAME = NXQL.ECM_ACL + "/*/" + NXQL.ECM_ACL_NAME;

    public static final String ECM_SIMPLE_ACP_POS = NXQL.ECM_ACL + "/*/" + NXQL.ECM_ACL_POS;

    /**
     * @since 7.4
     */
    public static final String ECM_SIMPLE_ACP_CREATOR = NXQL.ECM_ACL + "/*/" + NXQL.ECM_ACL_CREATOR;

    /**
     * @since 7.4
     */
    public static final String ECM_SIMPLE_ACP_BEGIN = NXQL.ECM_ACL + "/*/" + NXQL.ECM_ACL_BEGIN;

    /**
     * @since 7.4
     */
    public static final String ECM_SIMPLE_ACP_END = NXQL.ECM_ACL + "/*/" + NXQL.ECM_ACL_END;

    /**
     * @since 7.4
     */
    public static final String ECM_SIMPLE_ACP_STATUS = NXQL.ECM_ACL + "/*/" + NXQL.ECM_ACL_STATUS;

    public static final String ECM_TAG_STAR = NXQL.ECM_TAG + "/*";

    protected static final String TABLE_HIER_ALIAS = "_H";

    protected static final String TABLE_FRAG_ALIAS = "_F";

    protected static final String SUBQUERY_ARRAY_ALIAS = "_A";

    protected static final String COL_ALIAS_PREFIX = "_C";

    protected static final String UNION_ALIAS = "_T";

    protected static final String WITH_ALIAS_PREFIX = "_W";

    protected static final String READ_ACL_ALIAS = "_RACL";

    protected static final String READ_ACL_USER_MAP_ALIAS = "_ACLRUSERMAP";

    protected static final String DATE_CAST = "DATE";

    protected static final String COUNT_FUNCTION = "COUNT";

    protected static final String AVG_FUNCTION = "AVG";

    protected static final List<String> AGGREGATE_FUNCTIONS = Arrays.asList(COUNT_FUNCTION, AVG_FUNCTION, "SUM", "MIN",
            "MAX");

    /*
     * Fields used by the search service.
     */

    protected SQLInfo sqlInfo;

    protected Database database;

    protected Dialect dialect;

    protected Model model;

    protected Set<String> neverPerInstanceMixins;

    protected PathResolver pathResolver;

    protected final Map<String, String> aliasesByName = new HashMap<>();

    protected final List<String> aliases = new LinkedList<>();

    /**
     * Whether the query must match only proxies (TRUE), no proxies (FALSE), or not specified (null).
     */
    protected Boolean proxyClause;

    /** The reason why proxyClause was set to non-null. */
    protected String proxyClauseReason;

    // The hierarchy table for the hierarchy/name, may be an alias table
    protected Table hierTable;

    // The hierarchy table of the data
    protected Table dataHierTable;

    // The proxies table if querying for proxies
    protected Table proxyTable;

    protected List<Join> joins;

    protected List<String> whereClauses;

    protected List<Serializable> whereParams;

    // fragmentName or prefix/fragmentName -> fragment table to join
    protected Map<String, Table> propertyFragmentTables = new HashMap<>();

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
        DIRECT, PROXY
    }

    @Override
    public Query buildQuery(SQLInfo sqlInfo, Model model, PathResolver pathResolver, String query,
            QueryFilter queryFilter, Object... params) {
        this.sqlInfo = sqlInfo;
        database = sqlInfo.database;
        dialect = sqlInfo.dialect;
        this.model = model;
        this.pathResolver = pathResolver;
        neverPerInstanceMixins = new HashSet<>(model.getNoPerDocumentQueryFacets());
        // compat
        Set<String> npim = model.getRepositoryDescriptor().neverPerInstanceMixins;
        if (npim != null) {
            neverPerInstanceMixins.addAll(npim);
        }

        // transform the query according to the transformers defined by the
        // security policies
        SQLQuery sqlQuery = SQLQueryParser.parse(query);
        for (SQLQuery.Transformer transformer : queryFilter.getQueryTransformers()) {
            sqlQuery = transformer.transform(queryFilter.getPrincipal(), sqlQuery);
        }

        // SELECT * -> SELECT ecm:uuid
        SelectClause selectClause = sqlQuery.select;
        if (selectClause.isEmpty()) {
            // turned into SELECT ecm:uuid
            selectClause.add(new Reference(NXQL.ECM_UUID));
        }
        boolean selectStar = selectClause.getSelectList().size() == 1
                && (selectClause.get(0).equals(new Reference(NXQL.ECM_UUID)));

        /*
         * Analyze query to find all relevant types and keys for the criteria, and fulltext matches.
         */

        QueryAnalyzer queryAnalyzer = newQueryAnalyzer(queryFilter.getFacetFilter());
        try {
            queryAnalyzer.visitQuery(sqlQuery);
        } catch (QueryCannotMatchException e) {
            // query cannot match
            return null;
        }

        boolean distinct = selectClause.isDistinct();
        if (selectStar && queryAnalyzer.hasWildcardIndex) {
            distinct = true;
        }

        boolean reAnalyze = false;
        // add a default ORDER BY ecm:fulltextScore DESC
        if (queryAnalyzer.ftCount == 1 && !distinct && sqlQuery.orderBy == null) {
            sqlQuery.orderBy = new OrderByClause(
                    new OrderByList(new OrderByExpr(new Reference(NXQL.ECM_FULLTEXT_SCORE), true)), false);
            queryAnalyzer.orderByScore = true;
            reAnalyze = true;
        }
        // if ORDER BY ecm:fulltextScore, make sure we SELECT on it too
        if (queryAnalyzer.orderByScore && !queryAnalyzer.selectScore) {
            selectClause.add(new Reference(NXQL.ECM_FULLTEXT_SCORE));
            reAnalyze = true;
        }
        if (reAnalyze) {
            queryAnalyzer.visitQuery(sqlQuery);
        }

        if (queryAnalyzer.ftCount > 1 && (queryAnalyzer.orderByScore || queryAnalyzer.selectScore)) {
            throw new QueryParseException(
                    "Cannot use " + NXQL.ECM_FULLTEXT_SCORE + " with more than one fulltext match expression");
        }

        /*
         * Find whether to check proxies, relations.
         */

        if (!model.getRepositoryDescriptor().getProxiesEnabled()) {
            if (proxyClause == Boolean.TRUE) {
                throw new QueryParseException(
                        "Proxies are disabled by configuration, a query with " + proxyClauseReason + " is disallowed");
            }
            proxyClause = Boolean.FALSE;
        }
        if (queryAnalyzer.onlyRelations) {
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

        boolean hasSelectCollection = queryAnalyzer.hasSelectCollection;
        if (doUnion || distinct) {
            // if UNION, we need all the ORDER BY columns in the SELECT list
            // for aliasing
            List<String> whatColumnNames = queryAnalyzer.whatColumnNames;
            if (distinct && !whatColumnNames.contains(NXQL.ECM_UUID)) {
                // when using DISTINCT, we can't also ORDER BY on an artificial ecm:uuid + pos
                hasSelectCollection = false;
            }
            Set<String> onlyOrderByColumnNames = new HashSet<>(queryAnalyzer.orderByColumnNames);
            if (hasSelectCollection) {
                onlyOrderByColumnNames.add(NXQL.ECM_UUID);
            }
            onlyOrderByColumnNames.removeAll(whatColumnNames);
            if (distinct && !onlyOrderByColumnNames.isEmpty()) {
                // if DISTINCT, check that the ORDER BY columns are all in the
                // SELECT list
                if (!selectStar) {
                    throw new QueryParseException(
                            "For SELECT DISTINCT the ORDER BY columns must be in the SELECT list, missing: "
                                    + onlyOrderByColumnNames);
                }
                // for a SELECT *, we can add the needed columns if they
                // don't involve wildcard index array elements
                if (queryAnalyzer.orderByHasWildcardIndex) {
                    throw new QueryParseException("For SELECT * the ORDER BY columns cannot use wildcard indexes");
                }
            }
            for (String name : onlyOrderByColumnNames) {
                selectClause.add(new Reference(name));
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
        List<String> statements = new ArrayList<>(2);
        List<Serializable> selectParams = new LinkedList<>();
        List<String> withTables = new LinkedList<>();
        List<Select> withSelects = new LinkedList<>();
        List<String> withSelectsStatements = new LinkedList<>();
        List<Serializable> withParams = new LinkedList<>();
        Table hier = database.getTable(Model.HIER_TABLE_NAME);

        for (DocKind docKind : docKinds) {

            // Quoted id in the hierarchy. This is the id returned by the query.
            String hierId;

            joins = new LinkedList<>();
            whereClauses = new LinkedList<>();
            whereParams = new LinkedList<>();
            propertyFragmentTables = new HashMap<>();
            fragJoinCount = 0;

            switch (docKind) {
            case DIRECT:
                hierTable = hier;
                dataHierTable = hierTable;
                hierId = hierTable.getColumn(Model.MAIN_KEY).getFullQuotedName();
                from = hierTable.getQuotedName();
                proxyTable = null;
                break;
            case PROXY:
                hierTable = new TableAlias(hier, TABLE_HIER_ALIAS);
                dataHierTable = hier;
                // TODO use dialect
                from = hier.getQuotedName() + " " + hierTable.getQuotedName();
                hierId = hierTable.getColumn(Model.MAIN_KEY).getFullQuotedName();
                // proxies
                proxyTable = database.getTable(Model.PROXY_TABLE_NAME);
                // join all that
                addJoin(Join.INNER, null, proxyTable, Model.MAIN_KEY, hierTable, Model.MAIN_KEY, null, -1, null);
                addJoin(Join.INNER, null, dataHierTable, Model.MAIN_KEY, proxyTable, Model.PROXY_TARGET_KEY, null, -1,
                        null);
                break;
            default:
                throw new AssertionError(docKind);
            }
            fixInitialJoins();

            // init builder

            WhereBuilder whereBuilder = newWhereBuilder(docKind == DocKind.PROXY);
            selectClause.accept(whereBuilder);
            whatColumns = whereBuilder.whatColumns;
            whatKeys = whereBuilder.whatKeys;

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
             * Process WHAT to select.
             */

            // alias columns in all cases to simplify logic
            List<String> whatNames = new ArrayList<>(1);
            List<Serializable> whatNamesParams = new ArrayList<>(1);
            String mainAlias = hierId;
            aliasesByName.clear();
            aliases.clear();
            for (int i = 0; i < whatColumns.size(); i++) {
                Column col = whatColumns.get(i);
                String key = whatKeys.get(i);
                String alias;
                String whatName;
                if (NXQL.ECM_FULLTEXT_SCORE.equals(key)) {
                    FulltextMatchInfo ftMatchInfo = whereBuilder.ftMatchInfo;
                    if (ftMatchInfo == null) {
                        throw new QueryParseException(
                                NXQL.ECM_FULLTEXT_SCORE + " cannot be used without " + NXQL.ECM_FULLTEXT);
                    }
                    alias = ftMatchInfo.scoreAlias;
                    whatName = ftMatchInfo.scoreExpr;
                    if (ftMatchInfo.scoreExprParam != null) {
                        whatNamesParams.add(ftMatchInfo.scoreExprParam);
                    }
                } else {
                    alias = dialect.openQuote() + COL_ALIAS_PREFIX + (i + 1) + dialect.closeQuote();
                    whatName = getSelectColName(col, key);
                    if (col.getTable().getRealTable() == hier && col.getKey().equals(Model.MAIN_KEY)) {
                        mainAlias = alias;
                    }
                }
                aliasesByName.put(key, alias);
                aliases.add(alias);
                whatNames.add(whatName + " AS " + alias);
            }

            fixWhatColumns(whatColumns);

            /*
             * Process ORDER BY.
             */

            // ORDER BY computed just once; may use just aliases
            if (orderBy == null) {
                if (hasSelectCollection) {
                    if (sqlQuery.orderBy == null) {
                        OrderByList obl = new OrderByList(null); // stupid constructor
                        obl.clear();
                        sqlQuery.orderBy = new OrderByClause(obl);
                    }
                    // always add ecm:uuid in ORDER BY
                    sqlQuery.orderBy.elements.add(new OrderByExpr(new Reference(NXQL.ECM_UUID), false));
                }
                if (sqlQuery.orderBy != null) {
                    // needs aliasesByName
                    whereBuilder.aliasOrderByColumns = doUnion;
                    whereBuilder.buf.setLength(0);
                    sqlQuery.orderBy.accept(whereBuilder);
                    // ends up in WhereBuilder#visitOrderByExpr
                    if (hasSelectCollection) {
                        // also add all pos columns
                        whereBuilder.visitOrderByPosColumns();
                    }
                    orderBy = whereBuilder.buf.toString();
                }
            }

            String selectWhat = StringUtils.join(whatNames, ", ");
            if (!doUnion && distinct) {
                selectWhat = "DISTINCT " + selectWhat;
            }

            /*
             * Soft delete.
             */

            if (model.getRepositoryDescriptor().getSoftDeleteEnabled()) {
                whereClauses.add(hierTable.getColumn(Model.MAIN_IS_DELETED_KEY).getFullQuotedName() + " IS NULL");
            }

            /*
             * Security check.
             */

            String securityClause = null;
            List<Serializable> securityParams = new LinkedList<>();
            List<Join> securityJoins = new ArrayList<>(2);
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
                    // JOIN hierarchy_read_acl _RACL ON hierarchy.id = _RACL.id
                    // JOIN aclr_user_map _ACLRUSERMAP ON _RACL.acl_id =
                    // _ACLRUSERMAP.acl_id
                    // WHERE _ACLRUSERMAP.user_id = md5('bob,Everyone')
                    String racl = dialect.openQuote() + READ_ACL_ALIAS + dialect.closeQuote();
                    String aclrum = dialect.openQuote() + READ_ACL_USER_MAP_ALIAS + dialect.closeQuote();
                    securityJoins.add(new Join(Join.INNER, Model.HIER_READ_ACL_TABLE_NAME, READ_ACL_ALIAS, null, id,
                            racl + '.' + Model.HIER_READ_ACL_ID));
                    securityJoins.add(new Join(Join.INNER, Model.ACLR_USER_MAP_TABLE_NAME, READ_ACL_USER_MAP_ALIAS,
                            null, racl + '.' + Model.HIER_READ_ACL_ACL_ID, aclrum + '.' + Model.ACLR_USER_MAP_ACL_ID));
                    securityClause = dialect.getReadAclsCheckSql(aclrum + '.' + Model.ACLR_USER_MAP_USER_ID);
                    securityParams.add(principals);
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
                    String withTable = dialect.openQuote() + WITH_ALIAS_PREFIX + (statements.size() + 1)
                            + dialect.closeQuote();
                    withTables.add(withTable);
                    Select withSelect = new Select(null);
                    withSelect.setWhat("*");
                    String withFrom = withTable;
                    for (Join j : securityJoins) {
                        withFrom += j.toSql(dialect);
                    }
                    withSelect.setFrom(withFrom);
                    withSelect.setWhere(securityClause);
                    withSelects.add(withSelect);
                    withSelectsStatements.add(withSelect.getStatement());
                    withParams.addAll(securityParams);
                } else {
                    // add directly to main select
                    joins.addAll(securityJoins);
                    whereClauses.add(securityClause);
                    whereParams.addAll(securityParams);
                }
            }

            select = new Select(null);
            select.setWhat(selectWhat);
            selectParams.addAll(whatNamesParams);

            StringBuilder fromb = new StringBuilder(from);
            if (dialect.needsOracleJoins() && doUnion && !securityJoins.isEmpty() && queryAnalyzer.ftCount != 0) {
                // NXP-5410 we must use Oracle joins
                // when there's union all + fulltext + security
                for (Join join : joins) {
                    if (!join.whereClauses.isEmpty()) {
                        // we cannot use Oracle join when there are join filters
                        throw new QueryParseException("Query too complex for Oracle (NXP-5410)");
                    }
                }
                // implicit joins for Oracle
                List<String> joinClauses = new LinkedList<>();
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
                        joinClause += " AND " + StringUtils.join(join.whereClauses, " AND ");
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
                        joinClause += " AND " + StringUtils.join(join.whereClauses, " AND ");
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
                subselect = with.toString() + StringUtils.join(withSelectsStatements, " UNION ALL ");
                selectParams.addAll(withParams);
            }
            String selectFrom = '(' + subselect + ')';
            if (dialect.needsAliasForDerivedTable()) {
                selectFrom += " AS " + dialect.openQuote() + UNION_ALIAS + dialect.closeQuote();
            }
            select.setFrom(selectFrom);
        } else {
            // use last (and only) Select in above big loop
            if (!withSelects.isEmpty()) {
                select = new Select(null);
                String with = withTables.get(0) + " AS (" + statements.get(0) + ')';
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
        q.selectInfo = new SQLInfoSelect(select.getStatement(), whatColumns, mapMaker, null, null);
        q.selectParams = selectParams;
        return q;
    }

    // overridden by specialized query makers that need to tweak some joins
    protected void addJoin(int kind, String alias, Table table, String column, Table contextTable, String contextColumn,
            String name, int index, String primaryType) {
        Column column1 = contextTable.getColumn(contextColumn);
        Column column2 = table.getColumn(column);
        Join join = new Join(kind, table.getRealTable().getQuotedName(), alias, null, column1, column2);
        if (name != null) {
            String nameCol = table.getColumn(Model.HIER_CHILD_NAME_KEY).getFullQuotedName();
            join.addWhereClause(nameCol + " = ?", name);
        }
        if (index != -1) {
            String posCol = table.getColumn(Model.HIER_CHILD_POS_KEY).getFullQuotedName();
            join.addWhereClause(posCol + " = ?", Long.valueOf(index));
        }
        if (primaryType != null) {
            String typeCol = table.getColumn(Model.MAIN_PRIMARY_TYPE_KEY).getFullQuotedName();
            join.addWhereClause(typeCol + " = ?", primaryType);
        }
        joins.add(join);
    }

    /**
     * Gets the table for the given fragmentName in the given contextKey, and maybe adds a join if one is not already
     * done.
     * <p>
     * LEFT JOIN fragmentName _F123 ON contextHier.id = _F123.id
     */
    protected Table getFragmentTable(Table contextHier, String contextKey, String fragmentName, int index,
            boolean skipJoin) {
        return getFragmentTable(Join.LEFT, contextHier, contextKey, fragmentName, Model.MAIN_KEY, index, skipJoin,
                null);
    }

    /**
     * Adds a more general JOIN:
     * <p>
     * (LEFT) JOIN fragmentName _F123 ON contextTable.id = _F123.fragmentColumn
     */
    protected Table getFragmentTable(int joinKind, Table contextTable, String contextKey, String fragmentName,
            String fragmentColumn, int index, boolean skipJoin, String primaryType) {
        Table table = propertyFragmentTables.get(contextKey);
        if (table == null) {
            Table baseTable = database.getTable(fragmentName);
            String alias = TABLE_FRAG_ALIAS + ++fragJoinCount;
            table = new TableAlias(baseTable, alias);
            propertyFragmentTables.put(contextKey, table);
            if (!skipJoin) {
                addJoin(joinKind, alias, table, fragmentColumn, contextTable, Model.MAIN_KEY, null, index, primaryType);
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

    /** key used to extract array index if needed */
    protected String getSelectColName(Column col, String key) {
        String colName = getSelectColName(col);
        if (col.isArray()) {
            String[] segments = canonicalXPath(key).split("/");
            if (segments.length > 1) {
                // last segment
                String segment = segments[segments.length - 1];
                if (INDEX.matcher(segment).matches() && !segment.startsWith("*")) {
                    int arrayElementIndex = Integer.parseInt(segment);
                    colName = dialect.getArrayElementString(colName, arrayElementIndex);
                }
            }
        }
        return colName;
    }

    // overridden by specialized query makers that need to add COUNT
    protected void fixWhatColumns(List<Column> whatColumns) {
        // to be overridden
    }

    // overridden by specialized query makers that need to add GROUP BY
    protected void fixSelect(Select select) {
        // to be overridden
    }

    protected static boolean findFulltextIndexOrField(Model model, String[] nameref) {
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
                throw new QueryParseException("Unknown field: " + name);
            }
            useIndex = sep == '_';
            name = name.substring(NXQL.ECM_FULLTEXT.length() + 1);
            if (useIndex) {
                if (!model.getFulltextConfiguration().indexNames.contains(name)) {
                    throw new QueryParseException("No such fulltext index: " + name);
                }
            } else {
                // find if there's an index holding just that field
                String index = model.getFulltextConfiguration().fieldToIndexName.get(name);
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

    // wildcard index at the end in xpath
    protected final static Pattern HAS_FINAL_WILDCARD_INDEX = Pattern.compile(".*/(\\*|\\*\\d+)");

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
     * A star or a star followed by digits can be used instead of just the digits as well.
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

    public boolean hasFinalWildcardIndex(String xpath) {
        xpath = canonicalXPath(xpath);
        return HAS_FINAL_WILDCARD_INDEX.matcher(xpath).matches();
    }

    /* Turns foo/*123 into foo#123 */
    protected static String keyForPos(String name) {
        int i = name.lastIndexOf('/');
        if (i == -1) {
            throw new RuntimeException("Unexpected name: " + name);
        }
        return name.substring(0, i) + "#" + name.substring(i + 2);
    }

    protected QueryAnalyzer newQueryAnalyzer(FacetFilter facetFilter) {
        return new QueryAnalyzer(facetFilter);
    }

    protected static Set<String> getStringLiterals(LiteralList list) {
        Set<String> set = new HashSet<>();
        for (Literal literal : list) {
            if (!(literal instanceof StringLiteral)) {
                throw new QueryParseException("requires string literals");
            }
            set.add(((StringLiteral) literal).value);
        }
        return set;
    }

    protected static Serializable getSerializableLiteral(Literal literal) {
        Serializable value;
        if (literal instanceof BooleanLiteral) {
            value = Boolean.valueOf(((BooleanLiteral) literal).value);
        } else if (literal instanceof DateLiteral) {
            DateLiteral dLit = (DateLiteral) literal;
            value = dLit.onlyDate ? dLit.toSqlDate() : dLit.toCalendar();
        } else if (literal instanceof DoubleLiteral) {
            value = Double.valueOf(((DoubleLiteral) literal).value);
        } else if (literal instanceof IntegerLiteral) {
            value = Long.valueOf(((IntegerLiteral) literal).value);
        } else if (literal instanceof StringLiteral) {
            value = ((StringLiteral) literal).value;
        } else {
            throw new QueryParseException("type of literal in list is not recognized: " + literal.getClass());
        }
        return value;
    }

    protected static List<Serializable> getSerializableLiterals(LiteralList list) {
        List<Serializable> serList = new ArrayList<>(list.size());
        for (Literal literal : list) {
            serList.add(getSerializableLiteral(literal));
        }
        return serList;
    }

    /**
     * Collects various info about the query AST, and rewrites the toplevel AND {@link Predicate}s of the WHERE clause
     * into a single {@link MultiExpression} for easier analysis.
     */
    protected class QueryAnalyzer extends DefaultQueryVisitor {

        private static final long serialVersionUID = 1L;

        protected FacetFilter facetFilter;

        protected boolean inSelect;

        protected boolean inOrderBy;

        protected LinkedList<Operand> toplevelOperands;

        protected MultiExpression wherePredicate;

        /** Do we match only relations (and therefore no proxies). */
        protected boolean onlyRelations;

        protected List<String> whatColumnNames;

        protected List<String> orderByColumnNames;

        /** Do we have a SELECT somelist/* FROM ... */
        protected boolean hasSelectCollection;

        protected boolean hasWildcardIndex;

        protected boolean orderByHasWildcardIndex;

        protected int ftCount;

        protected boolean selectScore;

        protected boolean orderByScore;

        public QueryAnalyzer(FacetFilter facetFilter) {
            this.facetFilter = facetFilter;
        }

        protected void init() {
            toplevelOperands = new LinkedList<>();
            whatColumnNames = new LinkedList<>();
            orderByColumnNames = new LinkedList<>();
            hasWildcardIndex = false;
            orderByHasWildcardIndex = false;
            ftCount = 0;
            selectScore = false;
            orderByScore = false;
        }

        @Override
        public void visitQuery(SQLQuery node) {
            init();
            if (facetFilter != null) {
                addFacetFilterClauses(facetFilter);
            }
            visitSelectClause(node.select);
            visitFromClause(node.from);
            visitWhereClause(node.where); // may be null
            if (node.orderBy != null) {
                visitOrderByClause(node.orderBy);
            }
        }

        public void addFacetFilterClauses(FacetFilter facetFilter) {
            for (String mixin : facetFilter.required) {
                // every facet is required, not just any of them,
                // so do them one by one
                // expr = getMixinsMatchExpression(Collections.singleton(facet),
                // true);
                Expression expr = new Expression(new Reference(NXQL.ECM_MIXINTYPE), Operator.EQ,
                        new StringLiteral(mixin));
                toplevelOperands.add(expr);
            }
            if (!facetFilter.excluded.isEmpty()) {
                // expr = getMixinsMatchExpression(facetFilter.excluded, false);
                LiteralList list = new LiteralList();
                for (String mixin : facetFilter.excluded) {
                    list.add(new StringLiteral(mixin));
                }
                Expression expr = new Expression(new Reference(NXQL.ECM_MIXINTYPE), Operator.NOTIN, list);
                toplevelOperands.add(expr);
            }
        }

        @Override
        public void visitSelectClause(SelectClause node) {
            inSelect = true;
            super.visitSelectClause(node);
            inSelect = false;
        }

        /**
         * Finds all the types to take into account (all concrete types being a subtype of the passed types) based on
         * the FROM list.
         * <p>
         * Adds them as a ecm:primaryType match in the toplevel operands.
         */
        @Override
        public void visitFromClause(FromClause node) {
            onlyRelations = true;
            Set<String> fromTypes = new HashSet<>();
            FromList elements = node.elements;
            for (String typeName : elements.values()) {
                if (TYPE_DOCUMENT.equalsIgnoreCase(typeName)) {
                    typeName = TYPE_DOCUMENT;
                }
                Set<String> subTypes = model.getDocumentSubTypes(typeName);
                if (subTypes == null) {
                    throw new QueryParseException("Unknown type: " + typeName);
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
            fromTypes.remove(Model.ROOT_TYPE);
            LiteralList list = new LiteralList();
            for (String type : fromTypes) {
                list.add(new StringLiteral(type));
            }
            toplevelOperands.add(new Expression(new Reference(NXQL.ECM_PRIMARYTYPE), Operator.IN, list));
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
                        } else if (NXQL.ECM_PROXY_TARGETID.equals(name) || NXQL.ECM_PROXY_VERSIONABLEID.equals(name)) {
                            analyzeToplevelProxyProperty(expr);
                            // no return, we want the node
                        }
                    }
                }
            }
            toplevelOperands.add(node);
        }

        /**
         * Simplify ecm:primaryType positive references, and non-per-instance mixin types.
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
                        set = new HashSet<>(Collections.singleton(primaryType));
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
                    if (!neverPerInstanceMixins.contains(mixin)) {
                        // mixin per instance -> primary type checks not enough
                        continue;
                    }
                    Set<String> set = model.getMixinDocumentTypes(mixin);
                    if (primaryTypes == null) {
                        if (op == Operator.EQ) {
                            primaryTypes = new HashSet<>(set); // copy
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
                    expr = new Expression(new Reference(NXQL.ECM_PRIMARYTYPE), Operator.EQ, new StringLiteral(pt));
                } else { // primaryTypes.size() > 1
                    LiteralList list = new LiteralList();
                    for (String pt : primaryTypes) {
                        list.add(new StringLiteral(pt));
                    }
                    expr = new Expression(new Reference(NXQL.ECM_PRIMARYTYPE), Operator.IN, list);
                }
                toplevelOperands.addFirst(expr);
            }
        }

        protected void analyzeToplevelIsProxy(Expression expr) {
            if (!(expr.rvalue instanceof IntegerLiteral)) {
                throw new QueryParseException(NXQL.ECM_ISPROXY + " requires literal 0 or 1 as right argument");
            }
            long v = ((IntegerLiteral) expr.rvalue).value;
            if (v != 0 && v != 1) {
                throw new QueryParseException(NXQL.ECM_ISPROXY + " requires literal 0 or 1 as right argument");
            }
            boolean isEq = expr.operator == Operator.EQ;
            updateProxyClause(Boolean.valueOf((v == 1) == isEq), expr);
        }

        protected void analyzeToplevelProxyProperty(Expression expr) {
            // proxies required
            updateProxyClause(Boolean.TRUE, expr);
        }

        private void updateProxyClause(Boolean value, Expression expr) {
            if (proxyClause != null && proxyClause != value) {
                throw new QueryCannotMatchException();
            }
            proxyClause = value;
            proxyClauseReason = expr.toString();
        }

        @Override
        public void visitExpression(Expression node) {
            Reference ref = node.lvalue instanceof Reference ? (Reference) node.lvalue : null;
            String name = ref != null ? ref.name : null;
            if (name != null && name.startsWith(NXQL.ECM_FULLTEXT) && !NXQL.ECM_FULLTEXT_JOBID.equals(name)) {
                visitExpressionFulltext(node, name);
            } else {
                super.visitExpression(node);
            }
        }

        protected void visitExpressionFulltext(Expression node, String name) {
            if (node.operator != Operator.EQ && node.operator != Operator.LIKE) {
                throw new QueryParseException(NXQL.ECM_FULLTEXT + " requires = or LIKE operator");
            }
            if (!(node.rvalue instanceof StringLiteral)) {
                throw new QueryParseException(NXQL.ECM_FULLTEXT + " requires literal string as right argument");
            }
            if (model.getRepositoryDescriptor().getFulltextDescriptor().getFulltextSearchDisabled()) {
                throw new QueryParseException("Fulltext search disabled by configuration");
            }
            String[] nameref = new String[] { name };
            boolean useIndex = findFulltextIndexOrField(model, nameref);
            if (useIndex) {
                ftCount++;
            }
        }

        @Override
        public void visitReference(Reference node) {
            boolean hasTag = false;
            if (node.cast != null) {
                if (!DATE_CAST.equals(node.cast)) {
                    throw new QueryParseException("Invalid cast: " + node);
                }
            }
            String name = node.name;
            if (NXQL.ECM_PATH.equals(name) || //
                    NXQL.ECM_ANCESTORID.equals(name) || //
                    NXQL.ECM_ISPROXY.equals(name) || //
                    NXQL.ECM_MIXINTYPE.equals(name)) {
                if (inSelect) {
                    throw new QueryParseException("Cannot select on column: " + name);
                }
                if (inOrderBy) {
                    throw new QueryParseException("Cannot order by column: " + name);
                }
            } else if (NXQL.ECM_PRIMARYTYPE.equals(name) || //
                    NXQL.ECM_UUID.equals(name) || //
                    NXQL.ECM_NAME.equals(name) || //
                    NXQL.ECM_POS.equals(name) || //
                    NXQL.ECM_PARENTID.equals(name) || //
                    NXQL.ECM_LIFECYCLESTATE.equals(name) || //
                    NXQL.ECM_VERSIONLABEL.equals(name) || //
                    NXQL.ECM_VERSIONDESCRIPTION.equals(name) || //
                    NXQL.ECM_VERSIONCREATED.equals(name) || //
                    NXQL.ECM_VERSION_VERSIONABLEID.equals(name) || //
                    NXQL.ECM_ISLATESTVERSION.equals(name) || //
                    NXQL.ECM_ISLATESTMAJORVERSION.equals(name) || //
                    NXQL.ECM_ISVERSION_OLD.equals(name) || //
                    NXQL.ECM_ISVERSION.equals(name) || //
                    NXQL.ECM_ISCHECKEDIN.equals(name) || //
                    NXQL.ECM_LOCK_OWNER.equals(name) || //
                    NXQL.ECM_LOCK_CREATED.equals(name) || //
                    NXQL.ECM_PROXY_TARGETID.equals(name) || //
                    NXQL.ECM_PROXY_VERSIONABLEID.equals(name) || //
                    NXQL.ECM_FULLTEXT_JOBID.equals(name)) {
                // ok
            } else if (NXQL.ECM_TAG.equals(name) || name.startsWith(ECM_TAG_STAR)) {
                hasTag = true;
            } else if (NXQL.ECM_FULLTEXT_SCORE.equals(name)) {
                if (inOrderBy) {
                    orderByScore = true;
                } else if (inSelect) {
                    selectScore = true;
                } else {
                    throw new QueryParseException("Can only use column in SELECT or ORDER BY: " + name);
                }
            } else if (name.startsWith(NXQL.ECM_FULLTEXT)) {
                if (inSelect) {
                    throw new QueryParseException("Cannot select on column: " + name);
                }
                if (inOrderBy) {
                    throw new QueryParseException("Cannot order by column: " + name);
                }
                String[] nameref = new String[] { name };
                boolean useIndex = findFulltextIndexOrField(model, nameref);
                if (!useIndex) {
                    // LIKE on a field, continue analysing with that field
                    name = nameref[0];
                    checkProperty(name); // may throw
                }
                // else all is done in fulltext match info
            } else if (name.startsWith(NXQL.ECM_ACL)) {
                String simple = simpleXPath(name);
                if (simple.equals(ECM_SIMPLE_ACP_PRINCIPAL) || simple.equals(ECM_SIMPLE_ACP_PERMISSION)
                        || simple.equals(ECM_SIMPLE_ACP_GRANT) || simple.equals(ECM_SIMPLE_ACP_NAME)
                        || simple.equals(ECM_SIMPLE_ACP_POS) || simple.equals(ECM_SIMPLE_ACP_CREATOR)
                        || simple.equals(ECM_SIMPLE_ACP_BEGIN) || simple.equals(ECM_SIMPLE_ACP_END)
                        || simple.equals(ECM_SIMPLE_ACP_STATUS)) {
                    // ok
                } else {
                    throw new QueryParseException("Unknown field: " + name);
                }
            } else if (name.startsWith(NXQL.ECM_PREFIX)) {
                throw new QueryParseException("Unknown field: " + name);
            } else {
                checkProperty(name); // may throw
            }

            if (inSelect) {
                whatColumnNames.add(name);
                if (hasFinalWildcardIndex(name)) {
                    hasSelectCollection = true;
                }
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
         * @throws QueryParseException if the property doesn't exist
         */
        protected void checkProperty(String xpath) {
            String simple = simpleXPath(xpath);
            ModelProperty prop = model.getPathPropertyInfo(simple);
            if (prop == null || prop.isIntermediateSegment()) {
                throw new QueryParseException("No such property: " + xpath);
            }
        }

        @Override
        public void visitFunction(Function node) {
            if (!inSelect) {
                throw new QueryParseException("Function not supported in WHERE clause: " + node);
            }
            String func = node.name.toUpperCase();
            Operand arg;
            if (!AGGREGATE_FUNCTIONS.contains(func) || node.args.size() != 1
                    || !((arg = node.args.get(0)) instanceof Reference)) {
                throw new QueryParseException("Function not supported: " + node);
            }
            visitReference((Reference) arg);
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

        public final Column posColumn;

        public final int arrayElementIndex;

        public final boolean isArrayElement;

        public final boolean needsSubSelect;

        public ColumnInfo(Column column, Column posColumn, int arrayElementIndex, boolean isArrayElement, boolean isArray) {
            this.column = column;
            this.posColumn = posColumn;
            this.arrayElementIndex = arrayElementIndex;
            this.isArrayElement = isArrayElement;
            this.needsSubSelect = !isArrayElement && isArray && !column.getType().isArray();
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

        public final LinkedList<Column> whatColumns = new LinkedList<>();

        public final LinkedList<String> whatKeys = new LinkedList<>();

        public final StringBuilder buf = new StringBuilder();

        // used to assign unique numbers to join aliases for complex property
        // wildcard indexes or tags
        protected int uniqueJoinIndex = 0;

        protected int hierJoinCount = 0;

        // path prefix -> hier table to join,
        protected Map<String, Table> propertyHierTables = new HashMap<>();

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

        // arrayColumnName or prefix/arrayColumnName -> array column subquery to join
        protected Map<String, ArraySubQuery> propertyArraySubQueries = new HashMap<>();

        protected int arraySubQueryJoinCount = 0;

        // additional collection pos columns on which to ORDER BY
        protected Map<String, Column> posColumns = new LinkedHashMap<>(0);

        // collection pos columns for which we already did an ORDER BY
        protected List<Column> posColumnsInOrderBy = new ArrayList<>();

        public WhereBuilder(boolean isProxies) {
            this.isProxies = isProxies;
        }

        protected int getUniqueJoinIndex() {
            return ++uniqueJoinIndex;
        }

        /**
         * Gets the arraySubquery for the given arrayColumn in the given contextKey, and maybe adds a JOIN if one is not
         * already done.
         * <p>
         * LEFT JOIN (SELECT id, UNNEST(somecol) AS item, generate_subscripts(somecol, 1) AS pos FROM someschema) _A1 ON
         * _A1.id = hierarchy.id
         */
        protected ArraySubQuery getArraySubQuery(Table contextHier, String contextKey, Column arrayColumn,
                boolean skipJoin) {
            ArraySubQuery arraySubQuery = propertyArraySubQueries.get(contextKey);
            if (arraySubQuery == null) {
                String alias = SUBQUERY_ARRAY_ALIAS + ++arraySubQueryJoinCount;
                arraySubQuery = dialect.getArraySubQuery(arrayColumn, alias);
                propertyArraySubQueries.put(contextKey, arraySubQuery);
                if (!skipJoin) {
                    Join join = new Join(Join.LEFT, arraySubQuery.toSql(), alias, null,
                            arraySubQuery.getSubQueryIdColumn().getFullQuotedName(),
                            contextHier.getColumn(Model.MAIN_KEY).getFullQuotedName());
                    joins.add(join);
                }
            }
            return arraySubQuery;
        }

        protected ColumnInfo getSpecialColumnInfo(String name) {
            String propertyName = null;
            Table table = null;
            String fragmentKey = null;
            if (NXQL.ECM_UUID.equals(name)) {
                table = hierTable;
                fragmentKey = Model.MAIN_KEY;
            } else if (NXQL.ECM_NAME.equals(name)) {
                table = hierTable;
                fragmentKey = Model.HIER_CHILD_NAME_KEY;
            } else if (NXQL.ECM_POS.equals(name)) {
                table = hierTable;
                fragmentKey = Model.HIER_CHILD_POS_KEY;
            } else if (NXQL.ECM_PARENTID.equals(name)) {
                table = hierTable;
                fragmentKey = Model.HIER_PARENT_KEY;
            } else if (NXQL.ECM_ISVERSION_OLD.equals(name) || NXQL.ECM_ISVERSION.equals(name)) {
                table = hierTable;
                fragmentKey = Model.MAIN_IS_VERSION_KEY;
            } else if (NXQL.ECM_ISCHECKEDIN.equals(name)) {
                table = hierTable;
                fragmentKey = Model.MAIN_CHECKED_IN_KEY;
            } else if (NXQL.ECM_PRIMARYTYPE.equals(name)) {
                table = dataHierTable;
                fragmentKey = Model.MAIN_PRIMARY_TYPE_KEY;
            } else if (NXQL.ECM_MIXINTYPE.equals(name)) {
                // toplevel ones have been extracted by the analyzer
                throw new QueryParseException("Cannot use non-toplevel " + name + " in query");
            } else if (NXQL.ECM_LIFECYCLESTATE.equals(name)) {
                propertyName = Model.MISC_LIFECYCLE_STATE_PROP;
            } else if (NXQL.ECM_VERSIONLABEL.equals(name)) {
                propertyName = Model.VERSION_LABEL_PROP;
            } else if (NXQL.ECM_VERSIONDESCRIPTION.equals(name)) {
                propertyName = Model.VERSION_DESCRIPTION_PROP;
            } else if (NXQL.ECM_VERSIONCREATED.equals(name)) {
                propertyName = Model.VERSION_CREATED_PROP;
            } else if (NXQL.ECM_VERSION_VERSIONABLEID.equals(name)) {
                propertyName = Model.VERSION_VERSIONABLE_PROP;
            } else if (NXQL.ECM_ISLATESTVERSION.equals(name)) {
                propertyName = Model.VERSION_IS_LATEST_PROP;
            } else if (NXQL.ECM_ISLATESTMAJORVERSION.equals(name)) {
                propertyName = Model.VERSION_IS_LATEST_MAJOR_PROP;
            } else if (NXQL.ECM_LOCK_OWNER.equals(name)) {
                propertyName = Model.LOCK_OWNER_PROP;
            } else if (NXQL.ECM_LOCK_CREATED.equals(name)) {
                propertyName = Model.LOCK_CREATED_PROP;
            } else if (NXQL.ECM_PROXY_TARGETID.equals(name)) {
                table = proxyTable;
                fragmentKey = Model.PROXY_TARGET_KEY;
            } else if (NXQL.ECM_PROXY_VERSIONABLEID.equals(name)) {
                table = proxyTable;
                fragmentKey = Model.PROXY_VERSIONABLE_KEY;
            } else if (NXQL.ECM_FULLTEXT_JOBID.equals(name)) {
                propertyName = Model.FULLTEXT_JOBID_PROP;
            } else if (NXQL.ECM_FULLTEXT_SCORE.equals(name)) {
                throw new QueryParseException(NXQL.ECM_FULLTEXT_SCORE + " cannot be used in WHERE clause");
            } else if (name.startsWith(NXQL.ECM_FULLTEXT)) {
                throw new QueryParseException(NXQL.ECM_FULLTEXT + " must be used as left-hand operand");
            } else if (NXQL.ECM_TAG.equals(name) || name.startsWith(ECM_TAG_STAR)) {
                /*
                 * JOIN relation _F1 ON hierarchy.id = _F1.source JOIN hierarchy _F2 ON _F1.id = _F2.id AND
                 * _F2.primarytype = 'Tagging' and returns _F2.name
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
                Table rel = getFragmentTable(Join.INNER, dataHierTable, relContextKey, RELATION_TABLE, "source", -1,
                        false, null);
                String fragmentName = Model.HIER_TABLE_NAME;
                fragmentKey = Model.HIER_CHILD_NAME_KEY;
                String hierContextKey = "_tag_hierarchy" + suffix;
                table = getFragmentTable(Join.INNER, rel, hierContextKey, fragmentName, Model.MAIN_KEY, -1, false,
                        TYPE_TAGGING);
            } else if (name.startsWith(NXQL.ECM_ACL)) {
                // get index and suffix; we already checked that there are two slashes
                int i = name.indexOf('/');
                int j = name.lastIndexOf('/');
                String index = name.substring(i + 1, j); // like "*1"
                String suffix = name.substring(j + 1); // like "principal"
                // re-create pseudo property name, which the Model mapped to a ModelProperty
                String newName = NXQL.ECM_ACL + '.' + suffix + '/' + index;
                return getRegularColumnInfo(newName);
            } else {
                throw new QueryParseException("No such property: " + name);
            }
            if (table == null) {
                ModelProperty propertyInfo = model.getPropertyInfo(propertyName);
                String fragmentName = propertyInfo.fragmentName;
                fragmentKey = propertyInfo.fragmentKey;
                if (fragmentName.equals(Model.HIER_TABLE_NAME)) {
                    table = dataHierTable;
                } else {
                    table = getFragmentTable(dataHierTable, fragmentName, fragmentName, -1, false);
                }
            }
            Column column = table.getColumn(fragmentKey);
            return new ColumnInfo(column, null, -1, false, false);
        }

        /**
         * Finds info about column (special or not).
         */
        public ColumnInfo getColumnInfo(String name) {
            if (name.startsWith(NXQL.ECM_PREFIX)) {
                return getSpecialColumnInfo(name);
            } else {
                return getRegularColumnInfo(name);
            }
        }

        /**
         * Gets column information for a regular property.
         * <p>
         * Accumulates info about joins needed to get to this property.
         * <p>
         * IMPORTANT: THIS MUST NOT BE CALLED TWICE ON THE SAME PROPERTY as some structures are updated (joins,
         * counters).
         *
         * @throws QueryParseException if the property doesn't exist
         */
        protected ColumnInfo getRegularColumnInfo(String xpath) {
            Table contextHier;
            if (model.isProxySchemaPath(xpath)) {
                // if xpath for proxy, then change contextHier to proxyTable
                if (proxyTable != null) {
                    contextHier = hierTable;
                } else {
                    contextHier = dataHierTable;
                }
            } else {
                contextHier = dataHierTable;
            }
            xpath = canonicalXPath(xpath);
            String[] segments = xpath.split("/");
            String simple = null; // simplified prefix to match model
            String contextKey = null; // prefix used as key for table to join
            String segment;
            ModelProperty prop;
            for (int i = 0; i < segments.length; i++) {
                segment = segments[i];
                simple = simple == null ? segment : simple + '/' + segment;
                String contextStart = contextKey == null ? "" : contextKey + '/';
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
                    throw new QueryParseException("No such property: " + xpath);
                }
                if (i < segments.length - 1) {
                    // non-final segment
                    if (!prop.isIntermediateSegment()) {
                        throw new QueryParseException("No such property: " + xpath);
                    }
                    segment = prop.getIntermediateSegment(); // canonical
                    contextKey = contextStart + segment + contextSuffix;
                    Table table = propertyHierTables.get(contextKey);
                    if (table == null) {
                        // none existing
                        // create new Join with hierarchy from previous
                        String alias = TABLE_HIER_ALIAS + ++hierJoinCount;
                        table = new TableAlias(dataHierTable, alias);
                        propertyHierTables.put(contextKey, table);
                        addJoin(Join.LEFT, alias, table, Model.HIER_PARENT_KEY, contextHier, Model.MAIN_KEY, segment,
                                index, null);
                    }
                    contextHier = table;
                } else {
                    // last segment
                    if (prop.isIntermediateSegment()) {
                        throw new QueryParseException("No such property: " + xpath);
                    }
                    Table table = database.getTable(prop.fragmentName);
                    Column column = table.getColumn(prop.fragmentKey);
                    boolean skipJoin = !isArrayElement && prop.propertyType.isArray() && !column.isArray();
                    Column posColumn = null;
                    if (column.isArray() && star) {
                        contextKey = contextStart + segment + contextSuffix;
                        ArraySubQuery arraySubQuery = getArraySubQuery(contextHier, contextKey, column, skipJoin);
                        column = arraySubQuery.getSubQueryValueColumn();
                    } else {
                        // use fragment name, not segment, for table context key
                        contextKey = contextStart + prop.fragmentName + contextSuffix;
                        table = getFragmentTable(contextHier, contextKey, prop.fragmentName,
                                column.isArray() ? -1 : index, skipJoin);
                        column = table.getColumn(prop.fragmentKey);
                        if (star) {
                            // we'll have to do an ORDER BY on the pos column as well
                            posColumn = table.getColumn(Model.COLL_TABLE_POS_KEY);
                            posColumns.put(keyForPos(xpath), posColumn);
                        }
                    }
                    return new ColumnInfo(column, posColumn, column.isArray() ? index : -1, isArrayElement,
                            prop.propertyType.isArray());
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
            Reference ref = node.lvalue instanceof Reference ? (Reference) node.lvalue : null;
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
            } else if (NXQL.ECM_ANCESTORID.equals(name)) {
                visitExpressionAncestorId(node);
            } else if (NXQL.ECM_ISPROXY.equals(name)) {
                visitExpressionIsProxy(node);
            } else if (NXQL.ECM_ISVERSION_OLD.equals(name) || NXQL.ECM_ISVERSION.equals(name)) {
                visitExpressionWhereFalseIsNull(node);
            } else if (NXQL.ECM_ISCHECKEDIN.equals(name) || NXQL.ECM_ISLATESTVERSION.equals(name)
                    || NXQL.ECM_ISLATESTMAJORVERSION.equals(name)) {
                visitExpressionWhereFalseMayBeNull(node);
            } else if (NXQL.ECM_MIXINTYPE.equals(name)) {
                visitExpressionMixinType(node);
            } else if (name != null && name.startsWith(NXQL.ECM_FULLTEXT) && !NXQL.ECM_FULLTEXT_JOBID.equals(name)) {
                visitExpressionFulltext(node, name);
            } else if ((op == Operator.EQ || op == Operator.NOTEQ || op == Operator.IN || op == Operator.NOTIN
                    || op == Operator.LIKE || op == Operator.NOTLIKE || op == Operator.ILIKE
                    || op == Operator.NOTILIKE)) {
                ColumnInfo info = name == null ? null : getColumnInfo(name);
                // node.lvalue must not be accepted from now on
                if (info != null && info.needsSubSelect) {
                    // use EXISTS with subselect clause
                    boolean direct = op == Operator.EQ || op == Operator.IN || op == Operator.LIKE
                            || op == Operator.ILIKE;
                    Operator directOp = direct ? op
                            : (op == Operator.NOTEQ ? Operator.EQ
                                    : op == Operator.NOTIN ? Operator.IN
                                            : op == Operator.NOTLIKE ? Operator.LIKE : Operator.ILIKE);
                    if (!direct) {
                        buf.append("NOT ");
                    }
                    generateExistsStart(buf, info.column.getTable());
                    allowSubSelect = true;
                    visitColumnExpression(info.column, directOp, rvalue, cast, name, info.arrayElementIndex);
                    allowSubSelect = false;
                    generateExistsEnd(buf);
                } else if (info != null) {
                    // boolean literals have to be translated according the
                    // database dialect
                    if (info.column.getType() == ColumnType.BOOLEAN) {
                        rvalue = getBooleanLiteral(rvalue);
                    }
                    visitColumnExpression(info.column, op, rvalue, cast, name, info.arrayElementIndex);
                } else {
                    super.visitExpression(node);
                }
            } else if (op == Operator.BETWEEN || op == Operator.NOTBETWEEN) {
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
                throw new QueryParseException("Boolean expressions require literal 0 or 1 as right argument");
            }
            long v = ((IntegerLiteral) rvalue).value;
            if (v != 0 && v != 1) {
                throw new QueryParseException("Boolean expressions require literal 0 or 1 as right argument");
            }
            return new BooleanLiteral(v == 1);
        }

        protected void visitColumnExpression(Column column, Operator op, Operand rvalue, String cast, String lvalueName,
                int arrayElementIndex) {
            if (op == Operator.EQ || op == Operator.NOTEQ || op == Operator.IN || op == Operator.NOTIN) {
                visitExpressionEqOrIn(column, op, rvalue, cast, arrayElementIndex);
            } else if (op == Operator.LIKE || op == Operator.NOTLIKE) {
                visitExpressionLike(column, op, rvalue, lvalueName, arrayElementIndex);
            } else if (op == Operator.ILIKE || op == Operator.NOTILIKE) {
                visitExpressionIlike(column, op, rvalue, lvalueName, arrayElementIndex);
            } else {
                visitSimpleExpression(column, op, rvalue, cast, -1);
            }
        }

        protected void visitSimpleExpression(Column column, Operator op, Operand rvalue, String cast,
                int arrayElementIndex) {
            visitReference(column, cast, arrayElementIndex);
            op.accept(this);
            boolean oldVisitingId = visitingId;
            visitingId = column.getType().isId();
            rvalue.accept(this);
            visitingId = oldVisitingId;
        }

        /**
         * This operand is going to be used with a lvalue that has a DATE cast, so if it's a date literal make sure it's
         * not a TIMESTAMP.
         */
        protected void checkDateLiteralForCast(Operand value, Expression node) {
            if (value instanceof DateLiteral && !((DateLiteral) value).onlyDate) {
                throw new QueryParseException("DATE() cast must be used with DATE literal, not TIMESTAMP: " + node);
            }
        }

        protected void generateExistsStart(StringBuilder buf, Table table) {
            String tableName;
            if (table.isAlias()) {
                tableName = table.getRealTable().getQuotedName() + " " + table.getQuotedName();
            } else {
                tableName = table.getQuotedName();
            }
            buf.append(String.format("EXISTS (SELECT 1 FROM %s WHERE %s = %s AND ", tableName,
                    dataHierTable.getColumn(Model.MAIN_KEY).getFullQuotedName(),
                    table.getColumn(Model.MAIN_KEY).getFullQuotedName()));
        }

        protected void generateExistsEnd(StringBuilder buf) {
            buf.append(")");
        }

        protected void visitExpressionStartsWith(Expression node) {
            if (!(node.lvalue instanceof Reference)) {
                throw new QueryParseException("Illegal left argument for " + Operator.STARTSWITH + ": " + node.lvalue);
            }
            if (!(node.rvalue instanceof StringLiteral)) {
                throw new QueryParseException(Operator.STARTSWITH + " requires literal path as right argument");
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
            Serializable id = pathResolver.getIdForPath(path);
            if (id == null) {
                // no such path, always return a false
                // TODO remove the expression more intelligently from the parse
                // tree
                buf.append("0=1");
            } else {
                // id is always valid, no need to pass it as argument to getInTreeSql
                buf.append(dialect.getInTreeSql(hierTable.getColumn(Model.MAIN_KEY).getFullQuotedName(), null));
                whereParams.add(id);
            }
        }

        protected void visitExpressionStartsWithNonPath(Expression node, String path) {
            String name = ((Reference) node.lvalue).name;
            ColumnInfo info = getColumnInfo(name);
            if (info.needsSubSelect) {
                // use EXISTS with subselect clause
                generateExistsStart(buf, info.column.getTable());
            }
            buf.append('(');
            visitExpressionEqOrIn(info.column, Operator.EQ, new StringLiteral(path), null, -1);
            visitOperator(Operator.OR);
            // TODO escape % chars...
            visitExpressionLike(info.column, Operator.LIKE, new StringLiteral(path + PATH_SEP + '%'), name, -1);
            buf.append(')');
            if (info.needsSubSelect) {
                generateExistsEnd(buf);
            }
        }

        protected void visitExpressionEcmPath(Expression node) {
            if (node.operator != Operator.EQ && node.operator != Operator.NOTEQ) {
                throw new QueryParseException(NXQL.ECM_PATH + " requires = or <> operator");
            }
            if (!(node.rvalue instanceof StringLiteral)) {
                throw new QueryParseException(NXQL.ECM_PATH + " requires literal path as right argument");
            }
            String path = ((StringLiteral) node.rvalue).value;
            if (path.length() > 1 && path.endsWith(PATH_SEP)) {
                path = path.substring(0, path.length() - PATH_SEP.length());
            }
            Serializable id = pathResolver.getIdForPath(path);
            if (id == null) {
                // no such path, always return a false
                // TODO remove the expression more intelligently from the parse
                // tree
                buf.append("0=1");
            } else {
                visitReference(hierTable.getColumn(Model.MAIN_KEY));
                visitOperator(node.operator);
                visitId(model.idToString(id));
            }
        }

        protected void visitExpressionAncestorId(Expression node) {
            if (node.operator != Operator.EQ && node.operator != Operator.NOTEQ) {
                throw new QueryParseException(NXQL.ECM_ANCESTORID + " requires = or <> operator");
            }
            if (!(node.rvalue instanceof StringLiteral)) {
                throw new QueryParseException(NXQL.ECM_ANCESTORID + " requires literal id as right argument");
            }
            boolean not = node.operator == Operator.NOTEQ;
            String id = ((StringLiteral) node.rvalue).value;
            if (not) {
                buf.append("(NOT (");
            }
            String sql = dialect.getInTreeSql(hierTable.getColumn(Model.MAIN_KEY).getFullQuotedName(), id);
            if (sql == null) {
                buf.append("0=1");
            } else {
                buf.append(sql);
                whereParams.add(id);
            }
            if (not) {
                buf.append("))");
            }
        }

        protected void visitExpressionIsProxy(Expression node) {
            boolean bool = getBooleanRValue(NXQL.ECM_ISPROXY, node);
            buf.append(isProxies == bool ? "1=1" : "0=1");
        }

        protected void visitExpressionWhereFalseIsNull(Expression node) {
            String name = ((Reference) node.lvalue).name;
            boolean bool = getBooleanRValue(name, node);
            node.lvalue.accept(this);
            if (bool) {
                buf.append(" = ");
                buf.append(dialect.toBooleanValueString(true));
            } else {
                buf.append(" IS NULL");
            }
        }

        protected void visitExpressionWhereFalseMayBeNull(Expression node) {
            String name = ((Reference) node.lvalue).name;
            boolean bool = getBooleanRValue(name, node);
            if (bool) {
                node.lvalue.accept(this);
                buf.append(" = ");
                buf.append(dialect.toBooleanValueString(true));
            } else {
                buf.append('(');
                node.lvalue.accept(this);
                buf.append(" = ");
                buf.append(dialect.toBooleanValueString(false));
                buf.append(" OR ");
                node.lvalue.accept(this);
                buf.append(" IS NULL)");
            }
        }

        private boolean getBooleanRValue(String name, Expression node) {
            if (node.operator != Operator.EQ && node.operator != Operator.NOTEQ) {
                throw new QueryParseException(name + " requires = or <> operator");
            }
            long v;
            if (!(node.rvalue instanceof IntegerLiteral)
                    || ((v = ((IntegerLiteral) node.rvalue).value) != 0 && v != 1)) {
                throw new QueryParseException(name + " requires literal 0 or 1 as right argument");
            }
            return node.operator == Operator.EQ ^ v == 0;
        }

        /**
         * Include or exclude mixins.
         * <p>
         * include: primarytype IN (... types with Foo or Bar ...) OR mixintypes LIKE '%Foo%' OR mixintypes LIKE '%Bar%'
         * <p>
         * exclude: primarytype IN (... types without Foo or Bar ...) AND (mixintypes NOT LIKE '%Foo%' AND mixintypes
         * NOT LIKE '%Bar%' OR mixintypes IS NULL)
         */
        protected void visitExpressionMixinType(Expression expr) {
            boolean include;
            Set<String> mixins;

            Operator op = expr.operator;
            if (op == Operator.EQ || op == Operator.NOTEQ) {
                include = op == Operator.EQ;
                if (!(expr.rvalue instanceof StringLiteral)) {
                    throw new QueryParseException(NXQL.ECM_MIXINTYPE + " = requires literal string as right argument");
                }
                String value = ((StringLiteral) expr.rvalue).value;
                mixins = Collections.singleton(value);
            } else if (op == Operator.IN || op == Operator.NOTIN) {
                include = op == Operator.IN;
                if (!(expr.rvalue instanceof LiteralList)) {
                    throw new QueryParseException(NXQL.ECM_MIXINTYPE + " = requires string list as right argument");
                }
                mixins = getStringLiterals((LiteralList) expr.rvalue);
            } else {
                throw new QueryParseException(NXQL.ECM_MIXINTYPE + " unknown operator: " + op);
            }

            /*
             * Primary types
             */

            Set<String> types;
            if (include) {
                types = new HashSet<>();
                for (String mixin : mixins) {
                    types.addAll(model.getMixinDocumentTypes(mixin));
                }
            } else {
                types = new HashSet<>(model.getDocumentTypes());
                for (String mixin : mixins) {
                    types.removeAll(model.getMixinDocumentTypes(mixin));
                }
            }

            /*
             * Instance mixins
             */

            Set<String> instanceMixins = new HashSet<>();
            for (String mixin : mixins) {
                if (!neverPerInstanceMixins.contains(mixin)) {
                    instanceMixins.add(mixin);
                }
            }

            /*
             * SQL generation
             */

            if (!types.isEmpty()) {
                Column col = dataHierTable.getColumn(Model.MAIN_PRIMARY_TYPE_KEY);
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
                Column mixinsColumn = dataHierTable.getColumn(Model.MAIN_MIXIN_TYPES_KEY);
                String[] returnParam = new String[1];
                for (Iterator<String> it = instanceMixins.iterator(); it.hasNext();) {
                    String mixin = it.next();
                    String sql = dialect.getMatchMixinType(mixinsColumn, mixin, include, returnParam);
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
            if (useIndex) {
                // use actual fulltext query using a dedicated index
                String fulltextQuery = ((StringLiteral) node.rvalue).value;
                fulltextQuery = dialect.getDialectFulltextQuery(fulltextQuery);
                ftJoinNumber++;
                Column mainColumn = dataHierTable.getColumn(Model.MAIN_KEY);
                FulltextMatchInfo info = dialect.getFulltextScoredMatchInfo(fulltextQuery, name, ftJoinNumber,
                        mainColumn, model, database);
                ftMatchInfo = info;
                if (info.joins != null) {
                    joins.addAll(info.joins);
                }
                buf.append(info.whereExpr);
                if (info.whereExprParam != null) {
                    whereParams.add(info.whereExprParam);
                }
            } else {
                // single field matched with ILIKE
                log.warn("No fulltext index configured for field " + name + ", falling back on LIKE query");
                String value = ((StringLiteral) node.rvalue).value;

                // fulltext translation into pseudo-LIKE syntax
                Set<String> words = FullTextUtils.parseFullText(value, false);
                if (words.isEmpty()) {
                    // only stop words or empty
                    value = "DONTMATCHANYTHINGFOREMPTYQUERY";
                } else {
                    value = "%" + StringUtils.join(new ArrayList<>(words), "%") + "%";
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

        protected void visitExpressionEqOrIn(Column column, Operator op, Operand rvalue, String cast,
                int arrayElementIndex) {
            if (column.isArray() && arrayElementIndex == -1) {
                List<Serializable> params;
                if (rvalue instanceof Literal) {
                    Serializable param = getSerializableLiteral((Literal) rvalue);
                    params = Collections.singletonList(param);
                } else {
                    params = getSerializableLiterals((LiteralList) rvalue);
                }
                boolean positive = op == Operator.EQ || op == Operator.IN;
                String sql = dialect.getArrayInSql(column, cast, positive, params);
                buf.append(sql);
                whereParams.addAll(params);
            } else {
                visitSimpleExpression(column, op, rvalue, cast, arrayElementIndex);
            }
        }

        protected void visitExpressionLike(Column column, Operator op, Operand rvalue, String lvalueName,
                int arrayElementIndex) {
            if (column.isArray() && arrayElementIndex == -1) {
                if (lvalueName == null) {
                    throw new AssertionError("Name is required when lvalue is an array");
                }
                boolean positive = (op == Operator.LIKE);
                String sql = dialect.getArrayLikeSql(column, lvalueName, positive, dataHierTable);
                buf.append(sql);
                whereParams.add(getSerializableLiteral((Literal) rvalue));
            } else {
                visitSimpleExpression(column, op, rvalue, null, arrayElementIndex);
                addLikeEscaping();
            }
        }

        protected void visitExpressionIlike(Column column, Operator op, Operand rvalue, String lvalueName,
                int arrayElementIndex) {
            if (column.isArray() && arrayElementIndex == -1) {
                if (lvalueName == null) {
                    throw new AssertionError("Name is required when lvalue is an array");
                }
                boolean positive = op == Operator.ILIKE;
                String sql = dialect.getArrayIlikeSql(column, lvalueName, positive, dataHierTable);
                buf.append(sql);
                whereParams.add(getSerializableLiteral((Literal) rvalue));
            } else if (dialect.supportsIlike()) {
                visitSimpleExpression(column, op, rvalue, null, arrayElementIndex);
            } else {
                buf.append("LOWER(");
                visitReference(column, arrayElementIndex);
                buf.append(") ");
                if (op == Operator.NOTILIKE) {
                    buf.append("NOT ");
                }
                buf.append("LIKE");
                buf.append(" LOWER(");
                rvalue.accept(this);
                buf.append(")");
                addLikeEscaping();
            }
        }

        protected void addLikeEscaping() {
            String escape = dialect.getLikeEscaping();
            if (escape != null) {
                buf.append(escape);
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
            if (NXQL.ECM_FULLTEXT_SCORE.equals(name)) {
                visitScore();
                return;
            }
            ColumnInfo info = getColumnInfo(name);
            if (info.needsSubSelect && !allowSubSelect) {
                String msg = inOrderBy ? "Cannot use collection %s in ORDER BY clause"
                        : "Can only use collection %s with =, <>, IN or NOT IN clause";
                throw new QueryParseException(String.format(msg, name));
            }
            if (inSelect) {
                whatColumns.add(info.column);
                whatKeys.add(name);
                if (info.posColumn != null) {
                    whatColumns.add(info.posColumn);
                    whatKeys.add(keyForPos(name));
                }
            } else {
                visitReference(info.column, node.cast);
                if (inOrderBy && info.posColumn != null) {
                    buf.append(", ");
                    visitReference(info.posColumn);
                    posColumnsInOrderBy.add(info.posColumn);
                }
            }
        }

        protected void visitReference(Column column) {
            visitReference(column, null, -1);
        }

        protected void visitReference(Column column, String cast) {
            visitReference(column, cast, -1);
        }

        protected void visitReference(Column column, int arrayElementIndex) {
            visitReference(column, null, arrayElementIndex);
        }

        protected void visitReference(Column column, String cast, int arrayElementIndex) {
            if (DATE_CAST.equals(cast) && column.getType() != ColumnType.TIMESTAMP) {
                throw new QueryParseException("Cannot cast to " + cast + ": " + column);
            }
            String qname = column.getFullQuotedName();
            if (arrayElementIndex != -1) {
                if (column.isArray()) {
                    qname = dialect.getArrayElementString(qname, arrayElementIndex);
                } else {
                    throw new QueryParseException(
                            "Cannot use array index " + arrayElementIndex + " for non-array column " + column);
                }
            }
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
            buf.append(node.value);
        }

        @Override
        public void visitIntegerLiteral(IntegerLiteral node) {
            buf.append(node.value);
        }

        @Override
        public void visitBooleanLiteral(BooleanLiteral node) {
            buf.append('?');
            whereParams.add(Boolean.valueOf(node.value));
        }

        @Override
        public void visitFunction(Function node) {
            String func = node.name.toUpperCase();
            Reference ref = (Reference) node.args.get(0);
            ref.accept(this); // whatColumns / whatKeys for column

            // replace column info with aggregate
            Column col = whatColumns.removeLast();
            String key = whatKeys.removeLast();
            final String aggFQN = func + "(" + col.getFullQuotedName() + ")";
            final ColumnType aggType = getAggregateType(func, col.getType());
            final int aggJdbcType = dialect.getJDBCTypeAndString(aggType).jdbcType;
            Column cc = new Column(col, col.getTable()) {
                private static final long serialVersionUID = 1L;

                @Override
                public String getFullQuotedName() {
                    return aggFQN;
                }

                @Override
                public ColumnType getType() {
                    return aggType;
                }

                @Override
                public int getJdbcType() {
                    return aggJdbcType;
                }
            };
            whatColumns.add(cc);
            whatKeys.add(func + "(" + key + ")");
        }

        protected void visitScore() {
            if (inSelect) {
                Column col = new Column(hierTable, null, ColumnType.DOUBLE, null);
                whatColumns.add(col);
                whatKeys.add(NXQL.ECM_FULLTEXT_SCORE);
            } else {
                buf.append(aliasesByName.get(NXQL.ECM_FULLTEXT_SCORE));
            }
        }

        protected ColumnType getAggregateType(String func, ColumnType arg) {
            if (COUNT_FUNCTION.equals(func)) {
                return ColumnType.LONG;
            }
            if (AVG_FUNCTION.equals(func)) {
                return ColumnType.DOUBLE;
            }
            // SUM, MIN, MAX
            return arg;
        }

        @Override
        public void visitOrderByList(OrderByList node) {
            inOrderBy = true;
            for (OrderByExpr obe : node) {
                if (buf.length() != 0) {
                    // we can do this because we generate in an initially empty buffer
                    buf.append(", ");
                }
                obe.accept(this);
            }
            inOrderBy = false;
        }

        public void visitOrderByPosColumns() {
            inOrderBy = true;
            for (Entry<String, Column> es : posColumns.entrySet()) {
                Column col = es.getValue();
                if (posColumnsInOrderBy.contains(col)) {
                    continue;
                }
                if (buf.length() != 0) {
                    buf.append(", ");
                }
                int length = buf.length();
                visitReference(col);
                if (aliasOrderByColumns) {
                    // but don't use generated values
                    // make the ORDER BY clause uses the aliases instead
                    buf.setLength(length);
                    String alias = aliasesByName.get(es.getKey());
                    buf.append(alias);
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
