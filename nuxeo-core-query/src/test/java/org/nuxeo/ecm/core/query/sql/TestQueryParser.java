/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.query.sql;

import junit.framework.TestCase;

import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.model.DateLiteral;
import org.nuxeo.ecm.core.query.sql.model.DoubleLiteral;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.FromClause;
import org.nuxeo.ecm.core.query.sql.model.Function;
import org.nuxeo.ecm.core.query.sql.model.IntegerLiteral;
import org.nuxeo.ecm.core.query.sql.model.LiteralList;
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
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TestQueryParser extends TestCase {

    static final String[] GOOD_QUERIES = {
            "SELECT name, title, description FROM folder WHERE state = 2 AND created > \"20060523\""
    };

    static final String[] BAD_QUERIES = {
            "SELECT name WHERE title, description FROM folder WHERE state = 2 AND created > \"20060523\"",
            "name, ",
            "SELECT FROM WHERE",
            "SELECT name WHERE state=2",
            "PLEASE GET ME SOME DOCUMENTS"
    };

    /**
     * Checks that literals are correctly parsed.
     *
     */
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
        assertEquals(1.2, dl.value);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE title=.2");
        dl = (DoubleLiteral) query.getWhereClause().predicate.rvalue;
        assertEquals(0.2, dl.value);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE title=-1.2");
        dl = (DoubleLiteral) query.getWhereClause().predicate.rvalue;
        assertEquals(-1.2, dl.value);

        // dates
        DateLiteral datel;
        query = SQLQueryParser.parse("SELECT p FROM t WHERE title = DATE '2007-01-30'");
        datel = (DateLiteral) query.getWhereClause().predicate.rvalue;
        assertEquals("DATE '2007-01-30'", datel.toString());
        query = SQLQueryParser.parse("SELECT p FROM t WHERE title = TIMESTAMP '2007-01-30 01:02:03+04:00'");
        datel = (DateLiteral) query.getWhereClause().predicate.rvalue;
        assertEquals("TIMESTAMP '2007-01-30T01:02:03.000+04:00'", datel.toString());
    }

    public void testNamespace() {
        SQLQuery query = SQLQueryParser.parse("SELECT dc:title FROM Document WHERE dc:description = 'test'");
        SelectClause select = query.getSelectClause();
        String v = select.getVariable(0).name;
        assertEquals("dc:title", v);
    }

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
        assertEquals(Operator.SUB , op);
        query = SQLQueryParser.parse("SELECT p FROM t WHERE p * 2 = 3");
        op = ((Expression) query.getWhereClause().predicate.lvalue).operator;
        assertEquals(Operator.MUL, op);
        query = SQLQueryParser.parse("SELECT p FROM t WHERE p / 2 = 4");
        op = ((Expression) query.getWhereClause().predicate.lvalue).operator;
        assertEquals(Operator.DIV, op);
    }

    public void testLikeOperator() {
        SQLQuery query = SQLQueryParser.parse("SELECT p FROM t WHERE p LIKE '%test%'");
        Operator op = query.getWhereClause().predicate.operator;
        assertEquals(Operator.LIKE, op);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE p NOT LIKE '%test%'");
        op = query.getWhereClause().predicate.operator;
        assertEquals(Operator.NOTLIKE, op);
    }

    public void testInOperator() {
        SQLQuery query = SQLQueryParser.parse("SELECT p FROM t WHERE p IN (12, 13) AND q='test'");
        Operator op = query.getWhereClause().predicate.operator;
        assertEquals(Operator.AND, op);
        Expression e = (Expression) query.getWhereClause().predicate.lvalue;
        assertEquals(Operator.IN, e.operator);
        LiteralList list = new LiteralList();
        list.add(new IntegerLiteral(12)); list.add(new IntegerLiteral(13));
        assertEquals(list, e.rvalue);
        assertEquals("p", ((Reference) e.lvalue).name);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE p NOT IN (12, 13) AND q='test'");
        op = query.getWhereClause().predicate.operator;
        assertEquals(Operator.AND, op);
        e = (Expression) query.getWhereClause().predicate.lvalue;
        assertEquals(Operator.NOTIN, e.operator);
        list = new LiteralList();
        list.add(new IntegerLiteral(12)); list.add(new IntegerLiteral(13));
        assertEquals(list, e.rvalue);
        assertEquals("p", ((Reference) e.lvalue).name);
    }

    public void testBetweenOperator() {
        SQLQuery query = SQLQueryParser.parse("SELECT p FROM t WHERE p BETWEEN 10 AND 20 AND q='test'");
        Operator op = query.getWhereClause().predicate.operator;
        assertEquals(Operator.AND, op);
        Expression e = (Expression) query.getWhereClause().predicate.lvalue;
        assertEquals(Operator.BETWEEN, e.operator);
        LiteralList list = new LiteralList();
        list.add(new IntegerLiteral(10)); list.add(new IntegerLiteral(20));
        assertEquals(list, e.rvalue);
        assertEquals("p", ((Reference) e.lvalue).name);

        query = SQLQueryParser.parse("SELECT p FROM t WHERE p NOT BETWEEN 10 AND 20 AND q='test'");
        op = query.getWhereClause().predicate.operator;
        assertEquals(Operator.AND, op);
        e = (Expression) query.getWhereClause().predicate.lvalue;
        assertEquals(Operator.NOTBETWEEN, e.operator);
        list = new LiteralList();
        list.add(new IntegerLiteral(10)); list.add(new IntegerLiteral(20));
        assertEquals(list, e.rvalue);
        assertEquals("p", ((Reference) e.lvalue).name);
    }

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
     * <pre>
     *              OR
     *    p1>0                  AND
     *              p2<=10.2            =
     *                          p1-p2        5
     * </pre>
     * TODO: add tests for DateLiteral, other operators, paranthesys etc
     */
    public void testAST() {
        SQLQuery query = SQLQueryParser.parse(
                "SELECT p1, p2 FROM table WHERE p1 > 0 OR p2 <= 10.2 AND p1 - p2 = 5");

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
        assertEquals(10.2, ((DoubleLiteral) expr1.rvalue).value);

        Expression expr2 = (Expression) expr.rvalue;
        assertEquals(Operator.EQ, expr2.operator);
        assertEquals(5, ((IntegerLiteral) expr2.rvalue).value);

        Expression expr3 = (Expression) expr2.lvalue;
        assertEquals(Operator.SUB, expr3.operator);
        assertEquals("p1", ((Reference) expr3.lvalue).name);
        assertEquals("p2", ((Reference) expr3.rvalue).name);
    }

    /**
     * Tests the manual query creation and parser by comparing the two queries
     * (the manual one ith the parsed one).
     * <pre>
     *              OR
     *    title="test"         AND
     *              p2>=10.2            &lt;
     *                          p1+p2        5
     * </pre>
     */
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
        SQLQuery myquery = new SQLQuery(new SelectClause(), new FromClause(),
                new WhereClause(pred));
        myquery.getSelectClause().add(new Reference("p1"));
        myquery.getSelectClause().add(new Reference("p2"));
        myquery.getFromClause().add("t");

        assertEquals(myquery, query);
    }

    /**
     * Same query as before but with parenthesis on first OR condition.
     * <pre>
     *                          AND
     *               OR                     <
     *                                p1+p2      5
     *    title="test"   p2>=10.2
     * </pre>
     */
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

        SQLQuery myquery = new SQLQuery(new SelectClause(), new FromClause(),
                new WhereClause(pred));

        myquery.getSelectClause().add(new Reference("p1"));
        myquery.getSelectClause().add(new Reference("p2"));
        myquery.getFromClause().add("t");

        assertEquals(myquery, query);
    }

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

    public void testFromTypeClause(){
        SQLQuery query = SQLQueryParser.parse("SELECT p, q, r FROM TYPE t1");
        assertEquals(FromClause.DOCTYPE, query.getFromClause().getType());
        assertEquals("t1", query.getFromClause().elements.get(0));
        query = SQLQueryParser.parse("SELECT p, q, r FROM TYPE t1,t2,t3");
        assertEquals("t1", query.getFromClause().elements.get(0));
        assertEquals("t2", query.getFromClause().elements.get(1));
        assertEquals("t3", query.getFromClause().elements.get(2));
        query = SQLQueryParser.parse("SELECT p, q, r FROM t1,t2,t3");
        assertEquals(FromClause.DOCTYPE, query.getFromClause().getType());
    }

    public void testFromLocationClause(){
        SQLQuery query = SQLQueryParser.parse("SELECT p, q, r FROM LOCATION l1");
        assertEquals(FromClause.LOCATION, query.getFromClause().getType());
        assertEquals("l1", query.getFromClause().elements.get(0));
        query = SQLQueryParser.parse("SELECT p, q, r FROM TYPE l1, l2,l3");
        assertEquals("l1", query.getFromClause().elements.get(0));
        assertEquals("l2", query.getFromClause().elements.get(1));
        assertEquals("l3", query.getFromClause().elements.get(2));
    }

    public void testGroupByClause() {
        // TODO
        //Query query = QueryParser.parse("SELECT p, q, r FROM t GROUP BY p, q");
    }

    public void testHavingClause() {
        // TODO
        //Query query = QueryParser.parse("SELECT p, q, r FROM t HAVING p = 1");
    }

    public void testPrepareStringLiteral() {
        assertEquals("'foo'", SQLQueryParser.prepareStringLiteral("foo"));
        assertEquals("'can\\'t'", SQLQueryParser.prepareStringLiteral("can't"));
    }

}
