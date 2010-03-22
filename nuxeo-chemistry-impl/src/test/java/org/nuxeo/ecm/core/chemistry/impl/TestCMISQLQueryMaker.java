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
import org.nuxeo.ecm.core.storage.sql.DatabaseH2;
import org.nuxeo.ecm.core.storage.sql.DatabasePostgreSQL;
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
        if (database instanceof DatabaseH2) {
            expected = "SELECT HIERARCHY.ID, DUBLINCORE.TITLE, HIERARCHY.PRIMARYTYPE"
                    + " FROM HIERARCHY"
                    + " LEFT JOIN DUBLINCORE ON DUBLINCORE.ID = HIERARCHY.ID"
                    + " LEFT JOIN MISC ON MISC.ID = HIERARCHY.ID"
                    + " WHERE HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                    + "   AND MISC.LIFECYCLESTATE <> ?"
                    + "   AND ((DUBLINCORE.TITLE = ?) OR (DUBLINCORE.TITLE = ?))"
                    + " ORDER BY DUBLINCORE.DESCRIPTION DESC, HIERARCHY.PARENTID";
        } else if (database instanceof DatabasePostgreSQL) {
            expected = "SELECT hierarchy.id, dublincore.title, hierarchy.primarytype"
                    + " FROM hierarchy"
                    + " LEFT JOIN dublincore ON dublincore.id = hierarchy.id"
                    + " LEFT JOIN misc ON misc.id = hierarchy.id"
                    + " WHERE hierarchy.primarytype IN (?, ?, ?)"
                    + "   AND misc.lifecyclestate <> ?"
                    + "   AND ((dublincore.title = ?) OR (dublincore.title = ?))"
                    + " ORDER BY dublincore.description DESC, hierarchy.parentid";
        } else {
            return; // TODO other databases
        }
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
        if (database instanceof DatabaseH2) {
            expected = "SELECT HIERARCHY.ID, DUBLINCORE.TITLE, HIERARCHY.PRIMARYTYPE"
                    + " FROM HIERARCHY"
                    + " LEFT JOIN DUBLINCORE ON DUBLINCORE.ID = HIERARCHY.ID"
                    + " LEFT JOIN MISC ON MISC.ID = HIERARCHY.ID"
                    + " WHERE HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                    + "   AND MISC.LIFECYCLESTATE <> ?"
                    + "   AND ((DUBLINCORE.TITLE IN (?, ?)))";
        } else if (database instanceof DatabasePostgreSQL) {
            expected = "SELECT hierarchy.id, dublincore.title, hierarchy.primarytype"
                    + " FROM hierarchy"
                    + " LEFT JOIN dublincore ON dublincore.id = hierarchy.id"
                    + " LEFT JOIN misc ON misc.id = hierarchy.id"
                    + " WHERE hierarchy.primarytype IN (?, ?, ?)"
                    + "   AND misc.lifecyclestate <> ?"
                    + "   AND ((dublincore.title IN (?, ?)))";
        }
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
        if (database instanceof DatabaseH2) {
            expected = "SELECT HIERARCHY.ID, HIERARCHY.PRIMARYTYPE" //
                    + " FROM HIERARCHY"
                    + " LEFT JOIN MISC ON MISC.ID = HIERARCHY.ID"
                    + " WHERE HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                    + "   AND MISC.LIFECYCLESTATE <> ?"
                    + "   AND (EXISTS (SELECT 1 FROM DC_CONTRIBUTORS _nxm1_DC_CONTRIBUTORS" //
                    + "     WHERE HIERARCHY.ID = _nxm1_DC_CONTRIBUTORS.ID" //
                    + "       AND _nxm1_DC_CONTRIBUTORS.ITEM = ?" //
                    + "))";
        } else if (database instanceof DatabasePostgreSQL) {
            expected = "SELECT hierarchy.id, hierarchy.primarytype" //
                    + " FROM hierarchy"
                    + " LEFT JOIN misc ON misc.id = hierarchy.id"
                    + " WHERE hierarchy.primarytype IN (?, ?, ?)"
                    + "   AND misc.lifecyclestate <> ?"
                    + "   AND (EXISTS (SELECT 1 FROM dc_contributors _nxm1_dc_contributors" //
                    + "     WHERE hierarchy.id = _nxm1_dc_contributors.id" //
                    + "       AND _nxm1_dc_contributors.item = ?" //
                    + "))";
        }
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
        if (database instanceof DatabaseH2) {
            expected = "SELECT _A_HIERARCHY.ID, _B_DUBLINCORE.TITLE,"
                    + " _A_HIERARCHY.PRIMARYTYPE, _B_HIERARCHY.ID, _B_HIERARCHY.PRIMARYTYPE"
                    + " FROM HIERARCHY _A_HIERARCHY"
                    + " LEFT JOIN DUBLINCORE _A_DUBLINCORE ON _A_DUBLINCORE.ID = _A_HIERARCHY.ID"
                    + " LEFT JOIN MISC _A_MISC ON _A_MISC.ID = _A_HIERARCHY.ID"
                    + " JOIN DUBLINCORE _B_DUBLINCORE ON _A_HIERARCHY.ID = _B_DUBLINCORE.TITLE"
                    + " LEFT JOIN MISC _B_MISC ON _B_MISC.ID = _B_DUBLINCORE.ID"
                    + " LEFT JOIN HIERARCHY _B_HIERARCHY ON _B_HIERARCHY.ID = _B_DUBLINCORE.ID"
                    + " WHERE _A_HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                    + "   AND _A_MISC.LIFECYCLESTATE <> ?"
                    + "   AND _B_HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                    + "   AND _B_MISC.LIFECYCLESTATE <> ?"
                    + "   AND ((_A_DUBLINCORE.TITLE = ?) OR (_B_DUBLINCORE.TITLE = ?))";
        } else if (database instanceof DatabasePostgreSQL) {
            expected = "SELECT _A_hierarchy.id, _B_dublincore.title,"
                    + " _A_hierarchy.primarytype, _B_hierarchy.id, _B_hierarchy.primarytype"
                    + " FROM hierarchy _A_hierarchy"
                    + " LEFT JOIN dublincore _A_dublincore ON _A_dublincore.id = _A_hierarchy.id"
                    + " LEFT JOIN misc _A_misc ON _A_misc.id = _A_hierarchy.id"
                    + " JOIN dublincore _B_dublincore ON _A_hierarchy.id = _B_dublincore.title"
                    + " LEFT JOIN misc _B_misc ON _B_misc.id = _B_dublincore.id"
                    + " LEFT JOIN hierarchy _B_hierarchy ON _B_hierarchy.id = _B_dublincore.id"
                    + " WHERE _A_hierarchy.primarytype IN (?, ?, ?)"
                    + "   AND _A_misc.lifecyclestate <> ?"
                    + "   AND _B_hierarchy.primarytype IN (?, ?, ?)"
                    + "   AND _B_misc.lifecyclestate <> ?"
                    + "   AND ((_A_dublincore.title = ?) OR (_B_dublincore.title = ?))";
        }
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
        if (database instanceof DatabaseH2) {
            expected = "SELECT _A_HIERARCHY.ID, _B_DUBLINCORE.TITLE,"
                    + " _A_HIERARCHY.PRIMARYTYPE, _B_HIERARCHY.ID,"
                    + " _B_HIERARCHY.PRIMARYTYPE"
                    + " FROM HIERARCHY _A_HIERARCHY"
                    + " LEFT JOIN DUBLINCORE _A_DUBLINCORE ON _A_DUBLINCORE.ID = _A_HIERARCHY.ID"
                    + " LEFT JOIN MISC _A_MISC ON _A_MISC.ID = _A_HIERARCHY.ID"
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
        } else if (database instanceof DatabasePostgreSQL) {
            expected = "SELECT _A_hierarchy.id, _B_dublincore.title,"
                    + " _A_hierarchy.primarytype, _B_hierarchy.id, _B_hierarchy.primarytype"
                    + " FROM hierarchy _A_hierarchy"
                    + " LEFT JOIN dublincore _A_dublincore ON _A_dublincore.id = _A_hierarchy.id"
                    + " LEFT JOIN misc _A_misc ON _A_misc.id = _A_hierarchy.id"
                    + " JOIN hierarchy _B_hierarchy ON _A_hierarchy.id = _B_hierarchy.parentid"
                    + " LEFT JOIN dublincore _B_dublincore ON _B_dublincore.id = _B_hierarchy.id"
                    + " LEFT JOIN misc _B_misc ON _B_misc.id = _B_hierarchy.id"
                    + " WHERE _A_hierarchy.primarytype IN (?, ?, ?)"
                    + "   AND _A_misc.lifecyclestate <> ?"
                    + "   AND _B_hierarchy.primarytype IN (?, ?, ?)"
                    + "   AND _B_misc.lifecyclestate <> ?"
                    + "   AND ((_A_dublincore.title = ?) OR"
                    + "     EXISTS (SELECT 1 FROM dc_contributors _nxm1_dc_contributors" //
                    + "       WHERE _B_hierarchy.id = _nxm1_dc_contributors.id" //
                    + "         AND _nxm1_dc_contributors.item = ?" //
                    + "))";
        }
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
        if (database instanceof DatabaseH2) {
            expected = "SELECT HIERARCHY.ID, HIERARCHY.PRIMARYTYPE, HIERARCHY.NAME,"
                    + "   DUBLINCORE.CREATOR, DUBLINCORE.CREATED, DUBLINCORE.MODIFIED"
                    + " FROM HIERARCHY"
                    + " LEFT JOIN DUBLINCORE ON DUBLINCORE.ID = HIERARCHY.ID"
                    + " LEFT JOIN MISC ON MISC.ID = HIERARCHY.ID"
                    + " WHERE HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                    + "   AND MISC.LIFECYCLESTATE <> ?"
                    + "   AND ((DUBLINCORE.TITLE = ?))";
        } else if (database instanceof DatabasePostgreSQL) {
            expected = "SELECT hierarchy.id, hierarchy.primarytype, hierarchy.name,"
                    + "   dublincore.creator, dublincore.created, dublincore.modified"
                    + " FROM hierarchy"
                    + " LEFT JOIN dublincore ON dublincore.id = hierarchy.id"
                    + " LEFT JOIN misc ON misc.id = hierarchy.id"
                    + " WHERE hierarchy.primarytype IN (?, ?, ?)"
                    + "   AND misc.lifecyclestate <> ?"
                    + "   AND ((dublincore.title = ?))";
        }
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
        if (database instanceof DatabaseH2) {
            expected = "SELECT HIERARCHY.ID, HIERARCHY.PRIMARYTYPE"
                    + " FROM HIERARCHY"
                    + " LEFT JOIN DUBLINCORE ON DUBLINCORE.ID = HIERARCHY.ID"
                    + " LEFT JOIN MISC ON MISC.ID = HIERARCHY.ID"
                    + " WHERE HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                    + "   AND MISC.LIFECYCLESTATE <> ?"
                    + "   AND ((DUBLINCORE.TITLE = ?) OR (DUBLINCORE.TITLE = ?))";
        } else if (database instanceof DatabasePostgreSQL) {
            expected = "SELECT hierarchy.id, hierarchy.primarytype"
                    + " FROM hierarchy"
                    + " LEFT JOIN dublincore ON dublincore.id = hierarchy.id"
                    + " LEFT JOIN misc ON misc.id = hierarchy.id"
                    + " WHERE hierarchy.primarytype IN (?, ?, ?)"
                    + "   AND misc.lifecyclestate <> ?"
                    + "   AND ((dublincore.title = ?) OR (dublincore.title = ?))";
        }
        expectedP = Arrays.<Serializable> asList(Boolean.TRUE,
                GregorianCalendar.fromAtomPub("2010-01-01T00:00:00.123Z"));
        assertEquals(expected.replaceAll(" +", " "), sql);
        assertEquals(doc_note_file, new HashSet<Serializable>(
                q.selectParams.subList(0, 3)));
        assertEquals(LifeCycleConstants.DELETED_STATE, q.selectParams.get(3));
        assertEquals(expectedP, q.selectParams.subList(4, 6));
    }

    public void testCMISQLQueryMakerFulltext() throws Exception {
        Captured captured = new Captured();
        session.queryAndFetch("", CapturingQueryMaker.TYPE, captured);
        String query;
        Query q;
        String sql;
        String expected;
        Set<String> doc_note_file = new HashSet<String>(Arrays.asList(
                "Document", "Note", "File"));

        // basic query

        query = "SELECT cmis:objectId FROM cmis:document WHERE CONTAINS('foo')";
        q = new CMISQLQueryMaker().buildQuery(captured.sqlInfo, captured.model,
                captured.session, query, null, new Object[] { null });
        assertNotNull(q);
        sql = q.selectInfo.sql.replace("\"", ""); // more readable
        if (database instanceof DatabaseH2) {
            expected = "SELECT HIERARCHY.ID, HIERARCHY.PRIMARYTYPE"
                    + " FROM HIERARCHY"
                    + " LEFT JOIN MISC ON MISC.ID = HIERARCHY.ID"
                    + " LEFT JOIN NXFT_SEARCH('PUBLIC_FULLTEXT_default', ?) _FT ON HIERARCHY.ID = _FT.KEY"
                    + " WHERE HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                    + "   AND MISC.LIFECYCLESTATE <> ?"
                    + "   AND (_FT.KEY IS NOT NULL)";
            assertEquals("foo", q.selectParams.get(0));
            assertEquals(doc_note_file, new HashSet<Serializable>(
                    q.selectParams.subList(1, 4)));
            assertEquals(LifeCycleConstants.DELETED_STATE,
                    q.selectParams.get(4));
        } else if (database instanceof DatabasePostgreSQL) {
            expected = "SELECT hierarchy.id, hierarchy.primarytype"
                    + " FROM hierarchy"
                    + " LEFT JOIN misc ON misc.id = hierarchy.id"
                    + " LEFT JOIN fulltext ON fulltext.id = hierarchy.id"
                    + " WHERE hierarchy.primarytype IN (?, ?, ?)"
                    + "   AND misc.lifecyclestate <> ?"
                    + "   AND (NX_CONTAINS(fulltext.fulltext, ?))";
            assertEquals(doc_note_file, new HashSet<Serializable>(
                    q.selectParams.subList(0, 3)));
            assertEquals(LifeCycleConstants.DELETED_STATE,
                    q.selectParams.get(3));
            assertEquals("foo", q.selectParams.get(4));
        } else {
            return; // TODO other databases
        }
        assertEquals(expected.replaceAll(" +", " "), sql);
    }

}
