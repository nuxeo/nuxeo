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

public class TestPermissionService extends TestCase {

    PermissionService ptb;

    @Override
    public void setUp() {
        ptb = PermissionService.getInstance();
    }

    public void test1() throws ParseException {
        String expr = "a AND b OR c";
        PostfixExpression postfix = new PostfixExpression(expr);
        // System.out.println(postfix.toString().trim());
        Guard root = (Guard) postfix.visit(ptb);
        assertEquals("a b AND c OR", postfix.toString().trim());
        // System.out.println(root.toString());
        assertEquals("((PERM[a] AND PERM[b]) OR PERM[c])", root.toString());
    }

    public void test2() throws ParseException {
        String expr = "a AND (b OR c)";
        PostfixExpression postfix = new PostfixExpression(expr);
        Guard root = (Guard) postfix.visit(ptb);
        assertEquals("a b c OR AND", postfix.toString().trim());
        assertEquals("(PERM[a] AND (PERM[b] OR PERM[c]))", root.toString());
    }

    public void test3() throws ParseException {
        String expr = "(a OR b) AND (c OR d)";
        PostfixExpression postfix = new PostfixExpression(expr);
        Guard root = (Guard) postfix.visit(ptb);
        assertEquals("a b OR c d OR AND", postfix.toString().trim());
        assertEquals("((PERM[a] OR PERM[b]) AND (PERM[c] OR PERM[d]))", root.toString());
    }

}
