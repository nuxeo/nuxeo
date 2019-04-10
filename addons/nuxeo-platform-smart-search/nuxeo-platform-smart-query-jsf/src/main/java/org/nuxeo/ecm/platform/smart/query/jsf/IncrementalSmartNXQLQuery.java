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
 * NXQL implementation of an incremental smart query
 *
 * @since 5.4
 * @author Anahide Tchertchian
 */
public class IncrementalSmartNXQLQuery extends IncrementalSmartQuery {

    private static final long serialVersionUID = 1L;

    public static final String GENERIC_QUERY_SELECT = "SELECT * FROM DOCUMENT WHERE ";

    public static enum SPECIAL_OPERATORS {

        CONTAINS("CONTAINS"), BETWEEN("BETWEEN"), NOT_CONTAINS("NOT CONTAINS"), NOT_STARTSWITH(
                "NOT STARTSWITH");

        String stringValue;

        SPECIAL_OPERATORS(String stringValue) {
            this.stringValue = stringValue;
        }

        public String getStringValue() {
            return stringValue;
        }

    }

    // XXX: figure out when this is needed so that it is used
    public static final Escaper escaper = new LuceneMinimalEscaper();

    final SimpleDateFormat isoDate = new SimpleDateFormat("yyyy-MM-dd");

    final SimpleDateFormat isoTimeStamp = new SimpleDateFormat(
            "yyyy-MM-dd hh:mm:ss");

    public IncrementalSmartNXQLQuery(String existingQueryPart) {
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
            if (Boolean.TRUE.equals(addNotOperator)
                    || SPECIAL_OPERATORS.NOT_STARTSWITH.getStringValue().equals(
                            conditionalOperator)) {
                builder.append("NOT ");
            }
            if (leftExpression != null) {
                builder.append(leftExpression);
                builder.append(" ");
            }
            if (conditionalOperator != null) {
                if (SPECIAL_OPERATORS.CONTAINS.getStringValue().equals(
                        conditionalOperator)) {
                    builder.append("LIKE");
                } else if (SPECIAL_OPERATORS.NOT_CONTAINS.getStringValue().equals(
                        conditionalOperator)) {
                    builder.append("NOT LIKE");
                } else if (SPECIAL_OPERATORS.NOT_STARTSWITH.getStringValue().equals(
                        conditionalOperator)) {
                    // negation already added above
                    builder.append("STARTSWITH");
                } else {
                    builder.append(conditionalOperator);
                }
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
                    if (SPECIAL_OPERATORS.CONTAINS.getStringValue().equals(
                            conditionalOperator)
                            || SPECIAL_OPERATORS.NOT_CONTAINS.getStringValue().equals(
                                    conditionalOperator)) {
                        builder.append("'%");
                        if (Boolean.TRUE.equals(escapeValue)) {
                            builder.append(String.format("%s",
                                    escaper.escape(stringValue)));
                        } else {
                            builder.append(stringValue);
                        }
                        builder.append("%'");
                    } else {
                        if (Boolean.TRUE.equals(escapeValue)) {
                            builder.append(String.format("'%s'",
                                    escaper.escape(stringValue)));
                        } else {
                            builder.append(String.format("'%s'", stringValue));
                        }
                    }
                } else if (stringListValue != null) {
                    String[] values = new String[stringListValue.size()];
                    values = stringListValue.toArray(values);
                    if (Boolean.TRUE.equals(escapeValue)) {
                        for (int i = 0; i < values.length; i++) {
                            values[i] = String.format("'%s'",
                                    escaper.escape(values[i]));
                        }
                    } else {
                        for (int i = 0; i < values.length; i++) {
                            values[i] = String.format("'%s'", values[i]);
                        }
                    }
                    builder.append(String.format("(%s)", StringUtils.join(
                            values, ",")));
                } else if (stringArrayValue != null) {
                    String[] values = new String[stringArrayValue.length];
                    if (Boolean.TRUE.equals(escapeValue)) {
                        for (int i = 0; i < stringArrayValue.length; i++) {
                            values[i] = String.format("'%s'",
                                    escaper.escape(stringArrayValue[i]));
                        }
                    } else {
                        for (int i = 0; i < stringArrayValue.length; i++) {
                            values[i] = String.format("'%s'",
                                    stringArrayValue[i]);
                        }
                    }
                    builder.append(String.format("(%s)", StringUtils.join(
                            values, ",")));
                } else if (datetimeValue != null) {
                    builder.append(String.format(
                            "TIMESTAMP '%s'",
                            isoTimeStamp.format(Long.valueOf(datetimeValue.getTime()))));
                    if (otherDatetimeValue != null) {
                        builder.append(" AND ");
                        builder.append(String.format(
                                "TIMESTAMP '%s'",
                                isoTimeStamp.format(Long.valueOf(otherDatetimeValue.getTime()))));
                    }
                } else if (dateValue != null) {
                    // TODO: handle other date
                    builder.append(String.format("DATE '%s'",
                            isoDate.format(Long.valueOf(dateValue.getTime()))));
                    if (otherDateValue != null) {
                        builder.append(" AND ");
                        builder.append(String.format(
                                "DATE '%s'",
                                isoDate.format(Long.valueOf(otherDateValue.getTime()))));
                    }
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