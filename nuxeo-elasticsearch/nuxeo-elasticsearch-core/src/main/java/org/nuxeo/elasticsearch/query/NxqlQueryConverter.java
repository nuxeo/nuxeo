/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
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

package org.nuxeo.elasticsearch.query;

import static org.nuxeo.elasticsearch.ElasticSearchConstants.FULLTEXT_FIELD;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.SimpleQueryStringBuilder;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;
import org.nuxeo.ecm.core.query.sql.model.DefaultQueryVisitor;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.FromClause;
import org.nuxeo.ecm.core.query.sql.model.FromList;
import org.nuxeo.ecm.core.query.sql.model.Literal;
import org.nuxeo.ecm.core.query.sql.model.LiteralList;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operand;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.query.sql.model.SelectClause;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.storage.sql.jdbc.NXQLQueryMaker;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper class that holds the conversion logic.
 *
 * Conversion is based on the existing NXQL Parser, we are just using a visitor
 * to build the ES request.
 *
 */
final public class NxqlQueryConverter {
    private static final Log log = LogFactory.getLog(NxqlQueryConverter.class);
    private static final String SELECT_ALL = "SELECT * FROM Document";
    private static final String SELECT_ALL_WHERE = "SELECT * FROM Document WHERE ";

    private NxqlQueryConverter() {
    }

    public static QueryBuilder toESQueryBuilder(final String nxql) {
        final LinkedList<ExpressionBuilder> builders = new LinkedList<ExpressionBuilder>();
        SQLQuery nxqlQuery = getSqlQuery(nxql);
        final ExpressionBuilder ret = new ExpressionBuilder(null);
        builders.add(ret);
        final ArrayList<String> fromList = new ArrayList<String>();
        nxqlQuery.accept(new DefaultQueryVisitor() {

            private static final long serialVersionUID = 1L;

            @Override
            public void visitQuery(SQLQuery node) {
                super.visitQuery(node);
                // intentionally does not set limit or offset in the query
            }

            @Override
            public void visitFromClause(FromClause node) {
                FromList elements = node.elements;
                SchemaManager schemaManager = Framework
                        .getLocalService(SchemaManager.class);

                for (int i = 0; i < elements.size(); i++) {
                    String type = elements.get(i);
                    if (NXQLQueryMaker.TYPE_DOCUMENT.equalsIgnoreCase(type)) {
                        // From Document means all doc types
                        fromList.clear();
                        return;
                    }
                    Set<String> types = schemaManager
                            .getDocumentTypeNamesExtending(type);
                    if (types != null) {
                        fromList.addAll(types);
                    }
                }
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
                Operator op = node.operator;
                if (op == Operator.AND || op == Operator.OR
                        || op == Operator.NOT) {
                    builders.add(new ExpressionBuilder(op.toString()));
                    super.visitExpression(node);
                    ExpressionBuilder expr = builders.removeLast();
                    if (!builders.isEmpty()) {
                        builders.getLast().merge(expr);
                    }
                } else {
                    Reference ref = node.lvalue instanceof Reference ? (Reference) node.lvalue
                            : null;
                    String name = ref != null ? ref.name : node.lvalue
                            .toString();
                    String value = null;
                    try {
                        value = ((Literal) node.rvalue).asString();
                    } catch (Throwable e) {
                        if (node.rvalue != null) {
                            value = node.rvalue.toString();
                        }
                    }
                    Object[] values = null;
                    if (node.rvalue instanceof LiteralList) {
                        LiteralList items = (LiteralList) node.rvalue;
                        values = new Object[items.size()];
                        int i = 0;
                        for (Literal item : items) {
                            values[i++] = item.asString();
                        }
                    }
                    // add expression to the last builder
                    builders.getLast().add(
                            makeQueryFromSimpleExpression(op.toString(), name,
                                    value, values));
                }
            }
        });
        QueryBuilder queryBuilder = ret.get();
        if (!fromList.isEmpty()) {
            return QueryBuilders.filteredQuery(
                    queryBuilder,
                    makeQueryFromSimpleExpression("IN", NXQL.ECM_PRIMARYTYPE,
                            null, fromList.toArray()).filter);
        }
        return queryBuilder;
    }

    protected static SQLQuery getSqlQuery(String nxql) {
        String query = completeQueryWithSelect(nxql);
        SQLQuery nxqlQuery;
        try {
            nxqlQuery = SQLQueryParser.parse(new StringReader(query));
        } catch (QueryParseException e) {
            if (log.isDebugEnabled()) {
                log.debug(e.getMessage() + " for query:\n" + query);
            }
            throw e;
        }
        return nxqlQuery;
    }

    protected static String completeQueryWithSelect(String nxql) {
        String query = (nxql == null) ? "" : nxql.trim();
        if (query.isEmpty()) {
            query = SELECT_ALL;
        } else if (!query.toLowerCase().startsWith("select ")) {
            query = SELECT_ALL_WHERE + nxql;
        }
        return query;
    }

