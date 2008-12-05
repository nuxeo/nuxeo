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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.security;

import java.text.ParseException;

import junit.framework.TestCase;

public class TestPostfixExpression extends TestCase {

    public void test1() throws ParseException {
        String expr = "a AND b OR d";
        assertEquals("a b AND d OR", new PostfixExpression(expr).toString().trim());
    }

    public void test2() throws ParseException {
        String expr = "a OR b AND d";
        assertEquals("a b d AND OR", new PostfixExpression(expr).toString().trim());
    }

    public void test3() throws ParseException {
        String expr = "a OR b OR d";
        assertEquals("a b OR d OR", new PostfixExpression(expr).toString().trim());
    }

    public void test4() throws ParseException {
        String expr = "(a OR b) AND d";
        assertEquals("a b OR d AND", new PostfixExpression(expr).toString().trim());
    }

    public void test5() throws ParseException {
        String expr = "(a OR b) AND (c OR d) OR e";
        assertEquals("a b OR c d OR AND e OR", new PostfixExpression(expr).toString().trim());
    }

    public void test6() throws ParseException {
        String expr = "(a AND b OR c) AND ((d OR e) AND f)";
        assertEquals("a b AND c OR d e OR f AND AND", new PostfixExpression(expr).toString().trim());
    }

    public void test7() throws ParseException {
        String expr = "a AND b OR NOT c";
        assertEquals("a b AND c NOT OR", new PostfixExpression(expr).toString().trim());
    }

    public void test8() throws ParseException {
        String expr = "(a AND b) OR NOT c";
        assertEquals("a b AND c NOT OR", new PostfixExpression(expr).toString().trim());
    }

    public void test9() throws ParseException {
        String expr = "(a OR b) AND NOT c";
        assertEquals("a b OR c NOT AND", new PostfixExpression(expr).toString().trim());
    }
}
