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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.query.QueryFilter;
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
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.query.sql.model.StringLiteral;
import org.nuxeo.ecm.core.query.sql.model.WhereClause;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Model.PropertyInfo;
import org.nuxeo.ecm.core.storage.sql.SQLInfo.SQLInfoSelect;
import org.nuxeo.ecm.core.storage.sql.db.Column;
import org.nuxeo.ecm.core.storage.sql.db.Database;
import org.nuxeo.ecm.core.storage.sql.db.Dialect;
import org.nuxeo.ecm.core.storage.sql.db.Select;
import org.nuxeo.ecm.core.storage.sql.db.Table;
import org.nuxeo.ecm.core.storage.sql.db.TableAlias;

/**
 * Transformer of NXQL queries into underlying SQL queries to the actual
 * database.
 * <p>
 * This needs to transform statements like:
 *
 * <pre>
 * SELECT * FROM File
 *   WHERE
 *         dc:title = 'abc' AND uid:uid = '123'
 *     AND dc:contributors = 'bob' -- multi-valued
 * </pre>
 *
 * into:
 *
 * <pre>
 * SELECT DISTINCT hierarchy.id
 *   FROM hierarchy
 *     LEFT JOIN dublincore ON dublincore.id = hierarchy.id
 *     LEFT JOIN uid ON uid.id = hierarchy.id
 *     LEFT JOIN dc_contributors ON dc_contributors.id = hierarchy.id
 *   WHERE
 *         hierarchy.primarytype IN ('File', 'SubFile')
 *     AND (dublincore.title = 'abc' AND uid.uid = '123' AND dc_contributors.item = 'bob')
 *     AND NX_ACCESS_ALLOWED(hierarchy.id, 'user1|user2', 'perm1|perm2')
 * </pre>
 *
 * when proxies are potential matches, we must do two additional joins and
 * differentiate between the uses of "hierarchy" as the hierarchy table or the
 * main table.
 *
 * <pre>
 * SELECT DISTINCT _nxhier.id
 *   FROM hierarchy _nxhier
 *     LEFT JOIN proxies ON proxies.id = _nxhier.id
 *     JOIN hierarchy ON (hierarchy.id = _nxhier.id OR hierarchy.id = proxies.targetid)
 *     LEFT JOIN dublincore ON dublincore.id = hierarchy.id
 *     LEFT JOIN uid ON uid.id = hierarchy.id
 *     LEFT JOIN dc_contributors ON dc_contributors.id = hierarchy.id
 *   WHERE
 *         hierarchy.primarytype IN ('File', 'SubFile')
 *     AND (dublincore.title = 'abc' AND uid.uid = '123' AND dc_contributors.item = 'bob')
 *     AND NX_ACCESS_ALLOWED(_nxhier.id, 'user1|user2', 'perm1|perm2')
 * </pre>
 *
 * @author Florent Guillaume
 */
public class QueryMaker {

    private static final Log log = LogFactory.getLog(QueryMaker.class);

    /**
     * Name of the Immutable facet, added by {@code DocumentModelFactory} when
     * instantiating a proxy or a version.
     */
    public static final String FACET_IMMUTABLE = "Immutable";

    /*
     * Fields used by the search service.
     */

    public static final String ECM_PREFIX = "ecm:";

    public static final String ECM_UUID = "ecm:uuid";

    public static final String ECM_PATH = "ecm:path";

    public static final String ECM_NAME = "ecm:name";

    public static final String ECM_PARENTID = "ecm:parentId";

    public static final String ECM_MIXINTYPE = "ecm:mixinType";

    public static final String ECM_PRIMARYTYPE = "ecm:primaryType";

    public static final String ECM_ISPROXY = "ecm:isProxy";

    public static final String ECM_ISVERSION = "ecm:isCheckedInVersion";

    public static final String ECM_LIFECYCLESTATE = "ecm:currentLifeCycleState";

    public static final String ECM_VERSIONLABEL = "ecm:versionLabel";

    public static final String ECM_FULLTEXT = "ecm:fulltext";

    protected final SQLInfo sqlInfo;

    protected final Database database;

    protected final Dialect dialect;

    protected final Model model;

    protected final Session session;

    protected final SQLQuery query;

    protected final FacetFilter facetFilter;

    protected final QueryFilter queryFilter;

    /** true if the proxies table is used (not excluded by toplevel clause). */
    protected boolean considerProxies;

