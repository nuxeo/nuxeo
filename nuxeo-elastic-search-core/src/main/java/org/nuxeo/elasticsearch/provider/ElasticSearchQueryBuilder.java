package org.nuxeo.elasticsearch.provider;

import java.security.Principal;
import java.util.Calendar;
import java.util.Collection;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.elasticsearch.index.query.TermsFilterBuilder;
import org.elasticsearch.search.sort.SortOrder;
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

    /**
     * Create a ES request from a PP pattern
     */
    public static void makeQuery(final SearchRequestBuilder builder,
            final Principal principal, final String pattern,
            final Object[] params, final boolean quoteParameters,
            final boolean escape, final SortInfo... sortInfos)
            throws ClientException {
        String query = pattern;
        for (int i = 0; i < params.length; i++) {
            query = query.replaceFirst("\\?", convertParam(params[i]));
        }
        TermsFilterBuilder securityFilter = getSecurityFilter(principal);
        if (securityFilter != null) {
            builder.setQuery(QueryBuilders.filteredQuery(
                    QueryBuilders.queryString(query), securityFilter));
        } else {
            builder.setQuery(QueryBuilders.queryString(query));
        }
        addSortInfo(builder, sortInfos);
    }

    /**
     * Create a ES request from a PP whereClause
     */
    public static void makeQuery(final SearchRequestBuilder builder,
            final Principal principal, final DocumentModel model,
            final WhereClauseDefinition whereClause, final Object[] params,
            final SortInfo... sortInfos) throws ClientException {
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
            String field = predicate.getParameter();
            String operator = predicate.getOperator().toUpperCase();
            Object firstValue = val[0];
            if (operator.equals("=")) {
                filter.must(FilterBuilders.termFilter(field, firstValue));
            } else if (operator.equals("!=") || operator.equals("<>")) {
                filter.mustNot(FilterBuilders.termFilter(field, firstValue));
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
                query.must(QueryBuilders
                        .regexpQuery(field, (String) firstValue));
            } else if (operator.equals("ILIKE")) {
                // TODO convert ilike pattern
                query.must(QueryBuilders
                        .regexpQuery(field, (String) firstValue));
            } else if (operator.equals("IN")) {
                if (val[0] instanceof Collection<?>) {
                    // TODO check if all iterable are collection
                    Collection<?> vals = (Collection<?>) val[0];
                    Object[] valArray = vals.toArray(new Object[vals.size()]);
                    filter.must(FilterBuilders.inFilter(field, valArray));
                } else if (val[0] instanceof Object[]) {
                    Object[] vals = (Object[]) val[0];
                    filter.must(FilterBuilders.inFilter(field, vals));
                }
            } else if (operator.equals("BETWEEN")) {
                Object startValue = firstValue;
                Object endValue = null;
                if (val.length > 1) {
                    endValue = val[1];
                }
                RangeFilterBuilder range = FilterBuilders.rangeFilter(field);
                if (startValue != null) {
                    range.from(startValue);
                }
                if (endValue != null) {
                    range.to(endValue);
                }
                filter.must(range);
            } else if (operator.equals("IS NOT NULL")) {
                filter.must(FilterBuilders.existsFilter(field));
            } else if (operator.equals("IS NULL")) {
                filter.mustNot(FilterBuilders.existsFilter(field));
            } else if (operator.equals("FULLTEXT")) {
                // convention on the name of the fulltext analyzer to use
                query.must(QueryBuilders.simpleQueryString((String) firstValue)
                        .field("_all").analyzer("fulltext"));
            } else if (operator.equals("STARTSWITH")) {
                if (field.equals("ecm:path")) {
                    query.must(QueryBuilders.matchQuery(field + ".children",
                            firstValue));
                } else {
                    query.must(QueryBuilders.prefixQuery(field,
                            (String) firstValue));
                }
            } else {
                throw new ClientException("Not implemented operator: "
                        + operator);
            }
        }
        TermsFilterBuilder securityFilter = getSecurityFilter(principal);
        if (securityFilter != null) {
            filter.must(securityFilter);
        }
        builder.setQuery(QueryBuilders.filteredQuery(query, filter));
        addSortInfo(builder, sortInfos);
    }

    protected static TermsFilterBuilder getSecurityFilter(
            final Principal principal) {
        if (principal != null) {
            String[] principals = SecurityService
                    .getPrincipalsToCheck(principal);
            if (principals.length > 0) {
                return FilterBuilders.inFilter("ecm:acl", principals);
            }
        }
        return null;
    }

    /**
     * Append the sort option to the ES builder
     */
    protected static void addSortInfo(final SearchRequestBuilder builder,
            final SortInfo[] sortInfos) {
        for (SortInfo sortInfo : sortInfos) {
            builder.addSort(sortInfo.getSortColumn(), sortInfo
                    .getSortAscending() ? SortOrder.ASC : SortOrder.DESC);
        }

    }

    protected static String convertFieldName(final String parameter) {
        return parameter.replace(":", "\\:");
    }

    /**
     * Convert a param for a query_string style
     */
    protected static String convertParam(final Object param) {
        String ret;
        if (param == null) {
            ret = "";
        } else if (param instanceof Boolean) {
            ret = ((Boolean) param).toString();
        } else if (param instanceof Calendar) {
            ret = DateParser.formatW3CDateTime(((Calendar) param).getTime());
        } else if (param instanceof Double) {
            ret = ((Double) param).toString();
        } else if (param instanceof Integer) {
            ret = ((Integer) param).toString();
        } else {
            ret = "\"" + param.toString() + "\"";
        }
        return ret;
    }

    protected static boolean isNonNullParam(final Object[] val) {
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
