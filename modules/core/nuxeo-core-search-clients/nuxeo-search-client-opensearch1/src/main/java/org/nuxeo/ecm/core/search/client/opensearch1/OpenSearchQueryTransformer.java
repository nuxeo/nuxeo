/*
 * (C) Copyright 2014-2025 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.search.client.opensearch1;

import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.UNSUPPORTED_ACL;
import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_ACL;
import static org.nuxeo.ecm.core.search.SearchServiceImpl.RELATION_INDEXING_ENABLED_PROP;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_AVG;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_CARDINALITY;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_COUNT;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_MAX;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_MIN;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_MISSING;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_SUM;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_TYPE_DATE_HISTOGRAM;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_TYPE_DATE_RANGE;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_TYPE_HISTOGRAM;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_TYPE_RANGE;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_TYPE_SIGNIFICANT_TERMS;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_TYPE_TERMS;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.FuzzyQuery;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.model.DefaultQueryVisitor;
import org.nuxeo.ecm.core.query.sql.model.EsHint;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.FromClause;
import org.nuxeo.ecm.core.query.sql.model.Function;
import org.nuxeo.ecm.core.query.sql.model.Literal;
import org.nuxeo.ecm.core.query.sql.model.LiteralList;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SelectClause;
import org.nuxeo.ecm.core.query.sql.model.StringLiteral;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.ecm.core.search.SearchIndex;
import org.nuxeo.ecm.core.search.SearchQuery;
import org.nuxeo.ecm.core.search.SearchQueryTransformer;
import org.nuxeo.ecm.core.search.client.opensearch1.aggregate.AggregateDateHistogramParser;
import org.nuxeo.ecm.core.search.client.opensearch1.aggregate.AggregateDateRangeParser;
import org.nuxeo.ecm.core.search.client.opensearch1.aggregate.AggregateHistogramParser;
import org.nuxeo.ecm.core.search.client.opensearch1.aggregate.AggregateParserBase;
import org.nuxeo.ecm.core.search.client.opensearch1.aggregate.AggregateRangeParser;
import org.nuxeo.ecm.core.search.client.opensearch1.aggregate.AggregateSignificantTermParser;
import org.nuxeo.ecm.core.search.client.opensearch1.aggregate.AggregateSingleValueMetricParser;
import org.nuxeo.ecm.core.search.client.opensearch1.aggregate.AggregateTermParser;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.ecm.platform.query.core.AggregateDateRange;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchType;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.MatchPhrasePrefixQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.SimpleQueryStringBuilder;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.opensearch.search.sort.FieldSortBuilder;
import org.opensearch.search.sort.SortBuilder;
import org.opensearch.search.sort.SortOrder;

/**
 * Helper class that holds the conversion logic. Conversion is based on the existing NXQL Parser, we are just using a *
 * visitor to build the OpenSearch request.
 */
public class OpenSearchQueryTransformer implements SearchQueryTransformer<SearchRequest> {

    public static final String FULLTEXT_FIELD = "all_field";

    protected static final String SIMPLE_QUERY_PREFIX = "es: ";

    protected static final String TYPE_DOCUMENT = "Document";

    // @since 2025.19
    protected static final String TYPE_RELATION = "Relation";

    protected static final String SCORE_FIELD = "_score";

    // opensearch keep alive must be less than 1d
    protected static final long MAX_KEEP_ALIVE_SECONDS = Duration.ofDays(1).minusMinutes(10).toSeconds();

    protected final Map<String, String> indexes;

    protected final Map<String, OpenSearchHintQueryBuilder> hintBuilders;

    public OpenSearchQueryTransformer(Map<String, String> indexes,
            Map<String, OpenSearchHintQueryBuilder> hintBuilders) {
        this.indexes = indexes;
        this.hintBuilders = hintBuilders;
    }

