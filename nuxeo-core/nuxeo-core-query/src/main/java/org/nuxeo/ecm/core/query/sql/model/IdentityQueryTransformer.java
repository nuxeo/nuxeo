/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.nuxeo.ecm.core.query.QueryParseException;

/**
 * Transforms a query into the same query, doing a copy in the process.
 * <p>
 * Can be used as a base class for more complex transformations.
 *
 * @since 9.10
 */
public class IdentityQueryTransformer implements QueryTransformer {

    @Override
    public SQLQuery transform(SQLQuery node) {
        SelectClause select = transform(node.select);
        FromClause from = transform(node.from);
        WhereClause where = transform(node.where);
        GroupByClause groupBy = transform(node.groupBy);
        HavingClause having = transform(node.having);
        OrderByClause orderBy = transform(node.orderBy);
        long limit = transformLimit(node.limit);
        long offset = transformOffset(node.offset);
        return new SQLQuery(select, from, where, groupBy, having, orderBy, limit, offset);
    }

    @Override
    public SelectClause transform(SelectClause node) {
        return new SelectClause(transform(node.elements), node.distinct);
    }

    @Override
    public SelectList transform(SelectList node) {
        SelectList list = new SelectList();
        for (Entry<String, Operand> es : node.entrySet()) {
            list.put(es.getKey(), transform(es.getValue()));
        }
        return list;
    }

    @Override
    public FromClause transform(FromClause node) {
        return new FromClause(transform(node.elements));
    }

    @Override
    public FromList transform(FromList node) {
        FromList list = new FromList();
        list.putAll(node);
        return list;
    }

    @Override
    public WhereClause transform(WhereClause node) {
        return new WhereClause(transform(node.predicate));
    }

    @Override
    public GroupByClause transform(GroupByClause node) {
        if (node == null) {
            return null;
        }
        String[] elements = node.elements;
        if (elements != null) {
            return new GroupByClause(Arrays.asList(elements));
        } else {
            return new GroupByClause();
        }
    }

    @Override
    public HavingClause transform(HavingClause node) {
        if (node == null) {
            return null;
        }
        return new HavingClause(transform(node.predicate));
    }

    @Override
    public OrderByClause transform(OrderByClause node) {
        if (node == null) {
            return null;
        }
        return new OrderByClause(transform(node.elements));
    }

    @Override
    public OrderByList transform(OrderByList node) {
        OrderByList list = new OrderByList(null); // stupid constructor
        list.clear();
        for (OrderByExpr value : node) {
            list.add(transform(value));
        }
        return list;
    }

    @Override
    public OrderByExpr transform(OrderByExpr node) {
        return new OrderByExpr(transform(node.reference), node.isDescending);
    }

    @Override
    public long transformLimit(long limit) {
        return limit;
    }

    @Override
    public long transformOffset(long offset) {
        return offset;
    }

    @Override
    public Operand transform(Operand node) {
        if (node instanceof Literal) {
            return transform((Literal) node);
        } else if (node instanceof LiteralList) {
            return transform((LiteralList) node);
        } else if (node instanceof Function) {
            return transform((Function) node);
        } else if (node instanceof Expression) {
            return transform((Expression) node);
        } else if (node instanceof Reference) {
            return transform((Reference) node);
        } else {
            throw new QueryParseException("Unknown operand: " + node);
        }
    }

    @Override
    public Expression transform(Expression node) {
        Expression expr = new Expression(transform(node.lvalue), transform(node.operator), transform(node.rvalue));
        expr.info = node.info;
        return expr;
    }

    @Override
    public Expression transform(MultiExpression node) {
        List<Operand> list = new ArrayList<>(node.values.size());
        for (Operand o : node.values) {
            list.add(transform(o));
        }
        MultiExpression expr = new MultiExpression(transform(node.operator), list);
        expr.info = node.info;
        return expr;
    }

    @Override
    public Operator transform(Operator node) {
        return node;
    }

    @Override
    public Reference transform(Reference node) {
        Reference ref = new Reference(node.name, node.cast, node.esHint);
        ref.info = node.info;
        return ref;
    }

    @Override
    public ReferenceList transform(ReferenceList node) {
        ReferenceList list = new ReferenceList();
        for (Reference ref : node) {
            list.add(transform(ref));
        }
        return list;
    }

    @Override
    public Operand transform(Function node) {
        return new Function(node.name, transform(node.args));
    }

    @Override
    public OperandList transform(OperandList node) {
        OperandList list = new OperandList();
        for (Operand oper : node) {
            list.add(transform(oper));
        }
        return list;
    }

    @Override
    public Literal transform(Literal node) {
        if (node instanceof BooleanLiteral) {
            return transform((BooleanLiteral) node);
        } else if (node instanceof DateLiteral) {
            return transform((DateLiteral) node);
        } else if (node instanceof DoubleLiteral) {
            return transform((DoubleLiteral) node);
        } else if (node instanceof IntegerLiteral) {
            return transform((IntegerLiteral) node);
        } else if (node instanceof StringLiteral) {
            return transform((StringLiteral) node);
        } else {
            throw new QueryParseException("Unknown literal: " + node);
        }
    }

    @Override
    public LiteralList transform(LiteralList node) {
        LiteralList list = new LiteralList();
        for (Literal lit : node) {
            list.add(transform(lit));
        }
        return list;
    }

    @Override
    public Literal transform(BooleanLiteral node) {
        return node;
    }

    @Override
    public Literal transform(DateLiteral node) {
        return node;
    }

    @Override
    public Literal transform(DoubleLiteral node) {
        return node;
    }

    @Override
    public Literal transform(IntegerLiteral node) {
        return node;
    }

    @Override
    public Literal transform(StringLiteral node) {
        return node;
    }

}
