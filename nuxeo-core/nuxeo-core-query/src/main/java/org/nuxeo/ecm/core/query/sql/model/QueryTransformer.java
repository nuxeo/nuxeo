/*
 * (C) Copyright 2017 Nuxeo(http://nuxeo.com/) and others.
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
 * transformor pattern to transform a query into another.
 *
 * @since 9.10
 */
public interface QueryTransformer {

    SQLQuery transform(SQLQuery node);

    SelectClause transform(SelectClause node);

    SelectList transform(SelectList node);

    FromClause transform(FromClause node);

    FromList transform(FromList node);

    WhereClause transform(WhereClause node);

    GroupByClause transform(GroupByClause node);

    HavingClause transform(HavingClause node);

    OrderByClause transform(OrderByClause node);

    OrderByList transform(OrderByList node);

    OrderByExpr transform(OrderByExpr node);

    long transformLimit(long limit);

    long transformOffset(long offset);

    Operand transform(Operand node);

    Expression transform(Expression node);

    Expression transform(MultiExpression node);

    Operator transform(Operator node);

    Reference transform(Reference node);

    ReferenceList transform(ReferenceList node);

    Operand transform(Function node);

    OperandList transform(OperandList node);

    Literal transform(Literal node);

    LiteralList transform(LiteralList node);

    Literal transform(BooleanLiteral node);

    Literal transform(DateLiteral node);

    Literal transform(DoubleLiteral node);

    Literal transform(IntegerLiteral node);

    Literal transform(StringLiteral node);

}
