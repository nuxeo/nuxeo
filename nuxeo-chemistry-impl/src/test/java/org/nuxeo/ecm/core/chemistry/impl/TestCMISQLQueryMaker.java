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
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.core.storage.sql.CapturingQueryMaker.Captured;
import org.nuxeo.ecm.core.storage.sql.Session.PathResolver;
import org.nuxeo.ecm.core.storage.sql.jdbc.QueryMakerDescriptor;
import org.nuxeo.ecm.core.storage.sql.jdbc.QueryMakerService;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo;
import org.nuxeo.ecm.core.storage.sql.jdbc.QueryMaker.Query;
import org.nuxeo.runtime.api.Framework;

public class TestCMISQLQueryMaker extends SQLRepositoryTestCase {

    public static Set<String> doc_note_file = new HashSet<String>(
            Arrays.asList("Document", "Note", "File"));

    public NuxeoRepository repo;

    public Connection conn;

    private SQLInfo sqlInfo;

    private Model model;

    private PathResolver pathResolver;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        openSession();
        repo = new NuxeoRepository(session.getRepositoryName());
        conn = repo.getConnection(null);
        Captured captured = new Captured();
        QueryMakerService queryMakerService = Framework.getService(QueryMakerService.class);
        QueryMakerDescriptor queryMakerDescriptor = new QueryMakerDescriptor();
        queryMakerDescriptor.name = "capturing";
        queryMakerDescriptor.queryMaker = CapturingQueryMaker.class;
        queryMakerService.registerQueryMaker(queryMakerDescriptor);
        session.queryAndFetch("", CapturingQueryMaker.TYPE, captured);
        sqlInfo = captured.sqlInfo;
        model = captured.model;
        pathResolver = captured.pathResolver;
    }

    @Override
    public void tearDown() throws Exception {
        conn.close();
        session.cancel();
        closeSession();
        super.tearDown();
    }

    public void testBasic() throws Exception {
        String query = "SELECT cmis:ObjectId, dc:title" //
                + " FROM cmis:Document" //
                + " WHERE dc:title = 123 OR dc:title = 'xyz'" //
                + " ORDER BY dc:description DESC, cmis:parentid ASC";
        Query q = new CMISQLQueryMaker().buildQuery(sqlInfo, model,
                pathResolver, query, null, new Object[] { null });
        assertNotNull(q);
        String sql = q.selectInfo.sql.replace("\"", "");
        String expected;
        if (database instanceof DatabaseH2) {
            expected = "SELECT HIERARCHY.ID, DUBLINCORE.TITLE"
                    + " FROM HIERARCHY"
                    + " LEFT JOIN DUBLINCORE ON DUBLINCORE.ID = HIERARCHY.ID"
                    + " LEFT JOIN MISC ON MISC.ID = HIERARCHY.ID"
                    + " WHERE HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                    + "   AND MISC.LIFECYCLESTATE <> ?"
                    + "   AND ((DUBLINCORE.TITLE = ?) OR (DUBLINCORE.TITLE = ?))"
                    + " ORDER BY DUBLINCORE.DESCRIPTION DESC, HIERARCHY.PARENTID";
        } else if (database instanceof DatabasePostgreSQL) {
            expected = "SELECT hierarchy.id, dublincore.title"
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
        List<Serializable> expectedP = Arrays.<Serializable> asList(
                Long.valueOf(123), "xyz");
        assertEquals(expected.replaceAll(" +", " "), sql);
        assertEquals(doc_note_file, new HashSet<Serializable>(
                q.selectParams.subList(0, 3)));
        assertEquals(LifeCycleConstants.DELETED_STATE, q.selectParams.get(3));
        assertEquals(expectedP, q.selectParams.subList(4, 6));
    }

    public void testScalarIN() throws Exception {
        String query = "SELECT cmis:ObjectId, dc:title" //
                + " FROM cmis:document" //
                + " WHERE dc:title IN ('xyz', 'abc')";
        Query q = new CMISQLQueryMaker().buildQuery(sqlInfo, model,
                pathResolver, query, null, new Object[] { null });
        assertNotNull(q);
        String sql = q.selectInfo.sql.replace("\"", ""); // more readable
        String expected;
        if (database instanceof DatabaseH2) {
            expected = "SELECT HIERARCHY.ID, DUBLINCORE.TITLE"
                    + " FROM HIERARCHY"
                    + " LEFT JOIN DUBLINCORE ON DUBLINCORE.ID = HIERARCHY.ID"
                    + " LEFT JOIN MISC ON MISC.ID = HIERARCHY.ID"
                    + " WHERE HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                    + "   AND MISC.LIFECYCLESTATE <> ?"
                    + "   AND ((DUBLINCORE.TITLE IN (?, ?)))";
        } else if (database instanceof DatabasePostgreSQL) {
            expected = "SELECT hierarchy.id, dublincore.title"
                    + " FROM hierarchy"
                    + " LEFT JOIN dublincore ON dublincore.id = hierarchy.id"
                    + " LEFT JOIN misc ON misc.id = hierarchy.id"
                    + " WHERE hierarchy.primarytype IN (?, ?, ?)"
                    + "   AND misc.lifecyclestate <> ?"
                    + "   AND ((dublincore.title IN (?, ?)))";
        } else {
            return; // TODO other databases
        }
        List<Serializable> expectedP = Arrays.<Serializable> asList("xyz",
                "abc");
        assertEquals(expected.replaceAll(" +", " "), sql);
        assertEquals(doc_note_file, new HashSet<Serializable>(
                q.selectParams.subList(0, 3)));
        assertEquals(LifeCycleConstants.DELETED_STATE, q.selectParams.get(3));
        assertEquals(expectedP, q.selectParams.subList(4, 6));
    }

    public void testANY() throws Exception {
        String query = "SELECT cmis:objectId" //
                + " FROM cmis:document" //
                + " WHERE 'bob' = ANY dc:contributors";
        Query q = new CMISQLQueryMaker().buildQuery(sqlInfo, model,
                pathResolver, query, null, new Object[] { null });
        assertNotNull(q);
        String sql = q.selectInfo.sql.replace("\"", ""); // more readable
        String expected;
        if (database instanceof DatabaseH2) {
            expected = "SELECT HIERARCHY.ID" //
                    + " FROM HIERARCHY"
                    + " LEFT JOIN MISC ON MISC.ID = HIERARCHY.ID"
                    + " WHERE HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                    + "   AND MISC.LIFECYCLESTATE <> ?"
                    + "   AND (EXISTS (SELECT 1 FROM DC_CONTRIBUTORS _nxm1_DC_CONTRIBUTORS" //
                    + "     WHERE HIERARCHY.ID = _nxm1_DC_CONTRIBUTORS.ID" //
                    + "       AND _nxm1_DC_CONTRIBUTORS.ITEM = ?" //
                    + "))";
        } else if (database instanceof DatabasePostgreSQL) {
            expected = "SELECT hierarchy.id" //
                    + " FROM hierarchy"
                    + " LEFT JOIN misc ON misc.id = hierarchy.id"
                    + " WHERE hierarchy.primarytype IN (?, ?, ?)"
                    + "   AND misc.lifecyclestate <> ?"
                    + "   AND (EXISTS (SELECT 1 FROM dc_contributors _nxm1_dc_contributors" //
                    + "     WHERE hierarchy.id = _nxm1_dc_contributors.id" //
                    + "       AND _nxm1_dc_contributors.item = ?" //
                    + "))";
        } else {
            return; // TODO other databases
        }
        List<Serializable> expectedP = Arrays.<Serializable> asList("bob");
        assertEquals(expected.replaceAll(" +", " "), sql);
        assertEquals(doc_note_file, new HashSet<Serializable>(
                q.selectParams.subList(0, 3)));
        assertEquals(LifeCycleConstants.DELETED_STATE, q.selectParams.get(3));
        assertEquals(expectedP, q.selectParams.subList(4, 5));
    }

    public void testJOIN() throws Exception {
        // join on non-hierarchy table to test query generation and types
        String query = "SELECT A.cmis:ObjectId, B.dc:title" //
                + " FROM cmis:Document A" //
                + " JOIN cmis:Document B ON A.cmis:ObjectId = B.dc:title" //
                + " WHERE A.dc:title = '123' OR B.dc:title = 'xyz'";
        Query q = new CMISQLQueryMaker().buildQuery(sqlInfo, model,
                pathResolver, query, null, new Object[] { null });
        assertNotNull(q);
        String sql = q.selectInfo.sql.replace("\"", ""); // more readable
        String expected;
        if (database instanceof DatabaseH2) {
            expected = "SELECT _A_HIERARCHY.ID, _B_DUBLINCORE.TITLE,"
                    + " _B_HIERARCHY.ID"
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
                    + " _B_hierarchy.id"
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
        } else {
            return; // TODO other databases
        }
        List<Serializable> expectedP = Arrays.<Serializable> asList("123",
                "xyz");
        assertEquals(expected.replaceAll(" +", " "), sql);
        assertEquals(doc_note_file, new HashSet<Serializable>(
                q.selectParams.subList(0, 3)));
        assertEquals(LifeCycleConstants.DELETED_STATE, q.selectParams.get(3));
        assertEquals(doc_note_file, new HashSet<Serializable>(
                q.selectParams.subList(4, 7)));
        assertEquals(LifeCycleConstants.DELETED_STATE, q.selectParams.get(7));
        assertEquals(expectedP, q.selectParams.subList(8, 10));
    }

    public void testJOIN_ANY() throws Exception {
        String query = "SELECT A.cmis:objectId, B.dc:title" //
                + " FROM cmis:document A" //
                + " JOIN cmis:document B ON A.cmis:objectId = B.cmis:parentId" //
                + " WHERE A.dc:title = '123' OR 'bob' = ANY B.dc:contributors";
        Query q = new CMISQLQueryMaker().buildQuery(sqlInfo, model,
                pathResolver, query, null, new Object[] { null });
        assertNotNull(q);
        String sql = q.selectInfo.sql.replace("\"", ""); // more readable
        String expected;
        if (database instanceof DatabaseH2) {
            expected = "SELECT _A_HIERARCHY.ID, _B_DUBLINCORE.TITLE,"
                    + " _B_HIERARCHY.ID"
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
                    + " _B_hierarchy.id"
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
        } else {
            return; // TODO other databases
        }
        List<Serializable> expectedP = Arrays.<Serializable> asList("123",
                "bob");
        assertEquals(expected.replaceAll(" +", " "), sql);
        assertEquals(doc_note_file, new HashSet<Serializable>(
                q.selectParams.subList(0, 3)));
        assertEquals(LifeCycleConstants.DELETED_STATE, q.selectParams.get(3));
        assertEquals(doc_note_file, new HashSet<Serializable>(
                q.selectParams.subList(4, 7)));
        assertEquals(LifeCycleConstants.DELETED_STATE, q.selectParams.get(7));
        assertEquals(expectedP, q.selectParams.subList(8, 10));
    }

    public void testSELECT_STAR() throws Exception {
        String query = "SELECT * FROM cmis:document WHERE dc:title = 123";
        Query q = new CMISQLQueryMaker().buildQuery(sqlInfo, model,
                pathResolver, query, null, conn);
        assertNotNull(q);
        String sql = q.selectInfo.sql.replace("\"", ""); // more readable
        String expected;
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
        } else {
            return; // TODO other databases
        }
        List<Serializable> expectedP = Arrays.<Serializable> asList(Long.valueOf(123));
        assertEquals(expected.replaceAll(" +", " "), sql);
        assertEquals(doc_note_file, new HashSet<Serializable>(
                q.selectParams.subList(0, 3)));
        assertEquals(LifeCycleConstants.DELETED_STATE, q.selectParams.get(3));
        assertEquals(expectedP, q.selectParams.subList(4, 5));
    }

    public void testSELECT_DISTINCT() throws Exception {
        String query = "SELECT DISTINCT dc:title FROM cmis:document";
        Query q = new CMISQLQueryMaker().buildQuery(sqlInfo, model,
                pathResolver, query, null, conn);
        assertNotNull(q);
        String sql = q.selectInfo.sql.replace("\"", ""); // more readable
        String expected;
        if (database instanceof DatabaseH2) {
            expected = "SELECT DISTINCT DUBLINCORE.TITLE" //
                    + " FROM HIERARCHY"
                    + " LEFT JOIN DUBLINCORE ON DUBLINCORE.ID = HIERARCHY.ID"
                    + " LEFT JOIN MISC ON MISC.ID = HIERARCHY.ID"
                    + " WHERE HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                    + "   AND MISC.LIFECYCLESTATE <> ?";
        } else if (database instanceof DatabasePostgreSQL) {
            expected = "SELECT DISTINCT dublincore.title" //
                    + " FROM hierarchy"
                    + " LEFT JOIN dublincore ON dublincore.id = hierarchy.id"
                    + " LEFT JOIN misc ON misc.id = hierarchy.id"
                    + " WHERE hierarchy.primarytype IN (?, ?, ?)"
                    + "   AND misc.lifecyclestate <> ?";
        } else {
            return; // TODO other databases
        }
        assertEquals(expected.replaceAll(" +", " "), sql);
        assertEquals(doc_note_file, new HashSet<Serializable>(
                q.selectParams.subList(0, 3)));
        assertEquals(LifeCycleConstants.DELETED_STATE, q.selectParams.get(3));
    }

    public void testSELECT_DISTINCT_fail() throws Exception {
        String query;
        query = "SELECT DISTINCT dc:title, cmis:contentStreamLength FROM cmis:document";
        try {
            new CMISQLQueryMaker().buildQuery(sqlInfo, model, pathResolver,
                    query, null, conn);
            fail("Shouldn't be able to do DISTINCT on virtual column");
        } catch (CMISQLQueryMaker.QueryMakerException e) {
            // ok
        }

        query = "SELECT DISTINCT dc:title FROM cmis:document";
        try {
            new CMISQLQueryMaker().buildQuery(sqlInfo, model, pathResolver,
                    query, null, conn, Boolean.TRUE); // add system cols
            fail("Shouldn't be able to do DISTINCT with system columns added");
        } catch (CMISQLQueryMaker.QueryMakerException e) {
            // ok
        }
    }

    public void testBooleanDateTime() throws Exception {
        String query = "SELECT cmis:objectId FROM cmis:document "
                + "WHERE dc:title = true or dc:title = TIMESTAMP '2010-01-01T00:00:00.123Z'";
        Query q = new CMISQLQueryMaker().buildQuery(sqlInfo, model,
                pathResolver, query, null, new Object[] { null });
        assertNotNull(q);
        String sql = q.selectInfo.sql.replace("\"", ""); // more readable
        String expected;
        if (database instanceof DatabaseH2) {
            expected = "SELECT HIERARCHY.ID"
                    + " FROM HIERARCHY"
                    + " LEFT JOIN DUBLINCORE ON DUBLINCORE.ID = HIERARCHY.ID"
                    + " LEFT JOIN MISC ON MISC.ID = HIERARCHY.ID"
                    + " WHERE HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                    + "   AND MISC.LIFECYCLESTATE <> ?"
                    + "   AND ((DUBLINCORE.TITLE = ?) OR (DUBLINCORE.TITLE = ?))";
        } else if (database instanceof DatabasePostgreSQL) {
            expected = "SELECT hierarchy.id"
                    + " FROM hierarchy"
                    + " LEFT JOIN dublincore ON dublincore.id = hierarchy.id"
                    + " LEFT JOIN misc ON misc.id = hierarchy.id"
                    + " WHERE hierarchy.primarytype IN (?, ?, ?)"
                    + "   AND misc.lifecyclestate <> ?"
                    + "   AND ((dublincore.title = ?) OR (dublincore.title = ?))";
        } else {
            return; // TODO other databases
        }
        List<Serializable> expectedP = Arrays.<Serializable> asList(
                Boolean.TRUE,
                GregorianCalendar.fromAtomPub("2010-01-01T00:00:00.123Z"));
        assertEquals(expected.replaceAll(" +", " "), sql);
        assertEquals(doc_note_file, new HashSet<Serializable>(
                q.selectParams.subList(0, 3)));
        assertEquals(LifeCycleConstants.DELETED_STATE, q.selectParams.get(3));
        assertEquals(expectedP, q.selectParams.subList(4, 6));
    }

    public void testAlias() throws Exception {
        String query = "SELECT cmis:objectId ID, cmis:objectTypeId AS TYP"
                + "  FROM cmis:document ORDER BY ID, TYP DESC";
        Query q = new CMISQLQueryMaker().buildQuery(sqlInfo, model,
                pathResolver, query, null, new Object[] { null });
        assertNotNull(q);
        String sql = q.selectInfo.sql.replace("\"", ""); // more readable
        String expected;
        if (database instanceof DatabaseH2) {
            expected = "SELECT HIERARCHY.ID, HIERARCHY.PRIMARYTYPE "
                    + " FROM HIERARCHY"
                    + " LEFT JOIN MISC ON MISC.ID = HIERARCHY.ID"
                    + " WHERE HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                    + "   AND MISC.LIFECYCLESTATE <> ?"
                    + " ORDER BY HIERARCHY.ID, HIERARCHY.PRIMARYTYPE DESC";
        } else if (database instanceof DatabasePostgreSQL) {
            expected = "SELECT hierarchy.id, hierarchy.primarytype "
                    + " FROM hierarchy"
                    + " LEFT JOIN misc ON misc.id = hierarchy.id"
                    + " WHERE hierarchy.primarytype IN (?, ?, ?)"
                    + "   AND misc.lifecyclestate <> ?"
                    + " ORDER BY hierarchy.id, hierarchy.primarytype DESC";
        } else {
            return; // TODO other databases
        }
        assertEquals(expected.replaceAll(" +", " "), sql);
        assertEquals(doc_note_file, new HashSet<Serializable>(
                q.selectParams.subList(0, 3)));
        assertEquals(LifeCycleConstants.DELETED_STATE, q.selectParams.get(3));
    }

    public void testAliasMandatory() throws Exception {
        String query = "SELECT cmis:objectId ID"
                + "  FROM cmis:document ORDER BY cmis:objectId";
        try {
            new CMISQLQueryMaker().buildQuery(sqlInfo, model, pathResolver,
                    query, null, new Object[] { null });
            fail("should raise because of aliased colum used unaliased in ORDER BY");
        } catch (CMISQLQueryMaker.QueryMakerException e) {
            // ok
        }
    }

    public void testFulltext() throws Exception {
        String query = "SELECT cmis:objectId FROM cmis:document WHERE CONTAINS('foo')";
        Query q = new CMISQLQueryMaker().buildQuery(sqlInfo, model,
                pathResolver, query, null, new Object[] { null });
        assertNotNull(q);
        String sql = q.selectInfo.sql.replace("\"", ""); // more readable
        String expected;
        if (database instanceof DatabaseH2) {
            expected = "SELECT HIERARCHY.ID"
                    + " FROM HIERARCHY"
                    + " LEFT JOIN MISC ON MISC.ID = HIERARCHY.ID"
                    + " LEFT JOIN NXFT_SEARCH('PUBLIC_FULLTEXT_default', ?) _nxfttbl"
                    + "   ON _nxfttbl.KEY = HIERARCHY.ID"
                    + " WHERE HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                    + "   AND MISC.LIFECYCLESTATE <> ?"
                    + "   AND (_nxfttbl.KEY IS NOT NULL)";
        } else if (database instanceof DatabasePostgreSQL) {
            expected = "SELECT hierarchy.id" //
                    + " FROM hierarchy"
                    + " LEFT JOIN misc ON misc.id = hierarchy.id"
                    + " LEFT JOIN fulltext ON fulltext.id = hierarchy.id,"
                    + " TO_TSQUERY('french', ?) AS _nxquery"
                    + " WHERE hierarchy.primarytype IN (?, ?, ?)"
                    + "   AND misc.lifecyclestate <> ?"
                    + "   AND ((_nxquery @@ fulltext.fulltext))";
        } else {
            return; // TODO other databases
        }
        assertEquals(expected.replaceAll(" +", " "), sql);
        assertEquals("foo", q.selectParams.get(0));
        assertEquals(doc_note_file, new HashSet<Serializable>(
                q.selectParams.subList(1, 4)));
        assertEquals(LifeCycleConstants.DELETED_STATE, q.selectParams.get(4));
    }

    public void testFulltextScore() throws Exception {
        String query = "SELECT cmis:objectId, SCORE() as SC"
                + " FROM cmis:document WHERE CONTAINS('foo')" //
                + " ORDER BY SC DESC";
        Query q = new CMISQLQueryMaker().buildQuery(sqlInfo, model,
                pathResolver, query, null, new Object[] { null });
        assertNotNull(q);
        String sql = q.selectInfo.sql.replace("\"", ""); // more readable
        String expected;
        if (database instanceof DatabaseH2) {
            expected = "SELECT HIERARCHY.ID, 1 AS _nxscore"
                    + " FROM HIERARCHY"
                    + " LEFT JOIN MISC ON MISC.ID = HIERARCHY.ID"
                    + " LEFT JOIN NXFT_SEARCH('PUBLIC_FULLTEXT_default', ?) _nxfttbl"
                    + "   ON _nxfttbl.KEY = HIERARCHY.ID"
                    + " WHERE HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                    + "   AND MISC.LIFECYCLESTATE <> ?"
                    + "   AND (_nxfttbl.KEY IS NOT NULL)"
                    + " ORDER BY _nxscore DESC";
        } else if (database instanceof DatabasePostgreSQL) {
            expected = "SELECT hierarchy.id,"
                    + "   TS_RANK_CD(fulltext.fulltext, _nxquery, 32) AS _nxscore"
                    + "   FROM hierarchy"
                    + "   LEFT JOIN misc ON misc.id = hierarchy.id"
                    + "   LEFT JOIN fulltext ON fulltext.id = hierarchy.id,"
                    + "   TO_TSQUERY('french', ?) AS _nxquery"
                    + " WHERE hierarchy.primarytype IN (?, ?, ?)"
                    + "   AND misc.lifecyclestate <> ?"
                    + "   AND ((_nxquery @@ fulltext.fulltext))"
                    + " ORDER BY _nxscore DESC";
        } else {
            return; // TODO other databases
        }
        assertEquals(expected.replaceAll(" +", " "), sql);
        assertEquals("foo", q.selectParams.get(0));
        assertEquals(doc_note_file, new HashSet<Serializable>(
                q.selectParams.subList(1, 4)));
        assertEquals(LifeCycleConstants.DELETED_STATE, q.selectParams.get(4));
    }

}
