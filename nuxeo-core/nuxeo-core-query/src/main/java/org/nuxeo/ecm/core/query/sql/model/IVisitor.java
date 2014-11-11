/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    void visitMultiExpression(MultiExpression node);

    void visitReference(Reference node);

    void visitReferenceList(ReferenceList node);

    void visitQuery(SQLQuery node);

    void visitFunction(Function node);

}
