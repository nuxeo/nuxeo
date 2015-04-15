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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.CommonTermsQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.SimpleQueryStringBuilder;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;
import org.nuxeo.ecm.core.query.sql.model.DefaultQueryVisitor;
import org.nuxeo.ecm.core.query.sql.model.EsHint;
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
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.storage.sql.jdbc.NXQLQueryMaker;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper class that holds the conversion logic. Conversion is based on the existing NXQL Parser, we are just using a
 * visitor to build the ES request.
 */
final public class NxqlQueryConverter {
    private static final Log log = LogFactory.getLog(NxqlQueryConverter.class);

    private static final String SELECT_ALL = "SELECT * FROM Document";

    private static final String SELECT_ALL_WHERE = "SELECT * FROM Document WHERE ";

    private static final String SIMPLE_QUERY_PREFIX = "es: ";

    private NxqlQueryConverter() {
    }

    public static QueryBuilder toESQueryBuilder(final String nxql) {
        final LinkedList<ExpressionBuilder> builders = new LinkedList<>();
        SQLQuery nxqlQuery = getSqlQuery(nxql);
        final ExpressionBuilder ret = new ExpressionBuilder(null);
        builders.add(ret);
        final ArrayList<String> fromList = new ArrayList<>();
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
                SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);

                for (int i = 0; i < elements.size(); i++) {
                    String type = elements.get(i);
                    if (NXQLQueryMaker.TYPE_DOCUMENT.equalsIgnoreCase(type)) {
                        // From Document means all doc types
                        fromList.clear();
                        return;
                    }
                    Set<String> types = schemaManager.getDocumentTypeNamesExtending(type);
                    if (types != null) {
                        fromList.addAll(types);
                    }
                }
            }

            @Override
            public void visitMultiExpression(MultiExpression node) {
                for (Iterator<Operand> it = node.values.iterator(); it.hasNext();) {
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
                if (op == Operator.AND || op == Operator.OR || op == Operator.NOT) {
                    builders.add(new ExpressionBuilder(op.toString()));
                    super.visitExpression(node);
                    ExpressionBuilder expr = builders.removeLast();
                    if (!builders.isEmpty()) {
                        builders.getLast().merge(expr);
                    }
                } else {
                    Reference ref = node.lvalue instanceof Reference ? (Reference) node.lvalue : null;
                    String name = ref != null ? ref.name : node.lvalue.toString();
                    String value = null;
                    if (node.rvalue instanceof Literal) {
                        value = ((Literal) node.rvalue).asString();
                    } else if (node.rvalue != null) {
                        value = node.rvalue.toString();
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
                    EsHint hint = (ref != null) ? ref.esHint : null;
                    builders.getLast().add(makeQueryFromSimpleExpression(op.toString(), name, value, values, hint));
                }
            }
        });
        QueryBuilder queryBuilder = ret.get();
        if (!fromList.isEmpty()) {
            return QueryBuilders.filteredQuery(queryBuilder,
                    makeQueryFromSimpleExpression("IN", NXQL.ECM_PRIMARYTYPE, null, fromList.toArray(), null).filter);
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

    public static QueryAndFilter makeQueryFromSimpleExpression(String op, String name, Object value, Object[] values,
                                                               EsHint hint) {
        QueryBuilder query = null;
        FilterBuilder filter = null;
        if (NXQL.ECM_ISVERSION_OLD.equals(name)) {
            name = NXQL.ECM_ISVERSION;
        }
        name = getComplexFieldName(name);
        if (hint != null && hint.operator != null) {
            query = makeHintQuery(name, value, hint);
        } else if (name.startsWith(NXQL.ECM_FULLTEXT)
                && ("=".equals(op) || "!=".equals(op) || "<>".equals(op) || "LIKE".equals(op) || "NOT LIKE".equals(op))) {
            query = makeFulltextQuery(name, (String) value, hint);
            if ("!=".equals(op) || "<>".equals(op) || "NOT LIKE".equals(op)) {
                filter = FilterBuilders.notFilter(FilterBuilders.queryFilter(query));
                query = null;
            }
        } else if ("=".equals(op)) {
            filter = FilterBuilders.termFilter(name, value);
        } else if ("<>".equals(op) || "!=".equals(op)) {
            filter = FilterBuilders.notFilter(FilterBuilders.termFilter(name, value));
        } else if ("LIKE".equals(op) || "ILIKE".equals(op) || "NOT LIKE".equals(op) || "NOT ILIKE".equals(op)) {
            query = makeLikeQuery(op, name, (String) value, hint);
            if (op.startsWith("NOT")) {
                filter = FilterBuilders.notFilter(FilterBuilders.queryFilter(query));
                query = null;
            }
        } else if ("BETWEEN".equals(op) || "NOT BETWEEN".equals(op)) {
            filter = FilterBuilders.rangeFilter(name).from(values[0]).to(values[1]);
            if ("NOT BETWEEN".equals(op)) {
                filter = FilterBuilders.notFilter(filter);
            }
        } else if ("IN".equals(op) || "NOT IN".equals(op)) {
            filter = FilterBuilders.inFilter(name, values);
            if ("NOT IN".equals(op)) {
                filter = FilterBuilders.notFilter(filter);
            }
        } else if ("STARTSWITH".equals(op)) {
            filter = makeStartsWithQuery(name, value);
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

    private static QueryBuilder makeHintQuery(String name, Object value, EsHint hint) {
        QueryBuilder ret;
        String fieldName = (hint.index != null) ? hint.index :  name;
        switch (hint.operator) {
            case "match":
                MatchQueryBuilder matchQuery = QueryBuilders.matchQuery(fieldName, value);
                if (hint.analyzer != null) {
                    matchQuery.analyzer(hint.analyzer);
                }
                ret = matchQuery;
                break;
            case "match_phrase":
                matchQuery = QueryBuilders.matchPhraseQuery(fieldName, value);
                if (hint.analyzer != null) {
                    matchQuery.analyzer(hint.analyzer);
                }
                ret = matchQuery;
                break;
            case "match_phrase_prefix":
                matchQuery = QueryBuilders.matchPhrasePrefixQuery(fieldName, value);
                if (hint.analyzer != null) {
                    matchQuery.analyzer(hint.analyzer);
                }
                ret = matchQuery;
                break;
            case "multi_match":
                // hint.index must be set
                MultiMatchQueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(value, hint.getIndex());
                if (hint.analyzer != null) {
                    multiMatchQuery.analyzer(hint.analyzer);
                }
                ret = multiMatchQuery;
                break;
            case "regex":
                ret = QueryBuilders.regexpQuery(fieldName, (String) value);
                break;
            case "fuzzy":
                ret = QueryBuilders.fuzzyQuery(fieldName, (String) value);
                break;
            case "wildcard":
                ret = QueryBuilders.wildcardQuery(fieldName, (String) value);
                break;
            case "common":
                CommonTermsQueryBuilder commonQuery = QueryBuilders.commonTerms(fieldName, value);
                if (hint.analyzer != null) {
                    commonQuery.analyzer(hint.analyzer);
                }
                ret = commonQuery;
                break;
            case "query_string":
                QueryStringQueryBuilder queryString = QueryBuilders.queryString((String) value);
                if (hint.index != null) {
                    for (String index: hint.getIndex()) {
                        queryString.field(index);
                    }
                } else {
                    queryString.defaultField(fieldName);
                }
                if (hint.analyzer != null) {
                    queryString.analyzer(hint.analyzer);
                }
                ret = queryString;
                break;
            default:
                throw new UnsupportedOperationException("Operator: '" + hint.operator + "' is unknown");
        }
        return ret;
    }

    private static FilterBuilder makeStartsWithQuery(String name, Object value) {
        FilterBuilder filter;
        if (name.equals(NXQL.ECM_PATH)) {
            filter = FilterBuilders.termFilter(name + ".children", value);
        } else {
            filter = FilterBuilders.prefixFilter(name, (String) value);
        }
        return filter;
    }

    private static QueryBuilder makeLikeQuery(String op, String name, String value, EsHint hint) {
        // ILIKE will work only with a correct mapping
        String likeValue = value.replace("%", "*");
        String fieldName = name;
        if (op.contains("ILIKE")) {
            likeValue = likeValue.toLowerCase();
            fieldName = name + ".lowercase";
        }
        if (hint != null && hint.index != null) {
            fieldName = hint.index;
        }
        // use match phrase prefix when possible
        if (StringUtils.countMatches(likeValue, "*") == 1 && likeValue.endsWith("*")) {
            MatchQueryBuilder query = QueryBuilders.matchPhrasePrefixQuery(fieldName, likeValue.replace("*", ""));
            if (hint != null && hint.analyzer != null) {
                query.analyzer(hint.analyzer);
            }
            return query;
        }
        return QueryBuilders.wildcardQuery(fieldName, likeValue);
    }

    private static QueryBuilder makeFulltextQuery(String name, String value, EsHint hint) {
        String field = name.replace(NXQL.ECM_FULLTEXT, "");
        if (field.startsWith(".")) {
            field = field.substring(1) + ".fulltext";
        } else {
            // map ecm:fulltext_someindex to default
            field = FULLTEXT_FIELD;
        }
        String queryString = value;
        SimpleQueryStringBuilder.Operator defaultOperator;
        if (queryString.startsWith(SIMPLE_QUERY_PREFIX)) {
            // elasticsearch-specific syntax
            queryString = queryString.substring(SIMPLE_QUERY_PREFIX.length());
            defaultOperator = SimpleQueryStringBuilder.Operator.OR;
        } else {
            queryString = translateFulltextQuery(queryString);
            defaultOperator = SimpleQueryStringBuilder.Operator.AND;
        }
        String analyzer = (hint != null && hint.analyzer != null) ? hint.analyzer: "fulltext";
        SimpleQueryStringBuilder query = QueryBuilders.simpleQueryString(queryString).defaultOperator(defaultOperator)
                .analyzer(analyzer);
        if (hint != null && hint.index != null) {
            for (String index: hint.getIndex()) {
                query.field(index);
            }
        } else {
            query.field(field);
        }
        return query;
    }

    protected static String getComplexFieldName(String name) {
        name = name.replace("/*", "");
        name = name.replace("/", ".");
        return name;
    }

    public static List<SortInfo> getSortInfo(String nxql) {
        final List<SortInfo> sortInfos = new ArrayList<>();
        SQLQuery nxqlQuery = getSqlQuery(nxql);
        nxqlQuery.accept(new DefaultQueryVisitor() {

            private static final long serialVersionUID = 1L;

            @Override
            public void visitOrderByExpr(OrderByExpr node) {
                String name = node.reference.name;
                if (NXQL.ECM_FULLTEXT_SCORE.equals(name)) {
                    name = "_score";
                }
                sortInfos.add(new SortInfo(name, !node.isDescending));
            }
        });
        return sortInfos;
    }

    public static Map<String, Type> getSelectClauseFields(String nxql) {
        final Map<String, Type> fieldsAndTypes = new LinkedHashMap<>();
        SQLQuery nxqlQuery = getSqlQuery(nxql);
        nxqlQuery.accept(new DefaultQueryVisitor() {

            private static final long serialVersionUID = 1L;

            @Override
            public void visitSelectClause(SelectClause selectClause) {
                SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
                for (int i = 0; i < selectClause.getSelectList().size(); i++) {
                    Operand op = selectClause.get(i);
                    if (!(op instanceof Reference)) {
                        // ignore it
                        continue;
                    }
                    String name = ((Reference) op).name;
                    Field field = schemaManager.getField(name);
                    fieldsAndTypes.put(name, field == null ? null : field.getType());
                }
            }
        });
        return fieldsAndTypes;
    }

    /**
     * Translates from Nuxeo syntax to Elasticsearch simple_query_string syntax.
     */
    public static String translateFulltextQuery(String query) {
        return query.replace(" OR ", " | ").replace(" or ", " | ");
    }

    /**
     * Class to hold both a query and a filter
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
            if ((expr.operator != null) && expr.operator.equals(operator) && (query == null)) {
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
