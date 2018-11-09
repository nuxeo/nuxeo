/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory.sql;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.model.BooleanLiteral;
import org.nuxeo.ecm.core.query.sql.model.DateLiteral;
import org.nuxeo.ecm.core.query.sql.model.DefaultQueryVisitor;
import org.nuxeo.ecm.core.query.sql.model.DoubleLiteral;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.Function;
import org.nuxeo.ecm.core.query.sql.model.IntegerLiteral;
import org.nuxeo.ecm.core.query.sql.model.Literal;
import org.nuxeo.ecm.core.query.sql.model.LiteralList;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operand;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.StringLiteral;
import org.nuxeo.ecm.core.storage.sql.ColumnSpec;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;

/**
 * Builds the database-level WHERE query from the AST, and collects parameters associated to free variables along with
 * the database column to which they correspond.
 *
 * @since 10.3
 */
public class SQLQueryBuilder extends DefaultQueryVisitor {

    /** @since 10.3 */
    public static class ColumnAndValue {

        public final Column column;

        public final Serializable value;

        public ColumnAndValue(Column column, Serializable value) {
            this.column = column;
            this.value = value;
        }

        public Column getColumn() {
            return column;
        }

        public Serializable getValue() {
            return value;
        }
    }

    protected final SQLDirectory directory;

    public final StringBuilder clause = new StringBuilder();

    public final List<ColumnAndValue> params = new ArrayList<>();

    protected Column visitedColumn;

    public SQLQueryBuilder(SQLDirectory directory) {
        this.directory = directory;
    }

    @Override
    public void visitMultiExpression(MultiExpression node) {
        if (node.predicates.isEmpty()) {
            // if this happens we won't have a valid SQL expression
            // but the caller should check for empty clauses and prune them
            return;
        }
        clause.append('(');
        for (Iterator<Predicate> it = node.predicates.iterator(); it.hasNext();) {
            it.next().accept(this);
            if (it.hasNext()) {
                node.operator.accept(this);
            }
        }
        clause.append(')');
    }

    @Override
    public void visitExpression(Expression node) {
        clause.append('(');
        Operand lvalue = node.lvalue;
        Operand rvalue = node.rvalue;
        Column column = null;
        if (lvalue instanceof Reference) {
            Reference ref = (Reference) lvalue;
            if (ref.cast != null) {
                throw new QueryParseException("Cannot use cast: " + node);
            }
            column = getColumn(ref.name);
            Operator op = node.operator;
            if (op == Operator.BETWEEN || op == Operator.NOTBETWEEN) {
                visitExpressionBetween(column, op, (LiteralList) rvalue);
            } else if (op == Operator.LIKE || op == Operator.NOTLIKE) {
                visitExpressionLike(column, op, rvalue);
            } else if (op == Operator.ILIKE || op == Operator.NOTILIKE) {
                visitExpressionILike(column, op, rvalue);
            } else {
                visitExpression(column, op, rvalue);
            }
        } else {
            super.visitExpression(node);
        }
        clause.append(')');
    }

    protected void visitExpressionBetween(Column column, Operator op, LiteralList list) {
        visitColumn(column);
        op.accept(this);
        list.get(0).accept(this);
        clause.append(" AND ");
        list.get(1).accept(this);
    }

    protected void visitExpressionLike(Column column, Operator op, Operand rvalue) {
        visitExpression(column, op, rvalue);
        addLikeEscaping();
    }

    protected void visitExpressionILike(Column column, Operator op, Operand rvalue) {
        if (directory.getDialect().supportsIlike()) {
            visitExpression(column, op, rvalue);
        } else {
            clause.append("LOWER(");
            visitColumn(column);
            clause.append(") ");
            if (op == Operator.NOTILIKE) {
                clause.append("NOT ");
            }
            clause.append("LIKE");
            clause.append(" LOWER(");
            rvalue.accept(this);
            clause.append(")");
            addLikeEscaping();
        }
    }

    protected void addLikeEscaping() {
        String escape = directory.getDialect().getLikeEscaping();
        if (escape != null) {
            clause.append(escape);
        }
    }

    protected void visitExpression(Column column, Operator op, Operand rvalue) {
        visitColumn(column);
        if (op == Operator.EQ || op == Operator.NOTEQ) {
            if (column.getType().spec == ColumnSpec.BOOLEAN) {
                rvalue = getBooleanLiteral(rvalue);
            }
            if (directory.getDialect().hasNullEmptyString() && rvalue instanceof StringLiteral
                    && ((StringLiteral) rvalue).value.isEmpty()) {
                // see NXP-6172, empty values are Null in Oracle
                op = op == Operator.EQ ? Operator.ISNULL : Operator.ISNOTNULL;
                rvalue = null;
            }

        }
        op.accept(this);
        if (rvalue != null) {
            rvalue.accept(this);
        }
    }

    @Override
    public void visitOperator(Operator node) {
        if (node != Operator.NOT) {
            clause.append(' ');
        }
        clause.append(node.toString());
        clause.append(' ');
    }

    @Override
    public void visitReference(Reference node) {
        visitColumn(getColumn(node.name));
    }

    protected void visitColumn(Column column) {
        visitedColumn = column;
        clause.append(column.getQuotedName());
    }

    @Override
    public void visitLiteralList(LiteralList node) {
        clause.append('(');
        for (Iterator<Literal> it = node.iterator(); it.hasNext();) {
            it.next().accept(this);
            if (it.hasNext()) {
                clause.append(", ");
            }
        }
        clause.append(')');
    }

    @Override
    public void visitDateLiteral(DateLiteral node) {
        clause.append('?');
        if (node.onlyDate) {
            params.add(new ColumnAndValue(visitedColumn, node.toSqlDate()));
        } else {
            params.add(new ColumnAndValue(visitedColumn, node.toCalendar()));
        }
    }

    @Override
    public void visitStringLiteral(StringLiteral node) {
        clause.append('?');
        params.add(new ColumnAndValue(visitedColumn, node.value));
    }

    @Override
    public void visitDoubleLiteral(DoubleLiteral node) {
        clause.append('?');
        params.add(new ColumnAndValue(visitedColumn, Double.valueOf(node.value)));
    }

    @Override
    public void visitIntegerLiteral(IntegerLiteral node) {
        clause.append('?');
        params.add(new ColumnAndValue(visitedColumn, Long.valueOf(node.value)));
    }

    @Override
    public void visitBooleanLiteral(BooleanLiteral node) {
        clause.append('?');
        params.add(new ColumnAndValue(visitedColumn, Boolean.valueOf(node.value)));
    }

    @Override
    public void visitFunction(Function node) {
        throw new QueryParseException("Function not supported" + node);
    }

    protected Column getColumn(String name) {
        Column column = directory.getTable().getColumn(name);
        if (column == null) {
            throw new QueryParseException("No column: " + name + " for directory: " + directory.getName());
        }
        return column;
    }

    protected Operand getBooleanLiteral(Operand rvalue) {
        if (rvalue instanceof BooleanLiteral) {
            return rvalue;
        }
        long v;
        if (!(rvalue instanceof IntegerLiteral) || ((v = ((IntegerLiteral) rvalue).value) != 0 && v != 1)) {
            throw new QueryParseException("Boolean expressions require boolean or literal 0 or 1 as right argument");
        }
        return new BooleanLiteral(v == 1);
    }
}
