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

import org.junit.Before;
import org.junit.Test;

public class TestPermissionService {

    PermissionService ptb;

    @Before
    public void setUp() {
        ptb = PermissionService.getInstance();
    }

    @Test
    public void test1() throws ParseException {
        String expr = "a AND b OR c";
        PostfixExpression postfix = new PostfixExpression(expr);
        // System.out.println(postfix.toString().trim());
        Guard root = (Guard) postfix.visit(ptb);
        assertEquals("a b AND c OR", postfix.toString().trim());
        // System.out.println(root.toString());
        assertEquals("((PERM[a] AND PERM[b]) OR PERM[c])", root.toString());
    }

    @Test
    public void test2() throws ParseException {
        String expr = "a AND (b OR c)";
        PostfixExpression postfix = new PostfixExpression(expr);
        Guard root = (Guard) postfix.visit(ptb);
        assertEquals("a b c OR AND", postfix.toString().trim());
        assertEquals("(PERM[a] AND (PERM[b] OR PERM[c]))", root.toString());
    }

    @Test
    public void test3() throws ParseException {
        String expr = "(a OR b) AND (c OR d)";
        PostfixExpression postfix = new PostfixExpression(expr);
        Guard root = (Guard) postfix.visit(ptb);
        assertEquals("a b OR c d OR AND", postfix.toString().trim());
        assertEquals("((PERM[a] OR PERM[b]) AND (PERM[c] OR PERM[d]))", root.toString());
    }

}
