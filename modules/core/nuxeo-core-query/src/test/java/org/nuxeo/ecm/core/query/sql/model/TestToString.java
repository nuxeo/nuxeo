/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

/**
 * {@link Expression#toString} is used for debug so it should attempt to not be misleading, in particular in the way it
 * displays parentheses and operator priority.
 *
 * @since 11.4
 */
public class TestToString {

    @Test
    public void testAndOr() {
        var f = Predicates.eq("foo", "f1");
        assertEquals("(foo = 'f1')", f.toString());
        var b1 = Predicates.eq("bar", "b1");
        var b2 = Predicates.eq("bar", "b2");
        var b = Predicates.or(b1, b2);
        assertEquals("((bar = 'b1') OR (bar = 'b2'))", b.toString());
        var p = Predicates.and(f, b);
        assertEquals("((foo = 'f1') AND ((bar = 'b1') OR (bar = 'b2')))", p.toString());
    }

    @Test
    public void testIn() {
        var f = Predicates.in("foo", Collections.emptyList());
        assertEquals("(foo IN ())", f.toString());
        f = Predicates.in("foo", Arrays.asList("f1"));
        assertEquals("(foo IN ('f1'))", f.toString());
        f = Predicates.in("foo", Arrays.asList("f1", "f2"));
        assertEquals("(foo IN ('f1', 'f2'))", f.toString());
    }

    @Test
    public void testIsNull() {
        var f = Predicates.isnull("foo");
        assertEquals("foo IS NULL", f.toString());
        var b = Predicates.isnotnull("bar");
        assertEquals("bar IS NOT NULL", b.toString());
        var p = Predicates.and(f, b);
        assertEquals("(foo IS NULL AND bar IS NOT NULL)", p.toString());
    }

    @Test
    public void testNot() {
        var f = Predicates.eq("foo", "f1");
        var p = Predicates.not(f);
        assertEquals("NOT (foo = 'f1')", p.toString());
    }

    @Test
    public void testMultiExpression() {
        var p = new MultiExpression(Operator.AND, Collections.emptyList());
        assertEquals("AND()", p.toString());
        var f = Predicates.eq("foo", "f1");
        p = new MultiExpression(Operator.AND, Arrays.asList(f));
        assertEquals("(foo = 'f1')", p.toString());
        var b = Predicates.eq("bar", "b1");
        p = new MultiExpression(Operator.AND, Arrays.asList(f, b));
        assertEquals("((foo = 'f1') AND (bar = 'b1'))", p.toString());
        var g = Predicates.eq("gee", "g1");
        p = new MultiExpression(Operator.AND, Arrays.asList(f, b, g));
        assertEquals("((foo = 'f1') AND (bar = 'b1') AND (gee = 'g1'))", p.toString());
    }

}
