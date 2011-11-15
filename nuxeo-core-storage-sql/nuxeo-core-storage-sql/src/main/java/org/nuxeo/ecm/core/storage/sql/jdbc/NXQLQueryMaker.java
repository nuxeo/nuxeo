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
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.ModelProperty;
import org.nuxeo.ecm.core.storage.sql.PropertyType;
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
 * The examples below are based on the NXQL statement:
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

    protected static final String TABLE_HIER_ALIAS = "_H";

    protected static final String TABLE_FRAG_ALIAS = "_F";

    protected static final String COL_ALIAS_PREFIX = "_C";

    protected static final String UNION_ALIAS = "_T";

    protected static final String WITH_ALIAS_PREFIX = "_W";

    protected static final String READ_ACL_ALIAS = "_RACL";

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

        /*
         * Find all relevant types and keys for the criteria.
         */

        QueryAnalyzer info = newQueryAnalyzer(queryFilter.getFacetFilter());
        try {
            info.visitQuery(sqlQuery);
        } catch (QueryCannotMatchException e) {
            // query cannot match
            return null;
        } catch (QueryMakerException e) {
            throw new StorageException(e.getMessage(), e);
        }

        /*
         * Build the FROM / JOIN criteria for each select.
         */

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

        Table hier = database.getTable(model.HIER_TABLE_NAME);

        String from;
        List<Column> whatColumns = null;
        List<String> whatKeys = null;
        boolean doUnion = docKinds.length > 1;
        Select select = null;
        String orderBy = null;
        List<String> statements = new ArrayList<String>(2);
        List<Serializable> selectParams = new LinkedList<Serializable>();
        List<String> withTables = new LinkedList<String>();
        List<Select> withSelects = new LinkedList<Select>();
        List<String> withSelectsStatements = new LinkedList<String>();
        List<Serializable> withParams = new LinkedList<Serializable>();
        boolean distinct = sqlQuery.select.isDistinct();

        for (DocKind docKind : docKinds) {

            // Quoted id in the hierarchy. This is the id returned by the query.
            String hierId;
            // Quoted id attached to the data that matches.
            String dataHierId;

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
                dataHierId = hierId;
                from = hierTable.getQuotedName();
                break;
            case PROXY:
                hierTable = new TableAlias(hier, TABLE_HIER_ALIAS);
                dataHierTable = hier;
                // TODO use dialect
                from = hier.getQuotedName() + " " + hierTable.getQuotedName();
                hierId = hierTable.getColumn(model.MAIN_KEY).getFullQuotedName();
                dataHierId = dataHierTable.getColumn(model.MAIN_KEY).getFullQuotedName();
                // proxies
                Table proxies = database.getTable(model.PROXY_TABLE_NAME);
                String proxiesid = proxies.getColumn(model.MAIN_KEY).getFullQuotedName();
                String proxiestargetid = proxies.getColumn(
                        model.PROXY_TARGET_KEY).getFullQuotedName();
                // join all that
                addJoin(Join.INNER, null, proxies, model.MAIN_KEY, hierTable,
                        model.MAIN_KEY, null, -1);
                addJoin(Join.INNER, null, dataHierTable, model.MAIN_KEY,
                        proxies, model.PROXY_TARGET_KEY, null, -1);
                break;
            default:
                throw new AssertionError(docKind);
            }
            fixInitialJoins();

            /*
             * Parse the WHERE clause from the original query, and deduce from
             * it actual WHERE clauses and potential JOINs. Also checks what
             * columns we SELECT.
             */

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
            boolean selectDocuments = whereBuilder.whatColumns.isEmpty();
            if (selectDocuments) {
                whatColumns = Collections.singletonList(hierTable.getColumn(model.MAIN_KEY));
                whatKeys = Collections.singletonList(model.MAIN_KEY);
            } else {
                whatColumns = whereBuilder.whatColumns;
                whatKeys = whereBuilder.whatKeys;
            }

            /*
             * Process WHERE and ORDER BY.
             */

            if (info.wherePredicate != null) {
                info.wherePredicate.accept(whereBuilder);
                // WHERE clause
                String where = whereBuilder.buf.toString();
                if (where.length() != 0) {
                    whereClauses.add(where);
                }
            }

            if (selectDocuments && whereBuilder.needsDistinct) {
                distinct = true;
            }

            // ORDER BY computed just once; may use just aliases

            boolean orderByScoreDesc = sqlQuery.orderBy == null
                    && whereBuilder.ftJoinNumber == 1 && !distinct;
            FulltextMatchInfo ftMatchInfo = whereBuilder.ftMatchInfo;

            if (orderBy == null) {
                if (sqlQuery.orderBy != null) {
                    whereBuilder.aliasOrderByColumns = doUnion;
                    whereBuilder.buf.setLength(0);
                    // start alias count after selected columns
                    whereBuilder.nalias = whatColumns.size();
                    sqlQuery.orderBy.accept(whereBuilder);
                    // ends up in WhereBuilder#visitOrderByExpr
                    // uses nalias in builder
                    orderBy = whereBuilder.buf.toString();
                } else if (orderByScoreDesc) {
                    // add order by score desc
                    orderBy = ftMatchInfo.scoreAlias + " DESC";
                }
            }

            // add clauses associated with the complex property JOINs
            for (Join join : joins) {
                whereClauses.addAll(join.whereClauses);
                whereParams.addAll(join.whereParams);
            }

            /*
             * Columns on which to select and do ordering.
             */

            // alias columns in all cases to simplify logic
            int nalias = 0;
            List<String> whatNames = new ArrayList<String>(1);
            List<Serializable> whatNamesParams = new ArrayList<Serializable>(1);
            String mainAlias = hierId;
            for (Column col : whatColumns) {
                String name = getSelectColName(col);
                String alias = dialect.openQuote() + COL_ALIAS_PREFIX
                        + ++nalias + dialect.closeQuote();
                name += " AS " + alias;
                if (col.getTable().getRealTable() == hier
                        && col.getKey().equals(model.MAIN_KEY)) {
                    mainAlias = alias;
                }
                whatNames.add(name);
            }
            fixWhatColumns(whatColumns);
            if (doUnion) {
                // UNION, so we need all orderable columns aliased as well
                for (String key : info.orderKeys) {
                    Column column = whereBuilder.findColumn(key, false, true);
                    String name = column.getFullQuotedName();
                    String alias = dialect.openQuote() + COL_ALIAS_PREFIX
                            + ++nalias + dialect.closeQuote();
                    name += " AS " + alias;
                    whatNames.add(name);
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
             * Security check.
             */

            String securityClause = null;
            List<Serializable> securityParams = new LinkedList<Serializable>();
            Join securityJoin = null;
            if (queryFilter.getPrincipals() != null) {
                Serializable principals = queryFilter.getPrincipals();
                Serializable permissions = queryFilter.getPermissions();
                if (!dialect.supportsArrays()) {
                    principals = StringUtils.join((String[]) principals, '|');
                    permissions = StringUtils.join((String[]) permissions, '|');
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
            if (dialect.needsOracleJoins()) {
                // implicit joins for Oracle
                List<String> joinClauses = new LinkedList<String>();
                for (Join join : joins) {
                    fromb.append(", ");
                    fromb.append(join.getTable(dialect));
                    if (join.tableParam != null) {
                        selectParams.add(join.tableParam);
                    }
                    String joinClause = join.getClause();
                    if (join.kind == Join.LEFT) {
                        joinClause += "(+)"; // Oracle implicit LEFT JOIN syntax
                    }
                    joinClauses.add(joinClause);
                }
                whereClauses.addAll(0, joinClauses);
            } else {
                // else ANSI join
                Collections.sort(joins); // implicit JOINs last (PostgreSQL)
                for (Join join : joins) {
                    fromb.append(join.toSql(dialect));
                    if (join.tableParam != null) {
                        selectParams.add(join.tableParam);
                    }
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
            List<String> whatNames = new ArrayList<String>(whatColumns.size());
            for (int nalias = 1; nalias <= whatColumns.size(); nalias++) {
                String name = dialect.openQuote() + COL_ALIAS_PREFIX + nalias
                        + dialect.closeQuote();
                whatNames.add(name);
            }
            String selectWhat = StringUtils.join(whatNames, ", ");
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
            Table contextTable, String contextColumn, String name, int index) {
        String on1 = contextTable.getColumn(contextColumn).getFullQuotedName();
        String on2 = table.getColumn(column).getFullQuotedName();
        Join join = new Join(kind, table.getRealTable().getQuotedName(), alias,
                null, on1, on2);
        if (name != null) {
            String nameCol = table.getColumn(model.HIER_CHILD_NAME_KEY).getFullQuotedName();
            join.addWhereClause(nameCol + " = ?", name);
        }
        if (index != -1) {
            String posCol = table.getColumn(model.HIER_CHILD_POS_KEY).getFullQuotedName();
            join.addWhereClause(posCol + " = ?", Long.valueOf(index));
        }
        joins.add(join);
    }

    /**
     * Gets the table for the given fragmentName in the given contextKey, and
     * maybe adds a join if one is not already done.
     */
    protected Table getFragmentTable(Table contextHier, String contextKey,
            String fragmentName, int index, boolean skipJoin) {
        Table table = propertyFragmentTables.get(contextKey);
        if (table == null) {
            Table baseTable = database.getTable(fragmentName);
            String alias = TABLE_FRAG_ALIAS + ++fragJoinCount;
            table = new TableAlias(baseTable, alias);
            propertyFragmentTables.put(contextKey, table);
            if (!skipJoin) {
                int kind = index == -1 ? Join.LEFT : Join.INNER;
                addJoin(kind, alias, table, model.MAIN_KEY, contextHier,
                        model.MAIN_KEY, null, index);
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

    // digits or star or star followed by digits
    private final static Pattern INDEX = Pattern.compile("\\d+|\\*|\\*\\d+");

    // digits or star or star followed by digits, then slash
    private final static Pattern INDEX_SLASH = Pattern.compile("/(?:\\d+|\\*|\\*\\d+)(/|$)");

    // non-canonical index syntax
    private final static Pattern NON_CANON_INDEX = Pattern.compile("[^/\\[\\]]+" // name
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

    protected QueryAnalyzer newQueryAnalyzer(FacetFilter facetFilter) {
        return new QueryAnalyzer(facetFilter);
    }

    /**
     * Collects various info about the query AST, and rewrites the toplevel AND
     * {@link Predicate}s of the WHERE clause into a single
     * {@link MultiExpression} for easier analysis.
     */
    protected class QueryAnalyzer extends DefaultQueryVisitor {

        private static final long serialVersionUID = 1L;

        public final List<String> orderKeys = new LinkedList<String>();

        protected boolean inSelect;

        protected boolean inOrderBy;

        protected final List<Operand> toplevelOperands = new LinkedList<Operand>();

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
                return;
            }
            if (NXQL.ECM_PRIMARYTYPE.equals(name) || //
                    NXQL.ECM_UUID.equals(name) || //
                    NXQL.ECM_NAME.equals(name) || //
                    NXQL.ECM_POS.equals(name) || //
                    NXQL.ECM_PARENTID.equals(name)) {
                if (inOrderBy) {
                    orderKeys.add(name);
                }
                return;
            }
            if (NXQL.ECM_LIFECYCLESTATE.equals(name)) {
                if (inOrderBy) {
                    orderKeys.add(name);
                }
                return;
            }
            if (NXQL.ECM_VERSIONLABEL.equals(name)) {
                if (inOrderBy) {
                    orderKeys.add(name);
                }
                return;
            }
            if (NXQL.ECM_LOCK.equals(name) || NXQL.ECM_LOCK_OWNER.equals(name)) {
                if (inOrderBy) {
                    orderKeys.add(name);
                }
                return;
            }
            if (NXQL.ECM_LOCK_CREATED.equals(name)) {
                if (inOrderBy) {
                    orderKeys.add(name);
                }
                return;
            }
            if (NXQL.ECM_FULLTEXT_JOBID.equals(name)) {
                if (inOrderBy) {
                    orderKeys.add(name);
                }
                return;
            }
            if (name.startsWith(NXQL.ECM_FULLTEXT)) {
                if (model.getRepositoryDescriptor().fulltextDisabled) {
                    throw new QueryMakerException(
                            "Fulltext disabled by configuration");
                }
                String[] nameref = new String[] { name };
                boolean useIndex = findFulltextIndexOrField(model, nameref);
                if (useIndex) {
                    // all is done in fulltext match info
                    return;
                } else {
                    // LIKE on a field, continue analysing with that field
                    name = nameref[0];
                    // fall through
                }
                // fall through
            }
            if (name.startsWith(NXQL.ECM_PREFIX)) {
                throw new QueryMakerException("Unknown field: " + name);
            }

            checkProperty(name); // may throw

            if (inOrderBy) {
                orderKeys.add(name);
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
                prop = model.getPathPropertyInfo(simple);
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

        public final boolean isBoolean;

        public ColumnInfo(Column column, boolean isArrayElement,
                PropertyType propertyType) {
            this.column = column;
            this.isArrayElement = isArrayElement;
            this.needsSubSelect = !isArrayElement && propertyType.isArray();
            this.isBoolean = propertyType == PropertyType.BOOLEAN;
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
        // wildcard indexes
        protected int uniquePropIndex = 0;

        protected int hierJoinCount = 0;

        protected boolean needsDistinct;

        // path prefix -> hier table to join,
        protected Map<String, Table> propertyHierTables = new HashMap<String, Table>();

        private final boolean isProxies;

        private boolean aliasOrderByColumns;

        // internal fields

        private boolean allowSubSelect;

        private boolean inSelect;

        private boolean inOrderBy;

        private int nalias = 0;

        protected int ftJoinNumber;

        protected FulltextMatchInfo ftMatchInfo;

        public WhereBuilder(boolean isProxies) {
            this.isProxies = isProxies;
        }

        public Column findColumn(String name, boolean allowSubSelect,
                boolean inOrderBy) {
            Column column;
            if (name.startsWith(NXQL.ECM_PREFIX)) {
                column = getSpecialColumn(name);
            } else {
                ColumnInfo info = getColumnInfo(name); // may throw
                if (info.needsSubSelect && !allowSubSelect) {
                    String msg = inOrderBy ? "Cannot use collection %s in ORDER BY clause"
                            : "Can only use collection %s with =, <>, IN or NOT IN clause";
                    throw new QueryMakerException(String.format(msg, name));
                }
                column = info.column;
            }
            return column;
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
         * Gets column information for a property.
         * <p>
         * Accumulates info about joins needed to get to this property.
         * <p>
         * IMPORTANT: THIS MUST NOT BE CALLED TWICE ON THE SAME PROPERTY as some
         * structures are updated (joins, counters).
         *
         * @throws QueryMakerException if the property doesn't exist
         */
        protected ColumnInfo getColumnInfo(String xpath) {
            Table hier = dataHierTable;
            Table contextHier = hier;
            xpath = canonicalXPath(xpath);
            String[] segments = xpath.split("/");
            String simple = null; // simplified prefix to match model
            String lastContextKey = null; // prefix used as key for table to
                                          // join
            String segment = null;
            ModelProperty prop = null;
            for (int i = 0; i < segments.length; i++) {
                segment = segments[i];
                simple = simple == null ? segment : simple + '/' + segment;
                String contextKey = lastContextKey == null ? segment
                        : lastContextKey + '/' + segment;
                String contextKeySuffix = "";
                int index = -1;
                boolean star = false;
                if (i < segments.length - 1) {
                    // check if we have a complex list index in the next
                    // position
                    String next = segments[i + 1];
                    if (INDEX.matcher(next).matches()) {
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
                            contextKeySuffix = "/*";
                            if (index == -1) {
                                // any
                                contextKeySuffix = "-" + ++uniquePropIndex;
                            } else {
                                // named
                                contextKeySuffix = String.valueOf(index);
                            }
                            index = -1;
                        } else {
                            contextKeySuffix = "/" + index;
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
                    contextKey += contextKeySuffix;
                    Table table = propertyHierTables.get(contextKey);
                    if (table == null) {
                        // none existing
                        // create new Join with hierarchy from previous
                        String alias = TABLE_HIER_ALIAS + ++hierJoinCount;
                        table = new TableAlias(hier, alias);
                        propertyHierTables.put(contextKey, table);
                        addJoin(Join.INNER, alias, table,
                                model.HIER_PARENT_KEY, contextHier,
                                model.MAIN_KEY, segment, index);
                        needsDistinct = true;
                    }
                    contextHier = table;
                } else {
                    // last segment
                    if (prop == ModelProperty.NONE) {
                        throw new QueryMakerException("No such property: "
                                + xpath);
                    }
                    boolean isArrayElement = !contextKeySuffix.isEmpty();
                    // use fragment name, not segment, for table context key
                    contextKey = lastContextKey == null ? prop.fragmentName
                            : lastContextKey + '/' + prop.fragmentName;
                    contextKey += contextKeySuffix;
                    boolean skipJoin = !isArrayElement
                            && prop.propertyType.isArray();
                    if (isArrayElement) {
                        needsDistinct = true;
                    }
                    Table table = getFragmentTable(contextHier, contextKey,
                            prop.fragmentName, index, skipJoin);
                    return new ColumnInfo(table.getColumn(prop.fragmentKey),
                            isArrayElement, prop.propertyType);
                }
                lastContextKey = contextKey;
            }
            return null; // not reached
        }

        protected Set<String> getStringLiterals(LiteralList list)
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
            String name = node.lvalue instanceof Reference ? ((Reference) node.lvalue).name
                    : null;
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
                    || op == Operator.ILIKE || op == Operator.NOTILIKE)
                    && name != null && !name.startsWith(NXQL.ECM_PREFIX)) {
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
                    if (directOp == Operator.ILIKE) {
                        visitExpressionIlike(info.column, directOp, node.rvalue);
                    } else {
                        visitReference(info.column);
                        directOp.accept(this);
                        node.rvalue.accept(this);
                    }
                    allowSubSelect = false;
                    generateExistsEnd(buf);
                } else {
                    // boolean literals have to be translated according the
                    // database dialect
                    Operand rvalue = node.rvalue;
                    if (info.isBoolean) {
                        if (!(node.rvalue instanceof IntegerLiteral)) {
                            throw new QueryMakerException(
                                    "Boolean expressions require literal 0 or 1 as right argument");
                        }
                        long v = ((IntegerLiteral) node.rvalue).value;
                        if (v != 0 && v != 1) {
                            throw new QueryMakerException(
                                    "Boolean expressions require literal 0 or 1 as right argument");
                        }
                        rvalue = new BooleanLiteral(v == 1);
                    }
                    // use normal processing
                    if (op == Operator.ILIKE || op == Operator.NOTILIKE) {
                        visitExpressionIlike(info.column, op, rvalue);
                    } else {
                        visitReference(info.column);
                        op.accept(this);
                        rvalue.accept(this);
                    }
                }
            } else if (node.operator == Operator.BETWEEN
                    || node.operator == Operator.NOTBETWEEN) {
                LiteralList l = (LiteralList) node.rvalue;
                node.lvalue.accept(this);
                buf.append(' ');
                node.operator.accept(this);
                buf.append(' ');
                l.get(0).accept(this);
                buf.append(" AND ");
                l.get(1).accept(this);
            } else {
                super.visitExpression(node);
            }
            buf.append(')');
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
            ColumnInfo info = getColumnInfo(name); // may throw
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

                if (dialect.supportsIlike()) {
                    visitReference(name);
                    buf.append(" ILIKE ");
                    visitStringLiteral(value);
                } else {
                    buf.append("LOWER(");
                    visitReference(name);
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
            visitReference(node.name);
        }

        protected void visitReference(String name) {
            Column column = findColumn(name, allowSubSelect, inOrderBy);
            if (inSelect) {
                whatColumns.add(column);
                whatKeys.add(name);
            } else {
                visitReference(column);
            }
        }

        protected void visitReference(Column column) {
            String qname = column.getFullQuotedName();
            // some databases (Derby) can't do comparisons on CLOB
            if (column.getJdbcType() == Types.CLOB) {
                String colFmt = dialect.getClobCast(inOrderBy);
                if (colFmt != null) {
                    qname = String.format(colFmt, qname, Integer.valueOf(255));
                }
            }
            buf.append(qname);
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
            whereParams.add(node.toCalendar());
        }

        @Override
        public void visitStringLiteral(StringLiteral node) {
            visitStringLiteral(node.value);
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
                buf.append(dialect.openQuote());
                buf.append(COL_ALIAS_PREFIX);
                buf.append(++nalias);
                buf.append(dialect.closeQuote());
            }
            if (node.isDescending) {
                buf.append(" DESC");
            }
        }

    }

}
