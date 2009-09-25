/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import java.io.Serializable;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;
import org.nuxeo.ecm.core.query.sql.model.DateLiteral;
import org.nuxeo.ecm.core.query.sql.model.DefaultQueryVisitor;
import org.nuxeo.ecm.core.query.sql.model.DoubleLiteral;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.FromClause;
import org.nuxeo.ecm.core.query.sql.model.FromList;
import org.nuxeo.ecm.core.query.sql.model.Function;
import org.nuxeo.ecm.core.query.sql.model.IVisitor;
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
import org.nuxeo.ecm.core.query.sql.model.StringLiteral;
import org.nuxeo.ecm.core.query.sql.model.WhereClause;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Model.PropertyInfo;
import org.nuxeo.ecm.core.storage.sql.SQLInfo.SQLInfoSelect;
import org.nuxeo.ecm.core.storage.sql.db.Column;
import org.nuxeo.ecm.core.storage.sql.db.Database;
import org.nuxeo.ecm.core.storage.sql.db.Select;
import org.nuxeo.ecm.core.storage.sql.db.Table;
import org.nuxeo.ecm.core.storage.sql.db.TableAlias;
import org.nuxeo.ecm.core.storage.sql.db.dialect.Dialect;

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

    /**
     * Name of the Immutable facet, added by {@code DocumentModelFactory} when
     * instantiating a proxy or a version.
     */
    public static final String FACET_IMMUTABLE = "Immutable";

    protected static final String TABLE_HIER_ALIAS = "_H";

    protected static final String COL_ORDER_ALIAS_PREFIX = "_C";

    private static final String JOIN_ON = "%s ON %s = %s";

    /*
     * Fields used by the search service.
     */

    protected SQLInfo sqlInfo;

    protected Database database;

    protected Dialect dialect;

    protected Model model;

    protected Session session;

    private static class QueryMakerException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public QueryMakerException(String message) {
            super(message);
        }

        public QueryMakerException(Throwable cause) {
            super(cause);
        }
    }

    private static class QueryCannotMatchException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    public String getName() {
        return "NXQL";
    }

    public boolean accepts(String queryString) {
        return queryString.equals("NXQL")
                || queryString.toLowerCase().trim().startsWith("select ");
    }

    private enum DocKind {
        DIRECT, PROXY;
    }

    public Query buildQuery(SQLInfo sqlInfo, Model model, Session session,
            String query, QueryFilter queryFilter, Object... params)
            throws StorageException {
        this.sqlInfo = sqlInfo;
        database = sqlInfo.database;
        dialect = sqlInfo.dialect;
        this.model = model;
        this.session = session;
        // transform the query according to the transformers defined by the
        // security policies
        SQLQuery sqlQuery = SQLQueryParser.parse(query);
        for (SQLQuery.Transformer transformer : queryFilter.getQueryTransformers()) {
            sqlQuery = transformer.transform(sqlQuery);
        }

        /*
         * Find all relevant types and keys for the criteria.
         */

        QueryAnalyzer info = new QueryAnalyzer();
        try {
            info.visitQuery(sqlQuery);
        } catch (QueryCannotMatchException e) {
            // query cannot match
            return null;
        } catch (QueryMakerException e) {
            throw new StorageException(e.getMessage(), e);
        }

        /*
         * Find all the types to take into account (all concrete types being a
         * subtype of the passed types) based on the FROM list.
         */

        Set<String> types = new HashSet<String>();
        for (String typeName : info.fromTypes) {
            if ("document".equals(typeName)) {
                typeName = "Document";
            }
            Set<String> subTypes = model.getDocumentSubTypes(typeName);
            if (subTypes == null) {
                throw new StorageException("Unknown type: " + typeName);
            }
            types.addAll(subTypes);
        }
        types.remove(model.ROOT_TYPE);

        /*
         * Restrict types based on toplevel ecm:primaryType and ecm:mixinType
         * predicates.
         */

        types.removeAll(info.typesExcluded);
        if (!info.typesAnyRequired.isEmpty()) {
            types.retainAll(info.typesAnyRequired);
        }
        if (types.isEmpty()) {
            // conflicting types requirement, query cannot match
            return null;
        }

        /*
         * Merge facet filter into mixin clauses and immutable flag.
         */

        FacetFilter facetFilter = queryFilter.getFacetFilter();
        if (facetFilter == null) {
            facetFilter = FacetFilter.ALLOW;
        }
        info.mixinsExcluded.addAll(facetFilter.excluded);
        if (info.mixinsExcluded.remove(FACET_IMMUTABLE)) {
            if (info.immutableClause == Boolean.TRUE) {
                // conflict on immutable condition, query cannot match
                return null;
            }
            info.immutableClause = Boolean.FALSE;
        }
        info.mixinsAllRequired.addAll(facetFilter.required);
        if (info.mixinsAllRequired.remove(FACET_IMMUTABLE)) {
            if (info.immutableClause == Boolean.FALSE) {
                // conflict on immutable condition, query cannot match
                return null;
            }
            info.immutableClause = Boolean.TRUE;
        }

        /*
         * Find the relevant tables to join with.
         */

        Set<String> fragmentNames = new HashSet<String>();
        for (String prop : info.props) {
            PropertyInfo propertyInfo = model.getPropertyInfo(prop);
            if (propertyInfo == null) {
                throw new StorageException("Unknown field: " + prop);
            }
            fragmentNames.add(propertyInfo.fragmentName);
        }
        fragmentNames.remove(model.hierTableName);

        // Do we need to add the versions table too?
        if (info.needsVersionsTable || info.immutableClause != null) {
            fragmentNames.add(model.VERSION_TABLE_NAME);
        }

        /*
         * Build the FROM / JOIN criteria for each select.
         */

        DocKind[] docKinds;
        if (info.proxyClause == Boolean.TRUE) {
            if (info.immutableClause == Boolean.FALSE) {
                // proxy but not immutable: query cannot match
                return null;
            }
            docKinds = new DocKind[] { DocKind.PROXY };
        } else if (info.proxyClause == Boolean.FALSE
                || info.immutableClause == Boolean.FALSE) {
            docKinds = new DocKind[] { DocKind.DIRECT };
        } else {
            docKinds = new DocKind[] { DocKind.DIRECT, DocKind.PROXY };
        }

        Table hier = database.getTable(model.hierTableName);

        boolean aliasColumns = docKinds.length > 1;
        Select select = null;
        String orderBy = null;
        List<String> statements = new ArrayList<String>(2);
        List<Serializable> selectParams = new LinkedList<Serializable>();
        for (DocKind docKind : docKinds) {

            // The hierarchy table, which may be an alias table.
            Table hierTable;
            // Quoted id in the hierarchy. This is the id returned by the query.
            String hierId;
            // The hierarchy table of the data.
            Table dataHierTable;
            // Quoted id attached to the data that matches.
            String dataHierId;

            List<String> joins = new LinkedList<String>();
            List<Serializable> joinsParams = new LinkedList<Serializable>();
            LinkedList<String> leftJoins = new LinkedList<String>();
            List<Serializable> leftJoinsParams = new LinkedList<Serializable>();
            List<String> whereClauses = new LinkedList<String>();
            List<Serializable> whereParams = new LinkedList<Serializable>();

            switch (docKind) {
            case DIRECT:
                hierTable = hier;
                hierId = hierTable.getColumn(model.MAIN_KEY).getFullQuotedName();
                dataHierTable = hierTable;
                dataHierId = hierId;
                joins.add(hierTable.getQuotedName());
                break;
            case PROXY:
                hierTable = new TableAlias(hier, TABLE_HIER_ALIAS);
                String hierFrom = hier.getQuotedName() + " "
                        + hierTable.getQuotedName(); // TODO use dialect
                hierId = hierTable.getColumn(model.MAIN_KEY).getFullQuotedName();
                // joined (data)
                dataHierTable = hier;
                dataHierId = hier.getColumn(model.MAIN_KEY).getFullQuotedName();
                // proxies
                Table proxies = database.getTable(model.PROXY_TABLE_NAME);
                String proxiesid = proxies.getColumn(model.MAIN_KEY).getFullQuotedName();
                String proxiestargetid = proxies.getColumn(
                        model.PROXY_TARGET_KEY).getFullQuotedName();
                // join all that
                joins.add(hierFrom);
                joins.add(String.format(JOIN_ON, proxies.getQuotedName(),
                        hierId, proxiesid));
                joins.add(String.format(JOIN_ON, dataHierTable.getQuotedName(),
                        dataHierId, proxiestargetid));
                break;
            default:
                throw new AssertionError(docKind);
            }

            // main data joins
            for (String fragmentName : fragmentNames) {
                Table table = database.getTable(fragmentName);
                // the versions table joins on the real hier table
                boolean useHier = model.VERSION_TABLE_NAME.equals(fragmentName);
                leftJoins.add(String.format(JOIN_ON, table.getQuotedName(),
                        useHier ? hierId : dataHierId, table.getColumn(
                                model.MAIN_KEY).getFullQuotedName()));
            }

            /*
             * Filter on facets and mixin types, and create the structural WHERE
             * clauses for the type.
             */

            List<String> typeStrings = new ArrayList<String>(types.size());
            NEXT_TYPE: for (String type : types) {
                Set<String> facets = model.getDocumentTypeFacets(type);
                for (String facet : info.mixinsExcluded) {
                    if (facets.contains(facet)) {
                        continue NEXT_TYPE;
                    }
                }
                for (String facet : info.mixinsAllRequired) {
                    if (!facets.contains(facet)) {
                        continue NEXT_TYPE;
                    }
                }
                if (!info.mixinsAnyRequired.isEmpty()) {
                    Set<String> intersection = new HashSet<String>(
                            info.mixinsAnyRequired);
                    intersection.retainAll(facets);
                    if (intersection.isEmpty()) {
                        continue NEXT_TYPE;
                    }
                }
                // this type is good
                typeStrings.add("?");
                whereParams.add(type);
            }
            if (typeStrings.isEmpty()) {
                return null; // mixins excluded all types, no match possible
            }
            whereClauses.add(String.format(
                    "%s IN (%s)",
                    dataHierTable.getColumn(model.MAIN_PRIMARY_TYPE_KEY).getFullQuotedName(),
                    StringUtils.join(typeStrings, ", ")));

            /*
             * Add clause for immutable match.
             */

            if (docKind == DocKind.DIRECT && info.immutableClause != null) {
                String where = String.format("%s IS %s",
                        database.getTable(model.VERSION_TABLE_NAME).getColumn(
                                model.MAIN_KEY).getFullQuotedName(),
                        info.immutableClause.booleanValue() ? "NOT NULL"
                                : "NULL");
                whereClauses.add(where);
            }

            /*
             * Parse the WHERE clause from the original query, and deduce from
             * it actual WHERE clauses and potential JOINs.
             */

            WhereBuilder whereBuilder;
            try {
                whereBuilder = new WhereBuilder(database, session, hierTable,
                        hierId, dataHierTable, dataHierId,
                        docKind == DocKind.PROXY, aliasColumns);
            } catch (QueryMakerException e) {
                throw new StorageException(e.getMessage(), e);
            }
            if (info.wherePredicate != null) {
                info.wherePredicate.accept(whereBuilder);
                // JOINs added by fulltext queries
                joins.addAll(whereBuilder.joins);
                joinsParams.addAll(whereBuilder.joinsParams);
                // WHERE clause
                String where = whereBuilder.buf.toString();
                if (where.length() != 0) {
                    whereClauses.add(where);
                    whereParams.addAll(whereBuilder.whereParams);
                }
            }

            /*
             * Security check.
             */

            if (queryFilter.getPrincipals() != null) {
                Serializable principals = queryFilter.getPrincipals();
                Serializable permissions = queryFilter.getPermissions();
                if (!dialect.supportsArrays()) {
                    principals = StringUtils.join((String[]) principals, '|');
                    permissions = StringUtils.join((String[]) permissions, '|');
                }
                if (dialect.supportsReadAcl()) {
                    /* optimized read acl */
                    whereClauses.add(dialect.getReadAclsCheckSql("r.acl_id"));
                    whereParams.add(principals);
                    joins.add(String.format("%s AS r ON %s = r.id",
                            model.HIER_READ_ACL_TABLE_NAME, hierId));
                } else {
                    whereClauses.add(dialect.getSecurityCheckSql(hierId));
                    whereParams.add(principals);
                    whereParams.add(permissions);
                }
            }

            /*
             * Columns on which to do ordering.
             */

            String selectWhat = hierId;
            if (aliasColumns) {
                // UNION, so we need all orderable columns, aliased
                int n = 0;
                for (String key : info.orderKeys) {
                    Column column = whereBuilder.findColumn(key, false, true);
                    String qname = column.getFullQuotedName();
                    selectWhat += ", " + qname + " AS " + dialect.openQuote()
                            + COL_ORDER_ALIAS_PREFIX + ++n
                            + dialect.closeQuote();
                }
            }

            /*
             * Order by. Compute it just once. May use just aliases.
             */

            if (orderBy == null && sqlQuery.orderBy != null) {
                whereBuilder.buf.setLength(0);
                sqlQuery.orderBy.accept(whereBuilder);
                orderBy = whereBuilder.buf.toString();
            }

            /*
             * Resulting select.
             */

            select = new Select(null);
            select.setWhat(selectWhat);
            leftJoins.addFirst(StringUtils.join(joins, " JOIN "));
            select.setFrom(StringUtils.join(leftJoins, " LEFT JOIN "));
            select.setWhere(StringUtils.join(whereClauses, " AND "));
            selectParams.addAll(joinsParams);
            selectParams.addAll(leftJoinsParams);
            selectParams.addAll(whereParams);

            statements.add(select.getStatement());
        }

        /*
         * Create the whole select.
         */

        if (statements.size() > 1) {
            select = new Select(null);
            select.setWhat(hier.getColumn(model.MAIN_KEY).getQuotedName());
            String from = '(' + StringUtils.join(statements, " UNION ALL ") + ')';
            if (dialect.needsAliasForDerivedTable()) {
                from += " AS _T";
            }
            select.setFrom(from);
        }
        select.setOrderBy(orderBy);

        List<Column> whatColumns = Collections.singletonList(hier.getColumn(model.MAIN_KEY));
        Query q = new Query();
        q.selectInfo = new SQLInfoSelect(select.getStatement(), whatColumns,
                null, null);
        q.selectParams = selectParams;
        return q;
    }

    /**
     * Collects various info about the query AST, and rewrites the toplevel AND
     * {@link Predicates} of the WHERE clause into a single
     * {@link MultiExpression} for easier analysis.
     */
    public class QueryAnalyzer extends DefaultQueryVisitor {

        private static final long serialVersionUID = 1L;

        public final Set<String> fromTypes = new HashSet<String>();

        /** Single valued properties for which a join is needed. */
        public final Set<String> props = new HashSet<String>();

        public final Set<String> orderKeys = new HashSet<String>();

        public boolean needsVersionsTable;

        protected boolean inOrderBy;

        protected final List<Operand> toplevelOperands = new LinkedList<Operand>();

        /** One of these types is required (if not empty). */
        protected final Set<String> typesAnyRequired = new HashSet<String>();

        /** All these types are excluded. */
        protected final Set<String> typesExcluded = new HashSet<String>();

        /** One of these mixins is required (if not empty). */
        protected final Set<String> mixinsAnyRequired = new HashSet<String>();

        /** All these mixins are required. */
        protected final Set<String> mixinsAllRequired = new HashSet<String>();

        /** All these mixins are excluded. */
        protected final Set<String> mixinsExcluded = new HashSet<String>();

        protected Boolean immutableClause;

        protected Boolean proxyClause;

        // TODO protected Boolean versionClause;

        protected MultiExpression wherePredicate;

        @Override
        public void visitFromClause(FromClause node) {
            FromList elements = node.elements;
            for (int i = 0; i < elements.size(); i++) {
                String type = elements.get(i);
                fromTypes.add(type);
            }
        }

        @Override
        public void visitWhereClause(WhereClause node) {
            analyzeToplevelOperands(node.predicate);
            wherePredicate = new MultiExpression(Operator.AND, toplevelOperands);
            super.visitMultiExpression(wherePredicate);
        }

        /**
         * Checks toplevel ANDed operands, and extracts those that directly
         * impact the types restictions:
         * <ul>
         * <li>ecm:primaryType OP literal</li>
         * <li>ecm:mixinType OP literal (except for Immutable)</li>
         * </ul>
         * where OP is {@code =} or {@code <>}.
         * <p>
         * Immutable is left in the clause as it is a per-document facet, not a
         * per-type one.
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
                    boolean isEq = op == Operator.EQ;
                    // put reference on the left side
                    if (expr.rvalue instanceof Reference) {
                        expr = new Expression(expr.rvalue, op, expr.lvalue);
                    }
                    if (expr.lvalue instanceof Reference
                            && expr.rvalue instanceof StringLiteral) {
                        String name = ((Reference) expr.lvalue).name;
                        String value = ((StringLiteral) expr.rvalue).value;
                        if (NXQL.ECM_PRIMARYTYPE.equals(name)) {
                            (isEq ? typesAnyRequired : typesExcluded).add(value);
                            return;
                        }
                        if (NXQL.ECM_MIXINTYPE.equals(name)) {
                            if (FACET_IMMUTABLE.equals(value)) {
                                Boolean im = Boolean.valueOf(isEq);
                                if (immutableClause != null
                                        && immutableClause != im) {
                                    throw new QueryCannotMatchException();
                                }
                                immutableClause = im;
                                needsVersionsTable = true;
                            } else {
                                (isEq ? mixinsAllRequired : mixinsExcluded).add(value);
                            }
                            return;
                        }
                    }
                    if (expr.lvalue instanceof Reference
                            && expr.rvalue instanceof IntegerLiteral) {
                        String name = ((Reference) expr.lvalue).name;
                        long v = ((IntegerLiteral) expr.rvalue).value;
                        if (NXQL.ECM_ISPROXY.equals(name)) {
                            if (v != 0 && v != 1) {
                                throw new QueryMakerException(
                                        NXQL.ECM_ISPROXY
                                                + " requires literal 0 or 1 as right argument");
                            }
                            Boolean pr = Boolean.valueOf(v == 1);
                            if (proxyClause != null && proxyClause != pr) {
                                throw new QueryCannotMatchException();
                            }
                            proxyClause = pr;
                            return;
                        }
                    }
                }
                if (op == Operator.IN || op == Operator.NOTIN) {
                    boolean isIn = op == Operator.IN;
                    // put reference on the left side
                    if (expr.rvalue instanceof Reference) {
                        expr = new Expression(expr.rvalue, op, expr.lvalue);
                    }
                    if (expr.lvalue instanceof Reference
                            && expr.rvalue instanceof LiteralList) {
                        String name = ((Reference) expr.lvalue).name;
                        if (NXQL.ECM_PRIMARYTYPE.equals(name)) {
                            Set<String> set = new HashSet<String>();
                            for (Literal literal : (LiteralList) expr.rvalue) {
                                if (!(literal instanceof StringLiteral)) {
                                    throw new QueryMakerException(
                                            NXQL.ECM_PRIMARYTYPE
                                                    + " IN requires string literals");
                                }
                                set.add(((StringLiteral) literal).value);
                            }
                            if (isIn) {
                                if (typesAnyRequired.isEmpty()) {
                                    // use as new set
                                    typesAnyRequired.addAll(set);
                                } else {
                                    // intersect
                                    typesAnyRequired.retainAll(set);
                                    if (typesAnyRequired.isEmpty()) {
                                        throw new QueryCannotMatchException();
                                    }
                                }
                            } else {
                                typesExcluded.addAll(set);
                            }
                            return;
                        }
                        if (NXQL.ECM_MIXINTYPE.equals(name)) {
                            Set<String> set = new HashSet<String>();
                            for (Literal literal : (LiteralList) expr.rvalue) {
                                if (!(literal instanceof StringLiteral)) {
                                    throw new QueryMakerException(
                                            NXQL.ECM_MIXINTYPE
                                                    + " IN requires string literals");
                                }
                                String value = ((StringLiteral) literal).value;
                                if (FACET_IMMUTABLE.equals(value)) {
                                    Boolean im = Boolean.valueOf(isIn);
                                    if (immutableClause != null
                                            && immutableClause != im) {
                                        throw new QueryCannotMatchException();
                                    }
                                    immutableClause = im;
                                    needsVersionsTable = true;
                                } else {
                                    set.add(value);
                                }
                            }
                            if (isIn) {
                                if (mixinsAnyRequired.isEmpty()) {
                                    mixinsAnyRequired.addAll(set);
                                } else {
                                    throw new QueryMakerException(
                                            NXQL.ECM_MIXINTYPE
                                                    + " cannot have more than one IN clause");
                                }
                            } else {
                                mixinsExcluded.addAll(set);
                            }
                            return;
                        }
                    }
                }
            }
            toplevelOperands.add(node);
        }

        @Override
        public void visitReference(Reference node) {
            String name = node.name;
            if (NXQL.ECM_PATH.equals(name)) {
                if (inOrderBy) {
                    throw new QueryMakerException("Cannot order by: " + name);
                }
                return;
            }
            if (NXQL.ECM_ISPROXY.equals(name)) {
                if (inOrderBy) {
                    throw new QueryMakerException("Cannot order by: " + name);
                }
                return;
            }
            if (NXQL.ECM_ISVERSION.equals(name)) {
                if (inOrderBy) {
                    throw new QueryMakerException("Cannot order by: " + name);
                }
                needsVersionsTable = true;
                return;
            }
            if (NXQL.ECM_PRIMARYTYPE.equals(name) || //
                    NXQL.ECM_MIXINTYPE.equals(name) || //
                    NXQL.ECM_UUID.equals(name) || //
                    NXQL.ECM_NAME.equals(name) || //
                    NXQL.ECM_PARENTID.equals(name)) {
                if (inOrderBy) {
                    orderKeys.add(name);
                }
                return;
            }
            if (NXQL.ECM_LIFECYCLESTATE.equals(name)) {
                props.add(model.MISC_LIFECYCLE_STATE_PROP);
                if (inOrderBy) {
                    orderKeys.add(name);
                }
                return;
            }
            if (NXQL.ECM_VERSIONLABEL.equals(name)) {
                props.add(model.VERSION_LABEL_PROP);
                if (inOrderBy) {
                    orderKeys.add(name);
                }
                return;
            }
            if (name.startsWith(NXQL.ECM_FULLTEXT)) {
                if (dialect.isFulltextTableNeeded()) {
                    // we only use this for its fragment name
                    props.add(model.FULLTEXT_SIMPLETEXT_PROP);
                }
                return;
            }
            if (name.startsWith(NXQL.ECM_PREFIX)) {
                throw new QueryMakerException("Unknown field: " + name);
            }

            PropertyInfo propertyInfo = model.getPropertyInfo(name);
            if (propertyInfo == null) {
                throw new QueryMakerException("Unknown field: " + name);
            }
            if (!propertyInfo.propertyType.isArray()) {
                // join with table if it is a single valued property
                props.add(name);
            }
            if (inOrderBy) {
                orderKeys.add(name);
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
     * Boolean literal.
     */
    protected static class BooleanLiteral extends Literal {

        private static final long serialVersionUID = 1L;

        public final boolean value;

        public BooleanLiteral(boolean value) {
            this.value = value;
        }

        public void accept(IVisitor visitor) {
            ((WhereBuilder) visitor).visitBooleanLiteral(this);
        }

        @Override
        public String asString() {
            return String.valueOf(value);
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof BooleanLiteral) {
                return value == ((BooleanLiteral) obj).value;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Boolean.valueOf(value).hashCode();
        }

    }

    /**
     * Builds the database-level WHERE query from the AST.
     */
    public static class WhereBuilder extends DefaultQueryVisitor {

        private static final long serialVersionUID = 1L;

        public static final String PATH_SEP = "/";

        public final StringBuilder buf = new StringBuilder();

        public final List<String> joins = new LinkedList<String>();

        public final List<String> joinsParams = new LinkedList<String>();

        public final List<Serializable> whereParams = new LinkedList<Serializable>();

        private final Session session;

        private final Model model;

        private final Dialect dialect;

        private final Database database;

        private final Table hierTable;

        private final String hierId;

        private final Table dataHierTable;

        private final String dataHierId;

        private boolean isProxies;

        private boolean aliasColumns;

        // internal fields

        private boolean allowArray;

        private boolean inOrderBy;

        private int orderByCount;

        private int ftJoinNumber;

        public WhereBuilder(Database database, Session session,
                Table hierTable, String hierId, Table dataHierTable,
                String dataHierId, boolean isProxies, boolean aliasColumns) {
            try {
                this.session = session;
                this.model = session.getModel();
                this.dialect = model.getDialect();
                this.database = database;
                this.hierTable = hierTable;
                this.hierId = hierId;
                this.dataHierTable = dataHierTable;
                this.dataHierId = dataHierId;
                this.isProxies = isProxies;
                this.aliasColumns = aliasColumns;
            } catch (StorageException e) {
                throw new RuntimeException(e);
            }
        }

        public Column findColumn(String name, boolean allowArray,
                boolean inOrderBy) {
            Column column;
            if (name.startsWith(NXQL.ECM_PREFIX)) {
                column = getSpecialColumn(name);
            } else {
                PropertyInfo propertyInfo = model.getPropertyInfo(name);
                if (propertyInfo == null) {
                    throw new QueryMakerException("Unknown field: " + name);
                }
                Table table = database.getTable(propertyInfo.fragmentName);
                if (propertyInfo.propertyType.isArray()) {
                    if (!allowArray) {
                        String msg = inOrderBy ? "Cannot use collection %s in ORDER BY clause"
                                : "Can only use collection %s with =, <>, IN or NOT IN clause";
                        throw new QueryMakerException(String.format(msg, name));
                    }
                    // arrays are allowed when in a EXISTS subselect
                    column = table.getColumn(model.COLL_TABLE_VALUE_KEY);
                } else {
                    column = table.getColumn(propertyInfo.fragmentKey);
                }
            }
            return column;
        }

        protected Column getSpecialColumn(String name) {
            if (NXQL.ECM_PRIMARYTYPE.equals(name)) {
                return dataHierTable.getColumn(model.MAIN_PRIMARY_TYPE_KEY);
            }
            if (NXQL.ECM_MIXINTYPE.equals(name)) {
                // toplevel ones have been extracted by the analyzer
                throw new QueryMakerException("Cannot use non-toplevel " + name
                        + " in query");
            }
            if (NXQL.ECM_UUID.equals(name)) {
                return hierTable.getColumn(model.MAIN_KEY);
            }
            if (NXQL.ECM_NAME.equals(name)) {
                return hierTable.getColumn(model.HIER_CHILD_NAME_KEY);
            }
            if (NXQL.ECM_PARENTID.equals(name)) {
                return hierTable.getColumn(model.HIER_PARENT_KEY);
            }
            if (NXQL.ECM_LIFECYCLESTATE.equals(name)) {
                return database.getTable(model.MISC_TABLE_NAME).getColumn(
                        model.MISC_LIFECYCLE_STATE_KEY);
            }
            if (name.startsWith(NXQL.ECM_FULLTEXT)) {
                throw new QueryMakerException(NXQL.ECM_FULLTEXT
                        + " must be used as left-hand operand");
            }
            if (NXQL.ECM_VERSIONLABEL.equals(name)) {
                return database.getTable(model.VERSION_TABLE_NAME).getColumn(
                        model.VERSION_LABEL_KEY);
            }
            throw new QueryMakerException("Unknown field: " + name);
        }

        @Override
        public void visitQuery(SQLQuery node) {
            super.visitQuery(node);
            // intentionally does not set limit or offset in the query
        }

        @Override
        public void visitMultiExpression(MultiExpression node) {
            // Don't add parentheses as for now this is always toplevel.
            // This expression is implicitely ANDed with other toplevel clauses
            // for types and security.
            for (Iterator<Operand> it = node.values.iterator(); it.hasNext();) {
                it.next().accept(this);
                if (it.hasNext()) {
                    node.operator.accept(this);
                }
            }
        }

        @Override
        public void visitExpression(Expression node) {
            buf.append('(');
            String name = node.lvalue instanceof Reference ? ((Reference) node.lvalue).name
                    : null;
            Operator op = node.operator;
            if (op == Operator.STARTSWITH) {
                visitExpressionStartsWith(node, name);
            } else if (NXQL.ECM_PATH.equals(name)) {
                visitExpressionEcmPath(node);
            } else if (NXQL.ECM_ISPROXY.equals(name)) {
                visitExpressionIsProxy(node);
            } else if (NXQL.ECM_ISVERSION.equals(name)) {
                visitExpressionIsVersion(node);
            } else if (name != null && name.startsWith(NXQL.ECM_FULLTEXT)) {
                visitExpressionFulltext(node, name);
            } else if ((op == Operator.EQ || op == Operator.NOTEQ
                    || op == Operator.IN || op == Operator.NOTIN
                    || op == Operator.LIKE || op == Operator.NOTLIKE)
                    && name != null && !name.startsWith(NXQL.ECM_PREFIX)) {
                PropertyInfo propertyInfo = model.getPropertyInfo(name);
                if (propertyInfo == null) {
                    throw new QueryMakerException("Unknown field: " + name);
                }
                if (propertyInfo.propertyType.isArray()) {
                    // use EXISTS with subselect clause
                    boolean direct = op == Operator.EQ || op == Operator.IN
                            || op == Operator.LIKE;
                    Operator directOp = direct ? op
                            : (op == Operator.NOTEQ ? Operator.EQ
                                    : op == Operator.NOTIN ? Operator.IN
                                            : Operator.LIKE);
                    Table table = database.getTable(propertyInfo.fragmentName);
                    if (!direct) {
                        buf.append("NOT ");
                    }
                    buf.append(String.format(
                            "EXISTS (SELECT 1 FROM %s WHERE %s = %s AND (",
                            table.getQuotedName(), dataHierId, table.getColumn(
                                    model.MAIN_KEY).getFullQuotedName()));
                    allowArray = true;
                    node.lvalue.accept(this);
                    allowArray = false;
                    directOp.accept(this);
                    node.rvalue.accept(this);
                    buf.append("))");
                } else {
                    // boolean literals have to be translated according the
                    // database dialect
                    if (propertyInfo.propertyType == PropertyType.BOOLEAN) {
                        if (!(node.rvalue instanceof IntegerLiteral)) {
                            throw new QueryMakerException(
                                    "Boolean expressions require literal 0 or 1 as right argument");
                        }
                        long v = ((IntegerLiteral) node.rvalue).value;
                        if (v != 0 && v != 1) {
                            throw new QueryMakerException(
                                    "Boolean expressions require literal 0 or 1 as right argument");
                        }
                        node = new Predicate(node.lvalue, node.operator,
                                new BooleanLiteral(v == 1));
                    }
                    // use normal processing
                    super.visitExpression(node);
                }
            } else if (node.operator == Operator.BETWEEN) {
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

        protected void visitExpressionStartsWith(Expression node, String name) {
            if (name == null) {
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
            if (NXQL.ECM_PATH.equals(name)) {
                visitExpressionStartsWithPath(node, path);
            } else {
                visitExpressionStartsWithNonPath(node, name, path);
            }
        }

        protected void visitExpressionStartsWithPath(Expression node,
                String path) {
            // find the id from the path
            Serializable id;
            try {
                Node n = session.getNodeByPath(path, null);
                id = n == null ? null : n.getId();
            } catch (StorageException e) {
                throw new QueryMakerException(e);
            }
            if (id == null) {
                // no such path, always return a false
                // TODO remove the expression more intelligently from the parse
                // tree
                buf.append("0=1");
            } else {
                buf.append(dialect.getInTreeSql(hierId));
                whereParams.add(id);
            }
        }

        protected void visitExpressionStartsWithNonPath(Expression node,
                String name, String path) {
            PropertyInfo propertyInfo = model.getPropertyInfo(name);
            if (propertyInfo == null) {
                throw new QueryMakerException("Unknown field: " + name);
            }
            boolean isArray = propertyInfo.propertyType.isArray();
            if (isArray) {
                // use EXISTS with subselect clause
                Table table = database.getTable(propertyInfo.fragmentName);
                buf.append(String.format(
                        "EXISTS (SELECT 1 FROM %s WHERE %s = %s AND ",
                        table.getQuotedName(), dataHierId, table.getColumn(
                                model.MAIN_KEY).getFullQuotedName()));
            }
            buf.append('(');
            allowArray = true;
            node.lvalue.accept(this);
            buf.append(" = ");
            node.rvalue.accept(this);
            buf.append(" OR ");
            node.lvalue.accept(this);
            buf.append(" LIKE ");
            // TODO escape % chars...
            new StringLiteral(path + PATH_SEP + '%').accept(this);
            allowArray = false;
            buf.append(')');
            if (isArray) {
                buf.append(')');
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
                Node n = session.getNodeByPath(path, null);
                id = n == null ? null : n.getId();
            } catch (StorageException e) {
                throw new QueryMakerException(e);
            }
            if (id == null) {
                // no such path, always return a false
                // TODO remove the expression more intelligently from the parse
                // tree
                buf.append("0=1");
            } else {
                buf.append(hierTable.getColumn(model.MAIN_KEY).getFullQuotedName()
                        + " = ?");
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
            buf.append(database.getTable(model.VERSION_TABLE_NAME).getColumn(
                    model.MAIN_KEY).getFullQuotedName());
            buf.append(bool ? " IS NOT NULL" : " IS NULL");
        }

        protected void visitExpressionFulltext(Expression node, String name) {
            if (name.equals(NXQL.ECM_FULLTEXT)) {
                name = Model.FULLTEXT_DEFAULT_INDEX;
            } else {
                // ecm:fulltext_indexname
                name = name.substring(NXQL.ECM_FULLTEXT.length() + 1);
                if (!model.fulltextInfo.indexNames.contains(name)) {
                    throw new QueryMakerException("No such fulltext index: "
                            + name);
                }
            }
            if (node.operator != Operator.EQ && node.operator != Operator.LIKE) {
                throw new QueryMakerException(NXQL.ECM_FULLTEXT
                        + " requires = or LIKE operator");
            }
            if (!(node.rvalue instanceof StringLiteral)) {
                throw new QueryMakerException(NXQL.ECM_FULLTEXT
                        + " requires literal string as right argument");
            }
            String fulltextQuery = ((StringLiteral) node.rvalue).value;
            fulltextQuery = dialect.getDialectFulltextQuery(fulltextQuery);
            Column mainColumn = dataHierTable.getColumn(model.MAIN_KEY);
            String[] info = dialect.getFulltextMatch(name, fulltextQuery,
                    mainColumn, model, database);
            String joinExpr = info[0];
            String joinParam = info[1];
            String whereExpr = info[2];
            String whereParam = info[3];
            String joinAlias = getFtJoinAlias();
            if (joinExpr != null) {
                // specific join table (H2)
                joins.add(String.format(joinExpr, joinAlias));
                if (joinParam != null) {
                    joinsParams.add(joinParam);
                }
            }
            if (whereExpr != null) {
                buf.append(String.format(whereExpr, joinAlias));
                if (whereParam != null) {
                    whereParams.add(whereParam);
                }
            } else {
                buf.append("1=1"); // we still need an expression in the tree
            }
        }

        private String getFtJoinAlias() {
            ftJoinNumber++;
            if (ftJoinNumber == 1) {
                return "_FT";
            } else {
                return "_FT" + ftJoinNumber;
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
            Column column = findColumn(node.name, allowArray, inOrderBy);
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
            buf.append('?');
            whereParams.add(node.value);
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

        public void visitBooleanLiteral(BooleanLiteral node) {
            buf.append('?');
            whereParams.add(Boolean.valueOf(node.value));
        }

        @Override
        public void visitOrderByList(OrderByList node) {
            inOrderBy = true;
            orderByCount = 0;
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
            if (aliasColumns) {
                buf.append(dialect.openQuote());
                buf.append(COL_ORDER_ALIAS_PREFIX);
                buf.append(++orderByCount);
                buf.append(dialect.closeQuote());
            } else {
                node.reference.accept(this);
            }
            if (node.isDescending) {
                buf.append(" DESC");
            }
        }

    }

}
