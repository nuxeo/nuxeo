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
package org.nuxeo.ecm.platform.query.nxql;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.model.Literal;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.search.api.client.querymodel.Escaper;
import org.nuxeo.ecm.platform.query.api.PredicateDefinition;
import org.nuxeo.ecm.platform.query.api.PredicateFieldDefinition;
import org.nuxeo.ecm.platform.query.api.WhereClauseDefinition;
import org.nuxeo.ecm.platform.query.core.FieldDescriptor;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper to generate NXQL queries from XMap descriptors
 *
 * @since 5.4
 * @author Anahide Tchertchian
 */
public class NXQLQueryBuilder {

    public static final SimpleDateFormat sf = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");

    private NXQLQueryBuilder() {
    }

    public static String getSortClause(SortInfo... sortInfos) {
        StringBuilder queryBuilder = new StringBuilder();
        if (sortInfos != null) {
            int index = 0;
            for (SortInfo sortInfo : sortInfos) {
                String sortColumn = sortInfo.getSortColumn();
                boolean sortAscending = sortInfo.getSortAscending();
                if (index == 0) {
                    queryBuilder.append("ORDER BY ").append(sortColumn).append(
                            ' ').append(sortAscending ? "" : "DESC");
                } else {
                    queryBuilder.append(", ").append(sortColumn).append(' ').append(
                            sortAscending ? "" : "DESC");
                }
                index++;
            }
        }
        return queryBuilder.toString();
    }

    public static String getQuery(DocumentModel model,
            WhereClauseDefinition whereClause, Object[] params,
            SortInfo... sortInfos) throws ClientException {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT * FROM Document");
        if (whereClause != null) {
            queryBuilder.append(getQueryElement(model, whereClause, params));
        }
        String sortClause = getSortClause(sortInfos);
        if (sortClause != null && sortClause.length() > 0) {
            queryBuilder.append(" ");
            queryBuilder.append(sortClause);
        }
        return queryBuilder.toString().trim();
    }

    public static String getQueryElement(DocumentModel model,
            WhereClauseDefinition whereClause, Object[] params)
            throws ClientException {
        List<String> elements = new ArrayList<String>();
        PredicateDefinition[] predicates = whereClause.getPredicates();
        if (predicates != null) {
            try {
                Escaper escaper = null;
                Class<? extends Escaper> escaperClass = whereClause.getEscaperClass();
                if (escaperClass != null) {
                    escaper = escaperClass.newInstance();
                }
                for (PredicateDefinition predicate : predicates) {
                    String predicateString = getQueryElement(model, predicate,
                            escaper);
                    if (predicateString == null) {
                        continue;
                    } else {
                        predicateString = predicateString.trim();
                    }
                    if (!predicateString.equals("")) {
                        elements.add(predicateString);
                    }
                }
            } catch (IllegalAccessException e) {
                throw new ClientException(e);
            } catch (InstantiationException e) {
                throw new ClientException(e);
            }
        }
        // add fixed part if applicable
        String fixedPart = whereClause.getFixedPart();
        if (fixedPart != null && !fixedPart.equals("")) {
            if (elements.isEmpty()) {
                elements.add(getQuery(fixedPart, params,
                        whereClause.getQuoteFixedPartParameters(),
                        whereClause.getEscapeFixedPartParameters()));
            } else {
                elements.add('(' + getQuery(fixedPart, params,
                        whereClause.getQuoteFixedPartParameters(),
                        whereClause.getEscapeFixedPartParameters()) + ')');
            }
        }

        if (elements.isEmpty()) {
            return "";
        }

        // XXX: for now only a one level implement conjunctive WHERE clause
        String clauseValues = StringUtils.join(elements, " AND ").trim();

        // GR: WHERE (x = 1) is invalid NXQL
        while (elements.size() == 1 && clauseValues.startsWith("(")
                && clauseValues.endsWith(")")) {
            clauseValues = clauseValues.substring(1, clauseValues.length() - 1).trim();
        }
        if (clauseValues.length() == 0) {
            return "";
        }
        return " WHERE " + clauseValues;
    }

