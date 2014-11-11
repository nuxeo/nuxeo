/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.test.QueryTestCase;

/**
 * @author Florent Guillaume
 */
public class TestSQLRepositoryQuery extends QueryTestCase {

    @Override
    public void deployRepository() throws Exception {
        deployContrib("org.nuxeo.ecm.core.storage.sql.test.tests",
                "OSGI-INF/test-repo-core-types-contrib-2.xml");
        deployBundle("org.nuxeo.ecm.core.storage.sql");
        DatabaseHelper.DATABASE.setUp();
        deployContrib("org.nuxeo.ecm.core.storage.sql.test",
                DatabaseHelper.DATABASE.getDeploymentContrib());
        deployBundle("org.nuxeo.ecm.core.event");
    }

    @Override
    public void undeployRepository() throws Exception {
        DatabaseHelper.DATABASE.tearDown();
    }

    @Override
    protected void sleepForFulltext() {
        super.sleepForFulltext();
        DatabaseHelper.DATABASE.sleepForFulltext();
    }

    @Override
    public void testFulltextBlob() throws Exception {
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.core.convert.plugins");
        super.testFulltextBlob();
    }

    @Override
    public void testFulltextSecondary() throws Exception {
        if (!DatabaseHelper.DATABASE.supportsMultipleFulltextIndexes()) {
            System.out.println("Skipping multi-fulltext test for unsupported database: "
                    + DatabaseHelper.DATABASE.getClass().getName());
            return;
        }
        super.testFulltextSecondary();
    }

    @Override
    public void testFulltextExpressionPhrase() throws Exception {
        if (DatabaseHelper.DATABASE instanceof DatabasePostgreSQL) {
            System.out.println("Skipping fulltext phrase test for unsupported database: "
                    + DatabaseHelper.DATABASE.getClass().getName());
            return;
        }
        super.testFulltextExpressionPhrase();
    }

    public void testQueryIterable() throws Exception {
        createDocs();

        IterableQueryResult res = session.queryAndFetch("SELECT * FROM File",
                "NXQL");
        List<Map<String, Serializable>> l = new LinkedList<Map<String, Serializable>>();
        for (Map<String, Serializable> x : res) {
            l.add(x);
        }
        assertEquals(3, l.size());
        res.close();

        // cursor behavior
        res = session.queryAndFetch("SELECT * FROM File", "NXQL");
        Iterator<Map<String, Serializable>> it = res.iterator();
        assertEquals(0, res.pos());
        it.next();
        assertEquals(1, res.pos());
        assertEquals(3, res.size());
        assertEquals(1, res.pos());
        assertTrue(it.hasNext());
        assertEquals(1, res.pos());
        it.next();
        assertEquals(2, res.pos());
        assertTrue(it.hasNext());
        assertEquals(2, res.pos());
        it.next();
        assertEquals(3, res.pos());
        assertFalse(it.hasNext());
        assertEquals(3, res.pos());

        res.skipTo(1);
        assertEquals(3, res.size());
        assertTrue(it.hasNext());
        assertEquals(1, res.pos());
        it.next();
        assertEquals(2, res.pos());
        res.close();

        // checking size when at end
        res = session.queryAndFetch("SELECT * FROM File", "NXQL");
        it = res.iterator();
        it.next();
        it.next();
        it.next();
        assertFalse(it.hasNext());
        assertEquals(3, res.size());
        res.close();

        // size when query returns nothing
        res = session.queryAndFetch(
                "SELECT * FROM File WHERE dc:title = 'zzz'", "NXQL");
        it = res.iterator();
        assertFalse(it.hasNext());
        assertEquals(0, res.size());
        res.close();
    }

    public void testQueryIterableWithTransformer() throws Exception {
        createDocs();
        IterableQueryResult res;

        res = session.queryAndFetch("SELECT * FROM Document", "NXQL");
        assertEquals(7, res.size());
        res.close();

        // NoFile2SecurityPolicy
        deployContrib("org.nuxeo.ecm.core.query.test",
                "OSGI-INF/security-policy2-contrib.xml");

        res = session.queryAndFetch("SELECT * FROM Document", "NXQL");
        assertEquals(4, res.size());
        res.close();
    }

