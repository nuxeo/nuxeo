/*
 * (C) Copyright 2010-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.platform.query.nxql;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.model.Literal;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.SimpleTypeImpl;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.core.search.api.client.querymodel.Escaper;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.api.PredicateDefinition;
import org.nuxeo.ecm.platform.query.api.PredicateFieldDefinition;
import org.nuxeo.ecm.platform.query.api.WhereClauseDefinition;
import org.nuxeo.ecm.platform.query.core.FieldDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Helper to generate NXQL queries from XMap descriptors
 *
 * @since 5.4
 * @author Anahide Tchertchian
 */
public class NXQLQueryBuilder {

    // @since 5.9.2
    public static final String DEFAULT_SELECT_STATEMENT = "SELECT * FROM Document";

    // @since 5.9
    public static final String SORTED_COLUMN = "SORTED_COLUMN";

    public static final String REGEXP_NAMED_PARAMETER = "[^a-zA-Z]:\\s*" + "([a-zA-Z0-9:]*)";

    public static final String REGEXP_EXCLUDE_QUOTE = "'[^']*'";

    public static final String REGEXP_EXCLUDE_DOUBLE_QUOTE = "\"[^\"]*\"";

    private NXQLQueryBuilder() {
    }

    /**
     * @return the built sort clause from input parameters, always non null
     */
    public static String getSortClause(SortInfo... sortInfos) {
        StringBuilder queryBuilder = new StringBuilder();
        if (sortInfos != null) {
            int index = 0;
            for (SortInfo sortInfo : sortInfos) {
                String sortColumn = sortInfo.getSortColumn();
                boolean sortAscending = sortInfo.getSortAscending();
                if (index == 0) {
                    queryBuilder.append("ORDER BY ").append(sortColumn).append(' ').append(sortAscending ? "" : "DESC");
                } else {
                    queryBuilder.append(", ").append(sortColumn).append(' ').append(sortAscending ? "" : "DESC");
                }
                index++;
            }
        }
        return queryBuilder.toString();
    }

    public static String getQuery(DocumentModel model, WhereClauseDefinition whereClause, Object[] params,
            SortInfo... sortInfos) {
        return getQuery(model, whereClause, null, params, sortInfos);
    }

    /**
     * @since 8.4
     */
    public static String getQuery(DocumentModel model, WhereClauseDefinition whereClause, String quickFiltersClause,
            Object[] params, SortInfo... sortInfos) {
        StringBuilder queryBuilder = new StringBuilder();
        String selectStatement = whereClause.getSelectStatement();
        if (StringUtils.isBlank(selectStatement)) {
            selectStatement = DEFAULT_SELECT_STATEMENT;
        }
        queryBuilder.append(selectStatement);
        queryBuilder.append(getQueryElement(model, whereClause, quickFiltersClause, params));

        String sortClause = getSortClause(sortInfos);
        if (sortClause.length() > 0) {
            queryBuilder.append(" ");
            queryBuilder.append(sortClause);
        }
        return queryBuilder.toString().trim();
    }

    public static String getQueryElement(DocumentModel model, WhereClauseDefinition whereClause, Object[] params) {
        return getQueryElement(model, whereClause, null, params);
    }

    /**
     * @since 8.4
     */
    public static String getQueryElement(DocumentModel model, WhereClauseDefinition whereClause,
            String quickFiltersClause, Object[] params) {
        List<String> elements = new ArrayList<>();
        PredicateDefinition[] predicates = whereClause.getPredicates();
        if (predicates != null) {
            Escaper escaper = null;
            Class<? extends Escaper> escaperClass = whereClause.getEscaperClass();
            if (escaperClass != null) {
                try {
                    escaper = escaperClass.newInstance();
                } catch (ReflectiveOperationException e) {
                    throw new NuxeoException(e);
                }
            }
            for (PredicateDefinition predicate : predicates) {
                String predicateString = getQueryElement(model, predicate, escaper);
                if (predicateString == null) {
                    continue;
                }

                predicateString = predicateString.trim();
                if (!predicateString.equals("")) {
                    elements.add(predicateString);
                }
            }
        }
        // add fixed part if applicable
        String fixedPart = whereClause.getFixedPart();
        if (!StringUtils.isBlank(fixedPart)) {
            if (StringUtils.isNotBlank(quickFiltersClause)) {
                fixedPart = appendClause(fixedPart, quickFiltersClause);
            }
            if (elements.isEmpty()) {
                elements.add(getQuery(fixedPart, params, whereClause.getQuoteFixedPartParameters(),
                        whereClause.getEscapeFixedPartParameters(), model));
            } else {
                elements.add('(' + getQuery(fixedPart, params, whereClause.getQuoteFixedPartParameters(),
                        whereClause.getEscapeFixedPartParameters(), model) + ')');
            }
        } else if (StringUtils.isNotBlank(quickFiltersClause)) {
            fixedPart = quickFiltersClause;
        }

        if (elements.isEmpty()) {
            return "";
        }

        // XXX: for now only a one level implement conjunctive WHERE clause
        String clauseValues = StringUtils.join(elements, " AND ").trim();

        // GR: WHERE (x = 1) is invalid NXQL
        while (elements.size() == 1 && clauseValues.startsWith("(") && clauseValues.endsWith(")")) {
            clauseValues = clauseValues.substring(1, clauseValues.length() - 1).trim();
        }
        if (clauseValues.length() == 0) {
            return "";
        }
        return " WHERE " + clauseValues;
    }