    public static String getQuery(String pattern, Object[] params,
            boolean quoteParameters, boolean escape, SortInfo... sortInfos)
            throws ClientException {
        StringBuilder queryBuilder;
        if (params == null) {
            queryBuilder = new StringBuilder(pattern + ' ');
        } else {
            // XXX: the + " " is a workaround for the buggy implementation
            // of the split function in case the pattern ends with '?'
            String[] queryStrList = (pattern + ' ').split("\\?");
            queryBuilder = new StringBuilder(queryStrList[0]);
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof String[]) {
                    appendStringList(queryBuilder,
                            Arrays.asList((String[]) params[i]),
                            quoteParameters, escape);
                } else if (params[i] instanceof List) {
                    appendStringList(queryBuilder, (List<?>) params[i],
                            quoteParameters, escape);
                } else if (params[i] instanceof Boolean) {
                    boolean b = ((Boolean) params[i]).booleanValue();
                    queryBuilder.append(b ? 1 : 0);
                } else if (params[i] instanceof Number) {
                    queryBuilder.append(params[i]);
                } else if (params[i] instanceof Literal) {
                    if (quoteParameters) {
                        queryBuilder.append(params[i].toString());
                    } else {
                        queryBuilder.append(((Literal) params[i]).asString());
                    }
                } else {
                    if (params[i] == null) {
                        queryBuilder.append("''");
                    } else {
                        String queryParam = params[i].toString();
                        queryBuilder.append(prepareStringLiteral(queryParam,
                                quoteParameters, escape));
                    }
                }
                queryBuilder.append(queryStrList[i + 1]);
            }
        }
        queryBuilder.append(getSortClause(sortInfos));
        return queryBuilder.toString().trim();
    }

    public static void appendStringList(StringBuilder queryBuilder,
            List<?> listParam, boolean quoteParameters, boolean escape) {
        queryBuilder.append('(');
        List<String> result = new ArrayList<String>(listParam.size());
        for (Object param : listParam) {
            result.add(prepareStringLiteral(param.toString(), quoteParameters,
                    escape));
        }
        queryBuilder.append(StringUtils.join(result, ", "));
        queryBuilder.append(')');
    }

    /**
     * Return the string literal in a form ready to embed in an NXQL statement.
     */
    public static String prepareStringLiteral(String s, boolean quoteParameter,
            boolean escape) {
        String res;
        if (escape) {
            res = s.replaceAll("'", "\\\\'");
        } else {
            res = s;
        }
        if (quoteParameter) {
            res = "'" + res + "'";
        }
        return res;
    }

    public static String getQueryElement(DocumentModel model,
            PredicateDefinition predicateDescriptor, Escaper escaper)
            throws ClientException {
        String type = predicateDescriptor.getType();
        if (PredicateDefinition.ATOMIC_PREDICATE.equals(type)) {
            return atomicQueryElement(model, predicateDescriptor, escaper);
        }
        if (PredicateDefinition.SUB_CLAUSE_PREDICATE.equals(type)) {
            return subClauseQueryElement(model, predicateDescriptor);
        }
        throw new ClientException("Unknown predicate type: " + type);
    }

    protected static String subClauseQueryElement(DocumentModel model,
            PredicateDefinition predicateDescriptor) throws ClientException {
        PredicateFieldDefinition[] values = predicateDescriptor.getValues();
        if (values == null || values.length != 1) {
            throw new ClientException(
                    "subClause predicate needs exactly one field");
        }
        PredicateFieldDefinition fieldDescriptor = values[0];
        if (!getFieldType(model, fieldDescriptor).equals("string")) {
            if (fieldDescriptor.getXpath() != null) {
                throw new ClientException(String.format(
                        "type of field %s is not string",
                        fieldDescriptor.getXpath()));
            } else {
                throw new ClientException(String.format(
                        "type of field %s.%s is not string",
                        fieldDescriptor.getSchema(), fieldDescriptor.getName()));
            }
        }
        Object subclauseValue = getRawValue(model, fieldDescriptor);
        if (subclauseValue == null) {
            return "";
        }

        return "(" + subclauseValue + ")";
    }

    protected static String atomicQueryElement(DocumentModel model,
            PredicateDefinition predicateDescriptor, Escaper escaper)
            throws ClientException {
        String operator = null;
        String operatorField = predicateDescriptor.getOperatorField();
        String operatorSchema = predicateDescriptor.getOperatorSchema();
        String parameter = predicateDescriptor.getParameter();
        PredicateFieldDefinition[] values = predicateDescriptor.getValues();
        if (operatorField != null && operatorSchema != null) {
            PredicateFieldDefinition operatorFieldDescriptor = new FieldDescriptor(
                    operatorSchema, operatorField);
            operator = getPlainStringValue(model, operatorFieldDescriptor);
            if (operator != null) {
                operator = operator.toUpperCase();
            }
        }
        if (operator == null || "".equals(operator)) {
            operator = predicateDescriptor.getOperator();
        }

        if (operator.equals("=") || operator.equals("!=")
                || operator.equals("<") || operator.equals(">")
                || operator.equals("<=") || operator.equals(">=")
                || operator.equals("<>") || operator.equals("LIKE")
                || operator.equals("ILIKE")) {
            // Unary predicate
            String value = getStringValue(model, values[0]);
            if (value == null) {
                // value not provided: ignore predicate
                return "";
            }
            if (escaper != null
                    && (operator.equals("LIKE") || operator.equals("ILIKE"))) {
                value = escaper.escape(value);
            }
            return serializeUnary(parameter, operator, value);

        } else if (operator.equals("BETWEEN")) {
            String min = getStringValue(model, values[0]);
            String max = getStringValue(model, values[1]);

            if (min != null && max != null) {
                StringBuilder builder = new StringBuilder();
                builder.append(parameter);
                builder.append(' ');
                builder.append(operator);
                builder.append(' ');
                builder.append(min);
                builder.append(" AND ");
                builder.append(max);
                return builder.toString();
            } else if (max != null) {
                return serializeUnary(parameter, "<=", max);
            } else if (min != null) {
                return serializeUnary(parameter, ">=", min);
            } else {
                // both min and max are not provided, ignore predicate
                return "";
            }
        } else if (operator.equals("IN")) {
            List<String> options = getListValue(model, values[0]);
            if (options == null || options.isEmpty()) {
                return "";
            } else if (options.size() == 1) {
                return serializeUnary(parameter, "=", options.get(0));
            } else {
                // "IN" is not (yet?) supported by jackrabbit, so rewriting it
                // as a disjonction of exact matches
                StringBuilder builder = new StringBuilder();
                builder.append('(');
                for (int i = 0; i < options.size() - 1; i++) {
                    builder.append(serializeUnary(parameter, "=",
                            options.get(i)));
                    builder.append(" OR ");
                }
                builder.append(serializeUnary(parameter, "=",
                        options.get(options.size() - 1)));
                builder.append(')');
                return builder.toString();
            }
        } else if (operator.equals("STARTSWITH")) {
            String fieldType = getFieldType(model, values[0]);
            if (fieldType.equals("string")) {
                String value = getStringValue(model, values[0]);
                if (value == null) {
                    return "";
                } else {
                    return serializeUnary(parameter, operator, value);
                }
            } else {
                List<String> options = getListValue(model, values[0]);
                if (options == null || options.isEmpty()) {
                    return "";
                } else if (options.size() == 1) {
                    return serializeUnary(parameter, operator, options.get(0));
                } else {
                    StringBuilder builder = new StringBuilder();
                    builder.append('(');
                    for (int i = 0; i < options.size() - 1; i++) {
                        builder.append(serializeUnary(parameter, operator,
                                options.get(i)));
                        builder.append(" OR ");
                    }
                    builder.append(serializeUnary(parameter, operator,
                            options.get(options.size() - 1)));
                    builder.append(')');
                    return builder.toString();
                }
            }
        } else if (operator.equals("EMPTY") || operator.equals("ISEMPTY")) {
            return parameter + " = ''";
        } else if (operator.equals("FULLTEXT ALL") // BBB
                || operator.equals("FULLTEXT")) {
            String value = getPlainStringValue(model, values[0]);
            if (value == null) {
                // value not provided: ignore predicate
                return "";
            }
            String lhs = parameter.startsWith(NXQL.ECM_FULLTEXT) ? parameter
                    : NXQL.ECM_FULLTEXT + '.' + parameter;
            if (escaper != null) {
                value = escaper.escape(value);
            }
            return lhs + ' ' + serializeFullText(value);
        } else {
            throw new ClientException("Unsupported operator: " + operator);
        }
    }

    /**
     * Prepares a statement for a fulltext field by converting FULLTEXT virtual
     * operators to a syntax that the search syntax accepts.
     *
     * @param value
     * @return the serialized statement
     */

    public static final String SPECIAL_CHARACTERS_REGEXP = "[!-/:-@{-}`^~]";

    public static String serializeFullText(String value) {
        String res = "";
        value = value.replaceAll(SPECIAL_CHARACTERS_REGEXP, " ");
        value = value.trim();
        String[] tokens = value.split(" ");
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].length() > 0) {
                if (res.length() > 0) {
                    res += " ";
                }
                res += "+" + tokens[i];
            }
        }
        return "= " + prepareStringLiteral(res, true, true);
    }

    protected static String serializeUnary(String parameter, String operator,
            String rvalue) {
        StringBuilder builder = new StringBuilder();
        builder.append(parameter);
        builder.append(' ');
        builder.append(operator);
        builder.append(' ');
        builder.append(rvalue);
        return builder.toString();
    }

    public static String getPlainStringValue(DocumentModel model,
            PredicateFieldDefinition fieldDescriptor) {
        Object rawValue = getRawValue(model, fieldDescriptor);
        if (rawValue == null) {
            return null;
        }
        String value = (String) rawValue;
        if (value.equals("")) {
            return null;
        }
        return value;
    }

    public static Integer getIntValue(DocumentModel model,
            PredicateFieldDefinition fieldDescriptor) {
        Object rawValue = getRawValue(model, fieldDescriptor);
        if (rawValue == null || "".equals(rawValue)) {
            return null;
        } else if (rawValue instanceof Integer) {
            return (Integer) rawValue;
        } else if (rawValue instanceof String) {
            return Integer.valueOf((String) rawValue);
        } else {
            return Integer.valueOf(rawValue.toString());
        }
    }

    public static String getFieldType(DocumentModel model,
            PredicateFieldDefinition fieldDescriptor) throws ClientException {
        String xpath = fieldDescriptor.getXpath();
        String schema = fieldDescriptor.getSchema();
        String name = fieldDescriptor.getName();
        try {
            SchemaManager typeManager = Framework.getService(SchemaManager.class);
            Field field = null;
            if (xpath != null) {
                if (model != null) {
                    field = model.getProperty(xpath).getField();
                }
            } else {
                Schema schemaObj = typeManager.getSchema(schema);
                if (schemaObj == null) {
                    throw new ClientException("failed to obtain schema: "
                            + schema);
                }
                field = schemaObj.getField(name);
            }
            if (field == null) {
                throw new ClientException("failed to obtain field: " + schema
                        + ":" + name);
            }
            return field.getType().getName();
        } catch (Exception e) {
            throw new ClientException("failed to get field type for "
                    + (xpath != null ? xpath : (schema + ":" + name)), e);
        }
    }

    public static Object getRawValue(DocumentModel model,
            PredicateFieldDefinition fieldDescriptor) {
        String xpath = fieldDescriptor.getXpath();
        String schema = fieldDescriptor.getSchema();
        String name = fieldDescriptor.getName();
        try {
            if (xpath != null) {
                return model.getPropertyValue(xpath);
            } else {
                return model.getProperty(schema, name);
            }
        } catch (ClientException e) {
            return null;
        }
    }

    public static String getStringValue(DocumentModel model,
            PredicateFieldDefinition fieldDescriptor) throws ClientException {
        Object rawValue = getRawValue(model, fieldDescriptor);
        if (rawValue == null) {
            return null;
        }
        String value;
        if (rawValue instanceof GregorianCalendar) {
            GregorianCalendar gc = (GregorianCalendar) rawValue;
            value = "TIMESTAMP '" + sf.format(gc.getTime()) + "'";
        } else if (rawValue instanceof Date) {
            Date date = (Date) rawValue;
            value = "TIMESTAMP '" + sf.format(date) + "'";
        } else if (rawValue instanceof Integer || rawValue instanceof Long
                || rawValue instanceof Double) {
            value = rawValue.toString(); // no quotes
        } else if (rawValue instanceof Boolean) {
            value = ((Boolean) rawValue).booleanValue() ? "1" : "0";
        } else {
            value = rawValue.toString().trim();
            if (value.equals("")) {
                return null;
            }
            String fieldType = getFieldType(model, fieldDescriptor);
            if ("long".equals(fieldType) || "integer".equals(fieldType)
                    || "double".equals(fieldType)) {
                return value;
            } else {
                return prepareStringLiteral(value, true, true);
            }
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getListValue(DocumentModel model,
            PredicateFieldDefinition fieldDescriptor) {
        Object rawValue = getRawValue(model, fieldDescriptor);
        if (rawValue == null) {
            return null;
        }
        List<String> values = new ArrayList<String>();
        if (rawValue instanceof ArrayList) {
            rawValue = ((ArrayList<Object>) rawValue).toArray();
        }
        for (Object element : (Object[]) rawValue) {
            if (element != null) {
                String value = element.toString().trim();
                if (!value.equals("")) {
                    values.add("'" + value + "'");
                }
            }
        }
        return values;
    }

    public static Boolean getBooleanValue(DocumentModel model,
            PredicateFieldDefinition fieldDescriptor) {
        Object rawValue = getRawValue(model, fieldDescriptor);
        if (rawValue == null) {
            return null;
        } else {
            return (Boolean) rawValue;
        }
    }

}
