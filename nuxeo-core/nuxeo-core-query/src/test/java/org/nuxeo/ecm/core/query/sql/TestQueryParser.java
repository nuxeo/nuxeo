/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.query.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.model.DateLiteral;
import org.nuxeo.ecm.core.query.sql.model.DoubleLiteral;
import org.nuxeo.ecm.core.query.sql.model.EsHint;
import org.nuxeo.ecm.core.query.sql.model.EsIdentifierList;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.FromClause;
import org.nuxeo.ecm.core.query.sql.model.Function;
import org.nuxeo.ecm.core.query.sql.model.IntegerLiteral;
import org.nuxeo.ecm.core.query.sql.model.LiteralList;
import org.nuxeo.ecm.core.query.sql.model.Operand;
import org.nuxeo.ecm.core.query.sql.model.OperandList;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.query.sql.model.OrderByList;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.query.sql.model.SelectClause;
import org.nuxeo.ecm.core.query.sql.model.SelectList;
import org.nuxeo.ecm.core.query.sql.model.StringLiteral;
import org.nuxeo.ecm.core.query.sql.model.WhereClause;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TestQueryParser {

    static final String[] GOOD_QUERIES = {
            "SELECT name, title, description FROM folder WHERE state = 2 AND created > \"20060523\"" };

    static final String[] BAD_QUERIES = {
            "SELECT name WHERE title, description FROM folder WHERE state = 2 AND created > \"20060523\"", "name, ",
            "SELECT FROM WHERE", "SELECT name WHERE state=2", "PLEASE GET ME SOME DOCUMENTS" };

    /**
     * Checks that literals are correctly parsed.
     */
    @Test
    public void testLiterals() {
        // test double quoted strings
        SQLQuery query = SQLQueryParser.parse("SELECT p FROM t WHERE title=\"%test\"");
        StringLiteral sl = (StringLiteral) query.getWhereClause().predicate.rvalue;
        assertEquals("%test", sl.value);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE title=\"%te\\\"s't\"");
        sl = (StringLiteral) query.getWhereClause().predicate.rvalue;
        assertEquals("%te\"s't", sl.value);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE title=\"te\\st\"");
        sl = (StringLiteral) query.getWhereClause().predicate.rvalue;
        assertEquals("te\\st", sl.value);

        // test single quoted strings
        query = SQLQueryParser.parse("SELECT p FROM t WHERE title='%te\\'s\"t'");
        sl = (StringLiteral) query.getWhereClause().predicate.rvalue;
        assertEquals("%te's\"t", sl.value);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE title='te\\st'");
        sl = (StringLiteral) query.getWhereClause().predicate.rvalue;
        assertEquals("te\\st", sl.value);

        // integers
        query = SQLQueryParser.parse("SELECT p FROM t WHERE title=12");
        IntegerLiteral il = (IntegerLiteral) query.getWhereClause().predicate.rvalue;
        assertEquals(12, il.value);

        // doubles
        query = SQLQueryParser.parse("SELECT p FROM t WHERE title=1.2");
        DoubleLiteral dl = (DoubleLiteral) query.getWhereClause().predicate.rvalue;
        assertEquals(1.2, dl.value, 1e-8);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE title=.2");
        dl = (DoubleLiteral) query.getWhereClause().predicate.rvalue;
        assertEquals(0.2, dl.value, 1e-8);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE title=-1.2");
        dl = (DoubleLiteral) query.getWhereClause().predicate.rvalue;
        assertEquals(-1.2, dl.value, 1e-8);

        // dates
        DateLiteral datel;
        query = SQLQueryParser.parse("SELECT p FROM t WHERE title = DATE '2007-01-30'");
        datel = (DateLiteral) query.getWhereClause().predicate.rvalue;
        assertEquals("DATE '2007-01-30'", datel.toString());
        query = SQLQueryParser.parse("SELECT p FROM t WHERE title = TIMESTAMP '2007-01-30 01:02:03+04:00'");
        datel = (DateLiteral) query.getWhereClause().predicate.rvalue;
        assertEquals("TIMESTAMP '2007-01-30T01:02:03.000+04:00'", datel.toString());
    }

    @Test
    public void testDoubleBackslash() {
        SQLQuery query = SQLQueryParser.parse("SELECT p FROM t WHERE title='a\\\\b'");
        StringLiteral sl = (StringLiteral) query.getWhereClause().predicate.rvalue;
        assertEquals("a\\b", sl.value);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE title='a\\\\'");
        sl = (StringLiteral) query.getWhereClause().predicate.rvalue;
        assertEquals("a\\", sl.value);
    }

    @Test
    public void testNamespace() {
        SQLQuery query = SQLQueryParser.parse("SELECT dc:title FROM Document WHERE dc:description = 'test'");
        SelectClause select = query.getSelectClause();
        String v = ((Reference) select.operands().iterator().next()).name;
        assertEquals("dc:title", v);
    }

    @Test
    public void testComplexProperties() {
        SQLQuery query = SQLQueryParser.parse("SELECT dc:foo/bar/baz FROM Document"
                + " WHERE dc:foo/3/ho = dc:bar/*/bobby" + " OR dc:foo/bar[5]/gee = dc:foo/*6/hop");
        SelectClause select = query.getSelectClause();
        String v = ((Reference) select.operands().iterator().next()).name;
        assertEquals("dc:foo/bar/baz", v);
        Predicate where = query.getWhereClause().predicate;
        Predicate p1 = (Predicate) where.lvalue;
        Predicate p2 = (Predicate) where.rvalue;
        assertEquals("dc:foo/3/ho", ((Reference) p1.lvalue).name);
        assertEquals("dc:bar/*/bobby", ((Reference) p1.rvalue).name);
        assertEquals("dc:foo/bar[5]/gee", ((Reference) p2.lvalue).name);
        assertEquals("dc:foo/*6/hop", ((Reference) p2.rvalue).name);

        // check parsing of complex properties in other contexts
        SQLQueryParser.parse("SELECT x FROM Document" + " WHERE dc:foo/bar LIKE 'foo' OR dc:foo/bar IS NULL"
                + " OR dc:foo/bar BETWEEN 1 AND 1000" + " ORDER BY dc:foo/bar");
    }

    @Test
    public void testVariables() {
        SQLQuery query = SQLQueryParser.parse("SELECT p1, $id, p3 FROM t1, t2 WHERE state=1 AND title = 'test'");

        SelectClause select = query.getSelectClause();
        assertEquals("p1", select.getAlias(0));
        assertEquals("p1", select.getVariable(0).name);
        assertEquals("$id", select.getAlias(1));
        assertEquals("$id", select.getVariable(1).name);
        assertEquals("p3", select.getAlias(2));
        assertEquals("p3", select.getVariable(2).name);

        FromClause from = query.getFromClause();
        assertEquals("t1", from.getAlias(0));
        assertEquals("t1", from.get(0));

        Expression e1 = (Expression) query.getWhereClause().predicate.lvalue;
        Expression e2 = (Expression) query.getWhereClause().predicate.rvalue;
        assertEquals("state", ((Reference) e1.lvalue).name);
        assertEquals("title", ((Reference) e2.lvalue).name);
    }

    @Test
    public void testOperators() {
        SQLQuery query = SQLQueryParser.parse("SELECT p FROM t WHERE p = 'test'");
        Operator op = query.getWhereClause().predicate.operator;
        assertEquals(Operator.EQ, op);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE p > 1");
        op = query.getWhereClause().predicate.operator;
        assertEquals(Operator.GT, op);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE p >= 1");
        op = query.getWhereClause().predicate.operator;
        assertEquals(Operator.GTEQ, op);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE p < 1");
        op = query.getWhereClause().predicate.operator;
        assertEquals(Operator.LT, op);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE p <= 1");
        op = query.getWhereClause().predicate.operator;
        assertEquals(Operator.LTEQ, op);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE p <> 1");
        op = query.getWhereClause().predicate.operator;
        assertEquals(Operator.NOTEQ, op);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE p != 1");
        op = query.getWhereClause().predicate.operator;
        assertEquals(Operator.NOTEQ, op);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE p + 2 = 2");
        op = ((Expression) query.getWhereClause().predicate.lvalue).operator;
        assertEquals(Operator.SUM, op);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE p - 2 = 3");
        op = ((Expression) query.getWhereClause().predicate.lvalue).operator;
        assertEquals(Operator.SUB, op);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE p * 2 = 3");
        op = ((Expression) query.getWhereClause().predicate.lvalue).operator;
        assertEquals(Operator.MUL, op);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE p / 2 = 4");
        op = ((Expression) query.getWhereClause().predicate.lvalue).operator;
        assertEquals(Operator.DIV, op);
    }

    @Test
    public void testLikeOperator() {
        SQLQuery query = SQLQueryParser.parse("SELECT p FROM t WHERE p LIKE '%test%'");
        Operator op = query.getWhereClause().predicate.operator;
        assertEquals(Operator.LIKE, op);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE p NOT LIKE '%test%'");
        op = query.getWhereClause().predicate.operator;
        assertEquals(Operator.NOTLIKE, op);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE p ILIKE '%test%'");
        op = query.getWhereClause().predicate.operator;
        assertEquals(Operator.ILIKE, op);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE p NOT ILIKE '%test%'");
        op = query.getWhereClause().predicate.operator;
        assertEquals(Operator.NOTILIKE, op);
    }

    @Test
    public void testInOperator() {
        SQLQuery query = SQLQueryParser.parse("SELECT p FROM t WHERE p IN (12, 13) AND q='test'");
        Operator op = query.getWhereClause().predicate.operator;
        assertEquals(Operator.AND, op);

        Expression e = (Expression) query.getWhereClause().predicate.lvalue;
        assertEquals(Operator.IN, e.operator);

        LiteralList list = new LiteralList();
        list.add(new IntegerLiteral(12));
        list.add(new IntegerLiteral(13));
        assertEquals(list, e.rvalue);
        assertEquals("p", ((Reference) e.lvalue).name);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE p NOT IN (12, 13) AND q='test'");
        op = query.getWhereClause().predicate.operator;
        assertEquals(Operator.AND, op);

        e = (Expression) query.getWhereClause().predicate.lvalue;
        assertEquals(Operator.NOTIN, e.operator);

        list = new LiteralList();
        list.add(new IntegerLiteral(12));
        list.add(new IntegerLiteral(13));
        assertEquals(list, e.rvalue);
        assertEquals("p", ((Reference) e.lvalue).name);
    }

    @Test
    public void testBetweenOperator() {
        SQLQuery query = SQLQueryParser.parse("SELECT p FROM t WHERE p BETWEEN 10 AND 20 AND q='test'");
        Operator op = query.getWhereClause().predicate.operator;
        assertEquals(Operator.AND, op);

        Expression e = (Expression) query.getWhereClause().predicate.lvalue;
        assertEquals(Operator.BETWEEN, e.operator);

        LiteralList list = new LiteralList();
        list.add(new IntegerLiteral(10));
        list.add(new IntegerLiteral(20));
        assertEquals(list, e.rvalue);
        assertEquals("p", ((Reference) e.lvalue).name);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE p NOT BETWEEN 10 AND 20 AND q='test'");
        op = query.getWhereClause().predicate.operator;
        assertEquals(Operator.AND, op);

        e = (Expression) query.getWhereClause().predicate.lvalue;
        assertEquals(Operator.NOTBETWEEN, e.operator);

        list = new LiteralList();
        list.add(new IntegerLiteral(10));
        list.add(new IntegerLiteral(20));
        assertEquals(list, e.rvalue);
        assertEquals("p", ((Reference) e.lvalue).name);
    }

    @Test
    public void testIsNull() {
        SQLQuery query = SQLQueryParser.parse("SELECT p FROM t WHERE p IS NULL");
        Operator op = query.getWhereClause().predicate.operator;
        assertEquals(Operator.ISNULL, op);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE p IS NOT NULL");
        op = query.getWhereClause().predicate.operator;
        assertEquals(Operator.ISNOTNULL, op);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE p IS NULL OR p = 'abc'");
        Predicate p = (Predicate) query.getWhereClause().predicate.lvalue;
        assertEquals(Operator.ISNULL, p.operator);
    }

    @Test
    public void testFunction() {
        SQLQuery query = SQLQueryParser.parse("SELECT getDate(), p FROM t WHERE substring(title, 2) = 'test'");
        Operator op = query.getWhereClause().predicate.operator;
        assertEquals(Operator.EQ, op);

        Function fn = (Function) query.getWhereClause().predicate.lvalue;
        assertEquals("substring", fn.name);

        OperandList args = new OperandList();
        args.add(new Reference("title"));
        args.add(new IntegerLiteral(2));
        assertEquals(args, fn.args);
    }

    @Test
    public void testFunctions() {
        SQLQuery query = SQLQueryParser.parse("SELECT COUNT(p), AVG(p) FROM t");
        SelectClause select = query.getSelectClause();
        assertEquals(2, select.count());

        OperandList ops = new OperandList();
        ops.add(new Reference("p"));
        assertEquals(new Function("COUNT", ops), select.get(0));
        assertEquals(new Function("AVG", ops), select.get(1));
    }

    @Test
    public void testDateCast() {
        SQLQuery query = SQLQueryParser.parse("SELECT p FROM t WHERE DATE(dc:modified) = DATE '2010-01-01'");

        Operand lvalue = query.getWhereClause().predicate.lvalue;
        assertTrue(lvalue instanceof Reference); // with cast
        assertEquals(new Reference("dc:modified", "DATE"), lvalue);
    }

    @Test
    public void testEsHintIndex() {
        SQLQuery query = SQLQueryParser.parse("SELECT p FROM t WHERE /*+ES: INDEX(dc:title.ngram) */ dc:title = 'foo'");
        Operand lvalue = query.getWhereClause().predicate.lvalue;
        assertTrue(lvalue instanceof Reference);
        assertEquals(1, ((Reference) lvalue).esHint.getIndex().length);
        assertEquals(new Reference(new Reference("dc:title"),
                new EsHint(new EsIdentifierList("dc:title.ngram"), null, null)), lvalue);
    }

    @Test
    public void testEsHintIndexBoost() {
        SQLQuery query = SQLQueryParser.parse(
                "SELECT p FROM t WHERE /*+es: INDEX(dc:title.ngram^3) */ dc:title = 'foo'");
        Operand lvalue = query.getWhereClause().predicate.lvalue;
        assertTrue(lvalue instanceof Reference);
        assertEquals(1, ((Reference) lvalue).esHint.getIndex().length);
        assertEquals(new Reference(new Reference("dc:title"),
                new EsHint(new EsIdentifierList("dc:title.ngram^3"), null, null)), lvalue);
    }

    @Test
    public void testEsHintMultiIndex() {
        SQLQuery query = SQLQueryParser.parse(
                "SELECT p FROM t WHERE /*+ES: INDEX(dc:title,dc:description) */ ecm:fulltext = 'foo'");
        Operand lvalue = query.getWhereClause().predicate.lvalue;
        assertTrue(lvalue instanceof Reference);
        assertEquals(2, ((Reference) lvalue).esHint.getIndex().length);
        assertEquals(new Reference(new Reference("ecm:fulltext"),
                new EsHint(new EsIdentifierList("dc:title,dc:description"), null, null)), lvalue);
    }

    @Test
    public void testEsHintAnalyzer() {
        SQLQuery query = SQLQueryParser.parse("SELECT p FROM t WHERE /*+ES: ANALYZER(fulltext) */ dc:title LIKE 'foo'");
        Operand lvalue = query.getWhereClause().predicate.lvalue;
        assertTrue(lvalue instanceof Reference);
        assertEquals(new Reference(new Reference("dc:title"), new EsHint(null, "fulltext", null)), lvalue);
    }

    @Test
    public void testEsHintOperator() {
        SQLQuery query = SQLQueryParser.parse(
                "SELECT p FROM t WHERE /*+ES: OPERATOR(regex) */ dc:title = 'foo|bar|ba*'");
        Operand lvalue = query.getWhereClause().predicate.lvalue;
        assertTrue(lvalue instanceof Reference);
        assertEquals(new Reference(new Reference("dc:title"), new EsHint(null, null, "regex")), lvalue);
    }

    @Test
    public void testEsHintIndexAndAnalyzer() {
        SQLQuery query = SQLQueryParser.parse(
                "SELECT p FROM t WHERE /*+ES: Index(dc:title.ngram) analyzer(fulltext) */ dc:title = 'foo'");
        Operand lvalue = query.getWhereClause().predicate.lvalue;
        assertTrue(lvalue instanceof Reference);
        assertEquals(new Reference(new Reference("dc:title"),
                new EsHint(new EsIdentifierList("dc:title.ngram"), "fulltext", null)), lvalue);
    }

    @Test
    public void testEsHintIndexAnalyzerAndOperator() {
        SQLQuery query = SQLQueryParser.parse(
                "SELECT p FROM t WHERE /*+ES: Index(dc:title.ngram) analyzer(fulltext) operator(fuzzy)*/ dc:title = 'foo'");
        Operand lvalue = query.getWhereClause().predicate.lvalue;
        assertTrue(lvalue instanceof Reference);
        assertEquals(new Reference(new Reference("dc:title"),
                new EsHint(new EsIdentifierList("dc:title.ngram"), "fulltext", "fuzzy")), lvalue);
    }

    @Test
    public void testEsHintMultiClauses() {
        SQLQuery query = SQLQueryParser.parse("SELECT p FROM t WHERE /*+ES: INDEX(dc:title.ngram) */ dc:title = 'foo' "
                + "AND /*+ES: ANALYZER(fulltext2) */ dc:description LIKE 'bar'");
        Operand pred = query.getWhereClause().predicate.rvalue;
        assertTrue(pred instanceof Predicate);
        Operand lvalue = ((Predicate) pred).lvalue;
        assertTrue(lvalue instanceof Reference);
        assertEquals(new Reference(new Reference("dc:description"), new EsHint(null, "fulltext2", null)), lvalue);
    }

    @Test
    public void testEsHintEmpty() {
        SQLQuery query = SQLQueryParser.parse("SELECT p FROM t WHERE /*+ES: */ dc:title = 'foo'");
        Operand lvalue = query.getWhereClause().predicate.lvalue;
        assertTrue(lvalue instanceof Reference);
        assertEquals(new Reference(new Reference("dc:title"), (EsHint) null), lvalue);
    }

    @Test
    public void testAlias() {
        SQLQuery query = SQLQueryParser.parse("SELECT p AS pp, q AS qq, r FROM t AS t1");

        SelectClause select = query.getSelectClause();

        assertEquals("pp", select.getAlias(0));
        assertEquals("p", select.getVariable(0).name);
        assertEquals("qq", select.getAlias(1));
        assertEquals("q", select.getVariable(1).name);
        assertEquals("r", select.getAlias(2));
        assertEquals("r", select.getVariable(2).name);

        FromClause from = query.getFromClause();

        assertEquals("t1", from.getAlias(0));
        assertEquals("t", from.get(0));
    }

    @Test
    public void testOperatorPrecedence() {
        SQLQuery query = SQLQueryParser.parse("SELECT p FROM t WHERE p * -2 / 3 + 4 - 5 = 2");
        Predicate pred = query.getWhereClause().predicate;
        assertEquals(Operator.EQ, pred.operator);

        Expression e = (Expression) pred.lvalue;
        assertEquals(Operator.SUB, e.operator);

        e = (Expression) e.lvalue;
        assertEquals(Operator.SUM, e.operator);

        e = (Expression) e.lvalue;
        assertEquals(Operator.DIV, e.operator);

        e = (Expression) e.lvalue;
        assertEquals(Operator.MUL, e.operator);
        assertEquals(-2, ((IntegerLiteral) e.rvalue).value);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE p=2 AND q=1 AND s=3 OR r = 4 OR w = 10");
        pred = query.getWhereClause().predicate;
        assertEquals(Operator.OR, pred.operator);
        assertEquals(Operator.EQ, ((Expression) pred.rvalue).operator);

        e = (Expression) pred.lvalue;
        assertEquals(Operator.OR, e.operator);
        assertEquals(Operator.EQ, ((Expression) e.rvalue).operator);

        e = (Expression) e.lvalue;
        assertEquals(Operator.AND, e.operator);
        assertEquals(Operator.EQ, ((Expression) e.rvalue).operator);

        e = (Expression) e.lvalue;
        assertEquals(Operator.AND, e.operator);
        assertEquals(Operator.EQ, ((Expression) e.rvalue).operator);
        assertEquals(Operator.EQ, ((Expression) e.lvalue).operator);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE p=2 OR s=3 AND NOT q=4");
        pred = query.getWhereClause().predicate;
        assertEquals(Operator.OR, pred.operator);
        assertEquals(Operator.EQ, ((Expression) pred.lvalue).operator);

        e = (Expression) pred.rvalue;
        assertEquals(Operator.AND, e.operator);
        assertEquals(Operator.EQ, ((Expression) e.lvalue).operator);

        e = (Expression) e.rvalue;
        assertEquals(Operator.NOT, e.operator);
        assertEquals(Operator.EQ, ((Expression) e.lvalue).operator);
    }

    /**
     * Tests that good queries (queries from GOOD_QUERIES array) are successfully parsed.
     */
    @Test
    public void testGoodQueries() {
        int i = 0;
        try {
            for (; i < GOOD_QUERIES.length; i++) {
                SQLQueryParser.parse(GOOD_QUERIES[i]);
            }
        } catch (QueryParseException e) {
            // parse error
            fail("Failed to parse a good query: " + GOOD_QUERIES[i]);
        }
    }

    /**
     * Tests that parsing fails for bad queries (queries from BAD_QUERIES array).
     */
    @Test
    public void testBadQueries() {
        for (String badQuery : BAD_QUERIES) {
            try {
                SQLQueryParser.parse(badQuery);
                // Not so bad this query: bad query was successfully parsed -> error
                fail("A bad Query has been successfully parsed: " + badQuery);
            } catch (QueryParseException e) {
                // this is really a bad query -> continue
            }
        }
    }

    /**
     * Author the generated AST.
     *
     * <pre>
     *              OR
     *    p1>0                  AND
     *              p2<=10.2            =
     *                          p1-p2        5
     * </pre>
     *
     * TODO: add tests for DateLiteral, other operators, paranthesys etc
     */
    @Test
    public void testAST() {
        SQLQuery query = SQLQueryParser.parse("SELECT p1, p2 FROM table WHERE p1 > 0 OR p2 <= 10.2 AND p1 - p2 = 5");

        SelectList select = query.getSelectClause().elements;
        assertEquals(2, select.size());

        Expression where = query.getWhereClause().predicate;
        assertEquals(Operator.OR, where.operator);

        Expression expr = (Expression) where.lvalue;
        assertEquals(Operator.GT, expr.operator);
        assertEquals("p1", ((Reference) expr.lvalue).name);
        assertEquals(0, ((IntegerLiteral) expr.rvalue).value);

        // root expr
        expr = (Expression) where.rvalue;
        assertEquals(Operator.AND, expr.operator);

        Expression expr1 = (Expression) expr.lvalue;
        assertEquals(Operator.LTEQ, expr1.operator);
        assertEquals("p2", ((Reference) expr1.lvalue).name);
        assertEquals(10.2, ((DoubleLiteral) expr1.rvalue).value, 1e-8);

        Expression expr2 = (Expression) expr.rvalue;
        assertEquals(Operator.EQ, expr2.operator);
        assertEquals(5, ((IntegerLiteral) expr2.rvalue).value);

        Expression expr3 = (Expression) expr2.lvalue;
        assertEquals(Operator.SUB, expr3.operator);
        assertEquals("p1", ((Reference) expr3.lvalue).name);
        assertEquals("p2", ((Reference) expr3.rvalue).name);
    }

    /**
     * Tests the manual query creation and parser by comparing the two queries (the manual one ith the parsed one).
     *
     * <pre>
     *              OR
     *    title="test"         AND
     *              p2>=10.2            &lt;
     *                          p1+p2        5
     * </pre>
     */
    @Test
    public void testWhereClause() {
        SQLQuery query = SQLQueryParser.parse(
                "SELECT p1, p2 FROM t WHERE title = \"test\" OR p2 >= 10.2 AND p1 + p2 < 5");

        Expression expr1 = new Expression(new Reference("p1"), Operator.SUM, new Reference("p2"));
        expr1 = new Expression(expr1, Operator.LT, new IntegerLiteral(5));

        Predicate expr2 = new Predicate(new Reference("p2"), Operator.GTEQ, new DoubleLiteral(10.2));
        expr1 = new Expression(expr2, Operator.AND, expr1);

        expr2 = new Predicate(new Reference("title"), Operator.EQ, new StringLiteral("test"));

        Predicate pred = new Predicate(expr2, Operator.OR, expr1);

        // create the query by hand
        SQLQuery myquery = new SQLQuery(new SelectClause(), new FromClause(), new WhereClause(pred));
        myquery.getSelectClause().add(new Reference("p1"));
        myquery.getSelectClause().add(new Reference("p2"));
        myquery.getFromClause().add("t");

        assertEquals(myquery, query);
    }

    /**
     * Same query as before but with parenthesis on first OR condition.
     *
     * <pre>
     *                          AND
     *               OR                     <
     *                                p1+p2      5
     *    title="test"   p2>=10.2
     * </pre>
     */
    @Test
    public void testWhereClauseWithParenthesis() {
        SQLQuery query = SQLQueryParser.parse(
                "SELECT p1, p2 FROM t WHERE (title = \"test\" OR p2 >= 10.2) AND p1 + p2 < 5");

        // create the query by hand
        Predicate expr1 = new Predicate(new Reference("title"), Operator.EQ, new StringLiteral("test"));
        Expression expr2 = new Expression(new Reference("p2"), Operator.GTEQ, new DoubleLiteral(10.2));
        expr1 = new Predicate(expr1, Operator.OR, expr2);

        expr2 = new Expression(new Reference("p1"), Operator.SUM, new Reference("p2"));
        expr2 = new Expression(expr2, Operator.LT, new IntegerLiteral(5));

        Predicate pred = new Predicate(expr1, Operator.AND, expr2);

        SQLQuery myquery = new SQLQuery(new SelectClause(), new FromClause(), new WhereClause(pred));

        myquery.getSelectClause().add(new Reference("p1"));
        myquery.getSelectClause().add(new Reference("p2"));
        myquery.getFromClause().add("t");

        assertEquals(myquery, query);
    }

    @Test
    public void testSelectClause() {
        SQLQuery query = SQLQueryParser.parse("SELECT p FROM t");
        assertFalse(query.getSelectClause().distinct);

        query = SQLQueryParser.parse("SELECT DISTINCT p FROM t");
        assertTrue(query.getSelectClause().distinct);

        query = SQLQueryParser.parse("SELECT ALL p FROM t");
        assertFalse(query.getSelectClause().distinct);

        query = SQLQueryParser.parse("SELECT * FROM t");
        assertTrue(query.getSelectClause().getSelectList().isEmpty());
    }

    @Test
    public void testOrderByClause() {
        SQLQuery query = SQLQueryParser.parse("SELECT p, q, r FROM t ORDER BY p, q");
        String expected = "SELECT p, q, r FROM t ORDER BY p, q";
        assertEquals(expected, query.toString());

        OrderByClause clause = query.getOrderByClause();
        OrderByList elements = clause.elements;
        assertEquals("p", elements.get(0).reference.name);
        assertFalse(elements.get(0).isDescending);
        assertEquals("q", elements.get(1).reference.name);
        assertFalse(elements.get(1).isDescending);
        assertEquals(2, elements.size());

        query = SQLQueryParser.parse("SELECT p, q, r FROM t ORDER BY p, q ASC");
        expected = "SELECT p, q, r FROM t ORDER BY p, q ASC";
        assertEquals(expected, query.toString());

        clause = query.getOrderByClause();
        elements = clause.elements;
        assertEquals("p", elements.get(0).reference.name);
        assertFalse(elements.get(0).isDescending);
        assertEquals("q", elements.get(1).reference.name);
        assertFalse(elements.get(1).isDescending);
        assertEquals(2, elements.size());

        query = SQLQueryParser.parse("SELECT p, q, r FROM t ORDER BY p, q DESC");
        expected = "SELECT p, q, r FROM t ORDER BY p, q DESC";
        assertEquals(expected, query.toString());

        clause = query.getOrderByClause();
        elements = clause.elements;
        assertEquals("p", elements.get(0).reference.name);
        assertFalse(elements.get(0).isDescending);
        assertEquals("q", elements.get(1).reference.name);
        assertTrue(elements.get(1).isDescending);
        assertEquals(2, elements.size());

        query = SQLQueryParser.parse("SELECT p, q, r FROM t ORDER BY p DESC, q");
        expected = "SELECT p, q, r FROM t ORDER BY p DESC, q";
        assertEquals(expected, query.toString());

        clause = query.getOrderByClause();
        elements = clause.elements;
        assertEquals("p", elements.get(0).reference.name);
        assertTrue(elements.get(0).isDescending);
        assertEquals("q", elements.get(1).reference.name);
        assertFalse(elements.get(1).isDescending);
        assertEquals(2, elements.size());
    }

    @Test
    public void testFromTypeClause() {
        SQLQuery query = SQLQueryParser.parse("SELECT p, q, r FROM TYPE t1");
        assertEquals(FromClause.DOCTYPE, query.getFromClause().getType());
        assertEquals("t1", query.getFromClause().get(0));

        query = SQLQueryParser.parse("SELECT p, q, r FROM TYPE t1,t2,t3");
        assertEquals("t1", query.getFromClause().get(0));
        assertEquals("t2", query.getFromClause().get(1));
        assertEquals("t3", query.getFromClause().get(2));

        query = SQLQueryParser.parse("SELECT p, q, r FROM t1,t2,t3");
        assertEquals(FromClause.DOCTYPE, query.getFromClause().getType());
    }

    @Test
    public void testFromLocationClause() {
        SQLQuery query = SQLQueryParser.parse("SELECT p, q, r FROM LOCATION l1");
        assertEquals(FromClause.LOCATION, query.getFromClause().getType());
        assertEquals("l1", query.getFromClause().get(0));

        query = SQLQueryParser.parse("SELECT p, q, r FROM TYPE l1, l2,l3");
        assertEquals("l1", query.getFromClause().get(0));
        assertEquals("l2", query.getFromClause().get(1));
        assertEquals("l3", query.getFromClause().get(2));
    }

    @Test
    public void testGroupByClause() {
        // TODO
        // Query query = QueryParser.parse("SELECT p, q, r FROM t GROUP BY p, q");
    }

    @Test
    public void testHavingClause() {
        // TODO
        // Query query = QueryParser.parse("SELECT p, q, r FROM t HAVING p = 1");
    }

}
