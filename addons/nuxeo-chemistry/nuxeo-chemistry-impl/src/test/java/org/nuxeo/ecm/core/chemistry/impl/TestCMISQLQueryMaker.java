/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.chemistry.impl;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.chemistry.Connection;
import org.apache.chemistry.util.GregorianCalendar;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.storage.sql.CapturingQueryMaker;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.core.storage.sql.CapturingQueryMaker.Captured;
import org.nuxeo.ecm.core.storage.sql.QueryMaker.Query;

public class TestCMISQLQueryMaker extends SQLRepositoryTestCase {

    public NuxeoRepository repo;

    public Connection conn;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        openSession();
        repo = new NuxeoRepository(session.getRepositoryName());
        conn = repo.getConnection(null);
    }

    @Override
    public void tearDown() throws Exception {
        conn.close();
        session.cancel();
        closeSession();
        super.tearDown();
    }

    public void testCMISQLQueryMaker() throws Exception {
        Captured captured = new Captured();
        session.queryAndFetch("", CapturingQueryMaker.TYPE, captured);
        String query;
        Query q;
        String sql;
        String expected;
        List<Serializable> expectedP;
        Set<String> doc_note_file = new HashSet<String>(Arrays.asList(
                "Document", "Note", "File"));

        // basic query

        query = "SELECT cmis:ObjectId, dc:title" //
                + " FROM cmis:Document" //
                + " WHERE dc:title = 123 OR dc:title = 'xyz'" //
                + " ORDER BY dc:description DESC, cmis:parentid ASC";
        q = new CMISQLQueryMaker().buildQuery(captured.sqlInfo, captured.model,
                captured.session, query, null, new Object[] { null });
        assertNotNull(q);
        sql = q.selectInfo.sql.replace("\"", ""); // more readable
        expected = "SELECT HIERARCHY.ID, DUBLINCORE.TITLE, HIERARCHY.PRIMARYTYPE"
                + " FROM HIERARCHY"
                + " LEFT JOIN MISC ON MISC.ID = HIERARCHY.ID"
                + " LEFT JOIN DUBLINCORE ON DUBLINCORE.ID = HIERARCHY.ID"
                + " WHERE HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                + "   AND MISC.LIFECYCLESTATE <> ?"
                + "   AND ((DUBLINCORE.TITLE = ?) OR (DUBLINCORE.TITLE = ?))"
                + " ORDER BY DUBLINCORE.DESCRIPTION DESC, HIERARCHY.PARENTID";
        expectedP = Arrays.<Serializable> asList(Long.valueOf(123), "xyz");
        assertEquals(expected.replaceAll(" +", " "), sql);
        assertEquals(doc_note_file, new HashSet<Serializable>(
                q.selectParams.subList(0, 3)));
        assertEquals(LifeCycleConstants.DELETED_STATE, q.selectParams.get(3));
        assertEquals(expectedP, q.selectParams.subList(4, 6));

        // scalar IN

        query = "SELECT cmis:ObjectId, dc:title" //
                + " FROM cmis:document" //
                + " WHERE dc:title IN ('xyz', 'abc')";
        q = new CMISQLQueryMaker().buildQuery(captured.sqlInfo, captured.model,
                captured.session, query, null, new Object[] { null });
        assertNotNull(q);
        sql = q.selectInfo.sql.replace("\"", ""); // more readable
        expected = "SELECT HIERARCHY.ID, DUBLINCORE.TITLE, HIERARCHY.PRIMARYTYPE"
                + " FROM HIERARCHY"
                + " LEFT JOIN MISC ON MISC.ID = HIERARCHY.ID"
                + " LEFT JOIN DUBLINCORE ON DUBLINCORE.ID = HIERARCHY.ID"
                + " WHERE HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                + "   AND MISC.LIFECYCLESTATE <> ?"
                + "   AND ((DUBLINCORE.TITLE IN (?, ?)))";
        expectedP = Arrays.<Serializable> asList("xyz", "abc");
        assertEquals(expected.replaceAll(" +", " "), sql);
        assertEquals(doc_note_file, new HashSet<Serializable>(
                q.selectParams.subList(0, 3)));
        assertEquals(LifeCycleConstants.DELETED_STATE, q.selectParams.get(3));
        assertEquals(expectedP, q.selectParams.subList(4, 6));

        // query with ANY quantifier

        query = "SELECT cmis:objectId" //
                + " FROM cmis:document" //
                + " WHERE 'bob' = ANY dc:contributors";
        q = new CMISQLQueryMaker().buildQuery(captured.sqlInfo, captured.model,
                captured.session, query, null, new Object[] { null });
        assertNotNull(q);
        sql = q.selectInfo.sql.replace("\"", ""); // more readable
        expected = "SELECT HIERARCHY.ID, HIERARCHY.PRIMARYTYPE" //
                + " FROM HIERARCHY"
                + " LEFT JOIN MISC ON MISC.ID = HIERARCHY.ID"
                + " WHERE HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                + "   AND MISC.LIFECYCLESTATE <> ?"
                + "   AND (EXISTS (SELECT 1 FROM DC_CONTRIBUTORS _nxm1_DC_CONTRIBUTORS" //
                + "     WHERE HIERARCHY.ID = _nxm1_DC_CONTRIBUTORS.ID" //
                + "       AND _nxm1_DC_CONTRIBUTORS.ITEM = ?" //
                + "))";
        expectedP = Arrays.<Serializable> asList("bob");
        assertEquals(expected.replaceAll(" +", " "), sql);
        assertEquals(doc_note_file, new HashSet<Serializable>(
                q.selectParams.subList(0, 3)));
        assertEquals(LifeCycleConstants.DELETED_STATE, q.selectParams.get(3));
        assertEquals(expectedP, q.selectParams.subList(4, 5));

        // join query

        // join on non-hierarchy table to test query generation and types
        query = "SELECT A.cmis:ObjectId, B.dc:title" //
                + " FROM cmis:Document A" //
                + " JOIN cmis:Document B ON A.cmis:ObjectId = B.dc:title" //
                + " WHERE A.dc:title = '123' OR B.dc:title = 'xyz'";
        q = new CMISQLQueryMaker().buildQuery(captured.sqlInfo, captured.model,
                captured.session, query, null, new Object[] { null });
        assertNotNull(q);
        sql = q.selectInfo.sql.replace("\"", ""); // more readable
        expected = "SELECT _A_HIERARCHY.ID, _B_DUBLINCORE.TITLE,"
                + " _A_HIERARCHY.PRIMARYTYPE, _B_HIERARCHY.ID, _B_HIERARCHY.PRIMARYTYPE"
                + " FROM HIERARCHY _A_HIERARCHY"
                + " LEFT JOIN MISC _A_MISC ON _A_MISC.ID = _A_HIERARCHY.ID"
                + " LEFT JOIN DUBLINCORE _A_DUBLINCORE ON _A_DUBLINCORE.ID = _A_HIERARCHY.ID"
                + " JOIN DUBLINCORE _B_DUBLINCORE ON _A_HIERARCHY.ID = _B_DUBLINCORE.TITLE"
                + " LEFT JOIN MISC _B_MISC ON _B_MISC.ID = _B_DUBLINCORE.ID"
                + " LEFT JOIN HIERARCHY _B_HIERARCHY ON _B_HIERARCHY.ID = _B_DUBLINCORE.ID"
                + " WHERE _A_HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                + "   AND _A_MISC.LIFECYCLESTATE <> ?"
                + "   AND _B_HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                + "   AND _B_MISC.LIFECYCLESTATE <> ?"
                + "   AND ((_A_DUBLINCORE.TITLE = ?) OR (_B_DUBLINCORE.TITLE = ?))";
        expectedP = Arrays.<Serializable> asList("123", "xyz");
        assertEquals(expected.replaceAll(" +", " "), sql);
        assertEquals(doc_note_file, new HashSet<Serializable>(
                q.selectParams.subList(0, 3)));
        assertEquals(LifeCycleConstants.DELETED_STATE, q.selectParams.get(3));
        assertEquals(doc_note_file, new HashSet<Serializable>(
                q.selectParams.subList(4, 7)));
        assertEquals(LifeCycleConstants.DELETED_STATE, q.selectParams.get(7));
        assertEquals(expectedP, q.selectParams.subList(8, 10));

        // join query with ANY quantifier

        query = "SELECT A.cmis:objectId, B.dc:title" //
                + " FROM cmis:document A" //
                + " JOIN cmis:document B ON A.cmis:objectId = B.cmis:parentId" //
                + " WHERE A.dc:title = '123' OR 'bob' = ANY B.dc:contributors";
        q = new CMISQLQueryMaker().buildQuery(captured.sqlInfo, captured.model,
                captured.session, query, null, new Object[] { null });
        assertNotNull(q);
        sql = q.selectInfo.sql.replace("\"", ""); // more readable
        expected = "SELECT _A_HIERARCHY.ID, _B_DUBLINCORE.TITLE,"
                + " _A_HIERARCHY.PRIMARYTYPE, _B_HIERARCHY.ID,"
                + " _B_HIERARCHY.PRIMARYTYPE"
                + " FROM HIERARCHY _A_HIERARCHY"
                + " LEFT JOIN MISC _A_MISC ON _A_MISC.ID = _A_HIERARCHY.ID"
                + " LEFT JOIN DUBLINCORE _A_DUBLINCORE ON _A_DUBLINCORE.ID = _A_HIERARCHY.ID"
                + " JOIN HIERARCHY _B_HIERARCHY ON _A_HIERARCHY.ID = _B_HIERARCHY.PARENTID"
                + " LEFT JOIN DUBLINCORE _B_DUBLINCORE ON _B_DUBLINCORE.ID = _B_HIERARCHY.ID"
                + " LEFT JOIN MISC _B_MISC ON _B_MISC.ID = _B_HIERARCHY.ID"
                + " WHERE _A_HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                + "   AND _A_MISC.LIFECYCLESTATE <> ?"
                + "   AND _B_HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                + "   AND _B_MISC.LIFECYCLESTATE <> ?"
                + "   AND ((_A_DUBLINCORE.TITLE = ?) OR"
                + "     EXISTS (SELECT 1 FROM DC_CONTRIBUTORS _nxm1_DC_CONTRIBUTORS" //
                + "       WHERE _B_HIERARCHY.ID = _nxm1_DC_CONTRIBUTORS.ID" //
                + "         AND _nxm1_DC_CONTRIBUTORS.ITEM = ?" //
                + "))";
        expectedP = Arrays.<Serializable> asList("123", "bob");
        assertEquals(expected.replaceAll(" +", " "), sql);
        assertEquals(doc_note_file, new HashSet<Serializable>(
                q.selectParams.subList(0, 3)));
        assertEquals(LifeCycleConstants.DELETED_STATE, q.selectParams.get(3));
        assertEquals(doc_note_file, new HashSet<Serializable>(
                q.selectParams.subList(4, 7)));
        assertEquals(LifeCycleConstants.DELETED_STATE, q.selectParams.get(7));
        assertEquals(expectedP, q.selectParams.subList(8, 10));

        // SELECT *

        query = "SELECT * FROM cmis:document WHERE dc:title = 123";
        q = new CMISQLQueryMaker().buildQuery(captured.sqlInfo, captured.model,
                captured.session, query, null, conn);
        assertNotNull(q);
        sql = q.selectInfo.sql.replace("\"", ""); // more readable
        expected = "SELECT HIERARCHY.ID, HIERARCHY.PRIMARYTYPE, HIERARCHY.NAME,"
                + "   DUBLINCORE.CREATOR, DUBLINCORE.CREATED, DUBLINCORE.MODIFIED"
                + " FROM HIERARCHY"
                + " LEFT JOIN MISC ON MISC.ID = HIERARCHY.ID"
                + " LEFT JOIN DUBLINCORE ON DUBLINCORE.ID = HIERARCHY.ID"
                + " WHERE HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                + "   AND MISC.LIFECYCLESTATE <> ?"
                + "   AND ((DUBLINCORE.TITLE = ?))";
        expectedP = Arrays.<Serializable> asList(Long.valueOf(123));
        assertEquals(expected.replaceAll(" +", " "), sql);
        assertEquals(doc_note_file, new HashSet<Serializable>(
                q.selectParams.subList(0, 3)));
        assertEquals(LifeCycleConstants.DELETED_STATE, q.selectParams.get(3));
        assertEquals(expectedP, q.selectParams.subList(4, 5));

        // boolean / datetime

        query = "SELECT cmis:objectId FROM cmis:document "
                + "WHERE dc:title = true or dc:title = TIMESTAMP '2010-01-01T00:00:00.123Z'";
        q = new CMISQLQueryMaker().buildQuery(captured.sqlInfo, captured.model,
                captured.session, query, null, conn);
        assertNotNull(q);
        sql = q.selectInfo.sql.replace("\"", ""); // more readable
        expected = "SELECT HIERARCHY.ID, HIERARCHY.PRIMARYTYPE"
                + " FROM HIERARCHY"
                + " LEFT JOIN MISC ON MISC.ID = HIERARCHY.ID"
                + " LEFT JOIN DUBLINCORE ON DUBLINCORE.ID = HIERARCHY.ID"
                + " WHERE HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                + "   AND MISC.LIFECYCLESTATE <> ?"
                + "   AND ((DUBLINCORE.TITLE = ?) OR (DUBLINCORE.TITLE = ?))";
        expectedP = Arrays.<Serializable> asList(Boolean.TRUE,
                GregorianCalendar.fromAtomPub("2010-01-01T00:00:00.123Z"));
        assertEquals(expected.replaceAll(" +", " "), sql);
        assertEquals(doc_note_file, new HashSet<Serializable>(
                q.selectParams.subList(0, 3)));
        assertEquals(LifeCycleConstants.DELETED_STATE, q.selectParams.get(3));
        assertEquals(expectedP, q.selectParams.subList(4, 6));
    }

}
