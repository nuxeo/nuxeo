/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.usermanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.platform.usermanager.DefaultUserMultiTenantManagement.QueryTenantAdder;

public class TestQueryTenantAdder {

    protected static final String GROUP = "group";

    @Test
    public void testQueryTenantAdder() {
        Expression expr = Predicates.eq("foo", "bar");
        check("foo = 'bar'", expr);
        expr = Predicates.eq(GROUP, "admins");
        check("group = 'admins-tenantA'", expr);
        expr = Predicates.and(Predicates.eq("foo", "bar"), Predicates.eq(GROUP, "admins"));
        check("foo = 'bar' AND group = 'admins-tenantA'", expr);
        expr = new MultiExpression(Operator.AND,
                Arrays.asList(Predicates.eq("foo", "bar"), Predicates.eq(GROUP, "admins")));
        check("AND(foo = 'bar', group = 'admins-tenantA')", expr);
        expr = Predicates.noteq(GROUP, "admins");
        check("group <> 'admins-tenantA'", expr);
        expr = Predicates.in(GROUP, "admins", "supers");
        check("group IN 'admins-tenantA', 'supers-tenantA'", expr); // toString is for debug, not for real NXQL
        expr = Predicates.notin(GROUP, "losers");
        check("group NOT IN 'losers-tenantA'", expr);

        // error cases

        try {
            expr = Predicates.like(GROUP, "admins"); // only EQ, NOTEQ, IN and NOTIN are accepted
            check("fail", expr);
            fail("should not parse");
        } catch (QueryParseException e) {
            assertEquals("Cannot evaluate expression in multi-tenant mode", e.getMessage());
        }

    }

    protected void check(String expected, Expression expr) {
        QueryTenantAdder qta = new QueryTenantAdder(GROUP, "-tenantA");
        assertEquals(expected, qta.transform(expr).toString());
    }

}
