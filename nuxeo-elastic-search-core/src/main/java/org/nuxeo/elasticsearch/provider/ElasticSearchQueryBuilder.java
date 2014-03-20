package org.nuxeo.elasticsearch.provider;

import java.security.Principal;
import java.util.Calendar;
import java.util.Collection;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.ConstantScoreQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermsFilterBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.ecm.platform.query.api.PredicateDefinition;
import org.nuxeo.ecm.platform.query.api.PredicateFieldDefinition;
import org.nuxeo.ecm.platform.query.api.WhereClauseDefinition;

public class ElasticSearchQueryBuilder {

    public static void makeQuery(SearchRequestBuilder builder,
            final Principal principal, final String pattern,
            final Object[] params, final boolean quoteParameters,
            final boolean escape, final SortInfo... sortInfos)
            throws ClientException {
        String query = pattern;
        for (int i = 0; i < params.length; i++) {
            query = query.replaceFirst("\\?", convertParam(params[i]));
        }
        TermsFilterBuilder securityFilter = addSecurityFilter(principal);
        if (securityFilter != null) {
            builder.setQuery(QueryBuilders.filteredQuery(
                    QueryBuilders.queryString(query), securityFilter));
        } else {
            builder.setQuery(QueryBuilders.queryString(query));
        }
        addSortInfo(builder, sortInfos);
    }

