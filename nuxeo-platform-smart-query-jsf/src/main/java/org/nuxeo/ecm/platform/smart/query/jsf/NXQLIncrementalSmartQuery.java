/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.smart.query.jsf;

import java.text.SimpleDateFormat;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;
import org.nuxeo.ecm.core.search.api.client.querymodel.Escaper;
import org.nuxeo.ecm.core.search.api.client.querymodel.LuceneMinimalEscaper;
import org.nuxeo.ecm.platform.smart.query.IncrementalSmartQuery;

/**
 * @author Anahide Tchertchian
 */
public class NXQLIncrementalSmartQuery extends IncrementalSmartQuery {

    private static final long serialVersionUID = 1L;

    public static final String GENERIC_QUERY_SELECT = "SELECT * FROM DOCUMENT WHERE ";

    public static final Escaper escaper = new LuceneMinimalEscaper();

    final SimpleDateFormat isoDate = new SimpleDateFormat("yyyy-MM-dd");

    final SimpleDateFormat isoTimeStamp = new SimpleDateFormat(
            "yyyy-MM-dd hh:mm:ss");

    public NXQLIncrementalSmartQuery(String existingQueryPart) {
        super(existingQueryPart);
    }

    @Override
    public String buildQuery() {
        StringBuilder builder = new StringBuilder();
        if (existingQueryPart != null) {
            builder.append(existingQueryPart);
            builder.append(" ");
        }
        // perform simple check before changing query
        if (leftExpression != null && conditionalOperator != null) {
            if (logicalOperator != null) {
                builder.append(logicalOperator);
                builder.append(" ");
            }
            if (Boolean.TRUE.equals(openParenthesis)) {
                builder.append("(");
            }
            if (Boolean.TRUE.equals(addNotOperator)) {
                builder.append("NOT ");
            }
            if (leftExpression != null) {
                builder.append(leftExpression);
                builder.append(" ");
            }
            if (conditionalOperator != null) {
                builder.append(conditionalOperator);
                builder.append(" ");
            }
            if (value != null) {
                if (booleanValue != null) {
                    if (Boolean.TRUE.equals(booleanValue)) {
                        builder.append(1);
                    } else {
                        builder.append(0);
                    }
                } else if (stringValue != null) {
                    builder.append(String.format("'%s'",
                            escaper.escape(stringValue)));
                } else if (stringListValue != null) {
                    String[] values = new String[stringListValue.size()];
                    values = stringListValue.toArray(values);
                    for (int i = 0; i < values.length; i++) {
                        values[i] = String.format("'%s'",
                                escaper.escape(values[i]));
                    }
                    builder.append(String.format("(%s)", StringUtils.join(
                            values, ",")));
                } else if (datetimeValue != null) {
                    builder.append(String.format(
                            "TIMESTAMP '%s'",
                            isoTimeStamp.format(Long.valueOf(datetimeValue.getTime()))));
                } else if (dateValue != null) {
                    builder.append(String.format("DATE '%s'",
                            isoDate.format(Long.valueOf(dateValue.getTime()))));
                } else if (integerValue != null) {
                    builder.append(integerValue);
                } else if (floatValue != null) {
                    builder.append(floatValue);
                } else {
                    // value type not supported
                    builder.append(value.toString());
                }
            }
            if (Boolean.TRUE.equals(closeParenthesis)) {
                builder.append(")");
            }
        }
        String newValue = builder.toString().trim();
        clear();
        existingQueryPart = newValue;
        return existingQueryPart;
    }

    @Override
    public boolean isValid() {
        return isValid(existingQueryPart);
    }

    public static boolean isValid(String queryPart) {
        String query = GENERIC_QUERY_SELECT + queryPart;
        try {
            SQLQueryParser.parse(query);
        } catch (QueryParseException e) {
            return false;
        }
        return true;
    }

}