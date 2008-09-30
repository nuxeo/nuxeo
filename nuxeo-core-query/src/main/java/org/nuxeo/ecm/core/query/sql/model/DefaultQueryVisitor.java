/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.query.sql.model;

/**
 * A default implementation of a visitor that visits depth-first in standard
 * expression order.
 *
 * @author Florent Guillaume
 */
public class DefaultQueryVisitor implements IVisitor {

    private static final long serialVersionUID = 1L;

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

    public void visitSelectClause(SelectClause node) {
        SelectList elements = node.elements;
        for (int i = 0; i < elements.size(); i++) {
            elements.get(i).accept(this);
        }
    }

    public void visitFromClause(FromClause node) {
    }

    public void visitWhereClause(WhereClause node) {
        node.predicate.accept(this);
    }

    public void visitGroupByClause(GroupByClause node) {
    }

    public void visitHavingClause(HavingClause node) {
        if (node.predicate != null) {
            node.predicate.accept(this);
        }
    }

    public void visitOrderByClause(OrderByClause node) {
        node.elements.accept(this);
    }

    public void visitOrderByList(OrderByList node) {
        for (int i = 0; i < node.size(); i++) {
            node.get(i).accept(this);
        }
    }

    public void visitOrderByExpr(OrderByExpr node) {
        node.reference.accept(this);
    }

    public void visitExpression(Expression node) {
        node.lvalue.accept(this);
        node.operator.accept(this);
        node.rvalue.accept(this);
    }

    public void visitOperator(Operator node) {
    }

    public void visitReference(Reference node) {
    }

    public void visitReferenceList(ReferenceList node) {
        for (Reference reference : node) {
            reference.accept(this);
        }
    }

    public void visitLiteral(Literal node) {
    }

    public void visitLiteralList(LiteralList node) {
        for (Literal literal : node) {
            literal.accept(this);
        }
    }

    public void visitDateLiteral(DateLiteral node) {
        visitLiteral(node);
    }

    public void visitStringLiteral(StringLiteral node) {
        visitLiteral(node);
    }

    public void visitDoubleLiteral(DoubleLiteral node) {
        visitLiteral(node);
    }

    public void visitIntegerLiteral(IntegerLiteral node) {
        visitLiteral(node);
    }

    public void visitFunction(Function node) {
        for (Operand operand : node.args) {
            operand.accept(this);
        }
    }

    public void visitOperandList(OperandList node) {
        for (Operand operand : node) {
            operand.accept(this);
        }
    }

}