    @Override
    public SearchRequest apply(SearchQuery searchQuery) {
        var osSearchBuilder = new SearchSourceBuilder();
        osSearchBuilder.trackTotalHits(true);
        // fields selection
        osSearchBuilder.fetchSource(searchQuery.getSelectFields().keySet().toArray(String[]::new), null);
        if (searchQuery.isScrollSearch()) {
            // scroll size
            osSearchBuilder.size(searchQuery.getScrollSize());
        } else {
            // from and size
            osSearchBuilder.from(searchQuery.getOffset()).size(searchQuery.getLimit());
        }
        // sort
        makeSortBuilders(searchQuery).forEach(osSearchBuilder::sort);
        // highlight
        osSearchBuilder.highlighter(makeHighlighter(searchQuery));
        // aggregates
        makeAggregationBuilders(searchQuery).forEach(osSearchBuilder::aggregation);
        osSearchBuilder.postFilter(makeAggregatePostFilter(searchQuery));
        // nxql query
        var osQueryBuilder = makeQueryBuilder(searchQuery);
        osSearchBuilder.query(osQueryBuilder);
        // final OpenSearch request
        var osIndexes = searchQuery.getSearchIndexes().stream().map(SearchIndex::index).map(indexes::get).toList();
        var osSearchRequest = new SearchRequest(osIndexes.toArray(String[]::new));
        osSearchRequest.searchType(SearchType.DFS_QUERY_THEN_FETCH);
        osSearchRequest.source(osSearchBuilder);
        // scroll
        if (searchQuery.isScrollSearch()) {
            osSearchRequest.scroll(getKeepAlive(searchQuery));
        }
        return osSearchRequest;
    }

