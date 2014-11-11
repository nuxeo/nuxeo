/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.api.client.querymodel.descriptor;

import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.search.api.client.querymodel.Escaper;

@XObject(value = "predicate")
public class PredicateDescriptor {

    private static final String ATOMIC_PREDICATE = "atomic";

    private static final String SUB_CLAUSE_PREDICATE = "subClause";

    private static final String STATIC_PREDICATE = "static";

    @XNode("@parameter")
    protected String parameter;

    @XNode("@type")
    protected String type = ATOMIC_PREDICATE;

    protected String operator;

    @XNode("@operatorField")
    protected String operatorField;

    @XNode("@operatorSchema")
    protected String operatorSchema;

    @XNodeList(value = "field", componentType = FieldDescriptor.class, type = FieldDescriptor[].class)
    protected FieldDescriptor[] values;

    @XNode("@operator")
    public void setOperator(String operator) {
        this.operator = operator.toUpperCase();
    }

    public String getOperator() {
        return operator;
    }

    public String getQueryElement(DocumentModel model) throws ClientException {
        return getQueryElement(model, null);
    }

    public String getQueryElement(DocumentModel model, Escaper escaper)
            throws ClientException {
        if (ATOMIC_PREDICATE.equals(type)) {
            return atomicQueryElement(model, escaper);
        }
        if (SUB_CLAUSE_PREDICATE.equals(type)) {
            return subClauseQueryElement(model);
        }
        throw new ClientException("Unknown predicate type: " + type);
    }

    protected String subClauseQueryElement(DocumentModel model)
            throws ClientException {
        if (values == null || values.length != 1) {
            throw new ClientException(
                    "subClause predicate needs exactly one field");
        }
        FieldDescriptor fieldDescriptor = values[0];
        if (!fieldDescriptor.getFieldType(model).equals("string")) {
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

        Object subclause = fieldDescriptor.getRawValue(model);

        if (subclause == null) {
            // value not provided: ignore predicate
            return "";
        }

        return "(" + subclause + ")";
    }

    protected String atomicQueryElement(DocumentModel model, Escaper escaper)
            throws ClientException {
        String operator = null;
        if (operatorField != null && operatorSchema != null) {
            FieldDescriptor operatorFieldDescriptor = new FieldDescriptor(
                    operatorSchema, operatorField);
            operator = operatorFieldDescriptor.getPlainStringValue(model);
            if (operator != null) {
                operator = operator.toUpperCase();
            }
        }
        if (operator == null || "".equals(operator)) {
            operator = this.operator;
        }

        if (operator.equals("=") || operator.equals("!=")
                || operator.equals("<") || operator.equals(">")
                || operator.equals("<=") || operator.equals(">=")
                || operator.equals("<>") || operator.equals("LIKE")
                || operator.equals("ILIKE")) {
            // Unary predicate
            String value = values[0].getStringValue(model);
            if (value == null) {
                // value not provided: ignore predicate
                return "";
            }
            if (operator.equals("LIKE") || operator.equals("ILIKE")) {
                value = escaper.escape(value);
            }
            return serializeUnary(operator, value);

        } else if (operator.equals("BETWEEN")) {
            String min = values[0].getStringValue(model);
            String max = values[1].getStringValue(model);

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
                return serializeUnary("<=", max);
            } else if (min != null) {
                return serializeUnary(">=", min);
            } else {
                // both min and max are not provided, ignore predicate
                return "";
            }
        } else if (operator.equals("IN")) {
            List<String> options = values[0].getListValue(model);
            if (options == null || options.isEmpty()) {
                return "";
            } else if (options.size() == 1) {
                return serializeUnary("=", options.get(0));
            } else {
                // "IN" is not (yet?) supported by jackrabbit, so rewriting it
                // as a disjonction of exact matches
                StringBuilder builder = new StringBuilder();
                builder.append('(');
                for (int i = 0; i < options.size() - 1; i++) {
                    builder.append(serializeUnary("=", options.get(i)));
                    builder.append(" OR ");
                }
                builder.append(serializeUnary("=",
                        options.get(options.size() - 1)));
                builder.append(')');
                return builder.toString();
            }
        } else if (operator.equals("STARTSWITH")) {
            String fieldType = values[0].getFieldType(model);
            if (fieldType.equals("string")) {
                String value = values[0].getStringValue(model);
                if (value == null) {
                    return "";
                } else {
                    return serializeUnary(operator, value);
                }
            } else {
                List<String> options = values[0].getListValue(model);
                if (options == null || options.isEmpty()) {
                    return "";
                } else if (options.size() == 1) {
                    return serializeUnary(operator, options.get(0));
                } else {
                    StringBuilder builder = new StringBuilder();
                    builder.append('(');
                    for (int i = 0; i < options.size() - 1; i++) {
                        builder.append(serializeUnary(operator, options.get(i)));
                        builder.append(" OR ");
                    }
                    builder.append(serializeUnary(operator,
                            options.get(options.size() - 1)));
                    builder.append(')');
                    return builder.toString();
                }
            }
        } else if (operator.equals("EMPTY") || operator.equals("ISEMPTY")) {
            return parameter + " = ''";
        } else if (operator.equals("FULLTEXT ALL") // BBB
                || operator.equals("FULLTEXT")) {
            String value = values[0].getPlainStringValue(model);
            if (value == null) {
                // value not provided: ignore predicate
                return "";
            }
            String lhs = parameter.startsWith(NXQL.ECM_FULLTEXT) ? parameter
                    : NXQL.ECM_FULLTEXT + '.' + parameter;
            return lhs + ' ' + serializeFullText(escaper.escape(value));
        } else {
            throw new ClientException("Unsupported operator: " + operator);
        }
    }

    /**
     * Prepares a statement for a fulltext field by converting FULLTEXT* virtual
     * operators to a syntax that the SearchEngineBackend accepts.
     *
     * TODO this is hardcoded for Lucene Query Parser Syntax
     *
     * @param value
     * @return the serialized statement
     */
    protected String serializeFullText(String value) {
        // TODO Lucene Query Parser is the only supported one
        // TODO apply escapes
        String res = "";
        String[] tokens = value.split(" ");
        for (int i = 0; i < tokens.length; i++) {
            if (i != 0) {
                res += " ";
            }
            res += "+" + tokens[i];
        }
        // TODO move back to SQLQueryParser at org.nuxeo.ecm.core v 1.4
        return "= " + QueryModelDescriptor.prepareStringLiteral(res);
    }

    protected String serializeUnary(String operator, String rvalue) {
        StringBuilder builder = new StringBuilder();
        builder.append(parameter);
        builder.append(' ');
        builder.append(operator);
        builder.append(' ');
        builder.append(rvalue);
        return builder.toString();
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public FieldDescriptor[] getValues() {
        return values;
    }

    public void setValues(FieldDescriptor[] values) {
        this.values = values;
    }

}
