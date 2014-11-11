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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.repository.jcr.model;

import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.query.Query;
import org.nuxeo.ecm.core.query.QueryResult;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;

/**
 * Test Query.
 *
 * @author <a href="mailto:lgiura@nuxeo.com">Leonard Giura</a>
 */
public class TestQuery extends RepositoryTestCase {

    private Session session;
    private Document root;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        session = getRepository().getSession(null);
        root = session.getRootDocument();
    }

    @Override
    public void tearDown() throws Exception {
        if (session != null) {
            session.close();
        }
        session = null;
        root = null;
        super.tearDown();
    }

    public void testQuery() throws Exception {
        Query query = session.createQuery("SELECT * FROM document", Query.Type.NXQL);
        QueryResult qr = query.execute();
        assertFalse(qr.isEmpty());
        assertEquals(1, qr.count());
        assertTrue(qr.next());
        assertEquals("ecmdt:Root", qr.getString("jcr:primaryType"));
        assertFalse(qr.next());
    }

    public void testQueryWithEmptyResult() throws Exception {
        Query query = session.createQuery("SELECT * FROM document WHERE title=''", Query.Type.NXQL);
        QueryResult qr = query.execute();
        assertTrue(qr.isEmpty());
        assertEquals(0, qr.count());
        assertFalse(qr.next());
    }

    public void testJCRWhereClause() throws Exception {
        // put some data in workspace
        Document folder1 = root.addChild("folder1", "Folder");
        Document file1 = folder1.addChild("file1", "File");
        file1.setString("dc:title", "T1");
        file1.setString("dc:description", "description1");
        Document file2 = folder1.addChild("file2", "File");
        file2.setString("dc:title", "T2");
        file2.setString("dc:description", "description2");
        Document file3 = folder1.addChild("file3", "File");
        file3.setString("dc:title", "T3");
        file3.setString("dc:description", "description3");
        session.save();

        String queryStr = "SELECT * FROM document WHERE dc:title = 'T2'";
        QueryResult qr = session.createQuery(queryStr, Query.Type.NXQL).execute();
        qr.next();

        assertEquals("description2", qr.getString("dc:description"));

        queryStr = "SELECT * FROM document WHERE dc:title = 'T3' AND dc:description = 'description3' ";
        qr = session.createQuery(queryStr, Query.Type.NXQL).execute();
        qr.next();
        assertEquals("description3", qr.getString("dc:description"));

        folder1.remove();
    }

    public void testJCROrderbyClause() throws Exception {
//      put some data in workspace
        Document folder1 = root.addChild("folder1", "Folder");
        Document file1 = folder1.addChild("file1", "File");
        file1.setString("dc:title", "T1");
        file1.setString("dc:description", "description3");
        Document file2 = folder1.addChild("file2", "File");
        file2.setString("dc:title", "T2");
        file2.setString("dc:description", "description1");
        Document file3 = folder1.addChild("file3", "File");
        file3.setString("dc:title", "T3");
        file3.setString("dc:description", "description2");
        session.save();

        String queryStr = "SELECT * FROM document WHERE dc:title LIKE 'T%' ORDER BY dc:description";
        QueryResult qr = session.createQuery(queryStr, Query.Type.NXQL).execute();

        qr.next();
        assertEquals("description1", qr.getString("dc:description"));
        qr.next();
        assertEquals("description2", qr.getString("dc:description"));
        qr.next();
        assertEquals("description3", qr.getString("dc:description"));

        folder1.remove();
    }

    public void xxxtestQueryResultsTypes() throws Exception {
        Document folder1 = root.addChild("folder1", "Folder");
        Document file1 = folder1.addChild("file1", "MyDocType");
        file1.setString("title", "testQueryResultsTypes");
        file1.setBoolean("my:boolean", true);
        file1.setDouble("my:double", 3.14);
        file1.setLong("my:long", 1234567890);
        session.save();

        String queryStr = "SELECT * FROM document WHERE title LIKE 'test%'";
        QueryResult qr = session.createQuery(queryStr, Query.Type.NXQL).execute();

        qr.next();
        assertEquals("testQueryResultsTypes", qr.getString("title"));
        assertTrue(qr.getBoolean("my:boolean"));
        assertEquals(3.14, qr.getDouble("my:double", 3));
        assertEquals(1234567890, qr.getLong("my:long", -1));

        folder1.remove();
    }

    private static void buildTree(int max, int current, int children, Document parent) throws DocumentException {
        if (current < max) {
            for (int i = 0; i < children; i++) {
                Document document = parent.addChild(parent.getName() + i, "Folder");
                document.setString("dc:description", parent.getName() + i);
                buildTree(max, current + 1, children, document);
            }
        }
    }

    public void OBSOLETEtestLocationQuery() throws Exception {
        Document parent = root.addChild("f", "Folder");
        parent.setString("dc:description", "top folder");
        buildTree(3, 0, 3, parent);
        session.save();

        String queryStr = "SELECT * FROM LOCATION f.f1.f11 ";
        QueryResult qr = session.createQuery(queryStr, Query.Type.NXQL).execute();
        qr.next();
        assertEquals("f110", qr.getString("dc:description"));
        qr.next();
        assertEquals("f111", qr.getString("dc:description"));
        qr.next();
        assertEquals("f112", qr.getString("dc:description"));
        assertFalse(qr.next());

        queryStr = "SELECT * FROM LOCATION f ";
        qr = session.createQuery(queryStr, Query.Type.NXQL).execute();
        assertEquals(39, qr.count());
    }

    @SuppressWarnings("unchecked")
    public void testResultQueryGetObject() throws Exception {

        Document parent = root.addChild("root", "Folder");
        parent.setString("dc:description", "top folder");
        Document document = parent.addChild("doc", "File");
        document.setString("dc:title", "1 title");
        document.setString("dc:description", "1 description");
        document.setString("filename", "1 filename");
        session.save();

        String queryStr = "SELECT dc:title, filename FROM document WHERE dc:title LIKE '1%'";
        QueryResult qr = session.createQuery(queryStr, Query.Type.NXQL).execute();
        qr.next();
        Map<String, Map<String, Object>> map = (Map<String, Map<String, Object>>) qr.getObject();
        assertNotNull(map);
        Map<String, Object> subMap = map.get("default");
        assertNotNull(subMap);
        assertEquals("1 filename", subMap.get("filename"));

        queryStr = "SELECT * FROM document WHERE dc:title LIKE '1%'";
        qr = session.createQuery(queryStr, Query.Type.NXQL).execute();
        qr.next();
        map = (Map<String, Map<String, Object>>) qr.getObject();
        assertNotNull(map);
        subMap = map.get("file");
        assertNotNull(subMap);
        assertEquals(2, subMap.size());
        assertEquals("1 filename", subMap.get("filename"));
        subMap = map.get("dublincore");
        assertNotNull(subMap);
        // assertEquals(14, subMap.size());
        assertEquals("1 title", subMap.get("title"));
    }

}
