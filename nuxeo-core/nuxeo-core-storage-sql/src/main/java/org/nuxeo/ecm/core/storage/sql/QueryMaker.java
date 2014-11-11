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
import org.nuxeo.ecm.core.storage.sql.db.Select;
import org.nuxeo.ecm.core.storage.sql.db.Table;

/**
 * Transformer of NXQL queries into underlying SQL queries to the actual
 * database.
 * <p>
 * This needs to transform statements like:
 *
 * <pre>
 * SELECT * FROM File WHERE dc:title = 'abc' AND uid:uid = '123'
 * </pre>
 *
 * into:
 *
 * <pre>
 * SELECT hierarchy.id
 *   FROM hierarchy
 *     LEFT OUTER JOIN dublincore ON dublincore.id = hierarchy.id
 *     LEFT OUTER JOIN uid ON uid.id = hierarchy.id
 *   WHERE
 *         hierarchy.primarytype IN ('File', 'SubFile')
 *     -- AND hierarchy.isproperty = FALSE  // redundant with types
 *     AND (dublincore.title = 'abc' AND uid.uid = '123')
 * </pre>
 *
 * @author Florent Guillaume
 */
public class QueryMaker {

    private final SQLInfo sqlInfo;

    private final Model model;

    private final SQLQuery query;

    public SQLInfoSelect selectInfo;

    public List<Serializable> params;

    public QueryMaker(SQLInfo sqlInfo, Model model, SQLQuery query) {
        this.sqlInfo = sqlInfo;
        this.model = model;
        this.query = query;
    }

    public void makeQuery() throws StorageException {
        /*
         * Find all relevant types and keys for the criteria.
         */
        InfoCollector info = new InfoCollector();
        info.visitQuery(query);

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
         * From the keys, find the relevant tables.
         */
        Set<String> fragmentNames = new HashSet<String>();
        for (String key : info.keys) {
            PropertyInfo propertyInfo = model.getPropertyInfo(key);
            if (propertyInfo == null) {
                throw new StorageException("Unknown field: " + key);
            }
            String fragmentName = propertyInfo.fragmentName;
            fragmentNames.add(fragmentName);
        }
        fragmentNames.remove(model.hierTableName);

        /*
         * Build the JOIN criteria.
         */
        Table hierTable = sqlInfo.database.getTable(model.hierTableName);
        Column hierIdColumn = hierTable.getColumn(model.MAIN_KEY);
        String qhier = hierTable.getQuotedName();
        String qhierid = qhier + "." + hierIdColumn.getQuotedName();
        String join;
        if (fragmentNames.isEmpty()) {
            join = null;
        } else {
            List<String> joins = new ArrayList<String>(fragmentNames.size() + 1);
            joins.add("");
            for (String fragmentName : fragmentNames) {
                Table table = sqlInfo.database.getTable(fragmentName);
                String qname = table.getQuotedName();
                String qid = table.getColumn(model.MAIN_KEY).getQuotedName();
                joins.add(qname + " ON " + qname + "." + qid + " = " + qhierid);
            }
            join = StringUtils.join(joins, " LEFT OUTER JOIN ");
        }

        /*
         * Create the structural WHERE clauses for the type.
         */
        params = new LinkedList<Serializable>();
        String qprimary = hierTable.getColumn(model.MAIN_PRIMARY_TYPE_KEY).getQuotedName();
        // String qisprop =
        // hierTable.getColumn(model.HIER_CHILD_ISPROPERTY_KEY).getQuotedName();
        List<String> typeStrings = new ArrayList<String>(types.size());
        for (String type : types) {
            if (type.equals("Root")) {
                // skip root in types
                continue;
            }
            typeStrings.add("?");
            params.add(type);
        }
        // String whereClause = String.format("%s.%s IN (%s) AND %s.%s = %s",
        // qhier, qprimary, StringUtils.join(typeStrings, ", "), qhier,
        // qisprop, sqlInfo.dialect.toBooleanValueString(false));
        String whereClause = String.format("%s.%s IN (%s)", qhier, qprimary,
                StringUtils.join(typeStrings, ", "));

        /*
         * Create the part of the WHERE clause that comes from the original
         * query.
         */
        WhereBuilder whereBuilder = new WhereBuilder(model, sqlInfo);
        if (query.where != null) {
            query.where.accept(whereBuilder);
            String w = whereBuilder.buf.toString();
            if (w.length() != 0) {
                whereClause = whereClause + " AND (" + w + ')';
                params.addAll(whereBuilder.params);
            }
        }
        if (query.orderBy != null) {
            whereBuilder.buf.setLength(0);
            query.orderBy.accept(whereBuilder);
            whereClause = whereClause + whereBuilder.buf.toString();
        }

        /*
         * Create the whole select.
         */
        Select select = new Select(null);
        select.setWhat(qhierid);
        select.setFrom(qhier);
        select.setJoin(join);
        select.setWhere(whereClause);

        List<Column> whatColumns = Collections.singletonList(hierIdColumn);
        selectInfo = new SQLInfoSelect(select.getStatement(), whatColumns,
                Collections.<Column> emptyList());
    }

    public static class InfoCollector extends DefaultQueryVisitor {

        private static final long serialVersionUID = 1L;

        public Set<String> types = new HashSet<String>();

        public Set<String> keys = new HashSet<String>();

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
            keys.add(node.name);
        }

        @Override
        public void visitFunction(Function node) {
            throw new IllegalArgumentException("Function not supported: " +
                    node.toString());
        }

    }

    public static class WhereBuilder extends DefaultQueryVisitor {

        private static final long serialVersionUID = 1L;

        public final Model model;

        public final SQLInfo sqlInfo;

        public final Database database;

        public final StringBuilder buf;

        public final List<Serializable> params;

        public WhereBuilder(Model model, SQLInfo sqlInfo) {
            this.model = model;
            this.sqlInfo = sqlInfo;
            this.database = sqlInfo.database;
            buf = new StringBuilder();
            params = new LinkedList<Serializable>();
        }

        @Override
        public void visitExpression(Expression node) {
            buf.append('(');
            super.visitExpression(node);
            buf.append(')');
        }

        @Override
        public void visitOperator(Operator node) {
            if (node == Operator.STARTSWITH) {
                throw new IllegalArgumentException("STARTSWITH not supported");
            }
            buf.append(' ');
            buf.append(node.toString());
            buf.append(' ');
        }

        @Override
        public void visitReference(Reference node) {
            PropertyInfo propertyInfo = model.getPropertyInfo(node.name);
            if (propertyInfo == null) {
                throw new IllegalArgumentException("Unknown field: " +
                        node.name);
            }
            Table table = database.getTable(propertyInfo.fragmentName);
            Column column = table.getColumn(propertyInfo.fragmentKey);
            String qname = table.getQuotedName() + '.' + column.getQuotedName();
            // some databases (Derby) can't do comparisons on CLOB
            if (column.getSqlType() == Types.CLOB) {
                String colFmt = sqlInfo.dialect.textComparisonCasting();
                if (colFmt != null) {
                    qname = String.format(colFmt, qname, Integer.valueOf(255));
                }
            }
            buf.append(qname);
        }

        @Override
        public void visitLiteralList(LiteralList node) {
            for (Iterator<Literal> it = node.iterator(); it.hasNext();) {
                it.next().accept(this);
                if (it.hasNext()) {
                    buf.append(", ");
                }
            }
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
