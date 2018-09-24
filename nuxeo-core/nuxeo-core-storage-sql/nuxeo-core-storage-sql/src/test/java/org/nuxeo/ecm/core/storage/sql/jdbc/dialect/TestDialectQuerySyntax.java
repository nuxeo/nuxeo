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
package org.nuxeo.ecm.core.storage.sql.jdbc.dialect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hamcrest.core.StringContains;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.storage.FulltextQueryAnalyzer;
import org.nuxeo.ecm.core.storage.FulltextQueryAnalyzer.FulltextQuery;
import org.nuxeo.ecm.core.storage.FulltextQueryAnalyzer.Op;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;

@RunWith(JMock.class)
public class TestDialectQuerySyntax {

    Mockery jmcontext = new JUnit4Mockery();

    public DatabaseMetaData metadata;

    public RepositoryDescriptor repositoryDescriptor;

    public Dialect dialect;

    @Before
    public void setUp() throws SQLException {
        metadata = getMockDatabaseMetaData();
        repositoryDescriptor = new RepositoryDescriptor();
    }

    protected DatabaseMetaData getMockDatabaseMetaData() throws SQLException {
        final DatabaseMetaData m = jmcontext.mock(DatabaseMetaData.class);
        jmcontext.checking(new Expectations() {
            {
                allowing(m).storesUpperCaseIdentifiers();
                will(returnValue(false));

                allowing(m).getDatabaseMajorVersion();
                will(returnValue(9));

                allowing(m).getDatabaseMinorVersion();
                will(returnValue(0));

                allowing(m).getColumns(with(aNull(String.class)), with(aNull(String.class)), with(any(String.class)),
                        with(any(String.class)));
                will(returnValue(getMockEmptyResultSet()));

                allowing(m).getConnection();
                will(returnValue(getMockConnection()));
            }
        });
        return m;
    }

    protected ResultSet getMockEmptyResultSet() throws SQLException {
        final ResultSet m = jmcontext.mock(ResultSet.class, "empty");
        jmcontext.checking(new Expectations() {
            {
                allowing(m).next();
                will(returnValue(false));
            }
        });
        return m;
    }

    protected Connection getMockConnection() throws SQLException {
        final Connection m = jmcontext.mock(Connection.class);
        jmcontext.checking(new Expectations() {
            {
                allowing(m).createStatement();
                will(returnValue(getMockStatement()));
            }
        });
        return m;
    }

    protected Statement getMockStatement() throws SQLException {
        final Statement m = jmcontext.mock(Statement.class);
        jmcontext.checking(new Expectations() {
            {
                allowing(m).executeQuery(with(new StringContains("is_read_committed_snapshot_on")));
                will(returnValue(getMockResultSetReturningInt(1)));

                allowing(m).executeQuery(with(new StringContains("EngineEdition")));
                will(returnValue(getMockResultSetReturningInt(2)));

                allowing(m).close();
                will(returnValue(null));
            }
        });
        return m;
    }

