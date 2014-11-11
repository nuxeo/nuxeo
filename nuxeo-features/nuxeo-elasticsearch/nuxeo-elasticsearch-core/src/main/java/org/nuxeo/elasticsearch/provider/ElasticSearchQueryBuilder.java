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
 *     bdelbosc
 */

package org.nuxeo.elasticsearch.provider;

import static org.nuxeo.elasticsearch.ElasticSearchConstants.ACL_FIELD;

import java.security.Principal;
import java.util.Calendar;
import java.util.Collection;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsFilterBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.ecm.platform.query.api.PredicateDefinition;
import org.nuxeo.ecm.platform.query.api.PredicateFieldDefinition;
import org.nuxeo.ecm.platform.query.api.WhereClauseDefinition;
import org.nuxeo.elasticsearch.nxql.NxqlQueryConverter;

public class ElasticSearchQueryBuilder {

    /**
     * Create a ES request from a PP pattern
     *
     */
    public static QueryBuilder makeQuery(final String pattern,
            final Object[] params, final boolean quotePatternParameters,
            final boolean escapePatternParameters,
            final boolean useNativeQuery)
            throws ClientException {
        String query = pattern;
        for (int i = 0; i < params.length; i++) {
            query = query.replaceFirst("\\?",
                    convertParam(params[i], quotePatternParameters));
        }
        if (useNativeQuery) {
            return QueryBuilders.queryString(query);
        } else {
            return NxqlQueryConverter.toESQueryBuilder(query);
        }
    }

    /**
     * Create a ES request from a PP whereClause
     *
     */
    public static QueryBuilder makeQuery(final DocumentModel model,
            final WhereClauseDefinition whereClause, final Object[] params,
            final boolean useNativeQuery)
            throws ClientException {
        assert (model != null);
        assert (whereClause != null);
        NxqlQueryConverter.ExpressionBuilder eb = new NxqlQueryConverter.ExpressionBuilder(
                "AND");
        String fixedPart = whereClause.getFixedPart();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                fixedPart = fixedPart.replaceFirst("\\?",
                        convertParam(params[i], true));
            }
            if (useNativeQuery) {
                // Fixed part handled as query_string
                eb.add(QueryBuilders.queryString(fixedPart));
            } else {
                eb.add(NxqlQueryConverter.toESQueryBuilder(fixedPart));
            }
        }
        // Process predicates
        for (PredicateDefinition predicate : whereClause.getPredicates()) {
            PredicateFieldDefinition[] fieldDef = predicate.getValues();
            Object[] values;
            try {
                values = new Object[fieldDef.length];
                for (int fidx = 0; fidx < fieldDef.length; fidx++) {
                    if (fieldDef[fidx].getXpath() != null) {
                        values[fidx] = model.getPropertyValue(fieldDef[fidx]
                                .getXpath());
                    } else {
                        values[fidx] = model.getProperty(
                                fieldDef[fidx].getSchema(),
                                fieldDef[fidx].getName());
                    }
                }
            } catch (Exception e) {
                throw new ClientRuntimeException(e);
            }
            if (!isNonNullParam(values)) {
                // skip predicate where all values are null
                continue;
            }
            Object value = values[0];
            if (values[0] instanceof Collection<?>) {
                Collection<?> vals = (Collection<?>) values[0];
                values = vals.toArray(new Object[vals.size()]);
            } else if (values[0] instanceof Object[]) {
                values = (Object[]) values[0];
            }
            String name = predicate.getParameter();
            String operator = predicate.getOperator().toUpperCase();
            if ("FULLTEXT".equals(operator) || "FULLTEXT ALL".equals(operator)) {
                operator = "=";
                if (!name.startsWith(NXQL.ECM_FULLTEXT)) {
                    name = NXQL.ECM_FULLTEXT + "." + name;
                }
            }
            eb.add(NxqlQueryConverter.makeQueryFromSimpleExpression(operator,
                    name, value, values));
        }
        return eb.get();
    }

    protected static TermsFilterBuilder getSecurityFilter(
            final Principal principal) {
        if (principal != null) {
            String[] principals = SecurityService
                    .getPrincipalsToCheck(principal);
            if (principals.length > 0) {
                return FilterBuilders.inFilter(ACL_FIELD, principals);
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
    protected static String convertParam(final Object param, boolean quote) {
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
            ret = param.toString();
            if (quote) {
                ret = "\"" + ret + "\"";
            }
        }
        return ret;
    }

    @SuppressWarnings("rawtypes")
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
                } else if (v instanceof Collection) {
                    if (!((Collection) v).isEmpty()) {
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
