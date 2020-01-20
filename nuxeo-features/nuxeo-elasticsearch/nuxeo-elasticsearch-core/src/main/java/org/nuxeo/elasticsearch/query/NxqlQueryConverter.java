/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Tiry
 *     bdelbosc
 */
package org.nuxeo.elasticsearch.query;

import static org.nuxeo.elasticsearch.ElasticSearchConstants.ES_SCORE_FIELD;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.FULLTEXT_FIELD;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchPhrasePrefixQueryBuilder;
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.SimpleQueryStringBuilder;
import org.joda.time.DateTime;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.trash.TrashService;
import org.nuxeo.ecm.core.api.trash.TrashService.Feature;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;
import org.nuxeo.ecm.core.query.sql.model.DefaultQueryVisitor;
import org.nuxeo.ecm.core.query.sql.model.EsHint;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.FromClause;
import org.nuxeo.ecm.core.query.sql.model.FromList;
import org.nuxeo.ecm.core.query.sql.model.Function;
import org.nuxeo.ecm.core.query.sql.model.Literal;
import org.nuxeo.ecm.core.query.sql.model.LiteralList;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operand;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.query.sql.model.SelectClause;
import org.nuxeo.ecm.core.query.sql.model.StringLiteral;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.ecm.core.storage.sql.jdbc.NXQLQueryMaker;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.hint.MoreLikeThisESHintQueryBuilder;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper class that holds the conversion logic. Conversion is based on the existing NXQL Parser, we are just using a
 * visitor to build the ES request.
 */
public final class NxqlQueryConverter {
    private static final Log log = LogFactory.getLog(NxqlQueryConverter.class);

    private static final String SELECT_ALL = "SELECT * FROM Document";

    private static final String SELECT_ALL_WHERE = "SELECT * FROM Document WHERE ";

    private static final String SIMPLE_QUERY_PREFIX = "es: ";

    /**
     * @deprecated since 11.1. Use {@link MoreLikeThisESHintQueryBuilder#MORE_LIKE_THIS_MIN_TERM_FREQ} instead.
     */
    @Deprecated
    protected static final int MORE_LIKE_THIS_MIN_TERM_FREQ = MoreLikeThisESHintQueryBuilder.MORE_LIKE_THIS_MIN_TERM_FREQ;

    /**
     * @deprecated since 11.1. Use {@link MoreLikeThisESHintQueryBuilder#MORE_LIKE_THIS_MIN_DOC_FREQ} instead.
     */
    @Deprecated
    protected static final int MORE_LIKE_THIS_MIN_DOC_FREQ = MoreLikeThisESHintQueryBuilder.MORE_LIKE_THIS_MIN_DOC_FREQ;

    /**
     * @deprecated since 11.1. Use {@link MoreLikeThisESHintQueryBuilder#MORE_LIKE_THIS_MAX_QUERY_TERMS} instead.
     */
    @Deprecated
    protected static final int MORE_LIKE_THIS_MAX_QUERY_TERMS = MoreLikeThisESHintQueryBuilder.MORE_LIKE_THIS_MAX_QUERY_TERMS;

    private NxqlQueryConverter() {
    }

    public static QueryBuilder toESQueryBuilder(final String nxql) {
        return toESQueryBuilder(nxql, null);
    }

