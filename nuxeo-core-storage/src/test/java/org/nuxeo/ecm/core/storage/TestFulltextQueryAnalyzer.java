/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.nuxeo.ecm.core.storage.FulltextQueryAnalyzer.FulltextQuery;
import org.nuxeo.ecm.core.storage.FulltextQueryAnalyzer.FulltextQueryException;
import org.nuxeo.ecm.core.storage.FulltextQueryAnalyzer.Op;

public class TestFulltextQueryAnalyzer {

    protected void assertFulltextException(String query) {
        try {
            FulltextQueryAnalyzer.analyzeFulltextQuery(query);
            fail("Query should fail: " + query);
        } catch (FulltextQueryException e) {
            // ok
        }
    }

    protected static void dumpFulltextQuery(FulltextQuery ft, StringBuilder buf) {
        if (ft.op == Op.AND || ft.op == Op.OR) {
            assertNull(ft.word);
            buf.append('[');
            for (int i = 0; i < ft.terms.size(); i++) {
                if (i != 0) {
                    buf.append(' ');
                    buf.append(ft.op.name());
                    buf.append(' ');
                }
                dumpFulltextQuery(ft.terms.get(i), buf);
            }
            buf.append(']');
            return;
        } else {
            assertNull(ft.terms);
            if (ft.op == Op.NOTWORD) {
                buf.append('~');
            }
            boolean isPhrase = ft.word.contains(" ");
            if (isPhrase) {
                buf.append('{');
            }
            buf.append(ft.word);
            if (isPhrase) {
                buf.append('}');
            }
        }
    }

    protected void assertFulltextQuery(String expected, String query) {
        FulltextQuery ft = FulltextQueryAnalyzer.analyzeFulltextQuery(query);
        if (ft == null) {
            assertNull(expected);
        } else {
            StringBuilder buf = new StringBuilder();
            dumpFulltextQuery(ft, buf);
            assertEquals(expected, buf.toString());
        }
    }

    @Test
    public void testAnalyzeFulltextQuery() throws Exception {

        // invalid queries

        assertFulltextException("OR foo");
        assertFulltextException("OR foo bar");
        assertFulltextException("foo OR");
        assertFulltextException("foo bar OR");
        assertFulltextException("foo OR OR bar");
        assertFulltextException("foo bar OR OR baz");
        assertFulltextException("foo + bar");
        assertFulltextException("foo - bar");

        // ok queries

        assertFulltextQuery(null, "");
        assertFulltextQuery(null, "  ");
        assertFulltextQuery("foo", "foo");
        assertFulltextQuery("foo", " foo ");
        assertFulltextQuery("[foo AND bar]", "foo bar");
        assertFulltextQuery("[foo AND bar AND baz]", "foo bar baz");
        assertFulltextQuery("[foo AND ~bar]", "foo -bar");
        assertFulltextQuery("[foo AND baz AND ~bar]", "foo -bar baz");
        assertFulltextQuery("[foo AND ~bar AND ~baz]", "foo -bar -baz");
        assertFulltextQuery("[bar AND ~foo]", "-foo bar");
        assertFulltextQuery("[bar AND baz AND ~foo]", "-foo bar baz");

        // queries with OR

        assertFulltextQuery("[foo OR bar]", "foo OR bar");
        assertFulltextQuery("[foo OR [bar AND baz]]", "foo OR bar baz");
        assertFulltextQuery("[[foo AND bar] OR baz]", "foo bar OR baz");
        assertFulltextQuery("[foo OR bar OR baz]", "foo OR bar OR baz");
        assertFulltextQuery("[foo OR [bar AND baz] OR gee]",
                "foo OR bar baz OR gee");
        assertFulltextQuery("[[bar AND ~foo] OR baz]", "-foo bar OR baz");
        assertFulltextQuery("[foo OR [bar AND ~baz]]", "foo OR bar -baz");

        // queries containing suppressed terms

        assertFulltextQuery(null, "-foo");
        assertFulltextQuery(null, "-foo -bar");
        assertFulltextQuery("bar", "-foo OR bar");
        assertFulltextQuery("foo", "foo OR -bar");
        assertFulltextQuery(null, "-foo OR -bar");
        assertFulltextQuery("foo", "foo OR -bar -baz");
        assertFulltextQuery("baz", "-foo -bar OR baz");

        // query with phrases

        assertFulltextException("\"foo");
        assertFulltextException("\"foo bar");
        assertFulltextException("\"fo\"o\"");

        assertFulltextQuery(null, "\"\"");
        assertFulltextQuery(null, " \" \" ");
        assertFulltextQuery("foo", "\"foo\"");
        assertFulltextQuery("foo", "+\"foo\"");
        assertFulltextQuery(null, "-\"foo\"");
        assertFulltextQuery("foo", "\" foo\"");
        assertFulltextQuery("foo", "\"foo \"");
        assertFulltextQuery("foo", "\" foo \"");
        assertFulltextQuery("OR", "\"OR\"");
        assertFulltextQuery("{foo bar}", "\"foo bar\"");
        assertFulltextQuery("{foo bar}", "\" foo bar\"");
        assertFulltextQuery("{foo bar}", "\"foo bar \"");
        assertFulltextQuery("{foo bar}", "\" foo  bar \"");
        assertFulltextQuery("{foo bar}", "+\"foo bar\"");
        assertFulltextQuery("{foo or bar}", "\"foo or bar\"");
        assertFulltextQuery(null, "-\"foo bar\"");
        assertFulltextQuery("[foo AND {bar baz}]", "foo \"bar baz\"");
        assertFulltextQuery("[foo AND {bar baz}]", "foo +\"bar baz\"");
        assertFulltextQuery("[foo AND ~{bar baz}]", "foo -\"bar baz\"");
        assertFulltextQuery("[{foo bar} AND baz]", "\"foo bar\" baz");
        assertFulltextQuery("[{foo bar} AND baz]", "+\"foo bar\" baz");
        assertFulltextQuery("[baz AND ~{foo bar}]", "-\"foo bar\" baz");
        assertFulltextQuery("[{foo bar} AND ~{baz gee}]",
                "\"foo bar\" -\"baz gee\"");
    }

}
