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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.core.storage.sql.CapturingQueryMaker;
import org.nuxeo.ecm.core.storage.sql.QueryMaker;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.core.storage.sql.CapturingQueryMaker.Captured;
import org.nuxeo.ecm.core.storage.sql.QueryMaker.Query;

public class TestCMISQLQueryMaker extends SQLRepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        openSession();
    }

    @Override
    public void tearDown() throws Exception {
        session.cancel();
        closeSession();
        super.tearDown();
    }

    public void testCMISQLQueryMaker() throws Exception {
        Captured captured = new Captured();
        session.queryAndFetch("", CapturingQueryMaker.TYPE, captured);
        QueryMaker qm = new CMISQLQueryMaker();
        String query;
        Query q;
        String sql;
        String expected;

        // basic query

        query = "SELECT cmis:ObjectId, dc:title" //
                + " FROM cmis:Document" //
                + " WHERE dc:title = '123' OR dc:title = 'xyz'" //
                + " ORDER BY dc:description DESC, cmis:parentid ASC";
        q = qm.buildQuery(captured.sqlInfo, captured.model, captured.session,
                query, null);
        assertNotNull(q);
        sql = q.selectInfo.sql.replace("\"", ""); // more readable
        expected = "SELECT HIERARCHY.ID, DUBLINCORE.TITLE, HIERARCHY.PRIMARYTYPE"
                + " FROM HIERARCHY"
                + " LEFT JOIN DUBLINCORE ON DUBLINCORE.ID = HIERARCHY.ID"
                + " WHERE HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                + "   AND ((DUBLINCORE.TITLE = '123') OR (DUBLINCORE.TITLE = 'xyz'))"
                + " ORDER BY DUBLINCORE.DESCRIPTION DESC, HIERARCHY.PARENTID";
        assertEquals(expected.replaceAll(" +", " "), sql);
        Set<String> doc_note_file = new HashSet<String>(Arrays.asList(
                "Document", "Note", "File"));
        assertEquals(doc_note_file, new HashSet<Serializable>(q.selectParams));

        // query with ANY quantifier

        query = "SELECT cmis:objectId" //
                + " FROM cmis:document" //
                + " WHERE 'bob' = ANY dc:contributors";
        q = qm.buildQuery(captured.sqlInfo, captured.model, captured.session,
                query, null);
        assertNotNull(q);
        sql = q.selectInfo.sql.replace("\"", ""); // more readable
        expected = "SELECT HIERARCHY.ID, HIERARCHY.PRIMARYTYPE" //
                + " FROM HIERARCHY"
                + " WHERE HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                + "   AND (EXISTS (SELECT 1 FROM DC_CONTRIBUTORS _nxm1_DC_CONTRIBUTORS" //
                + "     WHERE HIERARCHY.ID = _nxm1_DC_CONTRIBUTORS.ID" //
                + "       AND _nxm1_DC_CONTRIBUTORS.ITEM = 'bob'" //
                + "))";
        assertEquals(expected.replaceAll(" +", " "), sql);
        assertEquals(doc_note_file, new HashSet<Serializable>(q.selectParams));

        // join query

        query = "SELECT A.cmis:ObjectId, B.dc:title" //
                + " FROM cmis:Document A" //
                + " JOIN cmis:Document B ON A.cmis:ObjectId = B.cmis:ParentId" //
                + " WHERE A.dc:title = '123' OR B.dc:title = 'xyz'";
        q = qm.buildQuery(captured.sqlInfo, captured.model, captured.session,
                query, null);
        assertNotNull(q);
        sql = q.selectInfo.sql.replace("\"", ""); // more readable
        expected = "SELECT _A_HIERARCHY.ID, _B_DUBLINCORE.TITLE,"
                + " _A_HIERARCHY.PRIMARYTYPE, _B_HIERARCHY.ID, _B_HIERARCHY.PRIMARYTYPE"
                + " FROM HIERARCHY _A_HIERARCHY"
                + " LEFT JOIN DUBLINCORE _A_DUBLINCORE ON _A_DUBLINCORE.ID = _A_HIERARCHY.ID"
                + " JOIN HIERARCHY _B_HIERARCHY ON _A_HIERARCHY.ID = _B_HIERARCHY.PARENTID"
                + " LEFT JOIN DUBLINCORE _B_DUBLINCORE ON _B_DUBLINCORE.ID = _B_HIERARCHY.ID"
                + " WHERE _A_HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                + "   AND _B_HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                + "   AND ((_A_DUBLINCORE.TITLE = '123') OR (_B_DUBLINCORE.TITLE = 'xyz'))";
        assertEquals(expected.replaceAll(" +", " "), sql);
        assertEquals(doc_note_file, new HashSet<Serializable>(
                q.selectParams.subList(0, 3)));
        assertEquals(doc_note_file, new HashSet<Serializable>(
                q.selectParams.subList(3, 6)));

        // join query with ANY quantifier

        query = "SELECT A.cmis:objectId, B.dc:title" //
                + " FROM cmis:document A" //
                + " JOIN cmis:document B ON A.cmis:objectId = B.cmis:parentId" //
                + " WHERE A.dc:title = '123' OR 'bob' = ANY B.dc:contributors";
        q = qm.buildQuery(captured.sqlInfo, captured.model, captured.session,
                query, null);
        assertNotNull(q);
        sql = q.selectInfo.sql.replace("\"", ""); // more readable
        expected = "SELECT _A_HIERARCHY.ID, _B_DUBLINCORE.TITLE,"
                + " _A_HIERARCHY.PRIMARYTYPE, _B_HIERARCHY.ID,"
                + " _B_HIERARCHY.PRIMARYTYPE"
                + " FROM HIERARCHY _A_HIERARCHY"
                + " LEFT JOIN DUBLINCORE _A_DUBLINCORE ON _A_DUBLINCORE.ID = _A_HIERARCHY.ID"
                + " JOIN HIERARCHY _B_HIERARCHY ON _A_HIERARCHY.ID = _B_HIERARCHY.PARENTID"
                + " LEFT JOIN DUBLINCORE _B_DUBLINCORE ON _B_DUBLINCORE.ID = _B_HIERARCHY.ID"
                + " WHERE _A_HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                + "   AND _B_HIERARCHY.PRIMARYTYPE IN (?, ?, ?)"
                + "   AND ((_A_DUBLINCORE.TITLE = '123') OR"
                + "     EXISTS (SELECT 1 FROM DC_CONTRIBUTORS _nxm1_DC_CONTRIBUTORS" //
                + "       WHERE _B_HIERARCHY.ID = _nxm1_DC_CONTRIBUTORS.ID" //
                + "         AND _nxm1_DC_CONTRIBUTORS.ITEM = 'bob'" //
                + "))";
        assertEquals(expected.replaceAll(" +", " "), sql);
        assertEquals(doc_note_file, new HashSet<Serializable>(
                q.selectParams.subList(0, 3)));
        assertEquals(doc_note_file, new HashSet<Serializable>(
                q.selectParams.subList(3, 6)));
    }
}
