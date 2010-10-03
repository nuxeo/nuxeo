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

package org.nuxeo.ecm.core.storage.sql.jdbc.dialect;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.DatabaseMetaData;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.storage.sql.BinaryManager;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.jdbc.QueryMaker.QueryMakerException;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect.FulltextQuery;

public class TestDialectQuerySyntax extends TestCase {

    protected static class DatabaseMetaDataInvocationHandler implements
            InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            String name = method.getName();
            if (name.equals("storesUpperCaseIdentifiers")) {
                return Boolean.FALSE;
            } else if (name.equals("getDatabaseMajorVersion")) {
                return Integer.valueOf(0);
            } else if (name.equals("getDatabaseMinorVersion")) {
                return Integer.valueOf(0);
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

    public DatabaseMetaData metadata;

    public BinaryManager binaryManager;

    public RepositoryDescriptor repositoryDescriptor;

    public Dialect dialect;

    @Override
    public void setUp() {
        metadata = getDatabaseMetaData();
        binaryManager = null;
        repositoryDescriptor = new RepositoryDescriptor();
    }

    protected void checkException(String query) throws Exception {
        try {
            dialect.analyzeFulltextQuery(query);
            fail("Query should fail: " + query);
        } catch (QueryMakerException e) {
            // ok
        }
    }

    protected void checkFulltextQuery(String query, List<String> pos,
            List<String> neg, List<List<String>> or) throws Exception {
        if (pos == null) {
            pos = Collections.emptyList();
        }
        if (neg == null) {
            neg = Collections.emptyList();
        }
        if (or == null) {
            or = Collections.emptyList();
        }
        FulltextQuery ft = dialect.analyzeFulltextQuery(query);
        assertEquals(pos, ft.pos);
        assertEquals(neg, ft.neg);
        assertEquals(or, ft.or);
    }

    @SuppressWarnings("unchecked")
    public void testAnalyzeFulltextQuery() throws Exception {
        dialect = new DialectH2(metadata, binaryManager, repositoryDescriptor);

        // invalid queries

        checkException("-foo OR bar");
        checkException("foo OR -bar");
        checkException("foo OR OR bar");
        checkException("foo + bar");
        checkException("foo - bar");
        checkException("foo -bar OR baz");
        checkException("foo bar OR -baz");
        checkException("foo bar OR OR baz");

        // ok queries

        checkFulltextQuery("foo", Arrays.asList("foo"), null, null);
        checkFulltextQuery("foo bar", Arrays.asList("foo", "bar"), null, null);
        checkFulltextQuery("foo bar baz", Arrays.asList("foo", "bar", "baz"),
                null, null);
        checkFulltextQuery("foo -bar", Arrays.asList("foo"),
                Arrays.asList("bar"), null);
        checkFulltextQuery("foo -bar baz", Arrays.asList("foo", "baz"),
                Arrays.asList("bar"), null);
        checkFulltextQuery("foo -bar -baz", Arrays.asList("foo"),
                Arrays.asList("bar", "baz"), null);
        checkFulltextQuery("-foo bar", Arrays.asList("bar"),
                Arrays.asList("foo"), null);
        checkFulltextQuery("-foo bar baz", Arrays.asList("bar", "baz"),
                Arrays.asList("foo"), null);

        // queries with OR

        checkFulltextQuery("foo OR bar", null, null,
                Collections.singletonList(Arrays.asList("foo", "bar")));
        checkFulltextQuery("foo OR bar baz", Arrays.asList("baz"), null,
                Collections.singletonList(Arrays.asList("foo", "bar")));
        checkFulltextQuery("foo bar OR baz", Arrays.asList("foo"), null,
                Collections.singletonList(Arrays.asList("bar", "baz")));
        checkFulltextQuery("foo OR bar OR baz", null, null,
                Collections.singletonList(Arrays.asList("foo", "bar", "baz")));
        checkFulltextQuery(
                "foo OR bar baz OR gee",
                null,
                null,
                Arrays.asList(Arrays.asList("foo", "bar"),
                        Arrays.asList("baz", "gee")));
        checkFulltextQuery("-foo bar OR baz", null, Arrays.asList("foo"),
                Collections.singletonList(Arrays.asList("bar", "baz")));
        checkFulltextQuery("foo OR bar -baz", null, Arrays.asList("baz"),
                Collections.singletonList(Arrays.asList("foo", "bar")));
    }

    protected void check(String expected, String query) {
        assertEquals(expected, dialect.getDialectFulltextQuery(query));
    }

    public void testH2() throws Exception {
        dialect = new DialectH2(metadata, binaryManager, repositoryDescriptor);
        check("foo", "foo");
        check("foo", "foo ");
        check("foo AND bar", "foo    bar ");
        check("foo AND -bar", "foo -bar");
        check("bar AND -foo", "-foo bar");
        check("+DONTMATCHANYTHINGFOREMPTYQUERY", "-foo");
        check("+DONTMATCHANYTHINGFOREMPTYQUERY", "-foo -bar");
        check("(foo OR bar)", "foo OR bar ");
        check("baz AND (foo OR bar)", "baz foo OR bar ");
        check("(foo OR bar) AND -baz", "-baz foo OR bar ");
    }

    public void testPostgreSQL() throws Exception {
        dialect = new DialectPostgreSQL(metadata, binaryManager,
                repositoryDescriptor);
        check("foo", "foo");
        check("foo", "foo ");
        check("foo & bar", "foo    bar ");
        check("foo & bar", "foo  &  bar"); // compat with native queries
        check("foo & !bar", "foo -bar");
        check("bar & !foo", "-foo bar");
        check("!foo", "-foo");
        check("!foo & !bar", "-foo -bar");
        check("(foo | bar)", "foo OR bar ");
        check("baz & (foo | bar)", "baz foo OR bar ");
        check("(foo | bar) & !baz", "-baz foo OR bar ");
    }

    public void testOracle() throws Exception {
        dialect = new DialectOracle(metadata, binaryManager,
                repositoryDescriptor);
        check("foo", "foo");
        check("foo", "foo ");
        check("foo%", "foo*");
        check("foo & bar", "foo    bar ");
        check("foo ~ bar", "foo -bar");
        check("bar ~ foo", "-foo bar");
        check("DONTMATCHANYTHINGFOREMPTYQUERY", "-foo");
        check("DONTMATCHANYTHINGFOREMPTYQUERY", "-foo -bar");
        check("(foo | bar)", "foo OR bar ");
        check("baz & (foo | bar)", "baz foo OR bar ");
        check("(foo | bar) ~ baz", "-baz foo OR bar ");
    }

    public void testSQLServer() throws Exception {
        dialect = new DialectSQLServer(metadata, binaryManager,
                repositoryDescriptor);
        check("foo", "foo");
        check("foo", "foo ");
        check("foo & bar", "foo    bar ");
        check("foo &! bar", "foo -bar");
        check("bar &! foo", "-foo bar");
        check("DONTMATCHANYTHINGFOREMPTYQUERY", "-foo");
        check("DONTMATCHANYTHINGFOREMPTYQUERY", "-foo -bar");
        check("(foo | bar)", "foo OR bar ");
        check("baz & (foo | bar)", "baz foo OR bar ");
        check("(foo | bar) &! baz", "-baz foo OR bar ");
    }

    public void testMySQL() throws Exception {
        dialect = new DialectMySQL(metadata, binaryManager,
                repositoryDescriptor);
        check("+foo", "foo");
        check("+foo", "foo ");
        check("+foo +bar", "foo    bar ");
        check("+foo -bar", "foo -bar");
        check("+bar -foo", "-foo bar");
        check("+DONTMATCHANYTHINGFOREMPTYQUERY", "-foo");
        check("+DONTMATCHANYTHINGFOREMPTYQUERY", "-foo -bar");
        check("+(foo bar)", "foo OR bar ");
        check("+baz +(foo bar)", "baz foo OR bar ");
        check("+(foo bar) -baz", "-baz foo OR bar ");
    }

}
