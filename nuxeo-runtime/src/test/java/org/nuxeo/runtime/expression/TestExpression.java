/* 
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
