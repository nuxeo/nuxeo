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

import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.query.sql.model.StringLiteral;
import org.nuxeo.ecm.core.query.sql.model.WhereClause;

/**
 * @author DM
 *
 */
public class TestQueryParser2 extends TestCase {

    static final String[] CANONICAL_QUERIES = new String[] {
            "SELECT * FROM document",
            "SELECT title FROM document",
            "SELECT title, author FROM document",
            "SELECT title FROM document WHERE created > '20060523'",
            "SELECT * FROM t",
            "SELECT DISTINCT p FROM t",
            "SELECT getDate(), p FROM t WHERE substring(title, 2) = 'test'",
            "SELECT dc:title FROM Document WHERE dc:description = 'test'",
            "SELECT name, title, description FROM folder WHERE state = 2 AND created > '20060523'",
            "SELECT p FROM t WHERE p * -2 / 3 + 4 - 5 = 2",
            "SELECT p FROM t WHERE p * 2 = 3",
            "SELECT p FROM t WHERE p + 2 = 2",
            "SELECT p FROM t WHERE p - 2 = 3",
            "SELECT p FROM t WHERE p / 2 = 4",
            "SELECT p FROM t WHERE p < 1",
            "SELECT p FROM t WHERE p <= 1",
            "SELECT p FROM t WHERE p <> 1",
            "SELECT p FROM t WHERE p = 'test'",
            "SELECT p FROM t WHERE p > 1",
            "SELECT p FROM t WHERE p >= 1",
            "SELECT p FROM t WHERE p LIKE '%test%'",
            "SELECT p FROM t WHERE p NOT LIKE '%test%'",
            "SELECT p FROM t WHERE p = 2 AND q = 1 AND s = 3 OR r = 4 OR w = 10",
            "SELECT p FROM t WHERE p = 2 OR s = 3 AND NOT q = 4",
            "SELECT p FROM t WHERE title = DATE '2007-01-30'",
            "SELECT p FROM t WHERE title = TIMESTAMP '2007-01-30T01:02:03.000+04:00'",
            "SELECT p FROM t WHERE title = '%test'",
    };

