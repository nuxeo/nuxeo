/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.security;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;

import org.junit.Test;

public class TestPostfixExpression {

    @Test
    public void test1() throws ParseException {
        String expr = "a AND b OR d";
        assertEquals("a b AND d OR", new PostfixExpression(expr).toString().trim());
    }

    @Test
    public void test2() throws ParseException {
        String expr = "a OR b AND d";
        assertEquals("a b d AND OR", new PostfixExpression(expr).toString().trim());
    }

    @Test
    public void test3() throws ParseException {
        String expr = "a OR b OR d";
        assertEquals("a b OR d OR", new PostfixExpression(expr).toString().trim());
    }

    @Test
    public void test4() throws ParseException {
        String expr = "(a OR b) AND d";
        assertEquals("a b OR d AND", new PostfixExpression(expr).toString().trim());
    }

    @Test
    public void test5() throws ParseException {
        String expr = "(a OR b) AND (c OR d) OR e";
        assertEquals("a b OR c d OR AND e OR", new PostfixExpression(expr).toString().trim());
    }

    @Test
    public void test6() throws ParseException {
        String expr = "(a AND b OR c) AND ((d OR e) AND f)";
        assertEquals("a b AND c OR d e OR f AND AND", new PostfixExpression(expr).toString().trim());
    }

    @Test
    public void test7() throws ParseException {
        String expr = "a AND b OR NOT c";
        assertEquals("a b AND c NOT OR", new PostfixExpression(expr).toString().trim());
    }

    @Test
    public void test8() throws ParseException {
        String expr = "(a AND b) OR NOT c";
        assertEquals("a b AND c NOT OR", new PostfixExpression(expr).toString().trim());
    }

    @Test
    public void test9() throws ParseException {
        String expr = "(a OR b) AND NOT c";
        assertEquals("a b OR c NOT AND", new PostfixExpression(expr).toString().trim());
    }
}
