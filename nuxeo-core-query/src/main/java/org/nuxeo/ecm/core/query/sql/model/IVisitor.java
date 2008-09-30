/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.query.sql.model;

import java.io.Serializable;

/**
 * @author Bogdan Stefanescu
 * @author Florent Guillaume
 */
public interface IVisitor extends Serializable {

    void visitLiteral(Literal node);

    void visitLiteralList(LiteralList node);

    void visitDateLiteral(DateLiteral node);

    void visitStringLiteral(StringLiteral node);

    void visitDoubleLiteral(DoubleLiteral node);

    void visitIntegerLiteral(IntegerLiteral node);

    void visitOperandList(OperandList node);

    void visitOperator(Operator node);

    void visitSelectClause(SelectClause node);

    void visitFromClause(FromClause node);

    void visitWhereClause(WhereClause node);

    void visitOrderByClause(OrderByClause node);

    void visitOrderByList(OrderByList node);

    void visitOrderByExpr(OrderByExpr node);

    void visitGroupByClause(GroupByClause node);

    void visitHavingClause(HavingClause node);

    void visitExpression(Expression node);

    void visitReference(Reference node);

    void visitReferenceList(ReferenceList node);

    void visitQuery(SQLQuery node);

    void visitFunction(Function node);

}
