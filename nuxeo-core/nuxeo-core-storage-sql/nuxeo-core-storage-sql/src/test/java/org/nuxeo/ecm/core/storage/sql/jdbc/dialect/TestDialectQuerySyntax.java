/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.jdbc.dialect;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.nuxeo.ecm.core.storage.sql.BinaryManager;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.jdbc.QueryMaker.QueryMakerException;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect.FulltextQuery;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect.FulltextQuery.Op;

public class TestDialectQuerySyntax extends MockObjectTestCase {

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

    public DatabaseMetaData getDatabaseMetaData() {
        Mock m = mock(DatabaseMetaData.class);
        m.stubs().method("storesUpperCaseIdentifiers").will(returnValue(false));
        m.stubs().method("getDatabaseMajorVersion").will(returnValue(9));
        m.stubs().method("getDatabaseMinorVersion").will(returnValue(0));
        m.stubs().method("getColumns").will(returnValue(getEmptyResultSet()));
        return (DatabaseMetaData) m.proxy();
    }

    public ResultSet getEmptyResultSet() {
        Mock m = mock(ResultSet.class);
        m.stubs().method("next").will(returnValue(false));
        return (ResultSet) m.proxy();
    }

    protected static void assertFulltextException(String query) {
        try {
            Dialect.analyzeFulltextQuery(query);
            fail("Query should fail: " + query);
        } catch (QueryMakerException e) {
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

    protected static void assertFulltextQuery(String expected, String query) {
        FulltextQuery ft = Dialect.analyzeFulltextQuery(query);
        if (ft == null) {
            assertNull(expected);
        } else {
            StringBuilder buf = new StringBuilder();
            dumpFulltextQuery(ft, buf);
            assertEquals(expected, buf.toString());
        }
    }

    protected static void assertPGPhraseBreak(String expected, String query) {
        FulltextQuery ft = Dialect.analyzeFulltextQuery(query);
        FulltextQuery broken = DialectPostgreSQL.breakPhrases(ft);
        StringBuilder buf = new StringBuilder();
        dumpFulltextQuery(broken, buf);
        assertEquals(expected, buf.toString());
    }

    protected static void assertPGRemoveToplevelAndedWord(String expected,
            String query) {
        FulltextQuery ft = Dialect.analyzeFulltextQuery(query);
        FulltextQuery simplified = DialectPostgreSQL.removeToplevelAndedWords(ft);
        if (simplified == null) {
            assertNull(expected);
        } else {
            StringBuilder buf = new StringBuilder();
            dumpFulltextQuery(simplified, buf);
            assertEquals(expected, buf.toString());
        }
    }

    protected static void assertPGLikeSql(String expected, String query) {
        FulltextQuery ft = Dialect.analyzeFulltextQuery(query);
        StringBuilder buf = new StringBuilder();
        DialectPostgreSQL.generateLikeSql(ft, buf);
        assertEquals(expected, buf.toString());
    }

    public void testAnalyzeFulltextQuery() throws Exception {
        dialect = new DialectH2(metadata, binaryManager, repositoryDescriptor);

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

    protected void assertDialectFT(String expected, String query) {
        assertEquals(expected, dialect.getDialectFulltextQuery(query));
    }

    public void testH2() throws Exception {
        dialect = new DialectH2(metadata, binaryManager, repositoryDescriptor);
        assertDialectFT("DONTMATCHANYTHINGFOREMPTYQUERY", "");
        assertDialectFT("foo", "foo");
        assertDialectFT("(foo AND bar)", "foo bar");
        assertDialectFT("(foo NOT bar)", "foo -bar");
        assertDialectFT("(bar NOT foo)", "-foo bar");
        assertDialectFT("(foo OR bar)", "foo OR bar");
        assertDialectFT("foo", "foo OR -bar");
        assertDialectFT("((foo AND bar) OR baz)", "foo bar OR baz");
        assertDialectFT("((bar NOT foo) OR baz)", "-foo bar OR baz");
        assertDialectFT("((foo NOT bar) OR baz)", "foo -bar OR baz");
        assertDialectFT("\"foo bar\"", "\"foo bar\"");
        assertDialectFT("(\"foo bar\" AND baz)", "\"foo bar\" baz");
        assertDialectFT("(\"foo bar\" OR baz)", "\"foo bar\" OR baz");
        assertDialectFT("((\"foo bar\" AND baz) OR \"gee man\")",
                "\"foo bar\" baz OR \"gee man\"");
        assertDialectFT("(\"foo bar\" NOT \"gee man\")",
                "\"foo bar\" -\"gee man\"");
    }

    public void testPostgreSQLPhraseBreak() throws Exception {
        assertPGPhraseBreak("foo", "foo");
        assertPGPhraseBreak("[foo AND bar]", "\"foo bar\"");
        assertPGPhraseBreak("[foo AND bar AND baz]", "\"foo bar\" baz");
        assertPGPhraseBreak("[foo AND bar AND baz]", "foo \"bar baz\"");
        assertPGPhraseBreak("[[foo AND bar] OR baz]", "\"foo bar\" OR baz");
        assertPGPhraseBreak("[[foo AND bar] OR [gee AND man]]",
                "\"foo bar\" OR \"gee man\"");
        assertPGPhraseBreak("foo", "foo -\"bar baz\"");
        assertPGPhraseBreak("foo", "foo OR -\"bar baz\"");
    }

    public void testPostgreSQLToplevelAndedWordRemoval() throws Exception {
        assertPGRemoveToplevelAndedWord(null, "foo");
        assertPGRemoveToplevelAndedWord(null, "foo bar");
        assertPGRemoveToplevelAndedWord("{foo bar}", "\"foo bar\"");
        assertPGRemoveToplevelAndedWord("{foo bar}", "\"foo bar\" baz");
        assertPGRemoveToplevelAndedWord("{bar baz}", "foo \"bar baz\"");
        assertPGRemoveToplevelAndedWord("[{foo bar} OR baz]",
                "\"foo bar\" OR baz");
        assertPGRemoveToplevelAndedWord("[{foo bar} OR {gee man}]",
                "\"foo bar\" OR \"gee man\"");
        assertPGRemoveToplevelAndedWord("~{bar baz}", "foo -\"bar baz\"");
        assertPGRemoveToplevelAndedWord(null, "foo OR -\"bar baz\"");
    }

    public void testPostgreSQLLikeSql() throws Exception {
        assertPGLikeSql("?? LIKE '% foo %'", "foo");
        assertPGLikeSql("?? LIKE '% foo %'", "FOO");
        assertPGLikeSql("?? LIKE '% caf\u00e9 %'", "CAF\u00c9");
        assertPGLikeSql("(?? LIKE '% foo %' AND ?? LIKE '% bar %')", "foo bar");
        assertPGLikeSql("?? LIKE '% foo bar %'", "\"foo bar\"");
        assertPGLikeSql("(?? LIKE '% foo bar %' AND ?? LIKE '% baz %')",
                "\"foo bar\" baz");
        assertPGLikeSql("(?? LIKE '% foo %' AND ?? LIKE '% bar baz %')",
                "foo \"bar baz\"");
        assertPGLikeSql("(?? LIKE '% foo bar %' OR ?? LIKE '% baz %')",
                "\"foo bar\" OR baz");
        assertPGLikeSql("(?? LIKE '% foo bar %' OR ?? LIKE '% gee man %')",
                "\"foo bar\" OR \"gee man\"");
        assertPGLikeSql("(?? LIKE '% foo %' AND ?? NOT LIKE '% bar baz %')",
                "foo -\"bar baz\"");
        assertPGLikeSql("?? LIKE '% foo %'", "foo OR -\"bar baz\"");
    }

    public void testPostgreSQL() throws Exception {
        dialect = new DialectPostgreSQL(metadata, binaryManager,
                repositoryDescriptor);
        assertDialectFT("", "-foo");
        assertDialectFT("foo", "foo");
        assertDialectFT("(foo & bar)", "foo bar ");
        assertDialectFT("(foo & bar)", "foo & bar"); // compat
        assertDialectFT("(foo & ! bar)", "foo -bar");
        assertDialectFT("(bar & ! foo)", "-foo bar");
        assertDialectFT("(foo | bar)", "foo OR bar");
        assertDialectFT("foo", "foo OR -bar");
        assertDialectFT("((foo & bar) | baz)", "foo bar OR baz");
        assertDialectFT("((bar & ! foo) | baz)", "-foo bar OR baz");
        assertDialectFT("((foo & ! bar) | baz)", "foo -bar OR baz");
        assertDialectFT("(foo & bar) @#AND#@ ?? LIKE '% foo bar %'",
                "\"foo bar\"");
        assertDialectFT("(foo & bar & baz) @#AND#@ ?? LIKE '% foo bar %'",
                "\"foo bar\" baz");
        assertDialectFT(
                "(foo & bar & baz) @#AND#@ (?? LIKE '% foo bar %' AND ?? NOT LIKE '% gee man %')",
                "\"foo bar\" baz -\"gee man\"");
        assertDialectFT(
                "((foo & bar) | baz) @#AND#@ (?? LIKE '% foo bar %' OR ?? LIKE '% baz %')",
                "\"foo bar\" OR baz");
        assertDialectFT(
                "((foo & bar & baz) | (gee & man)) @#AND#@ ((?? LIKE '% foo bar %' AND ?? LIKE '% baz %') OR ?? LIKE '% gee man %')",
                "\"foo bar\" baz OR \"gee man\"");
        assertDialectFT(
                "(foo & bar) @#AND#@ (?? LIKE '% foo bar %' AND ?? NOT LIKE '% gee man %')",
                "\"foo bar\" -\"gee man\"");
    }

    public void testMySQL() throws Exception {
        dialect = new DialectMySQL(metadata, binaryManager,
                repositoryDescriptor);
        assertDialectFT("DONTMATCHANYTHINGFOREMPTYQUERY", "-foo");
        assertDialectFT("foo", "foo");
        assertDialectFT("(+foo +bar)", "foo bar");
        assertDialectFT("(+foo -bar)", "foo -bar");
        assertDialectFT("(+bar -foo)", "-foo bar");
        assertDialectFT("(foo bar)", "foo OR bar");
        assertDialectFT("foo", "foo OR -bar");
        assertDialectFT("((+foo +bar) baz)", "foo bar OR baz");
        assertDialectFT("((+bar -foo) baz)", "-foo bar OR baz");
        assertDialectFT("((+foo -bar) baz)", "foo -bar OR baz");
        assertDialectFT("\"foo bar\"", "\"foo bar\"");
        assertDialectFT("(+\"foo bar\" +baz)", "\"foo bar\" baz");
        assertDialectFT("(\"foo bar\" baz)", "\"foo bar\" OR baz");
        assertDialectFT("((+\"foo bar\" +baz) \"gee man\")",
                "\"foo bar\" baz OR \"gee man\"");
        assertDialectFT("(+\"foo bar\" -\"gee man\")",
                "\"foo bar\" -\"gee man\"");
    }

    public void testOracle() throws Exception {
        dialect = new DialectOracle(metadata, binaryManager,
                repositoryDescriptor);
        assertDialectFT("DONTMATCHANYTHINGFOREMPTYQUERY", "-foo");
        assertDialectFT("{foo}", "foo");
        assertDialectFT("{foo_bar}", "foo_bar");
        assertDialectFT("foo%", "foo*"); // special, not quoted
        assertDialectFT("({foo} AND {bar})", "foo bar");
        assertDialectFT("({foo} NOT {bar})", "foo -bar");
        assertDialectFT("({bar} NOT {foo})", "-foo bar");
        assertDialectFT("({foo} OR {bar})", "foo OR bar");
        assertDialectFT("{foo}", "foo OR -bar");
        assertDialectFT("(({foo} AND {bar}) OR {baz})", "foo bar OR baz");
        assertDialectFT("(({bar} NOT {foo}) OR {baz})", "-foo bar OR baz");
        assertDialectFT("(({foo} NOT {bar}) OR {baz})", "foo -bar OR baz");
        assertDialectFT("{foo} {bar}", "\"foo bar\"");
        assertDialectFT("({foo} {bar} AND {baz})", "\"foo bar\" baz");
        assertDialectFT("({foo} {bar} OR {baz})", "\"foo bar\" OR baz");
        assertDialectFT("(({foo} {bar} AND {baz}) OR {gee} {man})",
                "\"foo bar\" baz OR \"gee man\"");
        assertDialectFT("({foo} {bar} NOT {gee} {man})",
                "\"foo bar\" -\"gee man\"");
        // reserved words
        assertDialectFT("({word} AND {and})", "word and");
        assertDialectFT("{word} {and}", "\"word and\"");
    }

    public void testSQLServer() throws Exception {
        dialect = new DialectSQLServer(metadata, binaryManager,
                repositoryDescriptor);
        assertDialectFT("DONTMATCHANYTHINGFOREMPTYQUERY", "-foo");
        assertDialectFT("foo", "foo");
        assertDialectFT("(foo AND bar)", "foo bar");
        assertDialectFT("(foo AND NOT bar)", "foo -bar");
        assertDialectFT("(bar AND NOT foo)", "-foo bar");
        assertDialectFT("(foo OR bar)", "foo OR bar");
        assertDialectFT("foo", "foo OR -bar");
        assertDialectFT("((foo AND bar) OR baz)", "foo bar OR baz");
        assertDialectFT("((bar AND NOT foo) OR baz)", "-foo bar OR baz");
        assertDialectFT("((foo AND NOT bar) OR baz)", "foo -bar OR baz");
        assertDialectFT("\"foo bar\"", "\"foo bar\"");
        assertDialectFT("(\"foo bar\" AND baz)", "\"foo bar\" baz");
        assertDialectFT("(\"foo bar\" OR baz)", "\"foo bar\" OR baz");
        assertDialectFT("((\"foo bar\" AND baz) OR \"gee man\")",
                "\"foo bar\" baz OR \"gee man\"");
        assertDialectFT("(\"foo bar\" AND NOT \"gee man\")",
                "\"foo bar\" -\"gee man\"");
    }

}