    public static QueryBuilder toESQueryBuilder(final String nxql, final CoreSession session) {
        final LinkedList<ExpressionBuilder> builders = new LinkedList<>();
        SQLQuery nxqlQuery = getSqlQuery(nxql);
        if (session != null) {
            nxqlQuery = addSecurityPolicy(session, nxqlQuery);
        }
        final ExpressionBuilder ret = new ExpressionBuilder(null);
        builders.add(ret);
        final ArrayList<String> fromList = new ArrayList<>();
        nxqlQuery.accept(new DefaultQueryVisitor() {

            @Override
            public void visitFromClause(FromClause node) {
                FromList elements = node.elements;
                SchemaManager schemaManager = Framework.getService(SchemaManager.class);

                for (String type : elements.values()) {
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
                for (Iterator<Predicate> it = node.predicates.iterator(); it.hasNext();) {
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
                    } else if (node.rvalue instanceof Function) {
                        Function function = (Function) node.rvalue;
                        String func = function.name;
                        if (NXQL.NOW_FUNCTION.equalsIgnoreCase(func)) {
                            String periodAndDurationText;
                            if (function.args == null || function.args.size() != 1) {
                                periodAndDurationText = null;
                            } else {
                                periodAndDurationText = ((StringLiteral) function.args.get(0)).value;
                            }
                            DateTime dateTime = NXQL.nowPlusPeriodAndDuration(periodAndDurationText);
                            Calendar calendar = dateTime.toGregorianCalendar();
                            value = DateParser.formatW3CDateTime(calendar);
                        } else {
                            throw new IllegalArgumentException("Unknown function: " + func);
                        }
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
                    builders.getLast()
                            .add(makeQueryFromSimpleExpression(op.toString(), name, value, values, hint, session));
                }
            }
        });
        QueryBuilder queryBuilder = ret.get();
        if (!fromList.isEmpty()) {
            return QueryBuilders.boolQuery()
                                .must(queryBuilder)
                                .filter(makeQueryFromSimpleExpression("IN", NXQL.ECM_PRIMARYTYPE, null,
                                        fromList.toArray(), null, null).filter);
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

    protected static SQLQuery addSecurityPolicy(CoreSession session, SQLQuery query) {
        Collection<SQLQuery.Transformer> transformers = NXCore.getSecurityService()
                                                              .getPoliciesQueryTransformers(
                                                                      session.getRepositoryName());
        for (SQLQuery.Transformer trans : transformers) {
            query = trans.transform(session.getPrincipal(), query);
        }
        return query;
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

    public static QueryAndFilter makeQueryFromSimpleExpression(String op, String nxqlName, Object value,
            Object[] values, EsHint hint, CoreSession session) {
        QueryBuilder query = null;
        QueryBuilder filter = null;
        String name = getFieldName(nxqlName, hint);
        if (hint != null && hint.operator != null) {
            if (ArrayUtils.isNotEmpty(values)) {
                filter = makeHintQuery(name, values, hint);
            } else {
                query = makeHintQuery(name, value, hint);
            }
        } else if (nxqlName.startsWith(NXQL.ECM_FULLTEXT) && ("=".equals(op) || "!=".equals(op) || "<>".equals(op)
                || "LIKE".equals(op) || "NOT LIKE".equals(op))) {
            query = makeFulltextQuery(nxqlName, (String) value, hint);
            if ("!=".equals(op) || "<>".equals(op) || "NOT LIKE".equals(op)) {
                filter = QueryBuilders.boolQuery().mustNot(query);
                query = null;
            }
        } else if (nxqlName.startsWith(NXQL.ECM_ANCESTORID)) {
            filter = makeAncestorIdFilter((String) value, session);
            if ("!=".equals(op) || "<>".equals(op)) {
                filter = QueryBuilders.boolQuery().mustNot(filter);
            }
        } else if (nxqlName.equals(NXQL.ECM_ISTRASHED)) {
            filter = makeTrashedFilter(op, name, (String) value);

        } else
            switch (op) {
            case "=":
                filter = QueryBuilders.termQuery(name, checkBoolValue(nxqlName, value));
                break;
            case "<>":
            case "!=":
                filter = QueryBuilders.boolQuery()
                                      .mustNot(QueryBuilders.termQuery(name, checkBoolValue(nxqlName, value)));
                break;
            case ">":
                filter = QueryBuilders.rangeQuery(name).gt(value);
                break;
            case "<":
                filter = QueryBuilders.rangeQuery(name).lt(value);
                break;
            case ">=":
                filter = QueryBuilders.rangeQuery(name).gte(value);
                break;
            case "<=":
                filter = QueryBuilders.rangeQuery(name).lte(value);
                break;
            case "BETWEEN":
            case "NOT BETWEEN":
                filter = QueryBuilders.rangeQuery(name).from(values[0]).to(values[1]);
                if (op.startsWith("NOT")) {
                    filter = QueryBuilders.boolQuery().mustNot(filter);
                }
                break;
            case "IN":
            case "NOT IN":
                filter = QueryBuilders.termsQuery(name, values);
                if (op.startsWith("NOT")) {
                    filter = QueryBuilders.boolQuery().mustNot(filter);
                }
                break;
            case "IS NULL":
                filter = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(name));
                break;
            case "IS NOT NULL":
                filter = QueryBuilders.existsQuery(name);
                break;
            case "LIKE":
            case "ILIKE":
            case "NOT LIKE":
            case "NOT ILIKE":
                query = makeLikeQuery(op, name, (String) value, hint);
                if (op.startsWith("NOT")) {
                    filter = QueryBuilders.boolQuery().mustNot(query);
                    query = null;
                }
                break;
            case "STARTSWITH":
                filter = makeStartsWithQuery(name, value);
                break;
            default:
                throw new UnsupportedOperationException("Operator: '" + op + "' is unknown");
            }
        return new QueryAndFilter(query, filter);
    }

    protected static Object checkBoolValue(String nxqlName, Object value) {
        if (!"0".equals(value) && !"1".equals(value)) {
            return value;
        }
        switch (nxqlName) {
        case NXQL.ECM_ISPROXY:
        case NXQL.ECM_ISCHECKEDIN:
        case NXQL.ECM_ISTRASHED:
        case NXQL.ECM_ISVERSION:
        case NXQL.ECM_ISVERSION_OLD:
        case NXQL.ECM_ISRECORD:
        case NXQL.ECM_HASLEGALHOLD:
        case NXQL.ECM_ISLATESTMAJORVERSION:
        case NXQL.ECM_ISLATESTVERSION:
            break;
        default:
            SchemaManager schemaManager = Framework.getService(SchemaManager.class);
            Field field = schemaManager.getField(nxqlName);
            if (field == null || !BooleanType.ID.equals(field.getType().getName())) {
                return value;
            }
        }
        return "0".equals(value) ? "false" : "true";
    }

    protected static QueryBuilder makeTrashedFilter(String op, String name, String value) {
        boolean equalsDeleted;
        switch (op) {
        case "=":
            equalsDeleted = true;
            break;
        case "<>":
        case "!=":
            equalsDeleted = false;
            break;
        default:
            throw new IllegalArgumentException(NXQL.ECM_ISTRASHED + " requires = or <> operator");
        }
        if ("0".equals(value)) {
            equalsDeleted = !equalsDeleted;
        } else if ("1".equals(value)) {
            // equalsDeleted unchanged
        } else {
            throw new IllegalArgumentException(NXQL.ECM_ISTRASHED + " requires literal 0 or 1 as right argument");
        }
        TrashService trashService = Framework.getService(TrashService.class);
        QueryBuilder filter = null;
        if (trashService.hasFeature(Feature.TRASHED_STATE_IS_DEDUCED_FROM_LIFECYCLE)) {
            filter = QueryBuilders.termQuery(NXQL.ECM_LIFECYCLESTATE, LifeCycleConstants.DELETED_STATE);
        } else if (trashService.hasFeature(Feature.TRASHED_STATE_IN_MIGRATION)) {
            filter = QueryBuilders.boolQuery()
                                  .should(QueryBuilders.termQuery(NXQL.ECM_LIFECYCLESTATE,
                                          LifeCycleConstants.DELETED_STATE))
                                  .should(QueryBuilders.termQuery(name, true));
        } else if (trashService.hasFeature(Feature.TRASHED_STATE_IS_DEDICATED_PROPERTY)) {
            filter = QueryBuilders.termQuery(name, true);
        }
        if (!equalsDeleted) {
            filter = QueryBuilders.boolQuery().mustNot(filter);
        }
        return filter;
    }

    protected static QueryBuilder makeHintQuery(String name, Object value, EsHint hint) {
        return Framework.getService(ElasticSearchAdmin.class)
                        .getHintByOperator(hint.operator)
                        .orElseThrow(() -> new UnsupportedOperationException(
                                String.format("Operator: %s is unknown", hint.operator)))
                        .make(hint, name, value);
    }

    /**
     * @deprecated since 11.1. Use {@link MoreLikeThisESHintQueryBuilder#getItems(Object)} instead.
     */
    @Deprecated
    protected static MoreLikeThisQueryBuilder.Item[] getItems(Object value) {
        return MoreLikeThisESHintQueryBuilder.getItems(value);
    }

    public static QueryBuilder makeStartsWithQuery(String name, Object value) {
        QueryBuilder filter;
        String indexName = name + ".children";
        if ("/".equals(value)) {
            if (NXQL.ECM_PATH.equals(name)) {
                // any non orphan|place-less document must have a path starting with "/"
                filter = QueryBuilders.existsQuery(NXQL.ECM_PARENTID);
            } else {
                // match any document with a populated field
                filter = QueryBuilders.existsQuery(indexName);
            }
        } else {
            String v = String.valueOf(value);
            if (v.endsWith("/")) {
                v = v.replaceAll("/$", "");
            }
            if (NXQL.ECM_PATH.equals(name)) {
                // we don't want to return the parent when searching on ecm:path, see NXP-18955
                filter = QueryBuilders.boolQuery()
                                      .must(QueryBuilders.termQuery(indexName, v))
                                      .mustNot(QueryBuilders.termQuery(name, value));
            } else {
                filter = QueryBuilders.termQuery(indexName, v);
            }
        }
        return filter;
    }

    private static QueryBuilder makeAncestorIdFilter(String value, CoreSession session) {
        String path;
        if (session == null) {
            return QueryBuilders.existsQuery("ancestorid-without-session");
        } else {
            try {
                DocumentModel doc = session.getDocument(new IdRef(value));
                path = doc.getPathAsString();
            } catch (DocumentNotFoundException e) {
                return QueryBuilders.existsQuery("ancestorid-not-found");
            }
        }
        return makeStartsWithQuery(NXQL.ECM_PATH, path);
    }

    private static QueryBuilder makeLikeQuery(String op, String name, String value, EsHint hint) {
        String fieldName = name;
        if (op.contains("ILIKE")) {
            // ILIKE will work only with a correct mapping
            value = value.toLowerCase();
            fieldName = name + ".lowercase";
        }
        if (hint != null && hint.index != null) {
            fieldName = hint.index;
        }
        // convert the value to a wildcard query
        String wildcard = likeToWildcard(value);
        // use match phrase prefix when possible
        if (StringUtils.countMatches(wildcard, "*") == 1 && wildcard.endsWith("*") && !wildcard.contains("?")
                && !wildcard.contains("\\")) {
            MatchPhrasePrefixQueryBuilder query = QueryBuilders.matchPhrasePrefixQuery(fieldName,
                    wildcard.replace("*", ""));
            if (hint != null && hint.analyzer != null) {
                query.analyzer(hint.analyzer);
            }
            return query;
        }
        return QueryBuilders.wildcardQuery(fieldName, wildcard);
    }

    /**
     * Turns a NXQL LIKE pattern into a wildcard for WildcardQuery.
     * <p>
     * % and _ are standard wildcards, and \ escapes them.
     *
     * @since 7.4
     */
    protected static String likeToWildcard(String like) {
        StringBuilder wildcard = new StringBuilder();
        char[] chars = like.toCharArray();
        boolean escape = false;
        for (char c : chars) {
            boolean escapeNext = false;
            switch (c) {
            case '?':
                wildcard.append("\\?");
                break;
            case '*': // compat, * = % in NXQL (for some backends)
            case '%':
                if (escape) {
                    wildcard.append(c);
                } else {
                    wildcard.append("*");
                }
                break;
            case '_':
                if (escape) {
                    wildcard.append(c);
                } else {
                    wildcard.append("?");
                }
                break;
            case '\\':
                if (escape) {
                    wildcard.append("\\\\");
                } else {
                    escapeNext = true;
                }
                break;
            default:
                wildcard.append(c);
                break;
            }
            escape = escapeNext;
        }
        if (escape) {
            // invalid string terminated by escape character, ignore
        }
        return wildcard.toString();
    }

    private static QueryBuilder makeFulltextQuery(String nxqlName, String value, EsHint hint) {
        String name = nxqlName.replace(NXQL.ECM_FULLTEXT, "");
        if (name.startsWith(".")) {
            name = name.substring(1) + ".fulltext";
        } else {
            // map ecm:fulltext_someindex to default
            name = FULLTEXT_FIELD;
        }
        String queryString = value;
        org.elasticsearch.index.query.Operator defaultOperator;
        if (queryString.startsWith(SIMPLE_QUERY_PREFIX)) {
            // elasticsearch-specific syntax
            queryString = queryString.substring(SIMPLE_QUERY_PREFIX.length());
            defaultOperator = org.elasticsearch.index.query.Operator.OR;
        } else {
            queryString = translateFulltextQuery(queryString);
            defaultOperator = org.elasticsearch.index.query.Operator.AND;
        }
        String analyzer = (hint != null && hint.analyzer != null) ? hint.analyzer : "fulltext";
        SimpleQueryStringBuilder query = QueryBuilders.simpleQueryStringQuery(queryString)
                                                      .defaultOperator(defaultOperator)
                                                      .analyzer(analyzer);
        if (hint != null && hint.index != null) {
            for (EsHint.FieldHint fieldHint : hint.getIndex()) {
                query.field(fieldHint.getField(), fieldHint.getBoost());
            }
        } else {
            query.field(name);
        }
        return query;
    }

    private static String getFieldName(String name, EsHint hint) {
        if (hint != null && hint.index != null) {
            return hint.index;
        }
        // compat
        if (NXQL.ECM_ISVERSION_OLD.equals(name)) {
            name = NXQL.ECM_ISVERSION;
        }
        // complex field
        name = name.replace("/*", "");
        name = name.replace("/", ".");
        return name;
    }

    public static List<SortInfo> getSortInfo(String nxql) {
        final List<SortInfo> sortInfos = new ArrayList<>();
        SQLQuery nxqlQuery = getSqlQuery(nxql);
        nxqlQuery.accept(new DefaultQueryVisitor() {

            @Override
            public void visitOrderByExpr(OrderByExpr node) {
                String name = getFieldName(node.reference.name, null);
                if (NXQL.ECM_FULLTEXT_SCORE.equals(name)) {
                    name = ES_SCORE_FIELD;
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

            @Override
            public void visitSelectClause(SelectClause selectClause) {
                SchemaManager schemaManager = Framework.getService(SchemaManager.class);
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
        // The AND operator does not exist in NXQL it is the default operator
        return query.replace(" OR ", " | ").replace(" or ", " | ");
    }

    /**
     * Class to hold both a query and a filter
     */
    public static class QueryAndFilter {

        public final QueryBuilder query;

        public final QueryBuilder filter;

        public QueryAndFilter(QueryBuilder query, QueryBuilder filter) {
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

        public void add(final QueryBuilder q, final QueryBuilder f) {
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
