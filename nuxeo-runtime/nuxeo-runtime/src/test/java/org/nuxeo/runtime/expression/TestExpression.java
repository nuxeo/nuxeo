/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.expression;

import junit.framework.TestCase;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author  <a href="mailto:rspivak@nuxeo.com">Ruslan Spivak</a>
 *
 */
public class TestExpression extends TestCase {

    public void testBoolean() throws Exception {
        Context ctx = new Context();
        ctx.put("doc", true);
        assertExpression(ctx, "doc == true", Boolean.TRUE);

        ctx.put("doc", false);
        assertExpression(ctx, "doc == true", Boolean.FALSE);
    }

    public void testExpression() throws Exception {
        Context ctx = new Context();
        ctx.put("number", 1);
        assertExpression(ctx, "number == 4", Boolean.FALSE);
        assertExpression(ctx, "number+3 == 4", Boolean.TRUE);
        assertExpression(ctx, "number+3 > 4", Boolean.FALSE);

        ctx.put("user", "Bogdan");
        assertExpression(ctx, "user == 'Bogdan'", Boolean.TRUE);
        assertExpression(ctx, "user != 'Me'", Boolean.TRUE);
    }

    public void testPropertyAccess() throws Exception {
        Context ctx = new Context();
        ctx.put("object", new FakeObject());
        ctx.put("principal", new FakePrincipal());
        assertExpression(ctx, "principal.group", "members");
        assertExpression(ctx, "principal.name", "Frodo");
        assertExpression(ctx, "object.getType()", "File");
        assertExpression(ctx, "principal.group == 'members'", Boolean.TRUE);

        assertExpression(ctx, "object.getType() == 'File' and principal.group == 'members'",
                Boolean.TRUE);
        assertExpression(ctx, "object.square(3)", 9);
    }

    protected static void assertExpression(Context ctx, String expression,
            Object expected) throws Exception {
        Expression e = new JexlExpression(expression);
        Object actual = e.eval(ctx);
        assertEquals(expression, expected, actual);
    }

    public static class FakeObject {
        public String getType() {
            return "File";
        }

        public Integer square(int value) {
            return value * value;
        }
    }

    public static class FakePrincipal {
        public String getName() {
            return "Frodo";
        }

        public String getGroup() {
            return "members";
        }
    }

}