    protected QueryBuilder makeQueryBuilder(SearchQuery searchQuery) {
        final LinkedList<ExpressionBuilder> builders = new LinkedList<>();
        final ExpressionBuilder ret = new ExpressionBuilder();
        builders.add(ret);
        final ArrayList<String> fromList = new ArrayList<>();
        searchQuery.getQuery().accept(new DefaultQueryVisitor() {

            @Override
            public void visitFromClause(FromClause node) {
                var elements = node.elements.values();
                if (elements.contains(TYPE_DOCUMENT)
                        && (!Framework.isBooleanPropertyTrue(RELATION_INDEXING_ENABLED_PROP)
                                || elements.contains(TYPE_RELATION))) {
                    // if Relations are indexed, "FROM Document, Relation" means all doc types
                    // otherwise, "FROM Document" also means all doc types
                    // leave fromList empty so no type filter is applied
                    return;
                }
                SchemaManager schemaManager = Framework.getService(SchemaManager.class);
                for (String type : elements) {
                    Set<String> types = schemaManager.getDocumentTypeNamesExtending(type);
                    if (types == null) {
                        throw new QueryParseException("Unknown type: " + type);
                    }
                    fromList.addAll(types);
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
                    } else if (node.rvalue instanceof Function function) {
                        String func = function.name;
                        if (NXQL.NOW_FUNCTION.equalsIgnoreCase(func)) {
                            String periodAndDurationText;
                            if (function.args == null || function.args.size() != 1) {
                                periodAndDurationText = null;
                            } else {
                                periodAndDurationText = ((StringLiteral) function.args.getFirst()).value;
                            }
                            ZonedDateTime dateTime = NXQL.nowPlusPeriodAndDuration(periodAndDurationText);
                            Calendar calendar = GregorianCalendar.from(dateTime);
                            value = DateParser.formatW3CDateTime(calendar);
                        } else {
                            throw new IllegalArgumentException("Unknown function: " + func);
                        }
                    } else if (node.rvalue != null) {
                        value = node.rvalue.toString();
                    }
                    Object[] values = null;
                    if (node.rvalue instanceof LiteralList items) {
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
        // security filter
        var principal = searchQuery.getPrincipal();
        if (principal != null && !principal.isAdministrator()) {
            QueryBuilder aclFilter = QueryBuilders.boolQuery()
                                                  .must(QueryBuilders.termsQuery(ECM_ACL,
                                                          SecurityService.getPrincipalsToCheck(principal)))
                                                  .mustNot(QueryBuilders.termsQuery(ECM_ACL, UNSUPPORTED_ACL));
            queryBuilder = QueryBuilders.boolQuery().must(queryBuilder).filter(aclFilter);
        }
        if (!fromList.isEmpty()) {
            return QueryBuilders.boolQuery()
                                .must(queryBuilder)
                                .filter(makeQueryFromSimpleExpression("IN", NXQL.ECM_PRIMARYTYPE, null,
                                        fromList.toArray(), null).filter);
        }
        return queryBuilder;
    }

    protected QueryAndFilter makeQueryFromSimpleExpression(String op, String nxqlName, Object value, Object[] values,
            EsHint hint) {
        QueryBuilder query = null;
        QueryBuilder filter = null;
        String name = getFieldName(nxqlName, hint);
        if (hint != null && hint.operator != null) {
            if (isNotEmpty(values)) {
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

    protected Object checkBoolValue(String nxqlName, Object value) {
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
            case NXQL.ECM_ISFLEXIBLERECORD:
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

    protected QueryBuilder makeHintQuery(String name, Object value, EsHint hint) {
        var hintBuilder = hintBuilders.get(hint.operator);
        if (hintBuilder == null) {
            throw new QueryParseException(String.format("Operator: %s is unknown", hint.operator));
        }
        return hintBuilder.make(hint, name, value);
    }

    protected QueryBuilder makeStartsWithQuery(String name, Object value) {
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
                                      .mustNot(QueryBuilders.termQuery(name, v));
            } else {
                filter = QueryBuilders.termQuery(indexName, v);
            }
        }
        return filter;
    }

    protected QueryBuilder makeLikeQuery(String op, String name, String value, EsHint hint) {
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
            ConfigurationService cs = Framework.getService(ConfigurationService.class);
            query.maxExpansions(cs.getInteger("elasticsearch.max_expansions", FuzzyQuery.defaultMaxExpansions));
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
    protected String likeToWildcard(String like) {
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
        return wildcard.toString();
    }

    protected QueryBuilder makeFulltextQuery(String nxqlName, String value, EsHint hint) {
        String name = nxqlName.replace(NXQL.ECM_FULLTEXT, "");
        if (name.startsWith(".")) {
            name = name.substring(1) + ".fulltext";
        } else {
            // map ecm:fulltext_someindex to default
            name = FULLTEXT_FIELD;
        }
        String queryString = value;
        org.opensearch.index.query.Operator defaultOperator;
        if (queryString.startsWith(SIMPLE_QUERY_PREFIX)) {
            // opensearch-specific syntax
            queryString = queryString.substring(SIMPLE_QUERY_PREFIX.length());
            defaultOperator = org.opensearch.index.query.Operator.OR;
        } else {
            queryString = translateFulltextQuery(queryString);
            defaultOperator = org.opensearch.index.query.Operator.AND;
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

    protected String getFieldName(String name, EsHint hint) {
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

    protected List<SortBuilder<?>> makeSortBuilders(SearchQuery searchQuery) {
        final List<SortBuilder<?>> sortBuilders = new ArrayList<>();
        searchQuery.getQuery().accept(new DefaultQueryVisitor() {

            @Override
            public void visitOrderByExpr(OrderByExpr node) {
                String name = getFieldName(node.reference.name, null);
                if (NXQL.ECM_FULLTEXT_SCORE.equals(name)) {
                    name = SCORE_FIELD;
                }
                sortBuilders.add(new FieldSortBuilder(name).order(node.isDescending ? SortOrder.DESC : SortOrder.ASC)
                                                           .unmappedType(guessFieldType(name)));
            }
        });
        return sortBuilders;
    }

    protected String guessFieldType(String field) {
        if (SCORE_FIELD.equals(field)) {
            // this special field should not have an unmappedType
            return null;
        }
        var schemaManager = Framework.getService(SchemaManager.class);
        var schemaField = schemaManager.getField(field);
        if (schemaField == null) {
            return "keyword";
        } else {
            String schemaFieldType = schemaField.getType().getName();
            return switch (schemaFieldType) {
                case "integer", "long", "boolean", "date" -> schemaFieldType;
                default -> "keyword";
            };
        }
    }

    /**
     * Translates from Nuxeo syntax to Elasticsearch simple_query_string syntax.
     */
    protected String translateFulltextQuery(String query) {
        // The AND operator does not exist in NXQL it is the default operator
        return query.replace(" OR ", " | ").replace(" or ", " | ");
    }

    protected HighlightBuilder makeHighlighter(SearchQuery searchQuery) {
        if (!searchQuery.getHighlights().isEmpty()) {
            HighlightBuilder hb = new HighlightBuilder();
            for (String field : searchQuery.getHighlights()) {
                hb.field(field);
            }
            hb.requireFieldMatch(false);
            return hb;
        }
        return null;
    }

    protected List<FilterAggregationBuilder> makeAggregationBuilders(SearchQuery searchQuery) {
        List<Aggregate<? extends Bucket>> aggregates = searchQuery.getAggregates();
        List<FilterAggregationBuilder> ret = new ArrayList<>(aggregates.size());
        for (var aggregate : aggregates) {
            FilterAggregationBuilder builder = new FilterAggregationBuilder(AggregateParserBase.getFilterId(aggregate),
                    getAggregateFilterExceptFor(searchQuery, aggregate.getId()));
            builder.subAggregation(getSubAggregation(aggregate));
            ret.add(builder);
        }
        return ret;
    }

    protected QueryBuilder getAggregateFilterExceptFor(SearchQuery searchQuery, String id) {
        BoolQueryBuilder ret = QueryBuilders.boolQuery();
        for (var aggregate : searchQuery.getAggregates()) {
            if (!aggregate.getId().equals(id)) {
                QueryBuilder filter = getAggregateFilter(aggregate);
                if (filter != null) {
                    ret.must(filter);
                }
            }
        }
        if (!ret.hasClauses()) {
            return QueryBuilders.matchAllQuery();
        }
        return ret;
    }

    protected QueryBuilder getAggregateFilter(Aggregate<? extends Bucket> aggregate) {
        if (aggregate.getSelection().isEmpty()) {
            return null;
        }
        return switch (aggregate.getType()) {
            case AGG_TYPE_TERMS, AGG_COUNT, AGG_CARDINALITY, AGG_SUM, AGG_AVG, AGG_MAX, AGG_MIN ->
                AggregateTermParser.getFilter(aggregate);
            case AGG_TYPE_RANGE -> AggregateRangeParser.getFilter(aggregate);
            case AGG_TYPE_HISTOGRAM -> AggregateHistogramParser.getFilter(aggregate);
            case AGG_TYPE_DATE_RANGE -> AggregateDateRangeParser.getFilter((AggregateDateRange) aggregate);
            case AGG_TYPE_DATE_HISTOGRAM -> AggregateDateHistogramParser.getFilter(aggregate);
            default -> throw new IllegalStateException("Unexpected value: " + aggregate.getType());
        };
    }

    protected AggregationBuilder getSubAggregation(Aggregate<? extends Bucket> aggregate) {
        return switch (aggregate.getType()) {
            case AGG_TYPE_TERMS -> AggregateTermParser.getAggregate(aggregate);
            case AGG_TYPE_RANGE -> AggregateRangeParser.getAggregate(aggregate);
            case AGG_TYPE_DATE_RANGE -> AggregateDateRangeParser.getAggregate(aggregate);
            case AGG_SUM, AGG_COUNT, AGG_CARDINALITY, AGG_AVG, AGG_MAX, AGG_MIN, AGG_MISSING ->
                AggregateSingleValueMetricParser.getAggregate(aggregate);
            case AGG_TYPE_HISTOGRAM -> AggregateHistogramParser.getAggregate(aggregate);
            case AGG_TYPE_DATE_HISTOGRAM -> AggregateDateHistogramParser.getAggregate(aggregate);
            case AGG_TYPE_SIGNIFICANT_TERMS -> AggregateSignificantTermParser.getAggregate(aggregate);
            default -> throw new IllegalStateException("Unexpected value: " + aggregate.getType());
        };
    }

    protected QueryBuilder makeAggregatePostFilter(SearchQuery searchQuery) {
        BoolQueryBuilder ret = QueryBuilders.boolQuery();
        searchQuery.getAggregates().stream().map(this::getAggregateFilter).filter(Objects::nonNull).forEach(ret::must);
        return ret.hasClauses() ? ret : null;
    }

    protected static TimeValue getKeepAlive(SearchQuery query) {
        long keepAlive = query.getScrollKeepAlive().toSeconds();
        if (keepAlive <= 0 || keepAlive > MAX_KEEP_ALIVE_SECONDS) {
            keepAlive = MAX_KEEP_ALIVE_SECONDS;
        }
        return TimeValue.timeValueSeconds(keepAlive);
    }

    /**
     * Class to hold both a query and a filter
     */
    public record QueryAndFilter(QueryBuilder query, QueryBuilder filter) {

    }

    public static class ExpressionBuilder {

        public final String operator;

        public QueryBuilder query;

        public ExpressionBuilder() {
            this(null);
        }

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
                switch (operator) {
                    case "AND" -> boolQuery.must(inputQuery);
                    case "OR" -> boolQuery.should(inputQuery);
                    case "NOT" -> boolQuery.mustNot(inputQuery);
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
