/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.query.sql.model;

/**
 * @author Bogdan Stefanescu
 * @author Florent Guillaume
 */
public interface IVisitor {

    void visitLiteral(Literal node);

    void visitLiteralList(LiteralList node);

    void visitDateLiteral(DateLiteral node);

    void visitStringLiteral(StringLiteral node);

    void visitDoubleLiteral(DoubleLiteral node);

    void visitIntegerLiteral(IntegerLiteral node);

    void visitBooleanLiteral(BooleanLiteral node);

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