    public static String getQuery(String pattern, Object[] params, boolean quoteParameters, boolean escape,
            DocumentModel searchDocumentModel, SortInfo... sortInfos) {
        String sortedColumn;
        if (sortInfos == null || sortInfos.length == 0) {
            // If there is no ORDER BY use the id
            sortedColumn = NXQL.ECM_UUID;
        } else {
            sortedColumn = sortInfos[0].getSortColumn();
        }
        if (pattern != null && pattern.contains(SORTED_COLUMN)) {
            pattern = pattern.replace(SORTED_COLUMN, sortedColumn);
        }
        StringBuilder queryBuilder;

        // handle named parameters replacements
        if (searchDocumentModel != null) {
            // Find all query named parameters as ":parameter" not between
            // quotes and add them to matches
            String query = pattern.replaceAll(REGEXP_EXCLUDE_DOUBLE_QUOTE, StringUtils.EMPTY);
            query = query.replaceAll(REGEXP_EXCLUDE_QUOTE, StringUtils.EMPTY);
            Pattern p1 = Pattern.compile(REGEXP_NAMED_PARAMETER);
            Matcher m1 = p1.matcher(query);
            List<String> matches = new ArrayList<>();
            while (m1.find()) {
                matches.add(m1.group().substring(m1.group().indexOf(":") + 1));
            }
            for (String key : matches) {
                Object parameter = getRawValue(searchDocumentModel, new FieldDescriptor(key));
                if (parameter == null) {
                    continue;
                }
                key = ":" + key;
                if (parameter instanceof String[]) {
                    pattern = replaceStringList(pattern, Arrays.asList((String[]) parameter), quoteParameters, escape, key);
                } else if (parameter instanceof List) {
                    pattern = replaceStringList(pattern, (List<?>) parameter, quoteParameters, escape, key);
                } else if (parameter instanceof Boolean) {
                    pattern = buildPattern(pattern, key, ((Boolean) parameter) ? "1" : "0");
                } else if (parameter instanceof Number) {
                    pattern = buildPattern(pattern, key, parameter.toString());
                } else if (parameter instanceof Literal) {
                    if (quoteParameters) {
                        pattern = buildPattern(pattern, key, "'" + parameter.toString() + "'");
                    } else {
                        pattern = buildPattern(pattern, key, ((Literal) parameter).asString());
                    }
                } else {
                    if (quoteParameters) {
                        pattern = buildPattern(pattern, key, "'" + parameter + "'");
                    } else {
                        pattern = buildPattern(pattern, key, parameter.toString());
                    }
                }
            }
        }

        if (params == null) {
            queryBuilder = new StringBuilder(pattern + ' ');
        } else {
            // handle "standard" parameters replacements (referenced by ? characters)
            // XXX: the + " " is a workaround for the buggy implementation
            // of the split function in case the pattern ends with '?'
            String[] queryStrList = (pattern + ' ').split("\\?");
            queryBuilder = new StringBuilder(queryStrList[0]);
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof String[]) {
                    appendStringList(queryBuilder, Arrays.asList((String[]) params[i]), quoteParameters, escape);
                } else if (params[i] instanceof List) {
                    appendStringList(queryBuilder, (List<?>) params[i], quoteParameters, escape);
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
                        if (quoteParameters) {
                            queryBuilder.append("''");
                        }
                    } else {
                        String queryParam = params[i].toString();
                        queryBuilder.append(prepareStringLiteral(queryParam, quoteParameters, escape));
                    }
                }
                queryBuilder.append(queryStrList[i + 1]);
            }
        }
        queryBuilder.append(getSortClause(sortInfos));
        return queryBuilder.toString().trim();
    }

    public static void appendStringList(StringBuilder queryBuilder, List<?> listParam, boolean quoteParameters,
            boolean escape) {
        // avoid appending parentheses if the query builder ends with one
        boolean addParentheses = !queryBuilder.toString().endsWith("(");
        if (addParentheses) {
            queryBuilder.append('(');
        }
        List<String> result = new ArrayList<>(listParam.size());
        for (Object param : listParam) {
            result.add(prepareStringLiteral(param.toString(), quoteParameters, escape));
        }
        queryBuilder.append(String.join(", ", result));
        if (addParentheses) {
            queryBuilder.append(')');
        }
    }

    public static String replaceStringList(String pattern, List<?> listParams, boolean quoteParameters, boolean escape,
            String key) {
        List<String> result = new ArrayList<>(listParams.size());
        for (Object param : listParams) {
            result.add(prepareStringLiteral(param.toString(), quoteParameters, escape));
        }

        return buildPattern(pattern, key, '(' + StringUtils.join(result, ", " + "") + ')');
    }

    /**
     * Return the string literal in a form ready to embed in an NXQL statement.
     */
    public static String prepareStringLiteral(String s, boolean quoteParameter, boolean escape) {
        if (escape) {
            if (quoteParameter) {
                return NXQL.escapeString(s);
            } else {
                return NXQL.escapeStringInner(s);
            }
        } else {
            if (quoteParameter) {
                return "'" + s + "'";
            } else {
                return s;
            }
        }
    }

    public static String getQueryElement(DocumentModel model, PredicateDefinition predicateDescriptor, Escaper escaper) {
        String type = predicateDescriptor.getType();
        if (PredicateDefinition.ATOMIC_PREDICATE.equals(type)) {
            return atomicQueryElement(model, predicateDescriptor, escaper);
        }
        if (PredicateDefinition.SUB_CLAUSE_PREDICATE.equals(type)) {
            return subClauseQueryElement(model, predicateDescriptor);
        }
        throw new NuxeoException("Unknown predicate type: " + type);
    }

    protected static String subClauseQueryElement(DocumentModel model, PredicateDefinition predicateDescriptor) {
        PredicateFieldDefinition[] values = predicateDescriptor.getValues();
        if (values == null || values.length != 1) {
            throw new NuxeoException("subClause predicate needs exactly one field");
        }
        PredicateFieldDefinition fieldDescriptor = values[0];
        if (!getFieldType(model, fieldDescriptor).equals("string")) {
            if (fieldDescriptor.getXpath() != null) {
                throw new NuxeoException(String.format("type of field %s is not string", fieldDescriptor.getXpath()));
            } else {
                throw new NuxeoException(String.format("type of field %s.%s is not string",
                        fieldDescriptor.getSchema(), fieldDescriptor.getName()));
            }
        }
        Object subclauseValue = getRawValue(model, fieldDescriptor);
        if (subclauseValue == null) {
            return "";
        }

        return "(" + subclauseValue + ")";
    }

    protected static String atomicQueryElement(DocumentModel model, PredicateDefinition predicateDescriptor,
            Escaper escaper) {
        String operator = null;
        String operatorField = predicateDescriptor.getOperatorField();
        String operatorSchema = predicateDescriptor.getOperatorSchema();
        PredicateFieldDefinition[] values = predicateDescriptor.getValues();
        if (operatorField != null && operatorSchema != null) {
            PredicateFieldDefinition operatorFieldDescriptor = new FieldDescriptor(operatorSchema, operatorField);
            operator = getPlainStringValue(model, operatorFieldDescriptor);
            if (operator != null) {
                operator = operator.toUpperCase();
            }
        }
        if (StringUtils.isBlank(operator)) {
            operator = predicateDescriptor.getOperator();
        }
        String hint = predicateDescriptor.getHint();
        String parameter = getParameterWithHint(operator, predicateDescriptor.getParameter(), hint);

        if (operator.equals("=") || operator.equals("!=") || operator.equals("<") || operator.equals(">")
                || operator.equals("<=") || operator.equals(">=") || operator.equals("<>") || operator.equals("LIKE")
                || operator.equals("ILIKE")) {
            // Unary predicate
            String value = getStringValue(model, values[0]);
            if (value == null) {
                // value not provided: ignore predicate
                return "";
            }
            if (escaper != null && (operator.equals("LIKE") || operator.equals("ILIKE"))) {
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
        } else if (operator.equals("IN") || operator.equals("NOT IN")) {
            List<String> options = getListValue(model, values[0]);
            if (options == null || options.isEmpty()) {
                return "";
            } else if (options.size() == 1) {
                if (operator.equals("NOT IN")) {
                    return serializeUnary(parameter, "!=", options.get(0));
                } else {
                    return serializeUnary(parameter, "=", options.get(0));
                }
            } else {
                StringBuilder builder = new StringBuilder();
                builder.append(parameter);
                if (operator.equals("NOT IN")) {
                    builder.append(" NOT IN (");
                } else {
                    builder.append(" IN (");
                }
                for (int i = 0; i < options.size(); i++) {
                    if (i != 0) {
                        builder.append(", ");
                    }
                    builder.append(options.get(i));
                }
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
                        builder.append(serializeUnary(parameter, operator, options.get(i)));
                        builder.append(" OR ");
                    }
                    builder.append(serializeUnary(parameter, operator, options.get(options.size() - 1)));
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
            if (escaper != null) {
                value = escaper.escape(value);
            }
            return parameter + ' ' + serializeFullText(value);
        } else if (operator.equals("IS NULL")) {
            Boolean value = getBooleanValue(model, values[0]);
            if (value == null) {
                // value not provided: ignore predicate
                return "";
            } else if (Boolean.TRUE.equals(value)) {
                return parameter + " IS NULL";
            } else {
                return parameter + " IS NOT NULL";
            }
        } else {
            throw new NuxeoException("Unsupported operator: " + operator);
        }
    }

    protected static String getParameterWithHint(String operator, String parameter, String hint) {
        String ret = parameter;
        // add ecm:fulltext. prefix if needed
        if ((operator.equals("FULLTEXT ALL") || operator.equals("FULLTEXT"))
                && !parameter.startsWith(NXQL.ECM_FULLTEXT)) {
             ret = NXQL.ECM_FULLTEXT + '.' + parameter;
        }
        // add the hint
        if (hint != null && !hint.isEmpty()) {
            ret = String.format("/*+%s */ %s", hint.trim(), ret);
        }
        return ret;
    }

    public static final String DEFAULT_SPECIAL_CHARACTERS_REGEXP = "!#$%&'()+,./\\\\:-@{|}`^~";

    public static final String IGNORED_CHARS_KEY = "org.nuxeo.query.builder.ignored.chars";

    /**
     * Remove any special character that could be mis-interpreted as a low level full-text query operator. This method
     * should be used by user facing callers of CoreQuery*PageProviders that use a fixed part or a pattern query. Fields
     * in where clause already dealt with.
     *
     * @since 5.6
     * @return sanitized text value
     */
    public static String sanitizeFulltextInput(String value) {
        // Ideally the low level full-text language
        // parser should be robust to any user input however this is much more
        // complicated to implement correctly than the following simple user
        // input filtering scheme.
        ConfigurationService cs = Framework.getService(ConfigurationService.class);
        String ignoredChars = cs.getProperty(IGNORED_CHARS_KEY, DEFAULT_SPECIAL_CHARACTERS_REGEXP);
        String res = "";
        value = value.replaceAll("[" + ignoredChars + "]", " ");
        value = value.trim();
        String[] tokens = value.split("[\\s]+");
        for (String token : tokens) {
            if ("-".equals(token) || "*".equals(token) || "*-".equals(token) || "-*".equals(token)) {
                continue;
            }
            if (res.length() > 0) {
                res += " ";
            }
            if (token.startsWith("-") || token.endsWith("*")) {
                res += token;
            } else {
                res += token.replace("-", " ").replace("*", " ");
            }
        }
        return res.trim();
    }

    public static String serializeFullText(String value) {
        value = sanitizeFulltextInput(value);
        return "= " + NXQL.escapeString(value);
    }

    protected static String serializeUnary(String parameter, String operator, String rvalue) {
        StringBuilder builder = new StringBuilder();
        builder.append(parameter);
        builder.append(' ');
        builder.append(operator);
        builder.append(' ');
        builder.append(rvalue);
        return builder.toString();
    }

    public static String getPlainStringValue(DocumentModel model, PredicateFieldDefinition fieldDescriptor) {
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

    public static Integer getIntValue(DocumentModel model, PredicateFieldDefinition fieldDescriptor) {
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

    public static String getFieldType(DocumentModel model, PredicateFieldDefinition fieldDescriptor) {
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
                if (schema != null) {
                    Schema schemaObj = typeManager.getSchema(schema);
                    if (schemaObj == null) {
                        throw new NuxeoException("failed to obtain schema: " + schema);
                    }
                    field = schemaObj.getField(name);
                } else {
                    // assume named parameter use case: hard-code on String in this case
                    return StringType.ID;
                }
            }
            if (field == null) {
                throw new NuxeoException("failed to obtain field: " + schema + ":" + name);
            }
            Type type = field.getType();
            if (type instanceof SimpleTypeImpl) {
                // type with constraint
                type = type.getSuperType();
            }
            return type.getName();
        } catch (PropertyException e) {
            e.addInfo("failed to get field type for " + (xpath != null ? xpath : (schema + ":" + name)));
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    public static Object getRawValue(DocumentModel model, PredicateFieldDefinition fieldDescriptor) {
        String xpath = fieldDescriptor.getXpath();
        String schema = fieldDescriptor.getSchema();
        String name = fieldDescriptor.getName();
        try {
            if (xpath != null) {
                return model.getPropertyValue(xpath);
            } else if (schema == null) {
                return model.getPropertyValue(name);
            } else {
                return model.getProperty(schema, name);
            }
        } catch (PropertyNotFoundException e) {
            // fall back on named parameters if any
            Map<String, Object> params = (Map<String, Object>) model.getContextData(
                    PageProviderService.NAMED_PARAMETERS);
            if (params != null) {
                if (xpath != null) {
                    return params.get(xpath);
                } else {
                    return params.get(name);
                }
            }
        } catch (PropertyException e) {
            return null;
        }
        return null;
    }

    public static String getStringValue(DocumentModel model, PredicateFieldDefinition fieldDescriptor) {
        Object rawValue = getRawValue(model, fieldDescriptor);
        if (rawValue == null) {
            return null;
        }
        String value;
        if (rawValue instanceof GregorianCalendar) {
            GregorianCalendar gc = (GregorianCalendar) rawValue;
            value = "TIMESTAMP '" + getDateFormat().format(gc.getTime()) + "'";
        } else if (rawValue instanceof Date) {
            Date date = (Date) rawValue;
            value = "TIMESTAMP '" + getDateFormat().format(date) + "'";
        } else if (rawValue instanceof Integer || rawValue instanceof Long || rawValue instanceof Double) {
            value = rawValue.toString(); // no quotes
        } else if (rawValue instanceof Boolean) {
            value = ((Boolean) rawValue).booleanValue() ? "1" : "0";
        } else {
            value = rawValue.toString().trim();
            if (value.equals("")) {
                return null;
            }
            String fieldType = getFieldType(model, fieldDescriptor);
            if ("long".equals(fieldType) || "integer".equals(fieldType) || "double".equals(fieldType)) {
                return value;
            } else {
                return NXQL.escapeString(value);
            }
        }
        return value;
    }

    protected static DateFormat getDateFormat() {
        // not thread-safe so don't use a static instance
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    @SuppressWarnings("unchecked")
    public static List<String> getListValue(DocumentModel model, PredicateFieldDefinition fieldDescriptor) {
        Object rawValue = getRawValue(model, fieldDescriptor);
        if (rawValue == null) {
            return null;
        }
        List<String> values = new ArrayList<>();
        if (rawValue instanceof ArrayList) {
            rawValue = ((ArrayList<Object>) rawValue).toArray();
        }
        for (Object element : (Object[]) rawValue) {
            if (element != null) {
                if (element instanceof Number) {
                    values.add(element.toString());
                } else {
                    String value = element.toString().trim();
                    if (!value.equals("")) {
                        values.add(NXQL.escapeString(value));
                    }
                }
            }
        }
        return values;
    }

    public static Boolean getBooleanValue(DocumentModel model, PredicateFieldDefinition fieldDescriptor) {
        Object rawValue = getRawValue(model, fieldDescriptor);
        if (rawValue == null) {
            return null;
        } else {
            return (Boolean) rawValue;
        }
    }

    /**
     * @since 8.4
     */
    public static String appendClause(String query, String clause) {
        return query + " AND " + clause;
    }

    /**
     * @since 8.4
     */
    public static String buildPattern(String pattern, String key, String replacement) {
        int index = pattern.indexOf(key);
        while (index >= 0) {
            // All keys not prefixed by a letter or a digit has to be replaced, because
            // It could be part of a schema name
            if (!Character.isLetterOrDigit(pattern.charAt(index - 1)) && (index + key.length() == pattern.length()
                    || !Character.isLetterOrDigit(pattern.charAt(index + key.length())))) {
                pattern = pattern.substring(0, index) + pattern.substring(index).replaceFirst(key, replacement);
            }
            index = pattern.indexOf(key, index + 1);
        }
        return pattern;
    }

}
