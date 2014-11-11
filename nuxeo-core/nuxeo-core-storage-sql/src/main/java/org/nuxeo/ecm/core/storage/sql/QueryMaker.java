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
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.OrderByList;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.query.sql.model.StringLiteral;
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
     * Name of the Immutable facet, added by {@link DocumentModelFactory} when
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

    public SQLInfoSelect selectInfo;

    public List<Serializable> selectParams;

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
        this.facetFilter = queryFilter.getFacetFilter();
        this.queryFilter = queryFilter;
    }

    public void makeQuery() throws StorageException {
        /*
         * Find all relevant types and keys for the criteria.
         */
        InfoCollector info = new InfoCollector();
        try {
            info.visitQuery(query);
        } catch (QueryMakerException e) {
            throw new StorageException(e.getMessage(), e);
        }

        /*
         * Find all the types to take into account (all concrete types being a
         * subtype of the passed types).
         */
        Set<String> types = new HashSet<String>();
        for (String typeName : info.types) {
            if (typeName.equals("document")) {
                typeName = "Document";
            }
            Set<String> subTypes = model.getDocumentSubTypes(typeName);
            if (subTypes == null) {
                throw new StorageException("Unknown type: " + typeName);
            }
            types.addAll(subTypes);
        }

        /*
         * Find the relevant tables.
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

        // TODO find when we can avoid joining with proxies
        boolean usesJoinWithProxies = true;

        if (info.needsProxies) {
            usesJoinWithProxies = true;
        }
        if (info.needsVersions) {
            fragmentNames.add(model.VERSION_TABLE_NAME);
        }

        /*
         * Build the FROM / JOIN criteria.
         */

        List<String> joins = new ArrayList<String>(fragmentNames.size() + 1);
        Table hier = database.getTable(model.hierTableName);
        if (usesJoinWithProxies) {
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
            boolean useHier = fragmentName.equals(model.VERSION_TABLE_NAME);
            joins.add(String.format("%s ON %s = %s", table.getQuotedName(),
                    useHier ? hierId : joinedHierId, table.getColumn(
                            model.MAIN_KEY).getFullQuotedName()));
        }
        String from = StringUtils.join(joins, " LEFT JOIN ");

        /*
         * Create the structural WHERE clauses for the type.
         */
        selectParams = new LinkedList<Serializable>();
        List<String> typeStrings = new ArrayList<String>(types.size());
        for (String type : types) {
            if (type.equals(model.ROOT_TYPE)) {
                // skip root in types
                continue;
            }
            if (!facetFilterAllows(type)) {
                continue;
            }
            typeStrings.add("?");
            selectParams.add(type);
        }
        String whereClause = String.format(
                "%s IN (%s)",
                joinedHierTable.getColumn(model.MAIN_PRIMARY_TYPE_KEY).getFullQuotedName(),
                StringUtils.join(typeStrings, ", "));

        Boolean immutable = facetFilterImmutable();
        if (immutable != null) {
            // TODO filter on proxies + versions
            log.warn("Cannot filter on Immutable facet");
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
        if (query.where != null) {
            query.where.accept(whereBuilder);
            String w = whereBuilder.buf.toString();
            if (w.length() != 0) {
                whereClause = whereClause + " AND (" + w + ')';
                selectParams.addAll(whereBuilder.params);
            }
        }

        /*
         * Security check.
         */
        if (queryFilter.getPrincipals() != null) {
            whereClause = whereClause +
                    String.format(" AND NX_ACCESS_ALLOWED(%s, ?, ?) = %s",
                            hierId, dialect.toBooleanValueString(true));
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

        if (query.orderBy != null) {
            whereBuilder.buf.setLength(0);
            query.orderBy.accept(whereBuilder);
            whereClause = whereClause + whereBuilder.buf.toString();
        }

        /*
         * Check if we need a DISTINCT.
         */
        String what = hierId;
        if (whereBuilder.needsDistinct || usesJoinWithProxies) {
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
                    Table table = database.getTable(propertyInfo.fragmentName);
                    if (propertyInfo.propertyType.isArray()) {
                        throw new StorageException("Cannot use collection" +
                                key + " in ORDER BY");
                    }
                    what += ", " +
                            table.getColumn(propertyInfo.fragmentKey).getFullQuotedName();
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
        select.setWhere(whereClause);

        List<Column> whatColumns = Collections.singletonList(hierTable.getColumn(model.MAIN_KEY));
        selectInfo = new SQLInfoSelect(select.getStatement(), whatColumns,
                Collections.<Column> emptyList());
    }

    public boolean facetFilterAllows(String typeName) {
        if (facetFilter == null) {
            return true;
        }
        if (facetFilter.excluded != null) {
            for (String facet : facetFilter.excluded) {
                if (FACET_IMMUTABLE.equals(facet)) {
                    continue;
                }
                if (model.documentTypeHasFacet(typeName, facet)) {
                    return false;
                }
            }
        }
        if (facetFilter.required != null) {
            for (String facet : facetFilter.required) {
                if (FACET_IMMUTABLE.equals(facet)) {
                    continue;
                }
                if (!model.documentTypeHasFacet(typeName, facet)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks if the Immutable facet is excluded ({@code FALSE}), required (
     * {@code TRUE}) or not filtered on ({@code null}).
     */
    public Boolean facetFilterImmutable() {
        if (facetFilter == null) {
            return null;
        }
        if (facetFilter.excluded != null &&
                facetFilter.excluded.contains(FACET_IMMUTABLE)) {
            return Boolean.FALSE;
        }
        if (facetFilter.required != null &&
                facetFilter.required.contains(FACET_IMMUTABLE)) {
            return Boolean.TRUE;
        }
        return null;
    }

    protected static class QueryMakerException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public QueryMakerException(String message) {
            super(message);
        }

        public QueryMakerException(Throwable cause) {
            super(cause);
        }
    }

    public class InfoCollector extends DefaultQueryVisitor {

        private static final long serialVersionUID = 1L;

        public Set<String> types = new HashSet<String>();

        public Set<String> props = new HashSet<String>();

        public Set<String> orderKeys = new HashSet<String>();

        public boolean needsProxies;

        public boolean needsVersions;

        protected boolean inOrderBy;

        @Override
        public void visitFromClause(FromClause node) {
            FromList elements = node.elements;
            for (int i = 0; i < elements.size(); i++) {
                String type = elements.get(i);
                types.add(type);
            }
        }

        @Override
        public void visitReference(Reference node) {
            String name = node.name;
            if (name.equals(ECM_PATH)) {
                if (inOrderBy) {
                    throw new QueryMakerException("Cannot order by: " + name);
                }
                return;
            }
            if (name.equals(ECM_ISPROXY)) {
                if (inOrderBy) {
                    throw new QueryMakerException("Cannot order by: " + name);
                }
                needsProxies = true;
                return;
            }
            if (name.equals(ECM_ISVERSION)) {
                if (inOrderBy) {
                    throw new QueryMakerException("Cannot order by: " + name);
                }
                needsVersions = true;
                return;
            }
            if (name.equals(ECM_PRIMARYTYPE) || name.equals(ECM_UUID) ||
                    name.equals(ECM_NAME) || name.equals(ECM_PARENTID)) {
                return;
            }
            if (name.equals(ECM_LIFECYCLESTATE)) {
                props.add(model.MISC_LIFECYCLE_STATE_PROP);
                return;
            }
            if (name.equals(ECM_VERSIONLABEL)) {
                props.add(model.VERSION_LABEL_PROP);
                return;
            }
            if (name.startsWith(ECM_PREFIX)) {
                throw new QueryMakerException("Unknown field: " + name);
            }
            props.add(name);
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

    public class WhereBuilder extends DefaultQueryVisitor {

        private static final long serialVersionUID = 1L;

        public final StringBuilder buf;

        public final List<Serializable> params;

        public boolean needsDistinct;

        public WhereBuilder() {
            buf = new StringBuilder();
            params = new LinkedList<Serializable>();
        }

        @Override
        public void visitExpression(Expression node) {
            buf.append('(');
            String lname = node.lvalue instanceof Reference ? ((Reference) node.lvalue).name
                    : null;
            if (node.operator == Operator.STARTSWITH) {
                if (!ECM_PATH.equals(lname)) {
                    throw new QueryMakerException("STARTSWITH requires " +
                            ECM_PATH + "as left argument");
                }
                visitExpressionSpecialStartsWith(node);
            } else if (ECM_ISPROXY.equals(lname)) {
                visitExpressionSpecialProxy(node);
            } else if (ECM_ISVERSION.equals(lname)) {
                visitExpressionSpecialVersion(node);
            } else {
                super.visitExpression(node);
            }
            buf.append(')');
        }

        protected void visitExpressionSpecialStartsWith(Expression node) {
            if (!(node.rvalue instanceof StringLiteral)) {
                throw new QueryMakerException(
                        "STARTSWITH requires literal path as right argument");
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

        protected void visitExpressionSpecialProxy(Expression node) {
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
            buf.append(database.getTable(model.PROXY_TABLE_NAME).getColumn(
                    model.MAIN_KEY).getFullQuotedName());
            buf.append(bool ? " IS NOT NULL" : " IS NULL");
        }

        protected void visitExpressionSpecialVersion(Expression node) {
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
                    column = table.getColumn(model.COLL_TABLE_VALUE_KEY);
                    needsDistinct = true; // table doesn't have unique keys
                    // TODO if operator is <>, we must use a different algorithm
                    // than just an outer join
                } else {
                    column = table.getColumn(propertyInfo.fragmentKey);
                }
            }
            String qname = column.getFullQuotedName();
            // some databases (Derby) can't do comparisons on CLOB
            if (column.getSqlType() == Types.CLOB) {
                String colFmt = dialect.textComparisonCasting();
                if (colFmt != null) {
                    qname = String.format(colFmt, qname, Integer.valueOf(255));
                }
            }
            buf.append(qname);
        }

        protected Column getSpecialColumn(String name) {
            if (name.equals(ECM_PRIMARYTYPE)) {
                return joinedHierTable.getColumn(model.MAIN_PRIMARY_TYPE_KEY);
            }
            if (name.equals(ECM_UUID)) {
                return hierTable.getColumn(model.MAIN_KEY);
            }
            if (name.equals(ECM_NAME)) {
                return hierTable.getColumn(model.HIER_CHILD_NAME_KEY);
            }
            if (name.equals(ECM_PARENTID)) {
                return hierTable.getColumn(model.HIER_PARENT_KEY);
            }
            if (name.equals(ECM_LIFECYCLESTATE)) {
                return database.getTable(model.MISC_TABLE_NAME).getColumn(
                        model.MISC_LIFECYCLE_STATE_KEY);
            }
            if (name.equals(ECM_VERSIONLABEL)) {
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
        public void visitOrderByClause(OrderByClause node) {
            if (!node.elements.isEmpty()) {
                buf.append(" ORDER BY ");
            }
            super.visitOrderByClause(node);
        }

        @Override
        public void visitOrderByList(OrderByList node) {
            for (Iterator<OrderByExpr> it = node.iterator(); it.hasNext();) {
                it.next().accept(this);
                if (it.hasNext()) {
                    buf.append(", ");
                }
            }
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
