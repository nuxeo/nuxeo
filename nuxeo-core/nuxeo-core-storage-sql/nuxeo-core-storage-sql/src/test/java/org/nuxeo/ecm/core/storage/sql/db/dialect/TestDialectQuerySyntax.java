/*
 * (C) Copyright 2008-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.db.dialect;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.DatabaseMetaData;

import junit.framework.TestCase;

import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;

public class TestDialectQuerySyntax extends TestCase {

    protected static class DatabaseMetaDataInvocationHandler implements
            InvocationHandler {
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            if (method.getName().equals("storesUpperCaseIdentifiers")) {
                return Boolean.FALSE;
            }
            return null;
        }
    }

    public static DatabaseMetaData getDatabaseMetaData() {
        return (DatabaseMetaData) Proxy.newProxyInstance(
                Session.class.getClassLoader(),
                new Class<?>[] { DatabaseMetaData.class },
                new DatabaseMetaDataInvocationHandler());
    }

    public Dialect dialect;

    public void check(String expected, String query) {
        assertEquals(expected, dialect.getDialectFulltextQuery(query));
    }

    public void testH2() throws Exception {
        dialect = new DialectH2(getDatabaseMetaData(),
                new RepositoryDescriptor());
        check("+foo", "foo");
        check("+foo +bar", "foo    bar");
        check("+foo -bar", "foo -bar");
        check("+bar -foo", "-foo bar");
        check("+DONTMATCHANYTHINGFOREMPTYQUERY", "-foo -bar");
    }

    public void testPostgreSQL() throws Exception {
        dialect = new DialectPostgreSQL(getDatabaseMetaData(),
                new RepositoryDescriptor());
        check("foo", "foo");
        check("foo & bar", "foo    bar");
        check("foo & bar", "foo  &  bar"); // compat with native queries
        check("foo & !bar", "foo -bar");
        check("!foo & bar", "-foo bar");
        check("!foo & !bar", "-foo -bar");
    }

    public void testOracle() throws Exception {
        dialect = new DialectOracle(getDatabaseMetaData(),
                new RepositoryDescriptor());
        check("foo", "foo");
        check("foo%", "foo*");
        check("foo & bar", "foo    bar");
        check("foo ~ bar", "foo -bar");
        check("bar ~ foo", "-foo bar");
        check("DONTMATCHANYTHINGFOREMPTYQUERY", "-foo -bar");
    }

    public void testSQLServer() throws Exception {
        dialect = new DialectSQLServer(getDatabaseMetaData(),
                new RepositoryDescriptor());
        check("foo", "foo");
        check("foo & bar", "foo    bar");
        check("foo &! bar", "foo -bar");
        check("bar &! foo", "-foo bar");
        check("DONTMATCHANYTHINGFOREMPTYQUERY", "-foo -bar");
    }

    public void testMySQL() throws Exception {
        dialect = new DialectMySQL(getDatabaseMetaData(),
                new RepositoryDescriptor());
        check("+foo", "foo");
        check("+foo +bar", "foo    bar");
        check("+foo -bar", "foo -bar");
        check("+bar -foo", "-foo bar");
        check("+DONTMATCHANYTHINGFOREMPTYQUERY", "-foo -bar");
    }

}