    public SQLInfoSelect selectInfo;

    public final List<Serializable> selectParams = new LinkedList<Serializable>();;

    /**
     * The hierarchy table, which may be an alias table.
     */
    protected Table hierTable;

    /**
     * Quoted id in the hierarchy. This is the id returned by the query.
     */
    protected String hierId;

    /**
     * The hierarchy table of the data.
     */
    protected Table joinedHierTable;

    /**
     * Quoted id attached to the data that matches.
     */
    protected String joinedHierId;

    public QueryMaker(SQLInfo sqlInfo, Model model, Session session,
            SQLQuery query, QueryFilter queryFilter) {
        this.sqlInfo = sqlInfo;
        database = sqlInfo.database;
        dialect = sqlInfo.dialect;
        this.model = model;
        this.session = session;
        if (queryFilter == null) {
            queryFilter = QueryFilter.EMPTY;
        }
        // transform the query according to the transformers defined by the
        // security policies
        for (SQLQuery.Transformer transformer : queryFilter.getQueryTransformers()) {
            query = transformer.transform(query);
        }
        this.query = query;
        FacetFilter filter = queryFilter.getFacetFilter();
        this.facetFilter = filter == null ? FacetFilter.ALLOW : filter;
        this.queryFilter = queryFilter;
    }

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

