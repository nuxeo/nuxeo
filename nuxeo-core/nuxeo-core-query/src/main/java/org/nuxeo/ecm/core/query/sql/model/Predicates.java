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
package org.nuxeo.ecm.core.query.sql.model;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Predicate builders.
 *
 * @since 9.3
 */
public class Predicates {

    private Predicates() {
        // no instantiation allowed
    }

    /** @since 10.3 */
    public static Predicate and(Predicate left, Predicate right) {
        return new Predicate(left, Operator.AND, right);
    }

    /** @since 10.3 */
    public static Predicate or(Predicate left, Predicate right) {
        return new Predicate(left, Operator.OR, right);
    }

    /** @since 10.3 */
    public static Predicate not(Predicate predicate) {
        return new Predicate(predicate, Operator.NOT, null);
    }

    public static Predicate eq(String name, Object value) {
        return createPredicate(name, Operator.EQ, value);
    }

    /** @since 10.3 */
    public static Predicate noteq(String name, Object value) {
        return createPredicate(name, Operator.NOTEQ, value);
    }

    public static Predicate lt(String name, Object value) {
        return createPredicate(name, Operator.LT, value);
    }

    public static Predicate lte(String name, Object value) {
        return createPredicate(name, Operator.LTEQ, value);
    }

    public static Predicate gte(String name, Object value) {
        return createPredicate(name, Operator.GTEQ, value);
    }

    public static Predicate gt(String name, Object value) {
        return createPredicate(name, Operator.GT, value);
    }

    public static Predicate startsWith(String name, Object value) {
        return createPredicate(name, Operator.STARTSWITH, value);
    }

    /** @since 10.3 */
    public static Predicate like(String name, Object value) {
        return createPredicate(name, Operator.LIKE, value);
    }

    /** @since 10.3 */
    public static Predicate notlike(String name, Object value) {
        return createPredicate(name, Operator.NOTLIKE, value);
    }

    /** @since 10.3 */
    public static Predicate ilike(String name, Object value) {
        return createPredicate(name, Operator.ILIKE, value);
    }

    /** @since 10.3 */
    public static Predicate notilike(String name, Object value) {
        return createPredicate(name, Operator.NOTILIKE, value);
    }


    public static Predicate in(String name, Iterable<?> values) {
        return createPredicate(name, Operator.IN, StreamSupport.stream(values.spliterator(), false));
    }

    public static <T> Predicate in(String name, T value, T... values) {
        return createPredicate(name, Operator.IN, Stream.concat(Stream.of(value), Stream.of(values)));
    }

    public static Predicate in(String name, Object[] values) {
        return createPredicate(name, Operator.IN, Stream.of(values));
    }

    /** @since 10.3 */
    public static Predicate notin(String name, Iterable<?> values) {
        return createPredicate(name, Operator.NOTIN, StreamSupport.stream(values.spliterator(), false));
    }

    /** @since 10.3 */
    public static <T> Predicate notin(String name, T value, T... values) {
        return createPredicate(name, Operator.NOTIN, Stream.concat(Stream.of(value), Stream.of(values)));
    }

    /** @since 10.3 */
    public static Predicate notin(String name, Object[] values) {
        return createPredicate(name, Operator.NOTIN, Stream.of(values));
    }

    /** @since 10.3 */
    public static Predicate between(String name, Object min, Object max) {
        LiteralList range = new LiteralList();
        range.add(Literals.toLiteral(min));
        range.add(Literals.toLiteral(max));
        return new Predicate(new Reference(name), Operator.BETWEEN, range);
    }

    /** @since 10.3 */
    public static Predicate notbetween(String name, Object min, Object max) {
        LiteralList range = new LiteralList();
        range.add(Literals.toLiteral(min));
        range.add(Literals.toLiteral(max));
        return new Predicate(new Reference(name), Operator.NOTBETWEEN, range);
    }

    /** @since 10.3 */
    public static Predicate isnull(String name) {
        return new Predicate(new Reference(name), Operator.ISNULL, null);
    }

    /** @since 10.3 */
    public static Predicate isnotnull(String name) {
        return new Predicate(new Reference(name), Operator.ISNOTNULL, null);
    }

    private static Predicate createPredicate(String name, Operator operator, Object value) {
        return new Predicate(new Reference(name), operator, Literals.toLiteral(value));
    }

    private static Predicate createPredicate(String name, Operator operator, Stream<?> values) {
        return new Predicate(new Reference(name), operator,
                values.map(Literals::toLiteral).collect(Collectors.toCollection(LiteralList::new)));
    }

}
