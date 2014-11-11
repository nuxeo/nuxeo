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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.query.test.QueryTestCase;

/**
 * @author Florent Guillaume
 */
public class TestSQLRepositoryQuery extends QueryTestCase {

    @Override
    public void deployRepository() throws Exception {
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
    protected void sleepForFulltext() throws Exception {
        super.sleepForFulltext();
        DatabaseHelper.DATABASE.sleepForFulltext();
    }

    @Override
    public void testFulltextBlob() throws Exception {
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.core.convert.plugins");
        deployBundle("org.nuxeo.ecm.core.storage.sql"); // event listener
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

    public void testQueryIterable() throws Exception {
        createDocs();

        IterableQueryResult res = session.queryAndFetch("SELECT * FROM File",
                "NXQL");
        List<Map<String, Serializable>> l = new LinkedList<Map<String, Serializable>>();
        for (Map<String, Serializable> x : res) {
            l.add(x);
        }
        assertEquals(3, l.size());

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
    }

    @Override
    public void testQueryBasic() throws Exception {
        // Documents without creation date don't match any DATE query
        // 2 documents with creation date
        super.testQueryBasic();
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
    }

}