    public static void makeQuery(SearchRequestBuilder builder,
            Principal principal, DocumentModel model,
            WhereClauseDefinition whereClause, Object[] params,
            SortInfo... sortInfos) throws ClientException {
        assert (model != null);
        assert (whereClause != null);

        BoolQueryBuilder query = QueryBuilders.boolQuery();
        BoolFilterBuilder filter = FilterBuilders.boolFilter();

        // Fixed part handled as query_string
        String fixedPart = whereClause.getFixedPart();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                fixedPart = fixedPart.replaceFirst("\\?",
                        convertParam(params[i]));
            }
            query.must(QueryBuilders.queryString(fixedPart));
        }
        // Process predicates
        for (PredicateDefinition predicate : whereClause.getPredicates()) {
            PredicateFieldDefinition[] fieldDef = predicate.getValues();
            Object[] val;
            try {
                val = new Object[fieldDef.length];
                for (int fidx = 0; fidx < fieldDef.length; fidx++) {
                    if (fieldDef[fidx].getXpath() != null) {
                        val[fidx] = model.getPropertyValue(fieldDef[fidx]
                                .getXpath());
                    } else {
                        val[fidx] = model.getProperty(
                                fieldDef[fidx].getSchema(),
                                fieldDef[fidx].getName());
                    }
                }
            } catch (Exception e) {
                throw new ClientRuntimeException(e);
            }
            if (!isNonNullParam(val)) {
                // skip predicate where all values are null
                continue;
            }
            String field = convertFieldName(predicate.getParameter());
            String operator = predicate.getOperator().toUpperCase();
            String firstValue = convertParam(val[0]);
            if (operator.equals("=")) {
                query.must(QueryBuilders.matchQuery(field, firstValue));
            } else if (operator.equals("!=") || operator.equals("<>")) {
                filter.mustNot(FilterBuilders.queryFilter(QueryBuilders
                        .matchQuery(field, firstValue)));
            } else if (operator.equals("<")) {
                filter.must(FilterBuilders.rangeFilter(field).lt(firstValue));
            } else if (operator.equals(">")) {
                filter.must(FilterBuilders.rangeFilter(field).gt(firstValue));
            } else if (operator.equals("<=")) {
                filter.must(FilterBuilders.rangeFilter(field).lte(firstValue));
            } else if (operator.equals(">=")) {
                filter.must(FilterBuilders.rangeFilter(field).gte(firstValue));
            } else if (operator.equals("LIKE")) {
                // TODO convert like pattern
                query.must(QueryBuilders.regexpQuery(field, firstValue));
            } else if (operator.equals("ILIKE")) {
                // TODO convert ilike pattern
                query.must(QueryBuilders.regexpQuery(field, firstValue));
            } else if (operator.equals("IN")) {
                if (val[0] instanceof Collection<?>) {
                    // TODO check if all iterable are collection
                    Collection<?> vals = (Collection<?>) val[0];
                    int len = ((Collection<?>) val[0]).size();
                    String[] valArray = new String[len];
                    int i = 0;
                    for (Object v : vals) {
                        valArray[i] = convertParam(v);
                        i++;
                    }
                    filter.must(FilterBuilders.inFilter(field, valArray));
                } else if (val[0] instanceof Object[]) {
                    Object[] vals = (Object[]) val[0];
                    String[] valArray = new String[vals.length];
                    for (int i = 0; i < valArray.length; i++) {
                        valArray[i] = convertParam(vals[i]);
                    }
                    filter.must(FilterBuilders.inFilter(field, valArray));
                }
            } else if (operator.equals("BETWEEN")) {
                Object startValue = convertParam(val[0]);
                Object endValue = null;
                if (val.length > 1) {
                    endValue = convertParam(val[1]);
                }
                RangeFilterBuilder range = FilterBuilders.rangeFilter(field);
                if (startValue != null) {
                    range.from(startValue);
                }
                if (endValue != null) {
                    range.to(endValue);
                }
                filter.must(range);
            } else if (operator.equals("LIKE")) {
                Object startValue = convertParam(val[0]);
                Object endValue = null;
                if (val.length > 1) {
                    endValue = convertParam(val[1]);
                }
                RangeQueryBuilder pred = QueryBuilders.rangeQuery(field);
                if (startValue != null) {
                    pred.from(startValue);
                }
                if (endValue != null) {
                    pred.to(endValue);
                }
                query.must(pred);
            } else if (operator.equals("IS NOT NULL")) {
                filter.must(FilterBuilders.existsFilter(field));
            } else if (operator.equals("IS NULL")) {
                filter.mustNot(FilterBuilders.existsFilter(field));
            } else {
                throw new ClientException(operator + " not impletmented");
                // TODO: handle STARTSWITH FULLTEXT
            }
        }
        TermsFilterBuilder securityFilter = addSecurityFilter(principal);
        if (securityFilter != null) {
            filter.must(securityFilter);
        }
        builder.setQuery(QueryBuilders.filteredQuery(query, filter));
        addSortInfo(builder, sortInfos);
    }

    protected static TermsFilterBuilder addSecurityFilter(Principal principal) {
        if (principal != null) {
            SecurityService securityService = NXCore.getSecurityService();
            String[] principals = SecurityService
                    .getPrincipalsToCheck(principal);
            if (principals.length > 0) {
                return FilterBuilders.inFilter("ecm:acl", principals);
            }
        }
        return null;
    }

    protected static void addSortInfo(SearchRequestBuilder builder,
            SortInfo[] sortInfos) {
        for (SortInfo sortInfo : sortInfos) {
            builder.addSort(sortInfo.getSortColumn(), sortInfo
                    .getSortAscending() ? SortOrder.ASC : SortOrder.DESC);
        }

    }

    protected static String convertFieldName(String parameter) {
        return parameter.replace(":", "\\:");
    }

    protected static String convertParam(Object param) {
        String ret = "";
        if (param != null) {
            if (param instanceof Calendar) {
                ret = DateParser
                        .formatW3CDateTime(((Calendar) param).getTime());
            } else {
                ret = param.toString();
            }
            ret = "\"" + param + "\"";
        }
        return ret;
    }

    protected static boolean isNonNullParam(Object[] val) {
        if (val == null) {
            return false;
        }
        for (Object v : val) {
            if (v != null) {
                if (v instanceof String) {
                    if (!((String) v).isEmpty()) {
                        return true;
                    }
                } else if (v instanceof String[]) {
                    if (((String[]) v).length > 0) {
                        return true;
                    }
                } else {
                    return true;
                }
            }
        }
        return false;
    }

}
