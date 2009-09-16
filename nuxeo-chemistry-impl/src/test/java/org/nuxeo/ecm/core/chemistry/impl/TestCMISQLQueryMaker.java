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

import java.util.Collections;

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

        query = "SELECT cmis:ObjectId, dc:title" //
                + " FROM cmis:Document" //
                + " WHERE dc:title = '123' OR dc:title = 'xyz'" //
                + " ORDER BY dc:description DESC, cmis:parentid ASC";
        q = qm.buildQuery(captured.sqlInfo, captured.model, captured.session,
                query, null);
        assertNotNull(q);
        sql = q.selectInfo.sql.replace("\"", ""); // more readable
        assertEquals(
                "SELECT HIERARCHY.ID, DUBLINCORE.TITLE"
                        + " FROM HIERARCHY"
                        + " LEFT JOIN DUBLINCORE ON DUBLINCORE.ID = HIERARCHY.ID"
                        + " WHERE ((DUBLINCORE.TITLE = '123') OR (DUBLINCORE.TITLE = 'xyz'))"
                        + " ORDER BY DUBLINCORE.DESCRIPTION DESC, HIERARCHY.PARENTID",
                sql);
        assertEquals(Collections.emptyList(), q.selectParams);

        query = "SELECT A.cmis:ObjectId, B.dc:title" //
                + " FROM cmis:Document A" //
                + " JOIN cmis:Document B ON A.cmis:ObjectId = B.cmis:ParentId" //
                + " WHERE A.dc:title = '123' OR B.dc:title = 'xyz'";
        q = qm.buildQuery(captured.sqlInfo, captured.model, captured.session,
                query, null);
        assertNotNull(q);
        sql = q.selectInfo.sql.replace("\"", ""); // more readable
        assertEquals(
                "SELECT _A_HIERARCHY.ID, _B_DUBLINCORE.TITLE"
                        + " FROM HIERARCHY _A_HIERARCHY"
                        + " LEFT JOIN DUBLINCORE _A_DUBLINCORE ON _A_DUBLINCORE.ID = _A_HIERARCHY.ID"
                        + " JOIN HIERARCHY _B_HIERARCHY ON _A_HIERARCHY.ID = _B_HIERARCHY.PARENTID"
                        + " LEFT JOIN DUBLINCORE _B_DUBLINCORE ON _B_DUBLINCORE.ID = _B_HIERARCHY.ID"
                        + " WHERE ((_A_DUBLINCORE.TITLE = '123') OR (_B_DUBLINCORE.TITLE = 'xyz'))",
                sql);
        assertEquals(Collections.emptyList(), q.selectParams);
    }
}