    @Override
    public void testQueryBasic() throws Exception {
        // Documents without creation date don't match any DATE query
        // 2 documents with creation date
        createDocs();
        DocumentModelList dml;

        if (DatabaseHelper.DATABASE == DatabaseDerby.INSTANCE) {
            // Derby 10.5.3.0 has bugs with LEFT JOIN and NOT BETWEEN
            // http://issues.apache.org/jira/browse/DERBY-4388
            return;
        }

        dml = session.query("SELECT * FROM Document WHERE dc:created NOT BETWEEN DATE '2007-01-01' AND DATE '2008-01-01'");
        assertEquals(0, dml.size()); // 2 Documents match the BETWEEN query

        dml = session.query("SELECT * FROM Document WHERE dc:created NOT BETWEEN DATE '2007-03-15' AND DATE '2008-01-01'");
        assertEquals(1, dml.size()); // 1 Document matches the BETWEEN query

        dml = session.query("SELECT * FROM Document WHERE dc:created NOT BETWEEN DATE '2009-03-15' AND DATE '2009-01-01'");
        assertEquals(2, dml.size()); // 0 Document matches the BETWEEN query

        dml = session.query("SELECT * FROM Document WHERE dc:title ILIKE 'test%'");
        assertEquals(5, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:title ILIKE 'Test%'");
        assertEquals(5, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:title NOT ILIKE 'foo%'");
        assertEquals(5, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:title NOT ILIKE 'Foo%'");
        assertEquals(5, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:subjects ILIKE '%oo'");
        assertEquals(1, dml.size());

        dml = session.query("SELECT * FROM Document WHERE dc:subjects NOT ILIKE '%oo'");
        assertEquals(6, dml.size());
    }

    public void testQueryComplexTypeFiles() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "myfile", "File");
        List<Object> files = new LinkedList<Object>();
        Map<String, Object> f = new HashMap<String, Object>();
        f.put("filename", "f1");
        files.add(f);
        doc.setProperty("files", "files", files);
        doc = session.createDocument(doc);
        session.save();

        DocumentModelList dml = session.query("SELECT * FROM File");
        assertEquals(1, dml.size());
        // with MySQL was logging:
        // ERROR Unknown document type: file
        // due to its case-insensitivity in = and IN tests...
        // and returning an empty query, cf SQLQueryResult.getDocumentModels
    }

    public void testSelectColumns() throws Exception {
        String query;
        IterableQueryResult res;
        Iterator<Map<String, Serializable>> it;
        Map<String, Serializable> map;

        createDocs();

        // check proper tables are joined even if not in FROM
        query = "SELECT ecm:uuid, dc:title FROM File";
        res = session.queryAndFetch(query, "NXQL");
        assertEquals(3, res.size());
        map = res.iterator().next();
        assertTrue(map.containsKey("dc:title"));
        assertTrue(map.containsKey(NXQL.ECM_UUID));
        res.close();

        // check with no proxies (no subselect)
        query = "SELECT ecm:uuid, dc:title FROM File where ecm:isProxy = 0";
        res = session.queryAndFetch(query, "NXQL");
        assertEquals(3, res.size());
        res.close();

        // check content
        query = "SELECT ecm:uuid, dc:title FROM File ORDER BY dc:title";
        res = session.queryAndFetch(query, "NXQL");
        assertEquals(3, res.size());
        it = res.iterator();
        map = it.next();
        assertEquals("testfile1_Title", map.get("dc:title"));
        map = it.next();
        assertEquals("testfile2_Title", map.get("dc:title"));
        map = it.next();
        assertEquals("testfile4Title", map.get("dc:title"));
        res.close();

        // check content with no proxies (simpler query with no UNION ALL)
        query = "SELECT ecm:uuid, dc:title FROM File WHERE ecm:isProxy = 0 ORDER BY dc:title";
        res = session.queryAndFetch(query, "NXQL");
        assertEquals(3, res.size());
        it = res.iterator();
        map = it.next();
        assertEquals("testfile1_Title", map.get("dc:title"));
        map = it.next();
        assertEquals("testfile2_Title", map.get("dc:title"));
        map = it.next();
        assertEquals("testfile4Title", map.get("dc:title"));
        res.close();
    }

    public void testSelectColumnsSameName() throws Exception {
        String query;
        IterableQueryResult res;
        Map<String, Serializable> map;

        // two fields with same key
        DocumentModel file = new DocumentModelImpl("/", "testfile", "File2");
        file.setPropertyValue("dc:title", "title1");
        file.setPropertyValue("tst2:title", "title2");
        file = session.createDocument(file);
        session.save();

        query = "SELECT tst2:title, dc:title FROM File WHERE dc:title = 'title1' AND ecm:isProxy = 0";
        res = session.queryAndFetch(query, "NXQL");
        assertEquals(1, res.size());
        map = res.iterator().next();
        assertEquals("title1", map.get("dc:title"));
        assertEquals("title2", map.get("tst2:title"));
        res.close();

        // now with proxies, which needs a subselect and re-selects columns
        query = "SELECT tst2:title, dc:title FROM File WHERE dc:title = 'title1' ORDER BY ecm:uuid";
        res = session.queryAndFetch(query, "NXQL");
        assertEquals(1, res.size());
        map = res.iterator().next();
        assertEquals("title1", map.get("dc:title"));
        assertEquals("title2", map.get("tst2:title"));
        res.close();

        // same without ORDER BY
        query = "SELECT tst2:title, dc:title FROM File WHERE dc:title = 'title1'";
        res = session.queryAndFetch(query, "NXQL");
        assertEquals(1, res.size());
        map = res.iterator().next();
        assertEquals("title1", map.get("dc:title"));
        assertEquals("title2", map.get("tst2:title"));
        res.close();
    }

    public void testSelectColumnsDistinct() throws Exception {
        String query;
        IterableQueryResult res;

        createDocs();

        query = "SELECT DISTINCT dc:title FROM File";
        res = session.queryAndFetch(query, "NXQL");
        assertEquals(3, res.size());
        res.close();

        // some parents are identical
        query = "SELECT DISTINCT ecm:parentId FROM File";
        res = session.queryAndFetch(query, "NXQL");
        assertEquals(2, res.size());
        res.close();

        // without column aliasing
        query = "SELECT DISTINCT ecm:parentId FROM File WHERE ecm:isProxy = 0";
        res = session.queryAndFetch(query, "NXQL");
        assertEquals(2, res.size());
        res.close();
    }

}