    protected ResultSet getMockResultSetReturningInt(final int value) throws SQLException {
        final ResultSet m = jmcontext.mock(ResultSet.class, "ResultSetReturningInt:" + value);
        jmcontext.checking(new Expectations() {
            {
                allowing(m).next();
                will(returnValue(true));

                allowing(m).getInt(with(any(Integer.class)));
                will(returnValue(value));

                allowing(m).close();
                will(returnValue(null));
            }
        });
        return m;
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

    protected void assertPGPhraseBreak(String expected, String query) {
        FulltextQuery ft = FulltextQueryAnalyzer.analyzeFulltextQuery(query);
        FulltextQuery broken = DialectPostgreSQL.breakPhrases(ft);
        StringBuilder buf = new StringBuilder();
        dumpFulltextQuery(broken, buf);
        assertEquals(expected, buf.toString());
    }

    protected void assertPGRemoveToplevelAndedWord(String expected, String query) {
        FulltextQuery ft = FulltextQueryAnalyzer.analyzeFulltextQuery(query);
        FulltextQuery simplified = DialectPostgreSQL.removeToplevelAndedWords(ft);
        if (simplified == null) {
            assertNull(expected);
        } else {
            StringBuilder buf = new StringBuilder();
            dumpFulltextQuery(simplified, buf);
            assertEquals(expected, buf.toString());
        }
    }

    protected void assertPGLikeSql(String expected, String query) {
        FulltextQuery ft = FulltextQueryAnalyzer.analyzeFulltextQuery(query);
        StringBuilder buf = new StringBuilder();
        DialectPostgreSQL.generateLikeSql(ft, buf);
        assertEquals(expected, buf.toString());
    }

    protected void assertDialectFT(String expected, String query) {
        assertEquals(expected, dialect.getDialectFulltextQuery(query));
    }

    @Test
    public void testH2() throws Exception {
        dialect = new DialectH2(metadata, repositoryDescriptor);
        assertDialectFT("DONTMATCHANYTHINGFOREMPTYQUERY", "");
        assertDialectFT("foo", "foo");
        assertDialectFT("FOO", "FOO");
        assertDialectFT("foo", "foo :");
        assertDialectFT("foo", "foo :)");
        assertDialectFT("foo", "foo -+-");
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
        assertDialectFT("((\"foo bar\" AND baz) OR \"gee man\")", "\"foo bar\" baz OR \"gee man\"");
        assertDialectFT("(\"foo bar\" NOT \"gee man\")", "\"foo bar\" -\"gee man\"");
        assertDialectFT("foo*", "foo*");
        assertDialectFT("(foo AND bar*)", "foo bar*");
    }

    @Test
    public void testPostgreSQLPhraseBreak() throws Exception {
        assertPGPhraseBreak("foo", "foo");
        assertPGPhraseBreak("[foo AND bar]", "\"foo bar\"");
        assertPGPhraseBreak("[foo AND bar AND baz]", "\"foo bar\" baz");
        assertPGPhraseBreak("[foo AND bar AND baz]", "foo \"bar baz\"");
        assertPGPhraseBreak("[[foo AND bar] OR baz]", "\"foo bar\" OR baz");
        assertPGPhraseBreak("[[foo AND bar] OR [gee AND man]]", "\"foo bar\" OR \"gee man\"");
        assertPGPhraseBreak("foo", "foo -\"bar baz\"");
        assertPGPhraseBreak("foo", "foo OR -\"bar baz\"");
    }

    @Test
    public void testPostgreSQLToplevelAndedWordRemoval() throws Exception {
        assertPGRemoveToplevelAndedWord(null, "foo");
        assertPGRemoveToplevelAndedWord(null, "foo bar");
        assertPGRemoveToplevelAndedWord("{foo bar}", "\"foo bar\"");
        assertPGRemoveToplevelAndedWord("{foo bar}", "\"foo bar\" baz");
        assertPGRemoveToplevelAndedWord("{bar baz}", "foo \"bar baz\"");
        assertPGRemoveToplevelAndedWord("[{foo bar} OR baz]", "\"foo bar\" OR baz");
        assertPGRemoveToplevelAndedWord("[{foo bar} OR {gee man}]", "\"foo bar\" OR \"gee man\"");
        assertPGRemoveToplevelAndedWord("~{bar baz}", "foo -\"bar baz\"");
        assertPGRemoveToplevelAndedWord(null, "foo OR -\"bar baz\"");
    }

    @Test
    public void testPostgreSQLLikeSql() throws Exception {
        assertPGLikeSql("?? ILIKE '% foo %'", "foo");
        assertPGLikeSql("?? ILIKE '% foo %'", "FOO");
        assertPGLikeSql("?? ILIKE '% caf\u00e9 %'", "CAF\u00c9");
        assertPGLikeSql("(?? ILIKE '% foo %' AND ?? ILIKE '% bar %')", "foo bar");
        assertPGLikeSql("?? ILIKE '% foo bar %'", "\"foo bar\"");
        assertPGLikeSql("(?? ILIKE '% foo bar %' AND ?? ILIKE '% baz %')", "\"foo bar\" baz");
        assertPGLikeSql("(?? ILIKE '% foo %' AND ?? ILIKE '% bar baz %')", "foo \"bar baz\"");
        assertPGLikeSql("(?? ILIKE '% foo bar %' OR ?? ILIKE '% baz %')", "\"foo bar\" OR baz");
        assertPGLikeSql("(?? ILIKE '% foo bar %' OR ?? ILIKE '% gee man %')", "\"foo bar\" OR \"gee man\"");
        assertPGLikeSql("(?? ILIKE '% foo %' AND ?? NOT ILIKE '% bar baz %')", "foo -\"bar baz\"");
        assertPGLikeSql("?? ILIKE '% foo %'", "foo OR -\"bar baz\"");
    }

    @Test
    public void testPostgreSQL() throws Exception {
        dialect = new DialectPostgreSQL(metadata, repositoryDescriptor);
        assertDialectFT("", "-foo");
        assertDialectFT("foo", "foo");
        assertDialectFT("FOO", "FOO");
        assertDialectFT("foo", "foo :");
        assertDialectFT("foo", "foo :)");
        assertDialectFT("foo", "foo -+-");
        assertDialectFT("(foo & bar)", "foo bar ");
        assertDialectFT("(foo & bar)", "foo & bar"); // compat
        assertDialectFT("(foo & ! bar)", "foo -bar");
        assertDialectFT("(bar & ! foo)", "-foo bar");
        assertDialectFT("(foo | bar)", "foo OR bar");
        assertDialectFT("foo", "foo OR -bar");
        assertDialectFT("((foo & bar) | baz)", "foo bar OR baz");
        assertDialectFT("((bar & ! foo) | baz)", "-foo bar OR baz");
        assertDialectFT("((foo & ! bar) | baz)", "foo -bar OR baz");
        assertDialectFT("(foo & bar) @#AND#@ ?? ILIKE '% foo bar %'", "\"foo bar\"");
        assertDialectFT("(foo & bar & baz) @#AND#@ ?? ILIKE '% foo bar %'", "\"foo bar\" baz");
        assertDialectFT("(foo & bar & baz) @#AND#@ (?? ILIKE '% foo bar %' AND ?? NOT ILIKE '% gee man %')",
                "\"foo bar\" baz -\"gee man\"");
        assertDialectFT("((foo & bar) | baz) @#AND#@ (?? ILIKE '% foo bar %' OR ?? ILIKE '% baz %')",
                "\"foo bar\" OR baz");
        assertDialectFT(
                "((foo & bar & baz) | (gee & man)) @#AND#@ ((?? ILIKE '% foo bar %' AND ?? ILIKE '% baz %') OR ?? ILIKE '% gee man %')",
                "\"foo bar\" baz OR \"gee man\"");
        assertDialectFT("(foo & bar) @#AND#@ (?? ILIKE '% foo bar %' AND ?? NOT ILIKE '% gee man %')",
                "\"foo bar\" -\"gee man\"");
        assertDialectFT("foo:*", "foo*");
        assertDialectFT("(foo & bar:*)", "foo bar*");
        assertDialectFT("(foo & bar:*) @#AND#@ ?? ILIKE '% foo bar%'", "\"foo bar*\"");
    }

    @Test
    public void testMySQL() throws Exception {
        dialect = new DialectMySQL(metadata, repositoryDescriptor);
        assertDialectFT("DONTMATCHANYTHINGFOREMPTYQUERY", "-foo");
        assertDialectFT("foo", "foo");
        assertDialectFT("FOO", "FOO");
        assertDialectFT("foo", "foo :");
        assertDialectFT("foo", "foo :)");
        assertDialectFT("foo", "foo -+-");
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
        assertDialectFT("((+\"foo bar\" +baz) \"gee man\")", "\"foo bar\" baz OR \"gee man\"");
        assertDialectFT("(+\"foo bar\" -\"gee man\")", "\"foo bar\" -\"gee man\"");
        assertDialectFT("foo*", "foo*");
        assertDialectFT("(+foo +bar*)", "foo bar*");
    }

    @Test
    public void testOracle() throws Exception {
        dialect = new DialectOracle(metadata, repositoryDescriptor);
        assertDialectFT("DONTMATCHANYTHINGFOREMPTYQUERY", "-foo");
        assertDialectFT("{foo}", "foo");
        assertDialectFT("{FOO}", "FOO");
        assertDialectFT("{foo}", "foo :");
        assertDialectFT("{foo}", "foo :)");
        assertDialectFT("{foo}", "foo -+-");
        assertDialectFT("{foo_bar}", "foo_bar");
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
        assertDialectFT("(({foo} {bar} AND {baz}) OR {gee} {man})", "\"foo bar\" baz OR \"gee man\"");
        assertDialectFT("({foo} {bar} NOT {gee} {man})", "\"foo bar\" -\"gee man\"");
        assertDialectFT("foo%", "foo*");
        assertDialectFT("foo%", "foo%");
        assertDialectFT("$foo", "$foo");
        assertDialectFT("${foo}", "${foo}");
        assertDialectFT("{fooBAR}", "{fooBAR}");
        assertDialectFT("({foo} AND bar%)", "foo bar*");
        assertDialectFT("{foo} bar%", "\"foo bar*\"");
        // reserved words
        assertDialectFT("({word} AND {and})", "word and");
        assertDialectFT("{word} {and}", "\"word and\"");
    }

    @Test
    public void testSQLServer() throws Exception {
        dialect = new DialectSQLServer(metadata, repositoryDescriptor);
        assertDialectFT("DONTMATCHANYTHINGFOREMPTYQUERY", "-foo");
        assertDialectFT("\"foo\"", "foo");
        assertDialectFT("\"FOO\"", "FOO");
        assertDialectFT("\"foo\"", "foo :");
        assertDialectFT("\"foo\"", "foo :)");
        assertDialectFT("\"foo\"", "foo -+-");
        assertDialectFT("(\"foo\" AND \"bar\")", "foo bar");
        assertDialectFT("(\"foo\" AND NOT \"bar\")", "foo -bar");
        assertDialectFT("(\"bar\" AND NOT \"foo\")", "-foo bar");
        assertDialectFT("(\"foo\" OR \"bar\")", "foo OR bar");
        assertDialectFT("\"foo\"", "foo OR -bar");
        assertDialectFT("((\"foo\" AND \"bar\") OR \"baz\")", "foo bar OR baz");
        assertDialectFT("((\"bar\" AND NOT \"foo\") OR \"baz\")", "-foo bar OR baz");
        assertDialectFT("((\"foo\" AND NOT \"bar\") OR \"baz\")", "foo -bar OR baz");
        assertDialectFT("\"foo bar\"", "\"foo bar\"");
        assertDialectFT("(\"foo bar\" AND \"baz\")", "\"foo bar\" baz");
        assertDialectFT("(\"foo bar\" OR \"baz\")", "\"foo bar\" OR baz");
        assertDialectFT("((\"foo bar\" AND \"baz\") OR \"gee man\")", "\"foo bar\" \"baz\" OR \"gee man\"");
        assertDialectFT("(\"foo bar\" AND NOT \"gee man\")", "\"foo bar\" -\"gee man\"");
        assertDialectFT("\"foo*\"", "foo*");
        assertDialectFT("(\"foo\" AND \"bar*\")", "foo bar*");
        assertDialectFT("\"foo bar*\"", "\"foo bar*\"");
    }

}
