/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 *
 */
package org.nuxeo.ecm.platform.audit.api;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operand;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.OrderByList;
import org.nuxeo.ecm.core.query.sql.model.Predicate;

/**
 * Query builder for querying audit.
 *
 * @since 9.3
 */
public class AuditQueryBuilder {

    /**
     * Here filter is a {@link MultiExpression} with operator AND by design.
     */
    protected MultiExpression filter;

    protected OrderByList orders;

    protected long offset = 0;

    // By default retrieve all results
    protected long limit = 0;

    public AuditQueryBuilder() {
        filter = new MultiExpression(Operator.AND, new ArrayList<>());
        orders = new OrderByList(null); // stupid constructor
        orders.clear();
    }

    public Predicate predicate() {
        return filter;
    }

    /**
     * Adds a new predicate to the list of AND predicates.
     */
    public AuditQueryBuilder addAndPredicate(Predicate predicate) {
        filter.values.add(predicate);
        return this;
    }

    /**
     * Sets the predicates to use when querying audit. Filters are composed with an AND operator.
     */
    public AuditQueryBuilder predicates(Predicate filter, Predicate... filters) {
        return predicates(Stream.concat(Stream.of(filter), Stream.of(filters)).collect(Collectors.toList()));
    }

    /**
     * Sets the predicates to use when querying audit. Filters are composed with an AND operator.
     */
    @SuppressWarnings("unchecked")
    public AuditQueryBuilder predicates(List<Predicate> filters) {
        this.filter = new MultiExpression(Operator.AND, (List<Operand>) ((List<?>) filters));
        return this;
    }

    /**
     * We currently only need to handle object instantiated through {@link OrderByExprs}.
     */
    public OrderByList orders() {
        return orders;
    }

    public AuditQueryBuilder defaultOrder() {
        return orders(OrderByExprs.desc(BuiltinLogEntryData.LOG_EVENT_DATE));
    }

    /**
     * Adds a new order to this query builder.
     */
    public AuditQueryBuilder order(OrderByExpr order) {
       this.orders.add(order);
        return this;
    }

    /**
     * Sets the orders to use when querying audit.
     */
    public AuditQueryBuilder orders(OrderByExpr order, OrderByExpr... orders) {
        return orders(Stream.concat(Stream.of(order), Stream.of(orders)).collect(Collectors.toList()));
    }

    /**
     * Sets the orders to use when querying audit.
     */
    public AuditQueryBuilder orders(List<OrderByExpr> orders) {
        this.orders.clear();
        this.orders.addAll(orders);
        return this;
    }

    public long offset() {
        return offset;
    }

    public AuditQueryBuilder offset(long offset) {
        this.offset = offset;
        return this;
    }

    public long limit() {
        return limit;
    }

    public AuditQueryBuilder limit(long limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