    static final String[] GOOD_QUERIES = new String[] {
        "SELECT * FROM document",
        "SELECT title FROM document",
        "SELECT title, author FROM document",
        "SELECT title FROM document WHERE created > \"20060523\"",

        "SELECT * FROM t",
        "SELECT ALL p FROM t",
        "SELECT DISTINCT p FROM t",
        "SELECT getDate(), p FROM t WHERE substring(title, 2) = 'test'",
        "SELECT dc:title FROM Document WHERE dc:description = 'test'",
        "SELECT name, title, description FROM folder WHERE state = 2 AND created > \"20060523\"",
        "SELECT p AS pp, q AS qq, r FROM t AS t1",
        "SELECT q FROM t WHERE q != 1",
        "SELECT p FROM t WHERE p * -2 / 3 + 4 - 5 = 2",
        "SELECT p FROM t WHERE p * 2 = 3",
        "SELECT p FROM t WHERE p + 2 = 2",
        "SELECT p FROM t WHERE p - 2 = 3",
        "SELECT p FROM t WHERE p / 2 = 4",
        "SELECT p FROM t WHERE p < 1",
        "SELECT p FROM t WHERE p <= 1",
        "SELECT p FROM t WHERE p <> 1",
        "SELECT p FROM t WHERE p = 'test'",
        "SELECT p FROM t WHERE p > 1",
        "SELECT p FROM t WHERE p >= 1",
        "SELECT p FROM t WHERE p BETWEEN 10 AND 20 AND q='test'",
        "SELECT p FROM t WHERE p IN (12, 13) AND q='test'",
        "SELECT p FROM t WHERE p LIKE '%test%'",
        "SELECT p FROM t WHERE p NOT BETWEEN 10 AND 20 AND q='test'",
        "SELECT p FROM t WHERE p NOT IN (12, 13) AND q='test'",
        "SELECT p FROM t WHERE p NOT LIKE '%test%'",
        "SELECT p FROM t WHERE p=2 AND q=1 AND s=3 OR r = 4 OR w = 10",
        "SELECT p FROM t WHERE p=2 OR s=3 AND NOT q=4",
        "SELECT p FROM t WHERE title = DATE '2007-01-30'",
        "SELECT p FROM t WHERE title = TIMESTAMP '2007-01-30 01:02:03+04:00'",
        "SELECT p FROM t WHERE title = '%te\\'s\"t'",
        "SELECT p FROM t WHERE title0 = 'te\\st'",
        "SELECT p FROM t WHERE title = .2",
        "SELECT p FROM t WHERE title = 1.2",
        "SELECT p FROM t WHERE title = 12",
        "SELECT p FROM t WHERE title1 = \"%te\\\"s't\"",
        "SELECT p FROM t WHERE title2 = \"%test\"",
        "SELECT p FROM t WHERE title3 = \"te\\st\"",
        "SELECT p, q FROM LOCATION l1",
        "SELECT p, q FROM TYPE l1, l2,l3",
        "SELECT p, q FROM TYPE t1",
        "SELECT p, q FROM TYPE t1,t2,t3",
        "SELECT p, q, r FROM t ORDER BY p, q",
        "SELECT p, q FROM t ORDER BY p, q ASC",
        "SELECT p, q FROM t ORDER BY p, q DESC",
        "SELECT p, q, r FROM t1, t2, t3",
        "SELECT p1, $id, p3 FROM t1, t2 WHERE state=1 AND title = 'test'",
        "SELECT p1, p2 FROM t WHERE (title = \"test\" OR p2 >= 10.2) AND p1 + p2 < 5",
        "SELECT p1, p2 FROM t WHERE title = \"test\" OR p2 >= 10.2 AND p1 + p2 < 5",
        "SELECT p1, p2 FROM table WHERE p1 > 0 OR p2 <= 10.2 AND p1 - p2 = 5",
        "SELECT * FROM Document WHERE (dc:creator = 'Pedro' OR dc:creator = 'Piotr' OR dc:creator = 'Pierre')",

        "SELECT * FROM Document WHERE (dc:creator = 'default1' OR dc:creator = 'default2')",
        "SELECT * FROM Document WHERE dc:contributors = 'Administrator' AND ecm:path STARTSWITH 'somelocation'",
        "SELECT * FROM Document WHERE dc:created < DATE '2006-12-15' ORDER BY dc:modified DESC",
        "SELECT * FROM Document WHERE dc:created < DATE '2006-12-15' ORDER BY dc:modified",
        "SELECT * FROM Document WHERE dc:created < DATE '2006-12-15'",
        "SELECT p1, p2 FROM t WHERE title = \"test\" OR p2 >= 10.2 AND p1 + p2 < 5",
        "SELECT p1, p2 FROM table WHERE p1 > 0 OR p2 <= 10.2 AND p1 - p2 = 5",
        "SELECT * FROM Document WHERE (dc:creator = 'Pedro' OR dc:creator = 'Piotr' OR dc:creator = 'Pierre')",

        "SELECT * FROM Document WHERE (dc:creator = 'default1' OR dc:creator = 'default2')",
        "SELECT * FROM Document WHERE dc:contributors = 'Administrator' AND ecm:path STARTSWITH 'somelocation'",
        "SELECT * FROM Document WHERE dc:created < DATE '2006-12-15' ORDER BY dc:modified DESC",
        "SELECT * FROM Document WHERE dc:created < DATE '2006-12-15' ORDER BY dc:modified",
        "SELECT * FROM Document WHERE dc:created < DATE '2006-12-15'",
        "SELECT * FROM Document WHERE dc:created > DATE '2006-10-12'",
        "SELECT * FROM Document WHERE dc:created BETWEEN DATE '2006-10-12' AND DATE '2006-12-15'",
        "SELECT * FROM Document WHERE dc:creator = 'Pedro'",
        "SELECT * FROM Document WHERE intparameter < '3'",
        "SELECT * FROM Document WHERE textparameter = 'some text' AND intparameter < '3'",
        "SELECT * FROM Document WHERE textparameter = 'some text'",

        "SELECT * FROM document WHERE ecm:path STARTSWITH '/'",
        "SELECT * FROM document WHERE ecm:path STARTSWITH '/a'",
        "SELECT * FROM document WHERE ecm:path STARTSWITH '/alpha/beta'",

        // Joins by the where clause
        "SELECT * FROM document WHERE ecm:id = relation.subject",
        "SELECT * FROM document WHERE ecm:id = relation.subject " +
            " AND dc:title='foo'",

        // Reported bogus
        "SELECT * FROM Document WHERE NOT ecm:path STARTSWITH '/some/path'",
    };

    static final String[] CASE_INDEPENDENCY_GOOD_QUERIES = new String[] {
        "Select title from document",
        "select title From document",
        "SELECT q FROM t where q != 1",
        "SELECT q FROM t Where q != 1",
        "SELECT p FROM t WHERE p Between 10 And 20 And q='test'",
        "SELECT p FROM t WHERE p between 10 And 20 and q='test'",
        "SELECT p FROM t WHERE p In (12, 13) AND q='test'",
        "SELECT p FROM t WHERE p in (12, 13) AND q='test'",
        "SELECT p FROM t WHERE p Like '%test%'",
        "SELECT p FROM t WHERE p like'%test%'",
        "SELECT p FROM t WHERE p not BETWEEN 10 AND 20 AND q='test'",
        "SELECT p FROM t WHERE p Not BETWEEN 10 AND 20 AND q='test'",
        "SELECT p FROM t WHERE p=2 or s=3 AND NOT q=4",
        "SELECT p FROM t WHERE p=2 Or s=3 AND NOT q=4",
        "SELECT p FROM t WHERE title = Timestamp '2007-01-30 01:02:03+04:00'",
        "SELECT p FROM t WHERE title = TimeStamp '2007-01-30 01:02:03+04:00'",
        "SELECT p FROM t WHERE title = timestamp '2007-01-30 01:02:03+04:00'",
        "SELECT p, q FROM location l1",
        "SELECT p, q FROM Location l1",
        "SELECT p, q FROM Type l1, l2,l3",
        "SELECT p, q FROM type l1, l2,l3",

        // Not parsed ?
        //"SELECT p, q, r FROM t GROUP BY p, q",
        //"SELECT p, q, r FROM t HAVING p = 1",

        "SELECT p, q, r FROM t order by p, q",
        "SELECT p, q, r FROM t Order By p, q",
        "SELECT p, q FROM t ORDER BY p, q Asc",
        "SELECT p, q FROM t ORDER BY p, q asc",
        "SELECT p, q FROM t ORDER BY p, q desc",
        "SELECT p, q FROM t ORDER BY p, q Desc",
        "SELECT * FROM Document WHERE dc:created < date '2006-12-15' ORDER BY dc:modified DESC",
        "SELECT * FROM Document WHERE dc:created < Date '2006-12-15' ORDER BY dc:modified DESC",
        "SELECT * FROM document WHERE ecm:path StartsWith '/'",
        "SELECT * FROM document WHERE ecm:path Startswith '/'",
        "SELECT * FROM document WHERE ecm:path startswith '/'",
    };