    public void makeQuery() throws StorageException {
        // clauses ANDed together
        List<String> whereClauses = new LinkedList<String>();

        /*
         * Find all relevant types and keys for the criteria.
         */

        QueryAnalyzer info = new QueryAnalyzer();
        try {
            info.visitQuery(query);
        } catch (QueryCannotMatchException e) {
            // query cannot match
            return;
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
            return;
        }

        /*
         * Merge facet filter into mixin clauses and immutable flag.
         */

        info.mixinsExcluded.addAll(facetFilter.excluded);
        if (info.mixinsExcluded.remove(FACET_IMMUTABLE)) {
            if (info.immutableClause == Boolean.TRUE) {
                // conflict on immutable condition, query cannot match
                return;
            }
            info.immutableClause = Boolean.FALSE;
        }
        info.mixinsAllRequired.addAll(facetFilter.required);
        if (info.mixinsAllRequired.remove(FACET_IMMUTABLE)) {
            if (info.immutableClause == Boolean.FALSE) {
                // conflict on immutable condition, query cannot match
                return;
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

        /*
         * Deal with proxies / immutable conditions.
         */

        if (info.proxyClause == Boolean.TRUE) {
            if (info.immutableClause == Boolean.FALSE) {
                // conflicting proxy requirements, query cannot match
                return;
            }
            info.immutableClause = null; // shortcut
        }

        // Do we have to join with the proxies table?
        considerProxies = info.proxyClause != Boolean.FALSE &&
                info.immutableClause != Boolean.FALSE;

        // Do we need to add the versions table too?
        if (info.needsVersionsTable || info.immutableClause != null) {
            fragmentNames.add(model.VERSION_TABLE_NAME);
        }

        /*
         * Build the FROM / JOIN criteria.
         */

        List<String> joins = new ArrayList<String>(fragmentNames.size() + 1);
        Table hier = database.getTable(model.hierTableName);
        if (considerProxies) {
            // complex case where we have to add a left joins to link to the
            // proxies table and another join to select both proxies targets and
            // direct documents
            // hier
            String alias = dialect.storesUpperCaseIdentifiers() ? "_NXHIER"
                    : "_nxhier";
            hierTable = new TableAlias(hier, alias);
            String hierfrom = hier.getQuotedName() + " " +
                    hierTable.getQuotedName(); // TODO dialect
            hierId = hierTable.getColumn(model.MAIN_KEY).getFullQuotedName();
            // joined (data)
            joinedHierTable = hier;
            joinedHierId = hier.getColumn(model.MAIN_KEY).getFullQuotedName();
            // proxies
            Table proxies = database.getTable(model.PROXY_TABLE_NAME);
            String proxiesid = proxies.getColumn(model.MAIN_KEY).getFullQuotedName();
            String proxiestargetid = proxies.getColumn(model.PROXY_TARGET_KEY).getFullQuotedName();
            // join all that
            joins.add(String.format(
                    "%s LEFT JOIN %s ON %s = %s JOIN %s ON (%s = %s OR %s = %s)",
                    hierfrom, proxies.getQuotedName(), proxiesid, hierId,
                    joinedHierTable.getQuotedName(), joinedHierId, hierId,
                    joinedHierId, proxiestargetid));
        } else {
            // simple case where we directly refer to the hierarchy table
            hierTable = hier;
            hierId = hierTable.getColumn(model.MAIN_KEY).getFullQuotedName();
            joinedHierTable = hierTable;
            joinedHierId = hierId;
            joins.add(hierTable.getQuotedName());
        }
        for (String fragmentName : fragmentNames) {
            Table table = database.getTable(fragmentName);
            // the versions table joins on the real hier table
            boolean useHier = model.VERSION_TABLE_NAME.equals(fragmentName);
            joins.add(String.format("%s ON %s = %s", table.getQuotedName(),
                    useHier ? hierId : joinedHierId, table.getColumn(
                            model.MAIN_KEY).getFullQuotedName()));
        }
        String from = StringUtils.join(joins, " LEFT JOIN ");

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
            selectParams.add(type);
        }
        if (typeStrings.isEmpty()) {
            return; // mixins excluded all types, no match possible
        }
        whereClauses.add(String.format("%s IN (%s)", joinedHierTable.getColumn(
                model.MAIN_PRIMARY_TYPE_KEY).getFullQuotedName(),
                StringUtils.join(typeStrings, ", ")));

        /*
         * Add clauses for proxy / version matches.
         */

        if (info.proxyClause == Boolean.TRUE &&
                info.immutableClause != Boolean.FALSE) {
            whereClauses.add(String.format("%s IS NOT NULL",
                    database.getTable(model.PROXY_TABLE_NAME).getColumn(
                            model.MAIN_KEY).getFullQuotedName()));
        }

        if (info.immutableClause != null) {
            boolean immutable = info.immutableClause.booleanValue();
            String version = String.format("%s IS %s",
                    database.getTable(model.VERSION_TABLE_NAME).getColumn(
                            model.MAIN_KEY).getFullQuotedName(),
                    immutable ? "NOT NULL" : "NULL");
            if (info.proxyClause == null) {
                if (immutable) {
                    // OR with the proxy check
                    String proxy = String.format(
                            "%s IS NOT NULL",
                            database.getTable(model.PROXY_TABLE_NAME).getColumn(
                                    model.MAIN_KEY).getFullQuotedName());
                    whereClauses.add(String.format("(%s OR %s)", version, proxy));
                } else {
                    whereClauses.add(version);
                    // proxy is null because no join was done
                }
            } else {
                whereClauses.add(version);
            }
        }

        /*
         * Create the part of the WHERE clause that comes from the original
         * query.
         */

        WhereBuilder whereBuilder;
        try {
            whereBuilder = new WhereBuilder();
        } catch (QueryMakerException e) {
            throw new StorageException(e.getMessage(), e);
        }
        if (info.wherePredicate != null) {
            info.wherePredicate.accept(whereBuilder);
            String where = whereBuilder.buf.toString();
            if (where.length() != 0) {
                whereClauses.add(where);
                selectParams.addAll(whereBuilder.params);
            }
        }

        /*
         * Security check.
         */

        if (queryFilter.getPrincipals() != null) {
            whereClauses.add(String.format("NX_ACCESS_ALLOWED(%s, ?, ?) = %s",
                    hierId, dialect.toBooleanValueString(true)));
            Serializable principals;
            Serializable permissions;
            if (dialect.supportsArrays()) {
                principals = queryFilter.getPrincipals();
                permissions = queryFilter.getPermissions();
            } else {
                principals = StringUtils.join(queryFilter.getPrincipals(), '|');
                permissions = StringUtils.join(queryFilter.getPermissions(),
                        '|');
            }
            selectParams.add(principals);
            selectParams.add(permissions);
        }

        /*
         * Order by.
         */

        String orderBy;
        if (query.orderBy == null) {
            orderBy = null;
        } else {
            whereBuilder.buf.setLength(0);
            query.orderBy.accept(whereBuilder);
            orderBy = whereBuilder.buf.toString();
        }

        /*
         * Check if we need a DISTINCT.
         */

        String what = hierId;
        if (considerProxies) {
            // We do LEFT JOINs with collection tables, so we could get
            // identical results.
            // For proxies, there's also a LEFT JOIN with a non equi-join
            // condition that can return two rows.
            what = "DISTINCT " + what;
            // Some dialects need any ORDER BY expressions to appear in the
            // SELECT list when using DISTINCT.
            // This is needed at least by PostgreSQL.
            if (dialect.needsOrderByKeysAfterDistinct()) {
                for (String key : info.orderKeys) {
                    PropertyInfo propertyInfo = model.getPropertyInfo(key);
                    if (propertyInfo == null) {
                        throw new StorageException("Unknown ORDER BY field: " +
                                key);
                    }
                    if (propertyInfo.propertyType.isArray()) {
                        throw new StorageException("Cannot use collection " +
                                key + " in ORDER BY");
                    }
                    Column column = database.getTable(propertyInfo.fragmentName).getColumn(
                            propertyInfo.fragmentKey);
                    String qname = column.getFullQuotedName();
                    what += ", " + qname;
                }
            }
        }
        // else no DISTINCT needed as all impacted tables have unique keys

        /*
         * Create the whole select.
         */
        Select select = new Select(null);
        select.setWhat(what);
        select.setFrom(from);
        select.setWhere(StringUtils.join(whereClauses, " AND "));
        select.setOrderBy(orderBy);

        List<Column> whatColumns = Collections.singletonList(hierTable.getColumn(model.MAIN_KEY));
        selectInfo = new SQLInfoSelect(select.getStatement(), whatColumns,
                Collections.<Column> emptyList());
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
         * Checks tolevel ANDed operands, and extracts those that directly
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
                    if (expr.lvalue instanceof Reference &&
                            expr.rvalue instanceof StringLiteral) {
                        String name = ((Reference) expr.lvalue).name;
                        String value = ((StringLiteral) expr.rvalue).value;
                        if (ECM_PRIMARYTYPE.equals(name)) {
                            (isEq ? typesAnyRequired : typesExcluded).add(value);
                            return;
                        }
                        if (ECM_MIXINTYPE.equals(name)) {
                            if (FACET_IMMUTABLE.equals(value)) {
                                Boolean im = Boolean.valueOf(isEq);
                                if (immutableClause != null &&
                                        immutableClause != im) {
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
                    if (expr.lvalue instanceof Reference &&
                            expr.rvalue instanceof IntegerLiteral) {
                        String name = ((Reference) expr.lvalue).name;
                        long v = ((IntegerLiteral) expr.rvalue).value;
                        if (ECM_ISPROXY.equals(name)) {
                            if (v != 0 && v != 1) {
                                throw new QueryMakerException(ECM_ISPROXY +
                                        " requires literal 0 or 1 as right argument");
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
                    if (expr.lvalue instanceof Reference &&
                            expr.rvalue instanceof LiteralList) {
                        String name = ((Reference) expr.lvalue).name;
                        if (ECM_PRIMARYTYPE.equals(name)) {
                            Set<String> set = new HashSet<String>();
                            for (Literal literal : (LiteralList) expr.rvalue) {
                                if (!(literal instanceof StringLiteral)) {
                                    throw new QueryMakerException(
                                            ECM_PRIMARYTYPE +
                                                    " IN requires string literals");
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
                        if (ECM_MIXINTYPE.equals(name)) {
                            Set<String> set = new HashSet<String>();
                            for (Literal literal : (LiteralList) expr.rvalue) {
                                if (!(literal instanceof StringLiteral)) {
                                    throw new QueryMakerException(
                                            ECM_MIXINTYPE +
                                                    " IN requires string literals");
                                }
                                String value = ((StringLiteral) literal).value;
                                if (FACET_IMMUTABLE.equals(value)) {
                                    Boolean im = Boolean.valueOf(isIn);
                                    if (immutableClause != null &&
                                            immutableClause != im) {
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
                                            ECM_MIXINTYPE +
                                                    " cannot have more than one IN clause");
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
            if (ECM_PATH.equals(name)) {
                if (inOrderBy) {
                    throw new QueryMakerException("Cannot order by: " + name);
                }
                return;
            }
            if (ECM_ISPROXY.equals(name)) {
                if (inOrderBy) {
                    throw new QueryMakerException("Cannot order by: " + name);
                }
                return;
            }
            if (ECM_ISVERSION.equals(name)) {
                if (inOrderBy) {
                    throw new QueryMakerException("Cannot order by: " + name);
                }
                needsVersionsTable = true;
                return;
            }
            if (ECM_PRIMARYTYPE.equals(name) || //
                    ECM_MIXINTYPE.equals(name) || //
                    ECM_UUID.equals(name) || //
                    ECM_NAME.equals(name) || //
                    ECM_PARENTID.equals(name)) {
                return;
            }
            if (ECM_LIFECYCLESTATE.equals(name)) {
                props.add(model.MISC_LIFECYCLE_STATE_PROP);
                return;
            }
            if (ECM_VERSIONLABEL.equals(name)) {
                props.add(model.VERSION_LABEL_PROP);
                return;
            }
            if (name.startsWith(ECM_PREFIX)) {
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
            throw new QueryMakerException("Function not supported: " +
                    node.toString());
        }

        @Override
        public void visitOrderByClause(OrderByClause node) {
            inOrderBy = true;
            super.visitOrderByClause(node);
            inOrderBy = false;
        }

    }

    /**
     * Builds the database-level WHERE query from the AST.
     */
    public class WhereBuilder extends DefaultQueryVisitor {

        private static final long serialVersionUID = 1L;

        public final StringBuilder buf = new StringBuilder();

        public final List<Serializable> params = new LinkedList<Serializable>();

        public boolean allowArray;

        private boolean inOrderBy;

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
                if (!ECM_PATH.equals(name)) {
                    throw new QueryMakerException("STARTSWITH requires " +
                            ECM_PATH + "as left argument");
                }
                visitExpressionStartsWith(node);
            } else if (ECM_ISPROXY.equals(name)) {
                visitExpressionIsProxy(node);
            } else if (ECM_ISVERSION.equals(name)) {
                visitExpressionIsVersion(node);
            } else if ((op == Operator.EQ || op == Operator.NOTEQ ||
                    op == Operator.IN || op == Operator.NOTIN) &&
                    name != null && !name.startsWith(ECM_PREFIX)) {
                PropertyInfo propertyInfo = model.getPropertyInfo(name);
                if (propertyInfo == null) {
                    throw new QueryMakerException("Unknown field: " + name);
                }
                if (propertyInfo.propertyType.isArray()) {
                    // use EXISTS with subselect clause
                    boolean direct = op == Operator.EQ || op == Operator.IN;
                    Operator directOp = direct ? op
                            : (op == Operator.NOTEQ ? Operator.EQ : Operator.IN);
                    Table table = database.getTable(propertyInfo.fragmentName);
                    if (!direct) {
                        buf.append("NOT ");
                    }
                    buf.append(String.format(
                            "EXISTS (SELECT 1 FROM %s WHERE %s = %s AND (",
                            table.getQuotedName(), joinedHierId,
                            table.getColumn(model.MAIN_KEY).getFullQuotedName()));
                    allowArray = true;
                    node.lvalue.accept(this);
                    allowArray = false;
                    directOp.accept(this);
                    node.rvalue.accept(this);
                    buf.append("))");
                } else {
                    // use normal processing
                    super.visitExpression(node);
                }
            } else {
                super.visitExpression(node);
            }
            buf.append(')');
        }

        protected void visitExpressionStartsWith(Expression node) {
            if (!(node.rvalue instanceof StringLiteral)) {
                throw new QueryMakerException(Operator.STARTSWITH +
                        " requires literal path as right argument");
            }
            String path = ((StringLiteral) node.rvalue).value;
            if (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
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
                buf.append("0 = 1");
            } else {
                buf.append("NX_IN_TREE(").append(hierId).append(", ?) = ");
                params.add(id);
                buf.append(dialect.toBooleanValueString(true));
            }
        }

        protected void visitExpressionIsProxy(Expression node) {
            if (node.operator != Operator.EQ && node.operator != Operator.NOTEQ) {
                throw new QueryMakerException(ECM_ISPROXY +
                        " requires = or <> operator");
            }
            if (!(node.rvalue instanceof IntegerLiteral)) {
                throw new QueryMakerException(ECM_ISPROXY +
                        " requires literal 0 or 1 as right argument");
            }
            long v = ((IntegerLiteral) node.rvalue).value;
            if (v != 0 && v != 1) {
                throw new QueryMakerException(ECM_ISPROXY +
                        " requires literal 0 or 1 as right argument");
            }
            boolean bool = node.operator == Operator.EQ ^ v == 0;
            if (considerProxies) {
                buf.append(database.getTable(model.PROXY_TABLE_NAME).getColumn(
                        model.MAIN_KEY).getFullQuotedName());
                buf.append(bool ? " IS NOT NULL" : " IS NULL");
            } else {
                // toplevel clauses excludes proxies
                buf.append(bool ? "0 = 1" : " 1 = 1");
            }
        }

        protected void visitExpressionIsVersion(Expression node) {
            if (node.operator != Operator.EQ && node.operator != Operator.NOTEQ) {
                throw new QueryMakerException(ECM_ISVERSION +
                        " requires = or <> operator");
            }
            if (!(node.rvalue instanceof IntegerLiteral)) {
                throw new QueryMakerException(ECM_ISVERSION +
                        " requires literal 0 or 1 as right argument");
            }
            long v = ((IntegerLiteral) node.rvalue).value;
            if (v != 0 && v != 1) {
                throw new QueryMakerException(ECM_ISVERSION +
                        " requires literal 0 or 1 as right argument");
            }
            boolean bool = node.operator == Operator.EQ ^ v == 0;
            buf.append(database.getTable(model.VERSION_TABLE_NAME).getColumn(
                    model.MAIN_KEY).getFullQuotedName());
            buf.append(bool ? " IS NOT NULL" : " IS NULL");
        }

        @Override
        public void visitOperator(Operator node) {
            buf.append(' ');
            buf.append(node.toString());
            buf.append(' ');
        }

        @Override
        public void visitReference(Reference node) {
            String name = node.name;
            Column column;
            if (name.startsWith(ECM_PREFIX)) {
                column = getSpecialColumn(name);
            } else {
                PropertyInfo propertyInfo = model.getPropertyInfo(name);
                if (propertyInfo == null) {
                    throw new QueryMakerException("Unknown field: " + name);
                }
                Table table = database.getTable(propertyInfo.fragmentName);
                if (propertyInfo.propertyType.isArray()) {
                    if (!allowArray) {
                        throw new QueryMakerException(
                                "Cannot only use collection " + name +
                                        " with =, <>, IN or NOT IN clause");
                    }
                    // arrays are allowed when in a EXISTS subselect
                    column = table.getColumn(model.COLL_TABLE_VALUE_KEY);
                } else {
                    column = table.getColumn(propertyInfo.fragmentKey);
                }
            }
            String qname = column.getFullQuotedName();
            // some databases (Derby) can't do comparisons on CLOB
            if (column.getSqlType() == Types.CLOB) {
                String colFmt;
                if (inOrderBy) {
                    colFmt = dialect.clobCastingInOrderBy();
                } else {
                    colFmt = dialect.clobCasting();
                }
                if (colFmt != null) {
                    qname = String.format(colFmt, qname, Integer.valueOf(255));
                }
            }
            buf.append(qname);
        }

        protected Column getSpecialColumn(String name) {
            if (ECM_PRIMARYTYPE.equals(name)) {
                return joinedHierTable.getColumn(model.MAIN_PRIMARY_TYPE_KEY);
            }
            if (ECM_MIXINTYPE.equals(name)) {
                // toplevel ones have been extracted by the analyzer
                throw new QueryMakerException("Cannot use non-toplevel " +
                        name + " in query");
            }
            if (ECM_UUID.equals(name)) {
                return hierTable.getColumn(model.MAIN_KEY);
            }
            if (ECM_NAME.equals(name)) {
                return hierTable.getColumn(model.HIER_CHILD_NAME_KEY);
            }
            if (ECM_PARENTID.equals(name)) {
                return hierTable.getColumn(model.HIER_PARENT_KEY);
            }
            if (ECM_LIFECYCLESTATE.equals(name)) {
                return database.getTable(model.MISC_TABLE_NAME).getColumn(
                        model.MISC_LIFECYCLE_STATE_KEY);
            }
            if (ECM_VERSIONLABEL.equals(name)) {
                return database.getTable(model.VERSION_TABLE_NAME).getColumn(
                        model.VERSION_LABEL_KEY);
            }
            throw new QueryMakerException("Unknown field: " + name);
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
            params.add(node.toCalendar());
        }

        @Override
        public void visitStringLiteral(StringLiteral node) {
            buf.append('?');
            params.add(node.value);
        }

        @Override
        public void visitDoubleLiteral(DoubleLiteral node) {
            buf.append('?');
            params.add(Double.valueOf(node.value));
        }

        @Override
        public void visitIntegerLiteral(IntegerLiteral node) {
            buf.append('?');
            params.add(Long.valueOf(node.value));
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
            node.reference.accept(this);
            if (node.isDescending) {
                buf.append(" DESC");
            }
        }

    }

}
