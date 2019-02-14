/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.storage.FulltextQueryAnalyzer.FulltextQuery;
import org.nuxeo.ecm.core.storage.FulltextQueryAnalyzer.Op;

public class TestFulltextQueryAnalyzer {

    protected void assertFulltextException(String query) {
        try {
            FulltextQueryAnalyzer.analyzeFulltextQuery(query);
            fail("Query should fail: " + query);
        } catch (QueryParseException e) {
            // ok
        }
    }

    protected static void dumpFulltextQuery(FulltextQuery ft, StringBuilder sb) {
        if (ft.op == Op.AND || ft.op == Op.OR) {
            assertNull(ft.word);
            sb.append('[');
            for (int i = 0; i < ft.terms.size(); i++) {
                if (i != 0) {
                    sb.append(' ');
                    sb.append(ft.op.name());
                    sb.append(' ');
                }
                dumpFulltextQuery(ft.terms.get(i), sb);
            }
            sb.append(']');
            return;
        } else {
            assertNull(ft.terms);
            if (ft.op == Op.NOTWORD) {
                sb.append('~');
            }
            boolean isPhrase = ft.word.contains(" ");
            if (isPhrase) {
                sb.append('{');
            }
            sb.append(ft.word);
            if (isPhrase) {
                sb.append('}');
            }
        }
    }

    protected void assertFulltextQuery(String expected, String query) {
        FulltextQuery ft = FulltextQueryAnalyzer.analyzeFulltextQuery(query);
        if (ft == null) {
            assertNull(expected);
        } else {
            StringBuilder sb = new StringBuilder();
            dumpFulltextQuery(ft, sb);
            assertEquals(expected, sb.toString());
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
        assertFulltextQuery("[foo OR [bar AND baz] OR gee]", "foo OR bar baz OR gee");
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
        assertFulltextQuery("[{foo bar} AND ~{baz gee}]", "\"foo bar\" -\"baz gee\"");
    }

}