    static final String[] BAD_QUERIES = new String[] {
            "SELECT * FROM document WHERE ecm:path STARTSWITH",
            "SELECT * FROM document WHERE ecm:path STARTWITH '/'",
            "SELECT * WHERE ecm:path STARTWITH '/'",
            "SELECT * WHERE ecm:path STARTSWITH",
            "SELECT * WHERE ecm:path STARTSWITH xxx" };

    static final String[] BAD_QUERIES_OLD = new String[] {
            "SELECT * FROM document WHERE LOCATION STARTSWITH '/a'",
            "SELECT * FROM document WHERE LOCATION STARTSWITH '/alpha/beta'" ,
            "SELECT * WHERE location STARTWITH '/'",
            "SELECT * WHERE location STARTSWITH",
            "SELECT * WHERE location STARTSWITH xxx" };

    /**
     * Tests that good queries (queries from GOOD_QUERIES array) are successfully
     * parsed.
     */
    public void testGoodQueries() {
        int i = 0;
        try {
            for (; i < GOOD_QUERIES.length; i++) {
                SQLQueryParser.parse(GOOD_QUERIES[i]);
            }
        } catch (QueryParseException e) {
            fail("Failed to parse a good query: \n" + GOOD_QUERIES[i]
                    + "\nCause is:\n"
                    + ExceptionUtils.getRootCause(e).getMessage());
        }
    }

    /**
     * Tests that parsing fail for bad queries (queries fom BAD_QUERIES array).
     */
    public void testBadQueries() {
        for (String badQuery : BAD_QUERIES) {
            try {
                SQLQueryParser.parse(badQuery);
                // Not so bad this query: bad query was successfully parsed ->
                // error
                fail("A bad Query has been successfully parsed: " + badQuery);
            } catch (QueryParseException e) {
                // this is really a bad query -> continue
            }
        }
    }

    public void testBadQueriesOld() {
        for (String badQuery : BAD_QUERIES_OLD) {
            try {
                SQLQueryParser.parse(badQuery);
                // Not so bad this query: bad query was successfully parsed ->
                // error
                fail("A bad Query has been successfully parsed: " + badQuery);
            } catch (QueryParseException e) {
                // this is really a bad query -> continue
            }
        }
    }

    public void testLocationLiterals() {
        // test double quoted strings
        SQLQuery query = SQLQueryParser
                .parse("SELECT p FROM t WHERE ecm:path STARTSWITH '/test'");
        WhereClause whereClause = query.getWhereClause();
        assertEquals(Operator.STARTSWITH, whereClause.predicate.operator);

        Reference sleft = (Reference) whereClause.predicate.lvalue;
        assertEquals("ecm:path", sleft.name);
        StringLiteral sright = (StringLiteral) whereClause.predicate.rvalue;
        assertEquals("/test", sright.value);
    }

    /**
     * Tests equals method on SQLQuery.
     */
    public void testEquals() {
        for (String s : GOOD_QUERIES) {
            SQLQuery query1 = SQLQueryParser.parse(s);
            SQLQuery query2 = SQLQueryParser.parse(s);
            assertEquals("Self equality test on " + s + ": ", query1, query2);
            assertEquals(query1.hashCode(), query2.hashCode());
            assertEquals(query1.toString(), query2.toString());
        }
    }

    /**
     * For non-equality, one must make sure that no two queries are equivalent
     * in the GOOD_QUERIES list (ex: "p != 1" and "p <> 1").
     */
    public void testNotEquals() {
        for (String s1 : GOOD_QUERIES) {
            for (String s2 : GOOD_QUERIES) {
                if (s1.equals(s2)) {
                    continue;
                }
                SQLQuery query1 = SQLQueryParser.parse(s1);
                SQLQuery query2 = SQLQueryParser.parse(s2);
                assertFalse("Non-equality test on " + s1 + " and " + s2 + ": ",
                        query1.equals(query2));
            }
        }
    }

    public void testToString() {
        for (String s : CANONICAL_QUERIES) {
            SQLQuery query1 = SQLQueryParser.parse(s);
            assertEquals(s, query1.toString());
        }
    }

}
