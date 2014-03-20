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
 *     Nuxeo
 */

package org.nuxeo.elasticsearch.nxql;

import java.io.StringReader;

import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;
import org.nuxeo.ecm.core.query.sql.model.DefaultQueryVisitor;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.LiteralList;
import org.nuxeo.ecm.core.query.sql.model.Operand;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.query.sql.model.SelectClause;

/**
 * Helper class that holds the conversion logic.
 *
 * Conversion is based on the existing NXQL Parser, we are just using a vistor
 * to build the ES QueryString
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public class NXQLQueryConverter {

    public static QueryStringQueryBuilder toESQueryStringQueryBuilder(
            String nxql) {
        return QueryBuilders.queryString(toESQueryString(nxql));
    }

    public static String toESQueryString(String nxql) {

        final StringBuffer sb = new StringBuffer();

        SQLQuery nxqlQuery = SQLQueryParser.parse(new StringReader(nxql));

        nxqlQuery.accept(new DefaultQueryVisitor() {

            private static final long serialVersionUID = 1L;

            @Override
            public void visitQuery(SQLQuery node) {
                super.visitQuery(node);
                // intentionally does not set limit or offset in the query
            }

            @Override
            public void visitOperator(Operator op) {
                sb.append(" " + op.toString() + " ");
            }

            @Override
            public void visitSelectClause(SelectClause node) {
                // NOP
            }

            @Override
            public void visitExpression(Expression node) {

                Reference ref = node.lvalue instanceof Reference ? (Reference) node.lvalue
                        : null;
                String name = ref != null ? ref.name : null;
                String cast = ref != null ? ref.cast : null;
                Operand rvalue = node.rvalue;

                Operator op = node.operator;

                String colName = getEscapedColumnName(node.lvalue.toString());
                String value = getEscapedValue(node.rvalue.toString());

                if (op == Operator.AND || op == Operator.OR) {
                    super.visitExpression(node);
                } else if (NXQL.ECM_PATH.equals(name)) {

                } else if (NXQL.ECM_MIXINTYPE.equals(name)) {

                } else if (name != null && name.startsWith(NXQL.ECM_FULLTEXT)
                        && !NXQL.ECM_FULLTEXT_JOBID.equals(name)) {

                } else if (op == Operator.EQ) {
                    sb.append(colName);
                    sb.append(":");
                    sb.append(value);
                } else if (op == Operator.NOTEQ) {
                    sb.append(" NOT ");
                    sb.append(colName);
                    sb.append(":");
                    sb.append(value);
                } else if (op == Operator.LIKE || op == Operator.ILIKE) {
                    sb.append(colName);
                    sb.append(":");
                    value = value.subSequence(0, value.length() - 1) + "*"
                            + "\"";
                    sb.append(value);
                } else if (op == Operator.NOTLIKE || op == Operator.NOTILIKE) {
                    sb.append(" NOT ");
                    sb.append(colName);
                    sb.append(":");
                    value = value.subSequence(0, value.length() - 1) + "*"
                            + "\"";
                    sb.append(value);
                } else if (op == Operator.BETWEEN) {
                    LiteralList l = (LiteralList) rvalue;
                    sb.append(colName);
                    sb.append(":[");
                    sb.append(l.get(0));
                    sb.append(" TO ");
                    sb.append(l.get(1));
                    sb.append("]");
                } else if (op == Operator.NOTBETWEEN) {
                    LiteralList l = (LiteralList) rvalue;
                    sb.append(" NOT ");
                    sb.append(colName);
                    sb.append(":[");
                    sb.append(l.get(0));
                    sb.append(" TO ");
                    sb.append(l.get(1));
                    sb.append("]");
                }
            }

            protected String getEscapedColumnName(String name) {
                return name.replace(":", "\\:");
            }

            protected String getEscapedValue(String value) {
                if (value.startsWith("'")) {
                    value = value.substring(1, value.length() - 1);
                }
                if (!value.startsWith("\"")) {
                    value = "\"" + value + "\"";
                }
                return value;
            }

        });

        return sb.toString();

    }

}