    public static QueryAndFilter makeQueryFromSimpleExpression(String op,
            String name, Object value, Object[] values) {
        QueryBuilder query = null;
        FilterBuilder filter = null;
        if (NXQL.ECM_ISVERSION_OLD.equals(name)) {
            name = NXQL.ECM_ISVERSION;
        }
        name = name.replace("/", ".");
        if (name.startsWith(NXQL.ECM_FULLTEXT)
                && ("=".equals(op) || "!=".equals(op) || "<>".equals(op)
                        || "LIKE".equals(op) || "NOT LIKE".equals(op))) {
            String field = name.replace(NXQL.ECM_FULLTEXT, "");
            if (field.startsWith(".")) {
                field = field.substring(1) + ".fulltext";
            } else {
                // map ecm:fulltext_someindex to default
                field = FULLTEXT_FIELD;
            }
            query = QueryBuilders.simpleQueryString((String) value)
                    .field(field)
                    .defaultOperator(SimpleQueryStringBuilder.Operator.OR)
                    .analyzer("fulltext");
            if ("!=".equals(op) || "<>".equals(op) || "NOT LIKE".equals(op)) {
                filter = FilterBuilders.notFilter(FilterBuilders
                        .queryFilter(query));
                query = null;
            }
        } else if ("=".equals(op)) {
            filter = FilterBuilders.termFilter(name, value);
        } else if ("<>".equals(op) || "!=".equals(op)) {
            filter = FilterBuilders.notFilter(FilterBuilders.termFilter(name,
                    value));
        } else if ("LIKE".equals(op) || "ILIKE".equals(op)
                || "NOT LIKE".equals(op) || "NOT ILIKE".equals(op)) {
            // ILIKE will work only with a correct mapping
            String likeValue = ((String) value).replace("%", "*");
            if (StringUtils.countMatches(likeValue, "*") == 1
                    && likeValue.endsWith("*")) {
                query = QueryBuilders.matchPhrasePrefixQuery(name,
                        likeValue.replace("*", ""));
            } else {
                query = QueryBuilders.regexpQuery(name,
                        likeValue.replace("*", ".*"));
            }
            if (op.startsWith("NOT")) {
                filter = FilterBuilders.notFilter(FilterBuilders
                        .queryFilter(query));
                query = null;
            }
        } else if ("BETWEEN".equals(op) || "NOT BETWEEN".equals(op)) {
            filter = FilterBuilders.rangeFilter(name).from(values[0])
                    .to(values[1]);
            if ("NOT BETWEEN".equals(op)) {
                filter = FilterBuilders.notFilter(filter);
            }
        } else if ("IN".equals(op)) {
            filter = FilterBuilders.inFilter(name, values);
        } else if ("STARTSWITH".equals(op)) {
            if (name.equals(NXQL.ECM_PATH)) {
                filter = FilterBuilders.termFilter(name + ".children", value);
            } else {
                filter = FilterBuilders.prefixFilter(name, (String) value);
            }
        } else if (">".equals(op)) {
            filter = FilterBuilders.rangeFilter(name).gt(value);
        } else if ("<".equals(op)) {
            filter = FilterBuilders.rangeFilter(name).lt(value);
        } else if (">=".equals(op)) {
            filter = FilterBuilders.rangeFilter(name).gte(value);
        } else if ("<=".equals(op)) {
            filter = FilterBuilders.rangeFilter(name).lte(value);
        } else if ("IS NULL".equals(op)) {
            filter = FilterBuilders.missingFilter(name).nullValue(true);
        } else if ("IS NOT NULL".equals(op)) {
            filter = FilterBuilders.existsFilter(name);
        }
        return new QueryAndFilter(query, filter);
    }

    public static List<SortInfo> getSortInfo(String nxql) {
        final List<SortInfo> sortInfos = new ArrayList<>();
        SQLQuery nxqlQuery = getSqlQuery(nxql);
        nxqlQuery.accept(new DefaultQueryVisitor() {

            private static final long serialVersionUID = 1L;

            @Override
            public void visitOrderByExpr(OrderByExpr node) {
                sortInfos.add(new SortInfo(node.reference.name,
                        !node.isDescending));
            }
        });
        return sortInfos;
    }

    /**
     * Class to hold both a query and a filter
     *
     */
    public static class QueryAndFilter {

        public final QueryBuilder query;
        public final FilterBuilder filter;

        public QueryAndFilter(QueryBuilder query, FilterBuilder filter) {
            this.query = query;
            this.filter = filter;
        }
    }

    public static class ExpressionBuilder {

        public final String operator;
        public QueryBuilder query;

        public ExpressionBuilder(final String op) {
            this.operator = op;
            this.query = null;
        }

        public void add(final QueryAndFilter qf) {
            if (qf != null) {
                add(qf.query, qf.filter);
            }
        }

        public void add(QueryBuilder q) {
            add(q, null);
        }

        public void add(final QueryBuilder q, final FilterBuilder f) {
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
                if ("AND".equals(operator)) {
                    boolQuery.must(inputQuery);
                } else if ("OR".equals(operator)) {
                    boolQuery.should(inputQuery);
                } else if ("NOT".equals(operator)) {
                    boolQuery.mustNot(inputQuery);
                }
            }
        }

        public void merge(ExpressionBuilder expr) {
            if ((expr.operator != null) && expr.operator.equals(operator)
                    && (query == null)) {
                query = expr.query;
            } else {
                add(new QueryAndFilter(expr.query, null));
            }
        }

        public QueryBuilder get() {
            if (query == null) {
                return QueryBuilders.matchAllQuery();
            }
            return query;
        }

        @Override
        public String toString() {
            return query.toString();
        }

    }
}
