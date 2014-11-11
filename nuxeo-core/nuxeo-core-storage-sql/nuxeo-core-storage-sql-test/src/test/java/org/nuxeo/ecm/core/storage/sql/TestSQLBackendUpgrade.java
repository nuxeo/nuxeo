/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.jdbc.JDBCMapper;

public class TestSQLBackendUpgrade extends SQLBackendTestCase {

    @BeforeClass
    public static void checkAssumptions() {
        assumeTrue(!"sequence".equals(DatabaseHelper.DEF_ID_TYPE));
        assumeTrue(!(DatabaseHelper.DATABASE instanceof DatabaseDerby));
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.core.storage.sql.test.tests",
                "OSGI-INF/test-backend-core-types-contrib.xml");
        JDBCMapper.testProps.put(JDBCMapper.TEST_UPGRADE, Boolean.TRUE);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        JDBCMapper.testProps.clear();
        super.tearDown();
    }

    protected void setUpTestProp(String prop) {
        for (String p : Arrays.asList(JDBCMapper.TEST_UPGRADE_VERSIONS,
                JDBCMapper.TEST_UPGRADE_LAST_CONTRIBUTOR,
                JDBCMapper.TEST_UPGRADE_LOCKS)) {
            JDBCMapper.testProps.put(p, Boolean.valueOf(p.equals(prop)));
        }
    }

    protected static boolean isLatestVersion(Node node) throws Exception {
        Boolean b = (Boolean) node.getSimpleProperty(
                Model.VERSION_IS_LATEST_PROP).getValue();
        return b.booleanValue();
    }

    protected static boolean isLatestMajorVersion(Node node) throws Exception {
        Boolean b = (Boolean) node.getSimpleProperty(
                Model.VERSION_IS_LATEST_MAJOR_PROP).getValue();
        return b.booleanValue();
    }

    protected static String getVersionLabel(Node node) throws Exception {
        return (String) node.getSimpleProperty(Model.VERSION_LABEL_PROP).getValue();
    }

    @Test
    public void testBasicsUpgrade() throws Exception {
        setUpTestProp("");

        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node nodea = session.addChildNode(root, "foo", null, "TestDoc", false);

        nodea.setSimpleProperty("tst:title", "hello world");
        nodea.setSimpleProperty("tst:rate", Double.valueOf(1.5));
        nodea.setSimpleProperty("tst:count", Long.valueOf(123456789));
        Calendar cal = new GregorianCalendar(2008, Calendar.JULY, 14, 12, 34,
                56);
        nodea.setSimpleProperty("tst:created", cal);
        nodea.setCollectionProperty("tst:subjects", new String[] { "a", "b",
                "c" });
        nodea.setCollectionProperty("tst:tags", new String[] { "1", "2" });

        assertEquals("hello world",
                nodea.getSimpleProperty("tst:title").getString());
        assertEquals(Double.valueOf(1.5),
                nodea.getSimpleProperty("tst:rate").getValue());
        assertEquals(Long.valueOf(123456789),
                nodea.getSimpleProperty("tst:count").getValue());
        assertNotNull(nodea.getSimpleProperty("tst:created").getValue());
        String[] subjects = nodea.getCollectionProperty("tst:subjects").getStrings();
        String[] tags = nodea.getCollectionProperty("tst:tags").getStrings();
        assertEquals(Arrays.asList("a", "b", "c"), Arrays.asList(subjects));
        assertEquals(Arrays.asList("1", "2"), Arrays.asList(tags));

        session.save();

        // now modify a property and re-save
        nodea.setSimpleProperty("tst:title", "another");
        nodea.setSimpleProperty("tst:rate", Double.valueOf(3.14));
        nodea.setSimpleProperty("tst:count", Long.valueOf(1234567891234L));
        nodea.setCollectionProperty("tst:subjects", new String[] { "z", "c" });
        nodea.setCollectionProperty("tst:tags", new String[] { "3" });
        session.save();

        // again
        nodea.setSimpleProperty("tst:created", null);
        session.save();

        // check the logs to see that the following doesn't do anything because
        // the value is unchanged since the last save (UPDATE optimizations)
        nodea.setSimpleProperty("tst:title", "blah");
        nodea.setSimpleProperty("tst:title", "another");
        session.save();

        // now read from another session
        session.close();
        session = repository.getConnection();
        root = session.getRootNode();
        assertNotNull(root);
        nodea = session.getChildNode(root, "foo", false);
        assertEquals("another",
                nodea.getSimpleProperty("tst:title").getString());
        assertEquals(Double.valueOf(3.14),
                nodea.getSimpleProperty("tst:rate").getValue());
        assertEquals(Long.valueOf(1234567891234L),
                nodea.getSimpleProperty("tst:count").getValue());
        subjects = nodea.getCollectionProperty("tst:subjects").getStrings();
        tags = nodea.getCollectionProperty("tst:tags").getStrings();
        assertEquals(Arrays.asList("z", "c"), Arrays.asList(subjects));
        assertEquals(Arrays.asList("3"), Arrays.asList(tags));

        // delete the node
        // session.removeNode(nodea);
        // session.save();
    }

    @Test
    public void testVersionsUpgrade() throws Exception {
        setUpTestProp(JDBCMapper.TEST_UPGRADE_VERSIONS);

        Node ver;
        Session session = repository.getConnection();

        // check normal doc is not a version
        Node doc = session.getNodeById("dddddddd-dddd-dddd-dddd-dddddddddddd");
        assertFalse(doc.isVersion());

        // v 1
        ver = session.getNodeById("11111111-0000-0000-2222-000000000000");
        assertTrue(ver.isVersion());
        assertFalse(isLatestVersion(ver));
        assertFalse(isLatestMajorVersion(ver));
        assertEquals("0.1", getVersionLabel(ver));

        // v 2
        ver = session.getNodeById("11111111-0000-0000-2222-000000000001");
        assertTrue(ver.isVersion());
        assertFalse(isLatestVersion(ver));
        assertTrue(isLatestMajorVersion(ver));
        assertEquals("1.0", getVersionLabel(ver));

        // v 3
        ver = session.getNodeById("11111111-0000-0000-2222-000000000002");
        assertTrue(ver.isVersion());
        assertTrue(isLatestVersion(ver));
        assertFalse(isLatestMajorVersion(ver));
        assertEquals("1.1", getVersionLabel(ver));

        // v 4 other doc
        ver = session.getNodeById("11111111-0000-0000-3333-000000000001");
        assertTrue(ver.isVersion());
        assertTrue(isLatestVersion(ver));
        assertTrue(isLatestMajorVersion(ver));
        assertEquals("1.0", getVersionLabel(ver));
    }

    @Test
    public void testLastContributorUpgrade() throws StorageException {
        setUpTestProp(JDBCMapper.TEST_UPGRADE_LAST_CONTRIBUTOR);

        Node ver;
        Session session = repository.getConnection();

        ver = session.getNodeById("12121212-dddd-dddd-dddd-000000000000");
        assertNotNull(ver);
        assertEquals("mynddoc", ver.getName());
        assertEquals("Administrator",
                ver.getSimpleProperty("dc:creator").getString());
        assertEquals("Administrator",
                ver.getSimpleProperty("dc:lastContributor").getString());

        ver = session.getNodeById("12121212-dddd-dddd-dddd-000000000001");
        assertNotNull(ver);
        assertEquals("myrddoc", ver.getName());
        assertEquals("Administrator",
                ver.getSimpleProperty("dc:creator").getString());
        assertEquals("FakeOne",
                ver.getSimpleProperty("dc:lastContributor").getString());
    }

    @Test
    public void testLocksUpgrade() throws Exception {
        setUpTestProp(JDBCMapper.TEST_UPGRADE_LOCKS);

        Session session = repository.getConnection();
        String id;
        Lock lock;

        // check lock has been upgraded from 'bob:Jan 26, 2011'
        id = "dddddddd-dddd-dddd-dddd-dddddddddddd";
        lock = session.getLock(id);
        assertNotNull(lock);
        assertEquals("bob", lock.getOwner());
        Calendar expected = new GregorianCalendar(2011, Calendar.JANUARY, 26,
                0, 0, 0);
        assertEquals(expected, lock.getCreated());

        // old lock was nulled after unlock
        id = "11111111-2222-3333-4444-555555555555";
        lock = session.getLock(id);
        assertNull(lock);
    }

}
