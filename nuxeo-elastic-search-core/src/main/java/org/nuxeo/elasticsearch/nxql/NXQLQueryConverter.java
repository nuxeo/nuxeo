/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Tiry
 *     bdelbosc
 */

package org.nuxeo.elasticsearch.nxql;

import java.io.StringReader;
import java.util.Iterator;
import java.util.LinkedList;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;
import org.nuxeo.ecm.core.query.sql.model.DefaultQueryVisitor;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.Literal;
import org.nuxeo.ecm.core.query.sql.model.LiteralList;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operand;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.query.sql.model.SelectClause;

/**
 * Helper class that holds the conversion logic.
 *
 * Conversion is based on the existing NXQL Parser, we are just using a visitor
 * to build the ES request.
 *
 */
public class NXQLQueryConverter {

    public static class ExpressionBuilder {

        public Operator operator;
        public QueryBuilder query;

        public ExpressionBuilder(Operator op) {
            this.operator = op;
            this.query = null;
        }

        public void add(QueryBuilder q, FilterBuilder f) {
            if (q == null && f == null) {
                return;
            }
            QueryBuilder inputQuery = q;
            if (inputQuery == null) {
                inputQuery = QueryBuilders.constantScoreQuery(f);
            }
            if (operator == null) {
                // first level expression
                query = inputQuery;
            } else {
                // boolean query
                if (query == null) {
                    query = QueryBuilders.boolQuery();
                }
                BoolQueryBuilder boolQuery = (BoolQueryBuilder) query;
                if (Operator.AND.equals(operator)) {
                    boolQuery.must(inputQuery);
                } else if (Operator.OR.equals(operator)) {
                    boolQuery.should(inputQuery);
                } else if (Operator.NOT.equals(operator)) {
                    boolQuery.mustNot(inputQuery);
                }
            }
        }

        public void merge(ExpressionBuilder expr) {
            if ((expr.operator == operator) && (query == null)) {
                query = expr.query;
            } else {
                add(expr.query, null);
            }
        }

        public QueryBuilder get() {
            return query;
        }

        @Override
        public String toString() {
            return get().toString();
        }

    }

    public static QueryBuilder toESQueryBuilder(String nxql) {
        final LinkedList<ExpressionBuilder> builders = new LinkedList<ExpressionBuilder>();
        SQLQuery nxqlQuery = SQLQueryParser.parse(new StringReader(nxql));
        final ExpressionBuilder ret = new ExpressionBuilder(null);
        builders.add(ret);
        nxqlQuery.accept(new DefaultQueryVisitor() {

            private static final long serialVersionUID = 1L;

            @Override
            public void visitQuery(SQLQuery node) {
                super.visitQuery(node);
                // intentionally does not set limit or offset in the query
            }

            @Override
            public void visitMultiExpression(MultiExpression node) {
                for (Iterator<Operand> it = node.values.iterator(); it
                        .hasNext();) {
                    it.next().accept(this);
                    if (it.hasNext()) {
                        node.operator.accept(this);
                    }
                }
            }

            @Override
            public void visitSelectClause(SelectClause node) {
                // NOP
            }

            @Override
            public void visitExpression(Expression node) {
                Reference ref = node.lvalue instanceof Reference ? (Reference) node.lvalue
                        : null;
                String name = ref != null ? ref.name : null;
                Operand rvalue = node.rvalue;

                FilterBuilder filter = null;
                QueryBuilder query = null;

                Operator op = node.operator;
                String colName = node.lvalue.toString();
                String value = node.rvalue.toString();
                try {
                    value = ((Literal) node.rvalue).asString();
                } catch (Throwable e) {
                    // do nothing
                }
                if (op == Operator.AND || op == Operator.OR
                        || op == Operator.NOT) {
                    builders.add(new ExpressionBuilder(op));
                    super.visitExpression(node);
                } else if (op == Operator.EQ) {
                    if (colName.equals("dc:title")
                            || colName.startsWith("ecm:fulltext")) {
                        query = QueryBuilders.matchQuery(colName, value)
                                .analyzer("fulltext");
                    } else {
                        filter = FilterBuilders.termFilter(colName, value);
                    }
                } else if (op == Operator.NOTEQ) {
                    // TODO NOT
                } else if (op == Operator.LIKE || op == Operator.ILIKE) {
                    query = QueryBuilders.regexpQuery(colName, value);
                    filter = FilterBuilders.termFilter(colName, value);
                } else if (op == Operator.BETWEEN) {
                    LiteralList l = (LiteralList) rvalue;
                    filter = FilterBuilders.rangeFilter(colName).from(l.get(0))
                            .to(l.get(1));
                } else if (NXQL.ECM_PATH.equals(name)) {
                    // TODO impl ecm:path
                } else if (NXQL.ECM_MIXINTYPE.equals(name)) {
                    // TODO impl ecm_mixintype
                } else if (name != null && name.startsWith(NXQL.ECM_FULLTEXT)
                        && !NXQL.ECM_FULLTEXT_JOBID.equals(name)) {
                    // TODO ecm:jobid
                }

                builders.get(builders.size() - 1).add(query, filter);

                if (op == Operator.AND || op == Operator.OR
                        || op == Operator.NOT) {
                    ExpressionBuilder expr = builders.removeLast();
                    if (!builders.isEmpty()) {
                        builders.getLast().merge(expr);
                    }
                }
            }

        });
        return ret.get();
    }

}
