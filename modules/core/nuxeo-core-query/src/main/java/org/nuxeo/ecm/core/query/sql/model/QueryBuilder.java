/*
 * (C) Copyright 2017-2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.query.sql.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Query builder for a query, including ordering, limit and offset.
 *
 * @since 10.3
 */
public class QueryBuilder {

    protected MultiExpression filter;

    protected OrderByList orders;

    protected long offset;

    protected long limit;

    protected boolean countTotal;

    public QueryBuilder() {
        filter = new MultiExpression(Operator.AND, new ArrayList<>());
        orders = new OrderByList();
    }

    /** Copy constructor. */
    public QueryBuilder(QueryBuilder other) {
        filter = new MultiExpression(other.filter);
        orders = new OrderByList(other.orders);
        offset = other.offset;
        limit = other.limit;
        countTotal = other.countTotal;
    }

    public MultiExpression predicate() {
        return filter;
    }

    /**
     * Adds a new predicate to the list of AND predicates.
     */
    public QueryBuilder and(Predicate predicate) {
        if (filter.predicates.isEmpty()) {
            throw new IllegalStateException("Cannot AND without a first predicate");
        }
        if (filter.operator != Operator.AND) {
            if (filter.predicates.size() == 1) {
                filter = new MultiExpression(Operator.AND, filter.predicates);
            } else {
                throw new IllegalStateException("Not an AND MultiExpression");
            }
        }
        return predicate(predicate);
    }

    /**
     * Adds a new predicate to the list of OR predicates.
     */
    public QueryBuilder or(Predicate predicate) {
        if (filter.predicates.isEmpty()) {
            throw new IllegalStateException("Cannot OR without a first predicate");
        }
        if (filter.operator != Operator.OR) {
            if (filter.predicates.size() == 1) {
                filter = new MultiExpression(Operator.OR, filter.predicates);
            } else {
                throw new IllegalStateException("Not an OR MultiExpression");
            }
        }
        return predicate(predicate);
    }

    /**
     * Adds a new predicate to the list.
     */
    public QueryBuilder predicate(Predicate predicate) {
        filter.predicates.add(predicate);
        return this;
    }

    /**
     * Sets the filter.
     */
    public QueryBuilder filter(MultiExpression filter) {
        this.filter = new MultiExpression(filter);
        return this;
    }

    /**
     * We currently only need to handle object instantiated through {@link OrderByExprs}.
     */
    public OrderByList orders() {
        return orders;
    }

    public QueryBuilder defaultOrder() {
        return this;
    }

    /**
     * Adds a new order to this query builder.
     */
    public QueryBuilder order(OrderByExpr order) {
        this.orders.add(order);
        return this;
    }

    /**
     * Sets the orders to use when querying audit.
     */
    public QueryBuilder orders(OrderByExpr order, OrderByExpr... orders) {
        return orders(Stream.concat(Stream.of(order), Stream.of(orders)).collect(Collectors.toList()));
    }

    /**
     * Sets the orders to use when querying audit.
     */
    public QueryBuilder orders(List<OrderByExpr> orders) {
        this.orders.clear();
        this.orders.addAll(orders);
        return this;
    }

    public long offset() {
        return offset;
    }

    public QueryBuilder offset(long offset) {
        this.offset = offset;
        return this;
    }

    public long limit() {
        return limit;
    }

    public QueryBuilder limit(long limit) {
        this.limit = limit;
        return this;
    }

    /**
     * May be used by <b>supported APIs</b> to include in the query result a count of total results if there was no
     * limit or offset.
     * <p>
     * If {@code true}, requests computation of the total size of the underlying list (the size if there was no limit or
     * offset), otherwise when {@code false} does a best effort but may return {@code -2} when unknown
     */
    public boolean countTotal() {
        return countTotal;
    }

    public QueryBuilder countTotal(boolean countTotal) {
        this.countTotal = countTotal;
        return this;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
