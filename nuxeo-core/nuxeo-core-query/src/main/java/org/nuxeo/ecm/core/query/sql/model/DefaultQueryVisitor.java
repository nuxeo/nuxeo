/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.query.sql.model;

/**
 * A default implementation of a visitor that visits depth-first in standard expression order.
 *
 * @author Florent Guillaume
 */
public class DefaultQueryVisitor implements IVisitor {

    private static final long serialVersionUID = 1L;

    @Override
    public void visitQuery(SQLQuery node) {
        node.select.accept(this);
        node.from.accept(this);
        if (node.where != null) {
            node.where.accept(this);
        }
        if (node.orderBy != null) {
            node.orderBy.accept(this);
        }
        if (node.groupBy != null) {
            node.groupBy.accept(this);
        }
        if (node.having != null) {
            node.having.accept(this);
        }
    }

    @Override
    public void visitSelectClause(SelectClause node) {
        node.elements.forEach((alias, operand) -> operand.accept(this));
    }

    @Override
    public void visitFromClause(FromClause node) {
    }

    @Override
    public void visitWhereClause(WhereClause node) {
        node.predicate.accept(this);
    }

    @Override
    public void visitGroupByClause(GroupByClause node) {
    }

    @Override
    public void visitHavingClause(HavingClause node) {
        if (node.predicate != null) {
            node.predicate.accept(this);
        }
    }

    @Override
    public void visitOrderByClause(OrderByClause node) {
        node.elements.accept(this);
    }

    @Override
    public void visitOrderByList(OrderByList node) {
        for (OrderByExpr aNode : node) {
            aNode.accept(this);
        }
    }

    @Override
    public void visitOrderByExpr(OrderByExpr node) {
        node.reference.accept(this);
    }

    @Override
    public void visitExpression(Expression node) {
        if (node.rvalue == null) {
            if (node.isSuffix()) {
                // IS NULL, IS NOT NULL
                node.lvalue.accept(this);
                node.operator.accept(this);
            } else {
                // NOT
                node.operator.accept(this);
                node.lvalue.accept(this);
            }
        } else {
            node.lvalue.accept(this);
            node.operator.accept(this);
            node.rvalue.accept(this);
        }
    }

    @Override
    public void visitMultiExpression(MultiExpression node) {
        for (Operand operand : node.values) {
            operand.accept(this);
        }
    }

    @Override
    public void visitOperator(Operator node) {
    }

    @Override
    public void visitReference(Reference node) {
    }

    @Override
    public void visitReferenceList(ReferenceList node) {
        for (Reference reference : node) {
            reference.accept(this);
        }
    }

    @Override
    public void visitLiteral(Literal node) {
    }

    @Override
    public void visitLiteralList(LiteralList node) {
        for (Literal literal : node) {
            literal.accept(this);
        }
    }

    @Override
    public void visitDateLiteral(DateLiteral node) {
        visitLiteral(node);
    }

    @Override
    public void visitStringLiteral(StringLiteral node) {
        visitLiteral(node);
    }

    @Override
    public void visitDoubleLiteral(DoubleLiteral node) {
        visitLiteral(node);
    }

    @Override
    public void visitIntegerLiteral(IntegerLiteral node) {
        visitLiteral(node);
    }

    @Override
    public void visitBooleanLiteral(BooleanLiteral node) {
        visitLiteral(node);
    }

    @Override
    public void visitFunction(Function node) {
        for (Operand operand : node.args) {
            operand.accept(this);
        }
    }

    @Override
    public void visitOperandList(OperandList node) {
        for (Operand operand : node) {
            operand.accept(this);
        }
    }

}
