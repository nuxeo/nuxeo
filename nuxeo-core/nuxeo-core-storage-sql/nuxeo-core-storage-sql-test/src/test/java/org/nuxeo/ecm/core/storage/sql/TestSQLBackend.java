/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import javax.naming.NamingException;
import javax.resource.ResourceException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.DocumentExistsException;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.binary.BinaryManager;
import org.nuxeo.ecm.core.blob.binary.BinaryManagerStatus;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.model.LockManager;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepositoryService;
import org.nuxeo.ecm.core.storage.sql.jdbc.JDBCConnection;
import org.nuxeo.ecm.core.storage.sql.jdbc.JDBCMapperConnector;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.runtime.transaction.TransactionRuntimeException;

@Deploy("org.nuxeo.ecm.core.storage.sql.test.tests:OSGI-INF/test-backend-core-types-contrib.xml")
public class TestSQLBackend extends SQLBackendTestCase {

    private static final Log log = LogFactory.getLog(TestSQLBackend.class);

    private static final int ITERATIONS = 5;

    private static final int THREADS = 5;

    protected boolean pathOptimizationsEnabled;

    @Override
    public void setUp() throws Exception {
        pathOptimizationsEnabled = true; // changed in a few tests
        super.setUp();
    }

    @Override
    protected RepositoryDescriptor newDescriptor(String name, long clusteringDelay) {
        RepositoryDescriptor descriptor = super.newDescriptor(name, clusteringDelay);
        descriptor.setPathOptimizationsEnabled(pathOptimizationsEnabled);
        return descriptor;
    }

    protected boolean useArrayColumns() {
        return false;
    }

    @Test
    public void testRootNode() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        assertNotNull(root);
        if (DatabaseHelper.DATABASE instanceof DatabaseOracle) {
            assertNull(root.getName()); // For Oracle empty strings are equivalent to NULL
        } else {
            assertEquals("", root.getName());
        }
        assertEquals("/", session.getPath(root));
        assertEquals("Root", root.getSimpleProperty("ecm:primaryType").getString());
        try {
            root.getSimpleProperty("tst:title");
            fail("Property should not exist");
        } catch (PropertyNotFoundException e) {
            // ok
        }
        session.save();
        session.close();
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.storage.sql.test.tests:OSGI-INF/test-schema-longname.xml")
    public void testSchemaWithLongName() {
        Session session = repository.getConnection();
        session.getRootNode();
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.storage.sql.test.tests:OSGI-INF/test-schema-reservedfieldname.xml")
    public void testSchemaWithReservedFieldName() {
        Session session = repository.getConnection();
        session.getRootNode();
    }

    protected int getChildrenHardSize(Session session) {
        return ((SessionImpl) session).context.hierNonComplex.hardMap.size();
    }

    @Test
    public void testChildren() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();

        try {
            session.addChildNode(root, "foo", null, "not_a_type", false);
            fail("Should not allow illegal type");
        } catch (IllegalArgumentException e) {
            // ok
        }

        // root doc /foo
        Node nodefoo = session.addChildNode(root, "foo", null, "TestDoc", false);
        assertEquals(root.getId(), session.getParentNode(nodefoo).getId());
        assertEquals("TestDoc", nodefoo.getPrimaryType());
        assertEquals("/foo", session.getPath(nodefoo));
        Node nodeabis = session.getChildNode(root, "foo", false);
        assertEquals(nodefoo.getId(), nodeabis.getId());

        // root is in hard because it has a created child
        assertEquals(1, getChildrenHardSize(session));

        // first child /foo/bar
        Node nodeb = session.addChildNode(nodefoo, "bar", null, "TestDoc", false);
        assertEquals("/foo/bar", session.getPath(nodeb));
        assertEquals(nodefoo.getId(), session.getParentNode(nodeb).getId());
        assertEquals(nodeb.getId(), session.getNodeByPath("/foo/bar", null).getId());

        // foo is now in hard as well
        assertEquals(2, getChildrenHardSize(session));

        session.save();
        // everything moved back to soft, therefore GCable
        assertEquals(0, getChildrenHardSize(session));
        session.close();

        /*
         * now from another session
         */
        session = repository.getConnection();
        root = session.getRootNode();
        nodefoo = session.getChildNode(root, "foo", false);
        assertEquals("foo", nodefoo.getName());
        assertEquals("/foo", session.getPath(nodefoo));

        // second child /foo/gee
        Node nodec = session.addChildNode(nodefoo, "gee", null, "TestDoc", false);
        assertEquals("/foo/gee", session.getPath(nodec));
        List<Node> children = session.getChildren(nodefoo, null, false);
        assertEquals(2, children.size());

        session.save();

        children = session.getChildren(nodefoo, null, false);
        assertEquals(2, children.size());

        // delete bar
        session.removeNode(nodefoo);
        // root in hard, has one removed child
        assertEquals(1, getChildrenHardSize(session));
        session.save();
        // everything moved back to soft
        assertEquals(0, getChildrenHardSize(session));
    }

    @Test
    public void testChildrenRemoval() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Serializable fooId = session.addChildNode(root, "foo", null, "TestDoc", false).getId();
        Serializable barId = session.addChildNode(root, "bar", null, "TestDoc", false).getId();
        session.save();
        session.close();

        // from another session
        // get one and remove it
        session = repository.getConnection();
        root = session.getRootNode();
        session.getNodeById(fooId); // one known child
        Node nodebar = session.getNodeById(barId); // another
        session.removeNode(nodebar); // remove one known
        // check removal in Children cache
        nodebar = session.getChildNode(root, "bar", false);
        assertNull(nodebar);
        // the following gets a complete list but skips deleted ones
        List<Node> children = session.getChildren(root, null, false);
        assertEquals(1, children.size());
        session.save();
    }

    @Test
    public void testChildrenRemoval2() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node foo = session.addChildNode(root, "foo", null, "TestDoc", false);
        session.removeNode(foo);
        List<Node> children = session.getChildren(root, null, false);
        assertEquals(0, children.size());
        session.save(); // important for the test
    }

    @Test
    public void testChildrenRemoval3() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node foo = session.addChildNode(root, "foo", null, "TestDoc", false);
        session.addChildNode(foo, "bar", null, "TestDoc", false);
        session.removeNode(foo);
        List<Node> children = session.getChildren(root, null, false);
        assertEquals(0, children.size());
        session.save(); // important for the test
    }

    @Test
    public void testRecursiveRemoval() throws Exception {
        int depth = DatabaseHelper.DATABASE.getRecursiveRemovalDepthLimit();
        if (depth == 0) {
            // no limit
            depth = 70;
        }
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node node = root;
        Serializable[] ids = new Serializable[depth];
        for (int i = 0; i < depth; i++) {
            node = session.addChildNode(node, String.valueOf(i), null, "TestDoc", false);
            ids[i] = node.getId();
        }
        session.save(); // TODO NXP-3126 shouldn't be needed
        // delete the second one
        session.removeNode(session.getNodeById(ids[1]));
        session.save();
        session.close();

        // check all children were really deleted recursively
        session = repository.getConnection();
        for (int i = 1; i < depth; i++) {
            assertNull(session.getNodeById(ids[i]));
        }
    }

    // same as above but without opening a new session
    @Test
    public void testRecursiveRemoval2() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node node = root;
        int depth = 5;
        Serializable[] ids = new Serializable[depth];
        for (int i = 0; i < depth; i++) {
            node = session.addChildNode(node, String.valueOf(i), null, "TestDoc", false);
            ids[i] = node.getId();
        }
        session.save();
        // delete the second one
        session.removeNode(session.getNodeById(ids[1]));
        session.save();

        // check all children were really deleted recursively
        for (int i = 1; i < depth; i++) {
            assertNull("" + i, session.getNodeById(ids[i]));
        }
    }

    // wider depth of tree
    @Test
    public void testRecursiveRemoval3() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node base = session.addChildNode(root, "base", null, "TestDoc", false);
        int depth = 3;
        int width = 3;
        List<Node> todo = Collections.singletonList(base);
        List<Serializable> ids = new ArrayList<>();
        for (int i = 0; i < depth; i++) {
            List<Node> newTodo = new ArrayList<>();
            for (Node node : todo) {
                for (int j = 0; j < width; j++) {
                    String name = node.getName() + "-" + j;
                    Node child = session.addChildNode(node, name, null, "TestDoc", false);
                    ids.add(child.getId());
                    newTodo.add(child);
                }
            }
            todo = newTodo;
        }
        session.save();
        // delete base
        session.removeNode(base);
        session.save();
        // check all children were really deleted recursively
        for (Serializable id : ids) {
            assertNull(id.toString(), session.getNodeById(id));
        }
    }

    // more than 1000 children without path optimizations (for Oracle, NXP-20211)
    @Test
    public void testRecursiveRemovalBigWithoutPathOptimizations() throws Exception {
        repository.close();
        // open a repository without path optimization
        pathOptimizationsEnabled = false;
        repository = newRepository(-1);

        testRecursiveRemovalBig();
    }

    // more than 1000 children
    @Test
    public void testRecursiveRemovalBig() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node base = session.addChildNode(root, "base", null, "TestDoc", false);
        int n = 1100; // > 1000, the max for Oracle
        List<Serializable> ids = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String name = "doc" + i;
            Node child = session.addChildNode(base, name, null, "TestDoc", false);
            ids.add(child.getId());
        }
        session.save();
        // delete base
        session.removeNode(base);
        session.save();
        // check all children were really deleted recursively
        for (Serializable id : ids) {
            assertNull(id.toString(), session.getNodeById(id));
        }
    }

    @Test
    public void testBasics() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node nodea = session.addChildNode(root, "foo", null, "TestDoc", false);

        nodea.setSimpleProperty("tst:title", "hello world");
        nodea.setSimpleProperty("tst:rate", Double.valueOf(1.5));
        nodea.setSimpleProperty("tst:count", Long.valueOf(123456789));
        Calendar cal = new GregorianCalendar(2008, Calendar.JULY, 14, 12, 34, 56);
        nodea.setSimpleProperty("tst:created", cal);
        nodea.setCollectionProperty("tst:subjects", new String[] { "a", "b", "c" });
        nodea.setCollectionProperty("tst:tags", new String[] { "1", "2" });

        assertEquals("hello world", nodea.getSimpleProperty("tst:title").getString());
        assertEquals(Double.valueOf(1.5), nodea.getSimpleProperty("tst:rate").getValue());
        assertEquals(Long.valueOf(123456789), nodea.getSimpleProperty("tst:count").getValue());
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
        assertEquals("another", nodea.getSimpleProperty("tst:title").getString());
        assertEquals(Double.valueOf(3.14), nodea.getSimpleProperty("tst:rate").getValue());
        assertEquals(Long.valueOf(1234567891234L), nodea.getSimpleProperty("tst:count").getValue());
        subjects = nodea.getCollectionProperty("tst:subjects").getStrings();
        tags = nodea.getCollectionProperty("tst:tags").getStrings();
        assertEquals(Arrays.asList("z", "c"), Arrays.asList(subjects));
        assertEquals(Arrays.asList("3"), Arrays.asList(tags));

        // delete the node
        // session.removeNode(nodea);
        // session.save();
    }

    /**
     * Test persistence of date in another time zone than the default one.
     *
     * @since 9.3
     */
    @Test
    public void testDateWithTimeZone() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node node = session.addChildNode(root, "foo", null, "TestDoc", false);

        GregorianCalendar cal = new GregorianCalendar(2008, Calendar.DECEMBER, 14, 12, 34, 56);

        // cal has a the default time zone, let's change it to UTC (keeping the same instant)
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        node.setSimpleProperty("tst:created", cal);
        session.save();

        // reopen a session to read from database and not caches
        session.close();
        session = repository.getConnection();
        node = session.getChildNode(session.getRootNode(), "foo", false);

        // read date should be the same instant
        GregorianCalendar readCal = (GregorianCalendar) node.getSimpleProperty("tst:created").getValue();
        assertEqualsCalendars(cal, readCal);

        // in case the default time zone is already UTC let's test with another one
        cal.setTimeZone(TimeZone.getTimeZone("EST"));
        node.setSimpleProperty("tst:created", cal);
        session.save();

        // reopen a session to read from database and not caches
        session.close();
        session = repository.getConnection();
        node = session.getChildNode(session.getRootNode(), "foo", false);

        // read date should be the same instant
        readCal = (GregorianCalendar) node.getSimpleProperty("tst:created").getValue();
        assertEqualsCalendars(cal, readCal);
    }

    /** Gives a clear error message with full dates on assertion failure. */
    public static void assertEqualsCalendars(GregorianCalendar a, GregorianCalendar b) {
        if (a.getTimeInMillis() != b.getTimeInMillis()) {
            assertEquals(a.toZonedDateTime().toString(), b.toZonedDateTime().toString());
        }
    }

    @Test
    public void testArrayUpdate() throws Exception {
        Session session = repository.getConnection();
        Node node = session.addChildNode(session.getRootNode(), "doc", null, "TestDoc", false);
        node.setCollectionProperty("tst:subjects", new String[] { "a" });
        session.save();

        // add elements to the end of the array
        node.setCollectionProperty("tst:subjects", new String[] { "a", "b", "c", "c" });
        session.save();

        // reopen
        session.close();
        session = repository.getConnection();
        node = session.getChildNode(session.getRootNode(), "doc", false);

        // check
        String[] subjects = node.getCollectionProperty("tst:subjects").getStrings();
        assertEquals(Arrays.asList("a", "b", "c", "c"), Arrays.asList(subjects));

        // remove elements from the end of the array
        node.setCollectionProperty("tst:subjects", new String[] { "a", "b", "c" });
        session.save();

        // reopen
        session.close();
        session = repository.getConnection();
        node = session.getChildNode(session.getRootNode(), "doc", false);

        // check
        subjects = node.getCollectionProperty("tst:subjects").getStrings();
        assertEquals(Arrays.asList("a", "b", "c"), Arrays.asList(subjects));
    }

    @Test
    public void testParallelArrayUpdate() throws Exception {
        Session session = repository.getConnection();
        Node node = session.addChildNode(session.getRootNode(), "doc", null, "TestDoc", false);
        node.setCollectionProperty("tst:subjects", new String[] { "a" });
        session.save();
        session.close();

        for (int i = 0; i < ITERATIONS; i++) {
            parallelArrayUpdaterJob();
        }

        session = repository.getConnection();
        node = session.getChildNode(session.getRootNode(), "doc", false);
        String[] subjects = node.getCollectionProperty("tst:subjects").getStrings();
        assertEquals(Arrays.toString(subjects), 1 + THREADS * ITERATIONS, subjects.length);
        session.close();
    }

    protected void parallelArrayUpdaterJob() throws Exception {
        List<Thread> threads = new ArrayList<>(THREADS);
        for (int n = 0; n < THREADS; n++) {
            threads.add(new ArrayUpdaterJob(repository));
        }
        try {
            for (Thread t : threads) {
                t.start();
            }
        } finally {
            for (Thread t : threads) {
                t.join();
            }
        }
        threads.clear();
    }

    protected static class ArrayUpdaterJob extends Thread {

        protected final Repository repository;

        protected Session session;

        protected ArrayUpdaterJob(Repository repository) {
            this.repository = repository;
        }

        @Override
        public void run() {
            try {
                session = repository.getConnection();
                try {
                    begin();
                    try {
                        updateArrayMaybeRetry();
                    } finally {
                        commit();
                    }
                } finally {
                    session.close();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        protected static final int TRIES = 10;

        protected static final Random RANDOM = new Random();

        protected void updateArrayMaybeRetry() {
            for (int i = 0; i < TRIES; i++) {
                try {
                    updateArray();
                    return;
                } catch (ConcurrentUpdateException e) {
                }
                // retry after flushing caches and making new database state visible
                ((SessionImpl) session).clearCaches();
                TransactionHelper.commitOrRollbackTransaction();
                // randomized wait to avoid everyone being in sync
                try {
                    Thread.sleep(Math.abs(RANDOM.nextInt() % 100));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                TransactionHelper.startTransaction();
            }
            throw new ConcurrentUpdateException("Cannot update, too much concurrency");
        }

        protected void updateArray() {
            Node node = session.getChildNode(session.getRootNode(), "doc", false);
            // read
            String[] subjects = node.getCollectionProperty("tst:subjects").getStrings();
            // append
            List<String> l = new ArrayList<>(Arrays.asList(subjects));
            l.add("x");
            String[] newSubjects = l.toArray(new String[0]);
            node.setCollectionProperty("tst:subjects", newSubjects);
            session.save();
        }

        protected void begin() throws Exception {
            TransactionHelper.startTransaction();
            TransactionHelper.lookupTransactionManager().getTransaction().enlistResource((SessionImpl) session);
        }

        protected void commit() {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.storage.sql.test.tests:OSGI-INF/test-restriction-contrib.xml")
    public void testSmallText() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node nodea = session.addChildNode(root, "foo", null, "Restriction", false);
        nodea.setSimpleProperty("restr:shortstring", "this-is-short");
        session.save();

        // now read from another session
        session.close();
        session = repository.getConnection();
        root = session.getRootNode();
        nodea = session.getChildNode(root, "foo", false);
        String readtext = nodea.getSimpleProperty("restr:shortstring").getString();
        assertEquals("this-is-short", readtext);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.storage.sql.test.tests:OSGI-INF/test-restriction-big-contrib.xml")
    public void testBigText() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node nodea = session.addChildNode(root, "foo", null, "RestrictionBig", false);
        StringBuilder buf = new StringBuilder();
        int size = 10000; // from testrestrictionbig.xsd
        for (int i = 0; i < size - 1; i++) {
            buf.append("x");
        }
        String bigstring = buf.toString();
        nodea.setSimpleProperty("restrbg:bigstring", bigstring);
        session.save();

        // now read from another session
        session.close();
        session = repository.getConnection();
        root = session.getRootNode();
        nodea = session.getChildNode(root, "foo", false);
        String readtext = nodea.getSimpleProperty("restrbg:bigstring").getString();
        assertEquals(bigstring, readtext);
    }

    @Test
    public void testClobText() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node nodea = session.addChildNode(root, "foo", null, "TestDoc", false);

        StringBuilder buf = new StringBuilder(5000);
        for (int i = 0; i < 1000; i++) {
            buf.append(String.format("%-5d", Integer.valueOf(i)));
        }
        String bigtext = buf.toString();
        assertEquals(5000, bigtext.length());
        nodea.setSimpleProperty("tst:bignote", bigtext);
        nodea.setCollectionProperty("tst:bignotes", new String[] { bigtext });
        assertEquals(bigtext, nodea.getSimpleProperty("tst:bignote").getString());
        assertEquals(bigtext, nodea.getCollectionProperty("tst:bignotes").getStrings()[0]);
        session.save();

        // now read from another session
        session.close();
        session = repository.getConnection();
        root = session.getRootNode();
        assertNotNull(root);
        nodea = session.getChildNode(root, "foo", false);
        String readtext = nodea.getSimpleProperty("tst:bignote").getString();
        assertEquals(bigtext, readtext);
        String[] readtexts = nodea.getCollectionProperty("tst:bignotes").getStrings();
        assertEquals(bigtext, readtexts[0]);
    }

    @Test
    public void testPropertiesSameName() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node nodea = session.addChildNode(root, "foo", null, "TestDoc", false);

        nodea.setSimpleProperty("tst:title", "hello world");
        assertEquals("hello world", nodea.getSimpleProperty("tst:title").getString());

        try {
            nodea.setSimpleProperty("tst2:title", "aha");
            fail("shouldn't allow setting property from foreign schema");
        } catch (Exception e) {
            // ok
        }

        session.save();
    }

    @Test
    public void testBinary() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node nodea = session.addChildNode(root, "foo", null, "TestDoc", false);

        String data = "900150983cd24fb0d6963f7d28e17f72";

        nodea.setSimpleProperty("tst:bin", data);
        session.save();
        session.close();

        // now read from another session
        session = repository.getConnection();
        root = session.getRootNode();
        nodea = session.getChildNode(root, "foo", false);
        SimpleProperty binProp = nodea.getSimpleProperty("tst:bin");
        assertNotNull(binProp);
        Serializable value = binProp.getValue();
        assertTrue(value instanceof String);
        assertEquals(data, value);
    }

    // assumes one read will read everything
    protected String readAllBytes(InputStream in) throws IOException {
        if (!(in instanceof BufferedInputStream)) {
            in = new BufferedInputStream(in);
        }
        int len = in.available();
        byte[] bytes = new byte[len];
        int read = in.read(bytes);
        assertEquals(len, read);
        assertEquals(-1, in.read()); // EOF
        return new String(bytes, "ISO-8859-1");
    }

    @Test
    @ConditionalIgnoreRule.Ignore(condition = ConditionalIgnoreRule.IgnoreWindows.class, cause = "windows doesn't have enough time granularity for such a test")
    public void testBinaryGC() throws Exception {
        SQLRepositoryService sqlRepositoryService = Framework.getService(SQLRepositoryService.class);

        Session session = repository.getConnection();

        // store some binaries
        for (String str : Arrays.asList("ABC", "DEF", "GHI", "JKL")) {
            addBinary(session, str, str);
            addBinary(session, str, str + "2");
        }
        session.save();

        BinaryManagerStatus status = runBinariesGC(0, session, true);
        assertEquals(4, status.numBinaries); // ABC, DEF, GHI, JKL
        assertEquals(4 * 3, status.sizeBinaries);
        assertEquals(0, status.numBinariesGC);
        assertEquals(0, status.sizeBinariesGC);

        // remove some binaries
        session.removeNode(session.getNodeByPath("/ABC", null));
        session.removeNode(session.getNodeByPath("/ABC2", null));
        session.removeNode(session.getNodeByPath("/DEF", null));
        session.removeNode(session.getNodeByPath("/DEF2", null));
        session.removeNode(session.getNodeByPath("/GHI", null)); // GHI2 remains
        // JKL and JKL2 remain
        session.save();

        // run GC in non-delete mode
        Thread.sleep(3 * 1000); // sleep before GC to pass its time threshold
        status = runBinariesGC(0, session, false);
        if (isSoftDeleteEnabled()) {
            // with soft delete nothing is actually deleted yet
            assertEquals(4, status.numBinaries);
            assertEquals(4 * 3, status.sizeBinaries);
            assertEquals(0, status.numBinariesGC);
            assertEquals(0 * 3, status.sizeBinariesGC);
            // do hard delete
            RepositoryManagement repoMgmt = sqlRepositoryService.getRepository(repository.getName());
            repoMgmt.cleanupDeletedDocuments(0, null);
            // rerun GC in non-delete mode
            Thread.sleep(3 * 1000);
            status = runBinariesGC(0, session, false);
        }
        assertEquals(2, status.numBinaries); // GHI, JKL
        assertEquals(2 * 3, status.sizeBinaries);
        assertEquals(2, status.numBinariesGC); // ABC, DEF
        assertEquals(2 * 3, status.sizeBinariesGC);

        // add a new binary during GC and revive one which was about to die
        status = runBinariesGC(1, session, true);
        assertEquals(4, status.numBinaries); // DEF3, GHI2, JKL, MNO
        assertEquals(4 * 3, status.sizeBinaries);
        assertEquals(1, status.numBinariesGC); // ABC
        assertEquals(1 * 3, status.sizeBinariesGC);

        Thread.sleep(3 * 1000);
        status = runBinariesGC(0, session, true);
        assertEquals(4, status.numBinaries); // DEF3, GHI2, JKL, MNO
        assertEquals(4 * 3, status.sizeBinaries);
        assertEquals(0, status.numBinariesGC);
        assertEquals(0, status.sizeBinariesGC);
    }

    protected void addBinary(Session session, String binstr, String name) throws Exception {
        Blob blob = Blobs.createBlob(binstr);
        BlobManager blobManager = Framework.getService(BlobManager.class);
        BlobProvider blobProvider = blobManager.getBlobProvider(session.getRepositoryName());
        String key = blobProvider.writeBlob(blob);
        session.addChildNode(session.getRootNode(), name, null, "TestDoc", false).setSimpleProperty("tst:bin", key);
    }

    protected BinaryManagerStatus runBinariesGC(int moreWork, Session session, boolean delete) throws Exception {
        BlobManager blobManager = Framework.getService(BlobManager.class);
        BlobProvider blobProvider = blobManager.getBlobProvider(session.getRepositoryName());
        BinaryManager binaryManager = blobProvider.getBinaryManager();
        BinaryGarbageCollector gc = binaryManager.getGarbageCollector();
        assertFalse(gc.isInProgress());
        gc.start();
        assertTrue(gc.isInProgress());
        repository.markReferencedBinaries();
        if (moreWork == 1) {
            // while GC is in progress
            // add a new binary
            addBinary(session, "MNO", "MNO");
            // and revive one that was about to be deleted
            // note that this wouldn't work if we didn't recreate the Binary
            // object from an InputStream and reused an old one
            addBinary(session, "DEF", "DEF3");
            session.save();
        }
        gc.stop(delete);
        return gc.getStatus();
    }

    @Test
    public void testACLs() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        CollectionProperty prop = root.getCollectionProperty(Model.ACL_PROP);
        assertNotNull(prop);
        assertEquals(3, prop.getValue().length); // root acls preexist
        ACLRow acl1 = new ACLRow(1, "test", true, "Write", "steve", null);
        ACLRow acl2 = new ACLRow(0, "test", true, "Read", null, "Members");
        prop.setValue(new ACLRow[] { acl1, acl2 });
        session.save();
        session.close();
        session = repository.getConnection();
        root = session.getRootNode();
        prop = root.getCollectionProperty(Model.ACL_PROP);
        ACLRow[] acls = (ACLRow[]) prop.getValue();
        assertEquals(2, acls.length);
        assertEquals("Members", acls[0].group);
        assertEquals("test", acls[0].name);
        assertEquals("steve", acls[1].user);
        assertEquals("test", acls[1].name);
    }

    /** ACL bigger than VARCHAR limit for databases. */
    @Test
    public void testBigACLs() throws Exception {
        if (!(DatabaseHelper.DATABASE instanceof DatabasePostgreSQL //
                || DatabaseHelper.DATABASE instanceof DatabaseOracle)) {
            return;
        }
        testBigACLs("foo100", 100); // len 2500-1
        testBigACLs("foo161", 161); // len 4025-1 failed on Oracle
        // TODO XXX test bigger ACL on PostgreSQL
        if (DatabaseHelper.DATABASE instanceof DatabaseOracle) {
            testBigACLs("foo1400", 1400); // len 35000-1
        }
    }

    protected void testBigACLs(String name, int n) throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        root.getCollectionProperty(Model.ACL_PROP).setValue(new ACLRow[0]);

        Node node = session.addChildNode(root, name, null, "TestDoc", false);
        CollectionProperty prop = node.getCollectionProperty(Model.ACL_PROP);

        String user = "foobarfoobarfoobarfoobar"; // 24+1=25
        ACLRow[] acls = new ACLRow[n];
        for (int i = 0; i < n; i++) {
            acls[i] = new ACLRow(i, "test", true, "Read", user, null);
        }
        prop.setValue(acls);
        session.save();
        session.updateReadAcls();

        QueryFilter qf;
        PartialList<Serializable> res;

        // random user with no groups cannot read
        qf = new QueryFilter(null, new String[] { "bob" }, new String[] { "Read" }, null,
                Collections.<SQLQuery.Transformer> emptyList(), 0, 0);
        String query = String.format("SELECT * FROM TestDoc WHERE ecm:name = '%s'", name);
        res = session.query(query, qf, false);
        assertEquals(0, res.size());

        // user in ACL can read
        qf = new QueryFilter(null, new String[] { user }, new String[] { "Read" }, null,
                Collections.<SQLQuery.Transformer> emptyList(), 0, 0);
        res = session.query(query, qf, false);
        assertEquals(1, res.size());

        session.close();
    }

    @Test
    public void testUpdateReadAclsDeadlock() throws Exception {
        repository.getConnection().close(); // create repo
        for (int i = 0; i < ITERATIONS; i++) {
            multiThreadedUpdateReadAclsJob(i);
        }
    }

    protected void multiThreadedUpdateReadAclsJob(int i) throws Exception {
        List<Thread> threads = new ArrayList<>(THREADS);
        for (int n = 0; n < THREADS; n++) {
            DocCreator creator = new DocCreator(repository, "doc-" + i + "-" + n);
            threads.add(new Thread(creator));
        }
        try {
            for (Thread t : threads) {
                t.start();
            }
        } finally {
            for (Thread t : threads) {
                t.join();
            }
        }
        threads.clear();

        Session session = repository.getConnection();
        Node root = session.getRootNode();
        List<Node> children = session.getChildren(root, null, false);
        assertEquals(THREADS * 2 * (i + 1), children.size());
        session.close();
    }

    protected static class DocCreator implements Runnable {

        protected final Repository repository;

        protected final String name;

        protected final Random random;

        protected Session session;

        protected DocCreator(Repository repository, String name) {
            this.repository = repository;
            this.name = name;
            random = new Random();
        }

        @Override
        public void run() {
            try {
                session = repository.getConnection();
                try {
                    begin();
                    try {
                        createDoc();
                    } finally {
                        commit();
                    }
                } finally {
                    session.close();
                }
            } catch (ResourceException | XAException | IllegalStateException | RollbackException | SystemException
                    | NamingException e) {
                throw new RuntimeException(e);
            }
        }

        protected void createDoc() {
            Node root = session.getRootNode();
            session.addChildNode(root, name, null, "TestDoc", false);
            session.save();
            session.updateReadAcls();
            // let's create another one in the same transaction
            session.addChildNode(root, name + "-bis", null, "TestDoc", false);
            session.save();
            session.updateReadAcls();
        }

        protected void begin()
                throws XAException, IllegalStateException, RollbackException, SystemException, NamingException {
            TransactionHelper.startTransaction();
            SessionImpl xares = (SessionImpl) session;
            TransactionHelper.lookupTransactionManager().getTransaction().enlistResource(xares);
        }

        protected void commit() throws XAException {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

    public void XXX_TODO_testConcurrentModification() throws Exception {
        Session session1 = repository.getConnection();
        Node root1 = session1.getRootNode();
        Node node1 = session1.addChildNode(root1, "foo", null, "TestDoc", false);
        SimpleProperty title1 = node1.getSimpleProperty("tst:title");
        session1.save();

        Session session2 = repository.getConnection();
        Node root2 = session2.getRootNode();
        Node node2 = session2.getChildNode(root2, "foo", false);
        SimpleProperty title2 = node2.getSimpleProperty("tst:title");

        // change title1
        title1.setValue("yo");
        assertNull(title2.getString());
        // save session1 and queue its invalidations to others
        session1.save();
        // session2 has not saved (committed) yet, so still unmodified
        assertNull(title2.getString());
        session2.save();
        // after commit, invalidations have been processed
        assertEquals("yo", title2.getString());

        // written properties aren't shared
        title1.setValue("mama");
        title2.setValue("glop");
        session1.save();
        assertEquals("mama", title1.getString());
        assertEquals("glop", title2.getString());
        try {
            session2.save();
            fail("expected ConcurrentModificationException");
        } catch (ConcurrentModificationException e) {
            // expected
        }
    }

    @Test
    public void testConcurrentNameCreation() throws Exception {
        // two docs with same name (possible at this low level)
        Session session1 = repository.getConnection();
        Node root1 = session1.getRootNode();
        Node foo1 = session1.addChildNode(root1, "foo", null, "TestDoc", false);
        session1.save();
        Session session2 = repository.getConnection();
        Node root2 = session2.getRootNode();
        Node foo2 = session2.addChildNode(root2, "foo", null, "TestDoc", false);
        try {
            session2.save();
        } catch (ConcurrentUpdateException e) {
            // low-level duplicates are disabled (through unique indexes or constraints)
            // no need to test further
            return;
        }
        // on read we get one or the other, but no crash
        Session session3 = repository.getConnection();
        Node root3 = session3.getRootNode();
        Node foo3 = session3.getChildNode(root3, "foo", false);
        assertTrue(foo3.getId().equals(foo1.getId()) || foo3.getId().equals(foo2.getId()));
    }

    @Test
    public void testConcurrentComplexPropCreation() throws Exception {
        // two docs with same name (possible at this low level)
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node doc = session.addChildNode(root, "foo", null, "TestDoc", false);
        Node owner = session.getChildNode(doc, "tst:owner", true); // complex prop auto-created by parent
        // create a second one
        Node ownerbis = session.addChildNode(doc, "tst:owner", null, "person", true);
        try {
            session.save();
        } catch (ConcurrentUpdateException e) {
            // low-level duplicates are disabled (through unique indexes or constraints)
            // no need to test further
            return;
        }
        // on read we get one or the other, but no crash
        Session session2 = repository.getConnection();
        Node root2 = session2.getRootNode();
        Node doc2 = session2.getChildNode(root2, "foo", false);
        Node owner2 = session2.getChildNode(doc2, "tst:owner", true);
        assertTrue(owner2.getId().equals(owner.getId()) || owner2.getId().equals(ownerbis.getId()));
    }

    @Test
    public void testConcurrentComplexListCreation() throws Exception {
        // two docs with same name (possible at this low level)
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node doc = session.addChildNode(root, "foo", null, "TestDoc", false);
        session.addChildNode(doc, "tst:friends", Long.valueOf(0), "person", true);
        // create a second one at same pos
        session.addChildNode(doc, "tst:friends", Long.valueOf(0), "person", true);
        try {
            session.save();
        } catch (ConcurrentUpdateException e) {
            // low-level duplicates are disabled (through unique indexes or constraints)
            // no need to test further
            return;
        }
        // on read we get both and no crash
        Session session2 = repository.getConnection();
        Node root2 = session2.getRootNode();
        Node doc2 = session2.getChildNode(root2, "foo", false);
        List<Node> friends = session2.getChildren(doc2, "tst:friends", true);
        assertEquals(2, friends.size());
    }

    @Test
    public void testConcurrentCollectionPosCreation() throws Exception {
        // two docs with same name (possible at this low level)
        Session session1 = repository.getConnection();
        Node root1 = session1.getRootNode();
        Node foo1 = session1.addChildNode(root1, "foo", null, "TestDoc", false);
        session1.save();
        Session session2 = repository.getConnection();
        Node root2 = session2.getRootNode();
        Node foo2 = session2.getChildNode(root2, "foo", false);

        foo1.setCollectionProperty("tst:subjects", new String[] { "a" });
        session1.save();
        try {
            foo2.setCollectionProperty("tst:subjects", new String[] { "b" });
            session2.save();
        } catch (ConcurrentUpdateException e) {
            // low-level duplicates are disabled (through unique indexes or constraints)
            // no need to test further
            return;
        }
        // on read we get both, but order is indeterminate
        session2.close();
        session2 = repository.getConnection();
        root2 = session2.getRootNode();
        foo2 = session2.getChildNode(root2, "foo", false);
        String[] subjects = foo2.getCollectionProperty("tst:subjects").getStrings();
        try {
            assertArrayEquals(new String[] { "a", "b" }, subjects);
        } catch (AssertionError e) {
            assertArrayEquals(new String[] { "b", "a" }, subjects);
        }
    }

    @Test
    // unfortunately on H2 there's nothing much we can do about this
    @Ignore
    public void testTimeoutLockingTableWithH2() throws Exception {
        Session session1 = repository.getConnection();
        Node root1 = session1.getRootNode();
        Node node1 = session1.addChildNode(root1, "foo1", null, "TestDoc", false);
        session1.save();

        Session session2 = repository.getConnection();
        Node root2 = session2.getRootNode();
        Node foo2 = session2.addChildNode(root2, "foo2", null, "TestDoc", false);
        session2.save();

        TransactionHelper.startTransaction();
        try {
            TransactionHelper.lookupTransactionManager()
                             .getTransaction()
                             .enlistResource(((SessionImpl) session1).getXAResource());
            node1.setSimpleProperty("tst:title", "t1");
            Transaction tx1 = TransactionHelper.suspendTransaction();
            try {
                try {
                    TransactionHelper.lookupTransactionManager()
                                     .getTransaction()
                                     .enlistResource(((SessionImpl) session2).getXAResource());
                    foo2.getSimpleProperty("tst:title");
                } finally {
                    TransactionHelper.commitOrRollbackTransaction();
                }
            } finally {
                TransactionHelper.resumeTransaction(tx1);
            }
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }

        session1.close();
        session2.close();
    }

    @Test
    public void testDeadlockDetection() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        session.addChildNode(root, "doc", null, "TestDoc", false);
        session.save();
        DeadlockTestJob r1 = new DeadlockTestJob("foo1");
        DeadlockTestJob r2 = new DeadlockTestJob("foo2");
        try {
            LockStepJob.run(r1, r2);
            fail("Expected ConcurrentUpdateStorageException");
        } catch (RuntimeException e) {
            Throwable[] suppressed = e.getSuppressed();
            assertNotNull(suppressed);
            assertTrue(suppressed[0].toString(), suppressed[0] instanceof ConcurrentUpdateException);
        }
    }

    protected class DeadlockTestJob extends LockStepJob {

        protected String string;

        public DeadlockTestJob(String string) {
            this.string = string;
        }

        @Override
        public void job() throws Exception {
            TransactionHelper.startTransaction();
            try {
                Session session = repository.getConnection();
                try {
                    Node root = session.getRootNode();
                    Node node = session.getChildNode(root, "doc", false);
                    if (thread(1)) {
                        node.setSimpleProperty("tst:title", "t1"); // TESTSCHEMA
                        session.save();
                    }
                    if (thread(2)) {
                        node.setSimpleProperty("ecm:lifeCycleState", "s2"); // MISC
                        session.save();
                    }
                    if (thread(1)) {
                        node.setSimpleProperty("ecm:lifeCycleState", "s1"); // MISC
                    } else {
                        node.setSimpleProperty("tst:title", "t2"); // TESTSCHEMA
                    }
                    session.save();
                } finally {
                    session.close();
                }
            } finally {
                TransactionHelper.setTransactionRollbackOnly();
                TransactionHelper.commitOrRollbackTransaction();
            }
        }
    }

    public void TODOtestConcurrentUpdate() throws Exception {
        Session session1 = repository.getConnection();
        Node root1 = session1.getRootNode();
        Node node1 = session1.addChildNode(root1, "foo", null, "TestDoc", false);
        SimpleProperty title1 = node1.getSimpleProperty("tst:title");
        session1.save();

        Session session2 = repository.getConnection();
        Node root2 = session2.getRootNode();
        Node node2 = session2.getChildNode(root2, "foo", false);
        SimpleProperty title2 = node2.getSimpleProperty("tst:title");

        title1.setValue("mama");
        title2.setValue("glop");
        session1.save();
        assertEquals("mama", title1.getString());
        assertEquals("glop", title2.getString());
        session2.save(); // and notifies invalidations
        // in non-transaction mode, session1 has not processed its invalidations
        // yet, call save() to process them artificially
        session1.save();
        // session2 save wins
        assertEquals("glop", title1.getString());
        assertEquals("glop", title2.getString());
    }

    @Test
    public void testCrossSessionChildrenInvalidationAdd() throws Exception {
        // in first session, create base folder
        Session session1 = repository.getConnection();
        Node root1 = session1.getRootNode();
        Node folder1 = session1.addChildNode(root1, "foo", null, "TestDoc", false);
        session1.save();

        // in second session, retrieve folder and check children
        Session session2 = repository.getConnection();
        Node root2 = session2.getRootNode();
        Node folder2 = session2.getChildNode(root2, "foo", false);
        session2.getChildren(folder2, null, false);

        // in first session, add document
        session1.addChildNode(folder1, "gee", null, "TestDoc", false);
        session1.save();

        // in second session, try to get document
        session2.save(); // process invalidations (non-transactional)
        Node doc2 = session2.getChildNode(folder2, "gee", false);
        assertNotNull(doc2);
    }

    @Test
    public void testCrossSessionChildrenInvalidationRemove() throws Exception {
        // in first session, create base folder and doc
        Session session1 = repository.getConnection();
        Node root1 = session1.getRootNode();
        Node folder1 = session1.addChildNode(root1, "foo", null, "TestDoc", false);
        Node doc1 = session1.addChildNode(folder1, "gee", null, "TestDoc", false);
        session1.save();

        // in second session, retrieve folder and check children
        Session session2 = repository.getConnection();
        Node root2 = session2.getRootNode();
        Node folder2 = session2.getChildNode(root2, "foo", false);
        List<Node> children2 = session2.getChildren(folder2, null, false);
        assertEquals(1, children2.size());

        // in first session, remove child
        session1.removeNode(doc1);
        session1.save();

        // in second session, check no more children
        session2.save(); // process invalidations (non-transactional)
        children2 = session2.getChildren(folder2, null, false);
        assertEquals(0, children2.size());
        // and doc1 seen as removed
        assertNull(session2.getNodeById(doc1.getId()));
    }

    @Test
    public void testCrossSessionChildrenInvalidationMove() throws Exception {
        // in first session, create base folders and doc
        Session session1 = repository.getConnection();
        Node root1 = session1.getRootNode();
        Node foldera1 = session1.addChildNode(root1, "foo", null, "TestDoc", false);
        Node folderb1 = session1.addChildNode(root1, "bar", null, "TestDoc", false);
        Node doc1 = session1.addChildNode(foldera1, "gee", null, "TestDoc", false);
        session1.save();

        // in second session, retrieve folders and check children
        Session session2 = repository.getConnection();
        Node root2 = session2.getRootNode();
        Node foldera2 = session2.getChildNode(root2, "foo", false);
        List<Node> childrena2 = session2.getChildren(foldera2, null, false);
        assertEquals(1, childrena2.size());
        Node folderb2 = session2.getChildNode(root2, "bar", false);
        List<Node> childrenb2 = session2.getChildren(folderb2, null, false);
        assertEquals(0, childrenb2.size());

        // in first session, move between folders
        session1.move(doc1, folderb1, null);
        session1.save();

        // in second session, check children count
        session2.save(); // process invalidations (non-transactional)
        childrena2 = session2.getChildren(foldera2, null, false);
        assertEquals(0, childrena2.size());
        childrenb2 = session2.getChildren(folderb2, null, false);
        assertEquals(1, childrenb2.size());
    }

    @Test
    public void testCrossSessionChildrenInvalidationCopy() throws Exception {
        // in first session, create base folders and doc
        Session session1 = repository.getConnection();
        Node root1 = session1.getRootNode();
        Node foldera1 = session1.addChildNode(root1, "foo", null, "TestDoc", false);
        Node folderb1 = session1.addChildNode(root1, "bar", null, "TestDoc", false);
        Node doc1 = session1.addChildNode(foldera1, "gee", null, "TestDoc", false);
        session1.save();

        // in second session, retrieve folders and check children
        Session session2 = repository.getConnection();
        Node root2 = session2.getRootNode();
        Node foldera2 = session2.getChildNode(root2, "foo", false);
        List<Node> childrena2 = session2.getChildren(foldera2, null, false);
        assertEquals(1, childrena2.size());
        Node folderb2 = session2.getChildNode(root2, "bar", false);
        List<Node> childrenb2 = session2.getChildren(folderb2, null, false);
        assertEquals(0, childrenb2.size());

        // in first session, copy between folders
        session1.copy(doc1, folderb1, null);
        session1.save();

        // in second session, check children count
        session2.save(); // process invalidations (non-transactional)
        childrena2 = session2.getChildren(foldera2, null, false);
        assertEquals(1, childrena2.size());
        childrenb2 = session2.getChildren(folderb2, null, false);
        assertEquals(1, childrenb2.size());
    }

    @Test
    public void testCrossSessionVersionsInvalidationAdd() throws Exception {
        // in first session, create base folder
        Session session1 = repository.getConnection();
        Node root1 = session1.getRootNode();
        Node node1 = session1.addChildNode(root1, "foo", null, "TestDoc", false);
        Serializable id = node1.getId();
        session1.save();

        // in second session, list versions (empty)
        Session session2 = repository.getConnection();
        assertEquals(0, session2.getVersions(id).size());

        // in first session, create version
        session1.checkIn(node1, "v1", "comment");
        session1.save();
        assertEquals(1, session1.getVersions(id).size());

        // in second session, list versions
        session2.save(); // process invalidations (non-transactional)
        assertEquals(1, session2.getVersions(id).size());
    }

    @Test
    public void testCrossSessionVersionsInvalidationRemove() throws Exception {
        // in first session, create base folder and version
        Session session1 = repository.getConnection();
        Node root1 = session1.getRootNode();
        Node node1 = session1.addChildNode(root1, "foo", null, "TestDoc", false);
        Serializable id = node1.getId();
        Node ver1 = session1.checkIn(node1, "v1", "comment");

        // in second session, list versions (empty)
        Session session2 = repository.getConnection();
        assertEquals(1, session2.getVersions(id).size());

        // in first session, remove version
        session1.removeNode(ver1);
        session1.save();
        assertEquals(0, session1.getVersions(id).size());

        // in second session, list versions
        session2.save(); // process invalidations (non-transactional)
        assertEquals(0, session2.getVersions(id).size());
    }

    @Test
    public void testCrossSessionProxiesInvalidationAdd() throws Exception {
        assumeTrue(isProxiesEnabled());

        // in first session, create base stuff
        Session session1 = repository.getConnection();
        Node root1 = session1.getRootNode();
        Node node1 = session1.addChildNode(root1, "foo", null, "TestDoc", false);
        Serializable id = node1.getId();
        Node ver1 = session1.checkIn(node1, "v1", "comment");
        Serializable verId = ver1.getId();
        session1.save();

        // in second session, list proxies (empty)
        Session session2 = repository.getConnection();
        Node ver2 = session2.getNodeById(verId);
        assertEquals(0, session2.getProxies(ver2, null).size()); // by target

        // in first session, create proxy
        session1.addProxy(ver1.getId(), id, root1, "proxy", null);
        session1.save();
        assertEquals(1, session1.getProxies(ver1, null).size()); // by target

        // in second session, list proxies
        session2.save(); // process invalidations (non-transactional)
        assertEquals(1, session2.getProxies(ver1, null).size()); // by target
    }

    @Test
    public void testCrossSessionProxiesInvalidationRemove() throws Exception {
        assumeTrue(isProxiesEnabled());

        // in first session, create base stuff
        Session session1 = repository.getConnection();
        Node root1 = session1.getRootNode();
        Node folder1 = session1.addChildNode(root1, "fold", null, "TestDoc", false);
        Node node1 = session1.addChildNode(root1, "foo", null, "TestDoc", false);
        Serializable id = node1.getId();
        Node ver1 = session1.checkIn(node1, "v1", "comment");
        Serializable verId = ver1.getId();
        Node proxy1 = session1.addProxy(ver1.getId(), id, folder1, "proxy", null);
        session1.save();

        // in second session, list proxies
        Session session2 = repository.getConnection();
        Node ver2 = session2.getNodeById(verId);
        assertEquals(1, session2.getProxies(ver2, null).size()); // by target

        // in first session, remove proxy
        session1.removeNode(proxy1);
        session1.save();
        assertEquals(0, session1.getProxies(ver1, null).size()); // by target

        // in second session, list proxies
        session2.save(); // process invalidations (non-transactional)
        assertEquals(0, session2.getProxies(ver1, null).size()); // by target
    }

    @Test
    public void testCrossSessionACLInvalidation() throws Exception {
        // init repo and root ACL
        Session session1 = repository.getConnection();
        session1.close();

        // read roots (with ACL) in two sessions
        session1 = repository.getConnection();
        Session session2 = repository.getConnection();
        Node root1 = session1.getRootNode();
        Node root2 = session2.getRootNode();
        CollectionProperty prop1 = root1.getCollectionProperty(Model.ACL_PROP);
        CollectionProperty prop2 = root2.getCollectionProperty(Model.ACL_PROP);

        // change ACL in session 1
        ACLRow acl = new ACLRow(0, "test", true, "Read", null, "Members");
        prop1.setValue(new ACLRow[] { acl });
        session1.save();

        // process invalidations in session 2
        session2.save();

        // read invalidated ACL in session 2
        prop2.getValue();
    }

    /*
     * Make sure the fulltext job id is correctly invalidated (it belongs to the fulltext table that used to be
     * completely skipped by invalidations). Also check opaque column behavior.
     */
    // ignored because now in low-level tests fulltext is disabled
    // because workers don't have any high-level repository to
    // get a session from
    @Test
    @Ignore
    public void testFulltextJobIdInvalidation() throws Exception {
        Session session1 = repository.getConnection();
        Node root1 = session1.getRootNode();
        Node node1 = session1.addChildNode(root1, "foo", null, "TestDoc", false);
        SimpleProperty jobid1 = node1.getSimpleProperty("ecm:fulltextJobId");
        SimpleProperty ft1 = node1.getSimpleProperty("ecm:simpleText");
        session1.save();
        jobid1.setValue("123");
        ft1.setValue("foo");
        session1.save();

        Session session2 = repository.getConnection();
        Node node2 = session2.getNodeById(node1.getId());
        SimpleProperty jobid2 = node2.getSimpleProperty("ecm:fulltextJobId");
        SimpleProperty ft2 = node2.getSimpleProperty("ecm:simpleText");
        assertEquals("123", jobid2.getString());

        // update fulltext job id in session 1
        jobid1.setValue("456");
        session1.save();

        // check in session 2 that it's been invalidated
        session2.save(); // process invalidations (non-transactional)
        assertEquals("456", jobid2.getString());
        assertEquals("OPAQUE_VALUE", ft2.getValue().toString());
    }

    @Test
    // @ConditionalIgnoreRule.Ignore(condition = ConditionalIgnoreRule.IgnoreWindows.class, cause = "windows doesn't
    // have enough time granularity for such a test")
    public void testClustering() throws Exception {
        if (!DatabaseHelper.DATABASE.supportsClustering()) {
            System.out.println("Skipping clustering test for unsupported database: "
                    + DatabaseHelper.DATABASE.getClass().getName());
            return;
        }

        repository.close();
        // get two clustered repositories
        long DELAY = 500; // ms
        repository = newRepository(DELAY);
        RepositoryImpl repository2 = newRepository(DELAY);

        ClusterTestJob r1 = new ClusterTestJob(repository, repository2);
        ClusterTestJob r2 = new ClusterTestJob(repository, repository2);
        LockStepJob.run(r1, r2);

        clearAndClose(repository2);
    }

    protected static class ClusterTestJob extends LockStepJob {

        protected Repository repository1;

        protected Repository repository2;

        private static final long DELAY = 500; // ms

        public ClusterTestJob(Repository repository1, Repository repository2) {
            this.repository1 = repository1;
            this.repository2 = repository2;
        }

        @Override
        public void job() throws Exception {
            Session session1 = null;
            Session session2 = null;
            Node folder1 = null;
            Node folder2 = null;
            SimpleProperty title1 = null;
            SimpleProperty title2 = null;
            if (thread(1)) {
                // session1 creates root node and does a save
                // which resets invalidation timeout
                session1 = repository1.getConnection();
            }
            if (thread(2)) {
                session2 = repository2.getConnection();
                session2.save(); // save resets invalidations timeout
            }
            if (thread(1)) {
                // in session1, create base folder
                Node root1 = session1.getRootNode();
                folder1 = session1.addChildNode(root1, "foo", null, "TestDoc", false);
                title1 = folder1.getSimpleProperty("tst:title");
                session1.save();
            }
            if (thread(2)) {
                // in session2, retrieve folder and check children
                Node root2 = session2.getRootNode();
                folder2 = session2.getChildNode(root2, "foo", false);
                assertNotNull(folder2);
                title2 = folder2.getSimpleProperty("tst:title");
                session2.getChildren(folder2, null, false);
            }
            if (thread(1)) {
                // in session1, add document
                session1.addChildNode(folder1, "gee", null, "TestDoc", false);
                session1.save();
            }
            if (thread(2)) {
                // in session2, try to get document
                // immediate check, invalidation delay means not done yet
                session2.save();
                Node doc2 = session2.getChildNode(folder2, "gee", false);
                // assertNull(doc2); // could fail if machine very slow
                Thread.sleep(DELAY + 1); // wait invalidation delay
                session2.save(); // process invalidations
                                 // (non-transactional)
                doc2 = session2.getChildNode(folder2, "gee", false);
                assertNotNull(doc2);
            }
            if (thread(1)) {
                // in session1 change title
                title1.setValue("yo");
            }
            if (thread(2)) {
                assertNull(title2.getString());
            }
            if (thread(1)) {
                // save session1 (queues its invalidations to others)
                session1.save();
            }
            if (thread(2)) {
                // session2 has not saved (committed) yet, so still
                // unmodified
                assertNull(title2.getString());
                // immediate check, invalidation delay means not done yet
                session2.save();
                // assertNull(title2.getString()); // could fail if machine
                // very
                // slow
                Thread.sleep(DELAY + 1); // wait invalidation delay
                session2.save();
                // after commit, invalidations have been processed
                assertEquals("yo", title2.getString());
            }
            if (thread(1)) {
                // written properties aren't shared
                title1.setValue("mama");
            }
            if (thread(2)) {
                title2.setValue("glop");
            }
            if (thread(1)) {
                session1.save();
                assertEquals("mama", title1.getString());
            }
            if (thread(2)) {
                assertEquals("glop", title2.getString());
                session2.save(); // and notifies invalidations
            }
            if (thread(1)) {
                // in non-transaction mode, session1 has not processed
                // its invalidations yet, call save() to process them
                // artificially
                Thread.sleep(DELAY + 1); // wait invalidation delay
                session1.save();
                // session2 save wins
                assertEquals("glop", title1.getString());
            }
            if (thread(2)) {
                assertEquals("glop", title2.getString());
            }
        }

    }

    @Test
    public void testRollback() throws Exception {
        if (!DatabaseHelper.DATABASE.supportsXA()) {
            return;
        }
        XAResource mockRes = new XAResource() {

            @Override
            public void start(Xid xid, int flags) throws XAException {

            }

            @Override
            public boolean setTransactionTimeout(int seconds) throws XAException {
                return false;
            }

            @Override
            public void rollback(Xid xid) throws XAException {
                ;
            }

            @Override
            public Xid[] recover(int flag) throws XAException {
                throw new UnsupportedOperationException();
            }

            @Override
            public int prepare(Xid xid) throws XAException {
                throw new XAException(XAException.XA_RBOTHER);
            }

            @Override
            public boolean isSameRM(XAResource xares) throws XAException {
                throw new UnsupportedOperationException();
            }

            @Override
            public int getTransactionTimeout() throws XAException {
                return 0;
            }

            @Override
            public void forget(Xid xid) throws XAException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void end(Xid xid, int flags) throws XAException {
                throw new XAException(XAException.XA_RBOTHER);
            }

            @Override
            public void commit(Xid xid, boolean onePhase) throws XAException {
                throw new UnsupportedOperationException();
            }
        };

        Session session = repository.getConnection();
        XAResource xaresource = ((SessionImpl) session).getXAResource();
        Node root = session.getRootNode();
        Node nodea = session.addChildNode(root, "foo", null, "TestDoc", false);
        nodea.setSimpleProperty("tst:title", "old");
        assertEquals("old", nodea.getSimpleProperty("tst:title").getString());
        session.save();

        /*
         * rollback before save (underlying XAResource saw no updates)
         */
        TransactionHelper.startTransaction();
        try {
            Transaction tx = TransactionHelper.lookupTransactionManager().getTransaction();
            tx.enlistResource(mockRes);
            tx.enlistResource(xaresource);
            nodea = session.getNodeByPath("/foo", null);
            nodea.setSimpleProperty("tst:title", "new");
        } finally {
            try {
                TransactionHelper.commitOrRollbackTransaction();
                throw new AssertionError("should rollback");
            } catch (TransactionRuntimeException cause) {
                assertEquals("Unable to commit: transaction marked for rollback", cause.getMessage());
            }
            nodea = session.getNodeByPath("/foo", null);
            assertEquals("old", nodea.getSimpleProperty("tst:title").getString());
        }

        /*
         * rollback after save (underlying XAResource does a rollback too)
         */
        TransactionHelper.startTransaction();
        try {
            Transaction tx = TransactionHelper.lookupTransactionManager().getTransaction();
            tx.enlistResource(mockRes);
            tx.enlistResource(xaresource);
            nodea = session.getNodeByPath("/foo", null);
            nodea.setSimpleProperty("tst:title", "new");
            session.save();
            TransactionHelper.setTransactionRollbackOnly();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            nodea = session.getNodeByPath("/foo", null);
            assertEquals("old", nodea.getSimpleProperty("tst:title").getString());
        }

    }

    @Test
    public void testSaveOnCommit() throws Exception {
        if (!DatabaseHelper.DATABASE.supportsXA()) {
            return;
        }

        Session session = repository.getConnection(); // init
        session.save();

        XAResource xaresource = ((SessionImpl) session).getXAResource();

        // first transaction
        TransactionHelper.startTransaction();
        try {
            TransactionHelper.lookupTransactionManager().getTransaction().enlistResource(xaresource);
            Node root = session.getRootNode();
            assertNotNull(root);
            session.addChildNode(root, "foo", null, "TestDoc", false);
            // let end do an implicit save
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }

        // should have saved, clearing caches should be harmless
        ((SessionImpl) session).clearCaches();

        // second transaction
        TransactionHelper.startTransaction();
        try {
            TransactionHelper.lookupTransactionManager().getTransaction().enlistResource(xaresource);
            Node foo = session.getNodeByPath("/foo", null);
            assertNotNull(foo);
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

    protected List<String> getNames(List<Node> nodes) {
        List<String> names = new ArrayList<>(nodes.size());
        for (Node node : nodes) {
            names.add(node.getName());
        }
        return names;
    }

    @Test
    public void testOrdered() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node fold = session.addChildNode(root, "fold", null, "OFolder", false);
        Node doca = session.addChildNode(fold, "a", null, "TestDoc", false);
        Node docb = session.addChildNode(fold, "b", null, "TestDoc", false);
        Node docc = session.addChildNode(fold, "c", null, "TestDoc", false);
        Node docd = session.addChildNode(fold, "d", null, "TestDoc", false);
        Node doce = session.addChildNode(fold, "e", null, "TestDoc", false);
        session.save();
        // check order
        List<Node> children = session.getChildren(fold, null, false);
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), getNames(children));

        // reorder self
        session.orderBefore(fold, docb, docb);
        children = session.getChildren(fold, null, false);
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), getNames(children));
        // reorder up
        session.orderBefore(fold, docd, docb);
        children = session.getChildren(fold, null, false);
        assertEquals(Arrays.asList("a", "d", "b", "c", "e"), getNames(children));
        // reorder first
        session.orderBefore(fold, docc, doca);
        children = session.getChildren(fold, null, false);
        assertEquals(Arrays.asList("c", "a", "d", "b", "e"), getNames(children));
        // reorder last
        session.orderBefore(fold, docd, null);
        children = session.getChildren(fold, null, false);
        assertEquals(Arrays.asList("c", "a", "b", "e", "d"), getNames(children));
        // reorder down
        session.orderBefore(fold, doca, docd);
        children = session.getChildren(fold, null, false);
        assertEquals(Arrays.asList("c", "b", "e", "a", "d"), getNames(children));
        // reorder out of range
        session.orderBefore(fold, doca, doce);
        children = session.getChildren(fold, null, false);
        assertEquals(Arrays.asList("c", "b", "a", "e", "d"), getNames(children));
    }

    @Test
    public void testMove() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node foldera = session.addChildNode(root, "folder_a", null, "TestDoc", false);
        Serializable prevId = foldera.getId();
        Node nodea = session.addChildNode(foldera, "node_a", null, "TestDoc", false);
        Node nodeac = session.addChildNode(nodea, "node_a_complex", null, "TestDoc", true);
        assertEquals("/folder_a/node_a/node_a_complex", session.getPath(nodeac));
        Node folderb = session.addChildNode(root, "folder_b", null, "TestDoc", false);
        session.addChildNode(folderb, "node_b", null, "TestDoc", false);
        session.save();

        // cannot move under itself
        try {
            session.move(foldera, nodea, "abc");
            fail();
        } catch (NuxeoException e) {
            String msg = e.getMessage();
            assertTrue(msg, msg.contains("Cannot move a node under itself"));
        }

        // cannot move to name that already exists
        try {
            session.move(foldera, folderb, "node_b");
            fail();
        } catch (NuxeoException e) {
            String msg = e.getMessage();
            assertTrue(msg, msg.contains("Destination name already exists"));
        }

        // do normal move
        Node node = session.move(foldera, folderb, "yo");
        assertEquals(prevId, node.getId());
        assertEquals("yo", node.getName());
        assertEquals("/folder_b/yo", session.getPath(node));
        assertEquals("/folder_b/yo/node_a/node_a_complex", session.getPath(nodeac));

        // move higher is allowed
        node = session.move(node, root, "underr");
        assertEquals(prevId, node.getId());
        assertEquals("underr", node.getName());
        assertEquals("/underr", session.getPath(node));
        assertEquals("/underr/node_a/node_a_complex", session.getPath(nodeac));

        session.save();
    }

    /*
     * Test that lots of moves don't break internal datastructures.
     */
    @Test
    public void testMoveMany() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        ArrayList<Node> nodes = new ArrayList<>();
        nodes.add(root);
        Random rnd = new Random(123456);
        List<String[]> graph = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            // create a node under a random node
            Node parent = nodes.get((int) Math.floor(rnd.nextFloat() * nodes.size()));
            Node child = session.addChildNode(parent, "child" + i, null, "TestDoc", false);
            nodes.add(child);
            // update graph
            addEdge(graph, parent.getId().toString(), child.getId().toString());
            if (i % 5 == 0) {
                // move a random node under a random parent
                int ip, ic;
                Node p, c;
                String pid, cid;
                do {
                    ip = (int) Math.floor(rnd.nextFloat() * nodes.size());
                    ic = (int) Math.floor(rnd.nextFloat() * nodes.size());
                    p = nodes.get(ip);
                    c = nodes.get(ic);
                    pid = p.getId().toString();
                    cid = c.getId().toString();
                    if (isUnder(graph, cid, pid)) {
                        // check we have an error for this move
                        try {
                            session.move(c, p, c.getName());
                            fail("shouldn't be able to move");
                        } catch (Exception e) {
                            // ok
                        }
                        ic = 0; // try again
                    }
                } while (ic == 0 || ip == ic);
                String oldpid = c.getParentId().toString();
                session.move(c, p, c.getName());
                removeEdge(graph, oldpid, cid);
                addEdge(graph, pid, cid);
            }
        }
        session.save();

        // dumpGraph(graph);
        // dumpDescendants(buildDescendants(graph, root.getId().toString()));
    }

    private static void addEdge(List<String[]> graph, String p, String c) {
        graph.add(new String[] { p, c });
    }

    private static void removeEdge(List<String[]> graph, String p, String c) {
        for (String[] edge : graph) {
            if (edge[0].equals(p) && edge[1].equals(c)) {
                graph.remove(edge);
                return;
            }
        }
        throw new IllegalArgumentException(String.format("No edge %s -> %s", p, c));
    }

    private static boolean isUnder(List<String[]> graph, String p, String c) {
        if (p.equals(c)) {
            return true;
        }
        Set<String> under = new HashSet<>();
        under.add(p);
        int oldSize = 0;
        // inefficient algorithm but for tests it's ok
        while (under.size() != oldSize) {
            oldSize = under.size();
            Set<String> add = new HashSet<>();
            for (String n : under) {
                for (String[] edge : graph) {
                    if (edge[0].equals(n)) {
                        String cc = edge[1];
                        if (c.equals(cc)) {
                            return true;
                        }
                        add.add(cc);
                    }
                }
            }
            under.addAll(add);
        }
        return false;
    }

    @Test
    public void testCopy() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node foldera = session.addChildNode(root, "folder_a", null, "TestDoc", false);
        Serializable prevFolderaId = foldera.getId();
        Node nodea = session.addChildNode(foldera, "node_a", null, "TestDoc", false);
        Serializable prevNodeaId = nodea.getId();
        Node nodeac = session.addChildNode(nodea, "node_a_complex", null, "TestDoc", true);
        Serializable prevNodeacId = nodeac.getId();
        nodea.setSimpleProperty("tst:title", "hello world");
        nodea.setCollectionProperty("tst:subjects", new String[] { "a", "b", "c" });
        nodea.setSimpleProperty("ecm:lifeCycleState", "foostate"); // misc table
        assertEquals("/folder_a/node_a/node_a_complex", session.getPath(nodeac));
        Node folderb = session.addChildNode(root, "folder_b", null, "TestDoc", false);
        session.addChildNode(folderb, "node_b", null, "TestDoc", false);
        Node folderc = session.addChildNode(root, "folder_c", null, "TestDoc", false);
        session.save();

        // cannot copy under itself
        try {
            session.copy(foldera, nodea, "abc");
            fail();
        } catch (NuxeoException e) {
            String msg = e.getMessage();
            assertTrue(msg, msg.contains("Cannot copy a node under itself"));
        }

        // cannot copy to name that already exists
        try {
            session.copy(foldera, folderb, "node_b");
            fail();
        } catch (NuxeoException e) {
            String msg = e.getMessage();
            assertTrue(msg, msg.contains("Destination name already exists"));
        }

        // do normal copy
        Node foldera2 = session.copy(foldera, folderb, "yo");
        // one children was known (complete), check it was invalidated
        Node n = session.getChildNode(folderb, "yo", false);
        assertNotNull(n);
        assertEquals(foldera2.getId(), n.getId());
        assertNotSame(prevFolderaId, foldera2.getId());
        assertEquals("yo", foldera2.getName());
        assertEquals("/folder_b/yo", session.getPath(foldera2));
        Node nodea2 = session.getChildNode(foldera2, "node_a", false);
        assertNotSame(prevNodeaId, nodea2.getId());
        assertEquals("hello world", nodea2.getSimpleProperty("tst:title").getString());
        assertEquals("foostate", nodea2.getSimpleProperty("ecm:lifeCycleState").getString());
        // check that the collection copy is different from the original
        String[] subjectsa2 = nodea2.getCollectionProperty("tst:subjects").getStrings();
        nodea.setCollectionProperty("tst:subjects", new String[] { "foo" });
        String[] subjectsa = nodea.getCollectionProperty("tst:subjects").getStrings();
        assertEquals(Arrays.asList("foo"), Arrays.asList(subjectsa));
        assertEquals(Arrays.asList("a", "b", "c"), Arrays.asList(subjectsa2));
        // complex children are there too
        Node nodeac2 = session.getChildNode(nodea2, "node_a_complex", true);
        assertNotNull(nodeac2);
        assertNotSame(prevNodeacId, nodeac2.getId());

        // copy to a folder that we know has no children
        // checks proper Children invalidation
        session.copy(nodea, folderc, "hm");
        Node nodea3 = session.getChildNode(folderc, "hm", false);
        assertNotNull(nodea3);

        session.save();
    }

    @Test
    public void testCopyOrdered() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node fold = session.addChildNode(root, "fold", null, "OFolder", false);
        Node doca = session.addChildNode(fold, "a", null, "TestDoc", false);
        Node docb = session.addChildNode(fold, "b", null, "TestDoc", false);
        session.save();
        // check order
        List<Node> children = session.getChildren(fold, null, false);
        assertEquals(Arrays.asList("a", "b"), getNames(children));

        // copy a as c, should be positioned last
        session.copy(doca, fold, "c");
        session.copy(docb, fold, "d");
        children = session.getChildren(fold, null, false);
        assertEquals(Arrays.asList("a", "b", "c", "d"), getNames(children));
    }

    @Test
    public void testVersioning() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node foldera = session.addChildNode(root, "folder_a", null, "TestDoc", false);
        Node nodea = session.addChildNode(foldera, "node_a", null, "TestDoc", false);
        Node nodeac = session.addChildNode(nodea, "node_a_complex", null, "TestDoc", true);
        nodea.setSimpleProperty("tst:title", "hello world");
        nodea.setCollectionProperty("tst:subjects", new String[] { "a", "b", "c" });
        // nodea.setSingleProperty("ecm:majorVersion", Long.valueOf(1));
        // nodea.setSingleProperty("ecm:minorVersion", Long.valueOf(0));
        session.save();
        Serializable nodeacId = nodeac.getId();

        /*
         * Check in.
         */
        Node version = session.checkIn(nodea, "foolab", "bardesc");
        assertNotNull(version);
        assertNotSame(version.getId(), nodea.getId());
        // doc is now checked in
        assertEquals(Boolean.TRUE, nodea.getSimpleProperty("ecm:isCheckedIn").getValue());
        assertEquals(version.getId(), nodea.getSimpleProperty("ecm:baseVersion").getValue());
        // the version info
        assertEquals("node_a", version.getName()); // keeps name
        assertNull(session.getParentNode(version));
        assertEquals("hello world", version.getSimpleProperty("tst:title").getString());
        assertNull(version.getSimpleProperty("ecm:baseVersion").getValue());
        assertNull(version.getSimpleProperty("ecm:isCheckedIn").getValue());
        assertEquals(nodea.getId(), version.getSimpleProperty("ecm:versionableId").getValue());
        // assertEquals(Long.valueOf(1), version.getSimpleProperty(
        // "ecm:majorVersion").getLong());
        // assertEquals(Long.valueOf(0), version.getSimpleProperty(
        // "ecm:minorVersion").getLong());
        assertNotNull(version.getSimpleProperty("ecm:versionCreated").getValue());
        assertEquals("foolab", version.getSimpleProperty("ecm:versionLabel").getValue());
        assertEquals("bardesc", version.getSimpleProperty("ecm:versionDescription").getValue());
        // the version child (complex prop)
        Node nodeacv = session.getChildNode(version, "node_a_complex", true);
        assertNotNull(nodeacv);
        assertNotSame(nodeacId, nodeacv.getId());

        /*
         * Check out.
         */
        session.checkOut(nodea);
        assertEquals(Boolean.FALSE, nodea.getSimpleProperty("ecm:isCheckedIn").getValue());
        assertEquals(version.getId(), nodea.getSimpleProperty("ecm:baseVersion").getValue());
        nodea.setSimpleProperty("tst:title", "blorp");
        nodea.setCollectionProperty("tst:subjects", new String[] { "x", "y" });
        Node nodeac2 = session.getChildNode(nodea, "node_a_complex", true);
        nodeac2.setSimpleProperty("tst:title", "comp");
        session.save();

        /*
         * Restore.
         */
        session.restore(nodea, version);
        assertEquals("hello world", nodea.getSimpleProperty("tst:title").getString());
        assertEquals(Arrays.asList("a", "b", "c"),
                Arrays.asList(nodea.getCollectionProperty("tst:subjects").getStrings()));
        Node nodeac3 = session.getChildNode(nodea, "node_a_complex", true);
        assertNotNull(nodeac3);
        SimpleProperty sp = nodeac3.getSimpleProperty("tst:title");
        assertNotNull(sp);
        assertNull(sp.getString());

        /*
         * Test checkout + checkin after restore.
         */
        session.checkOut(nodea);
        session.checkIn(nodea, "hop", null);
    }

    // NXP-12912: disabled because failing randomly
    @Ignore
    @Test
    public void testVersionFetching() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node folder = session.addChildNode(root, "folder", null, "TestDoc", false);
        Node node = session.addChildNode(folder, "node", null, "TestDoc", false);
        session.save();

        // create two versions
        Node ver1 = session.checkIn(node, "foolab1", "desc1");
        session.checkOut(node);
        Node ver2 = session.checkIn(node, "foolab2", "desc2");

        // get list
        List<Node> list = session.getVersions(node.getId());
        assertEquals(2, list.size());
        assertEquals(ver1.getId(), list.get(0).getId());
        assertEquals(ver2.getId(), list.get(1).getId());
        // get by label
        Node v = session.getVersionByLabel(node.getId(), "foolab1");
        assertEquals(ver1.getId(), v.getId());
        // get last
        v = session.getLastVersion(node.getId());
        assertEquals(ver2.getId(), v.getId());

        // remove version
        session.removeNode(ver1);

        // get list
        list = session.getVersions(node.getId());
        assertEquals(1, list.size());
        assertEquals(ver2.getId(), list.get(0).getId());

        // copy version
        // session.copy(ver1, null, "bar"); not possible right now
    }

    @Test
    public void testVersionCopy() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node foldera = session.addChildNode(root, "foldera", null, "TestDoc", false);
        Node nodea = session.addChildNode(foldera, "nodea", null, "TestDoc", false);
        Node ver = session.checkIn(nodea, "1", "ver 1");
        session.save();

        // copy checked in doc
        assertEquals(Boolean.TRUE, nodea.getSimpleProperty("ecm:isCheckedIn").getValue());
        assertNotNull(nodea.getSimpleProperty("ecm:baseVersion").getValue());
        Node nodeb = session.copy(nodea, root, "nodeb");
        assertNull(nodeb.getSimpleProperty("ecm:isCheckedIn").getValue());
        assertNull(nodeb.getSimpleProperty("ecm:baseVersion").getValue());

        // copy folder including checked in doc
        Node folderb = session.copy(foldera, root, "folderb");
        Node nodec = session.getChildNode(folderb, "nodea", false);
        assertNull(nodec.getSimpleProperty("ecm:isCheckedIn").getValue());

        // copy version as new doc
        assertTrue(ver.isVersion());
        assertEquals("1", ver.getSimpleProperty("ecm:versionLabel").getValue());
        Node vercop = session.copy(ver, root, "vercop");
        assertFalse(vercop.isVersion());
        assertNull(vercop.getSimpleProperty("ecm:versionLabel").getValue());
        assertNull(vercop.getSimpleProperty("ecm:isCheckedIn").getValue());
    }

    @Test
    public void testProxies() throws Exception {
        assumeTrue(isProxiesEnabled());

        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node foldera = session.addChildNode(root, "foldera", null, "TestDoc", false);
        Node nodea = session.addChildNode(foldera, "nodea", null, "TestDoc", false);
        Node folderb = session.addChildNode(root, "folderb", null, "TestDoc", false);

        /*
         * Check in.
         */
        Node version = session.checkIn(nodea, "v1", "");
        assertNotNull(version);
        session.checkOut(nodea);
        Node version2 = session.checkIn(nodea, "v2", "");
        /*
         * Make proxy (by hand).
         */
        Node proxy = session.addProxy(version.getId(), nodea.getId(), folderb, "proxy1", null);
        session.save();
        assertNotSame(version.getId(), proxy.getId());
        assertNotSame(nodea.getId(), proxy.getId());
        assertEquals("/folderb/proxy1", session.getPath(proxy));
        assertEquals(folderb.getId(), session.getParentNode(proxy).getId());
        /*
         * Searches.
         */
        // from versionable
        List<Node> proxies = session.getProxies(nodea, null);
        assertEquals(1, proxies.size());
        assertEquals(proxy, proxies.get(0));
        proxies = session.getProxies(nodea, folderb);
        assertEquals(1, proxies.size());
        proxies = session.getProxies(nodea, foldera);
        assertEquals(0, proxies.size());
        // from version
        proxies = session.getProxies(version, null);
        assertEquals(1, proxies.size());
        assertEquals(proxy, proxies.get(0));
        proxies = session.getProxies(version, folderb);
        assertEquals(1, proxies.size());
        proxies = session.getProxies(version, foldera);
        assertEquals(0, proxies.size());
        // from other version (which has no proxy)
        proxies = session.getProxies(version2, null);
        assertEquals(0, proxies.size());
        // from proxy
        proxies = session.getProxies(proxy, null);
        assertEquals(1, proxies.size());
        assertEquals(proxy, proxies.get(0));
        proxies = session.getProxies(proxy, folderb);
        assertEquals(1, proxies.size());
        proxies = session.getProxies(proxy, foldera);
        assertEquals(0, proxies.size());
    }

    @Test
    public void testProxyFetching() throws Exception {
        assumeTrue(isProxiesEnabled());

        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node folder1 = session.addChildNode(root, "folder1", null, "TestDoc", false);
        Node folder2 = session.addChildNode(root, "folder2", null, "TestDoc", false);
        Node node = session.addChildNode(folder1, "node", null, "TestDoc", false);
        session.save();

        // create two versions
        Node ver1 = session.checkIn(node, "foolab1", "desc1");
        session.checkOut(node);
        Node ver2 = session.checkIn(node, "foolab2", "desc2");

        // make proxies
        Node proxy1a = session.addProxy(ver1.getId(), node.getId(), folder1, "proxy1a", null);
        Node proxy1b = session.addProxy(ver1.getId(), node.getId(), folder2, "proxy1b", null);
        Node proxy2 = session.addProxy(ver2.getId(), node.getId(), folder1, "proxy2", null);

        // get by versionable id
        List<Node> list = session.getProxies(node, null);
        assertSameSet(list, proxy1a, proxy1b, proxy2);
        // get by proxy (same versionable id)
        list = session.getProxies(proxy1a, null);
        assertSameSet(list, proxy1a, proxy1b, proxy2);
        // get by target id
        list = session.getProxies(ver1, null);
        assertSameSet(list, proxy1a, proxy1b);
        list = session.getProxies(ver2, null);
        assertSameSet(list, proxy2);
        // get by versionable id and parent
        list = session.getProxies(node, folder2);
        assertSameSet(list, proxy1b);

        // remove proxy1a
        session.removeNode(proxy1a);
        list = session.getProxies(ver1, null);
        assertSameSet(list, proxy1b);
        list = session.getProxies(ver2, null);
        assertSameSet(list, proxy2);
        list = session.getProxies(node, null);
        assertSameSet(list, proxy1b, proxy2);

        // retarget proxy2 to ver1
        session.setProxyTarget(proxy2, ver1.getId());
        list = session.getProxies(ver1, null);
        assertSameSet(list, proxy1b, proxy2);
        list = session.getProxies(ver2, null);
        assertEquals(0, list.size());

        // copy proxy1b through its container folder2
        session.copy(folder2, root, "folder3");
        // don't fetch proxy3 yet
        list = session.getProxies(node, null);
        assertEquals(3, list.size()); // selection properly updated
    }

    @Test
    public void testProxyDeepRemoval() throws Exception {
        assumeTrue(isProxiesEnabled());

        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node folder1 = session.addChildNode(root, "folder1", null, "TestDoc", false);
        Node folder2 = session.addChildNode(root, "folder2", null, "TestDoc", false);
        Node folder3 = session.addChildNode(root, "folder3", null, "TestDoc", false);
        // create node in folder1
        Node node = session.addChildNode(folder1, "node", null, "TestDoc", false);
        // create version
        Node ver = session.checkIn(node, "foolab1", "desc1");
        // create proxy2 in folder2
        session.addProxy(ver.getId(), node.getId(), folder2, "proxy2", null);
        // create proxy3 in folder3
        Node proxy3 = session.addProxy(ver.getId(), node.getId(), folder3, "proxy3", null);

        List<Node> list;
        list = session.getProxies(ver, null); // by target
        assertEquals(2, list.size());
        list = session.getProxies(node, null); // by series
        assertEquals(2, list.size());

        // remove proxy through container folder2
        session.removeNode(folder2);

        // only proxy3 left
        list = session.getProxies(ver, null); // by target
        assertEquals(1, list.size());
        list = session.getProxies(node, null); // by series
        assertEquals(1, list.size());

        // remove target is forbidden while proxy still exists
        try {
            session.removeNode(ver);
        } catch (DocumentExistsException e) {
            String msg = e.getMessage();
            assertTrue(msg, msg.contains("is the target of proxy"));
        }

        // remove last proxy
        session.removeNode(proxy3);

        // check selections are correct
        list = session.getProxies(ver, null); // by target
        assertEquals(0, list.size());
    }

    @Test
    public void testProxyDeepCopy() throws Exception {
        assumeTrue(isProxiesEnabled());

        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node folder1 = session.addChildNode(root, "folder1", null, "TestDoc", false);
        Node folder2 = session.addChildNode(root, "folder2", null, "TestDoc", false);
        // create node in folder1
        Node node = session.addChildNode(folder1, "node", null, "TestDoc", false);
        // create version
        Node ver = session.checkIn(node, "foolab1", "desc1");
        // create proxy in folder2
        session.addProxy(ver.getId(), node.getId(), folder2, "proxy2", null);

        List<Node> list;
        list = session.getProxies(ver, null); // by target
        assertEquals(1, list.size());
        list = session.getProxies(node, null); // by series
        assertEquals(1, list.size());

        // copy folder2 to folder3
        session.copy(folder2, root, "fodler3");
        // one more proxy
        list = session.getProxies(ver, null); // by target
        assertEquals(2, list.size());
        list = session.getProxies(node, null); // by series
        assertEquals(2, list.size());
    }

    @Test
    public void testProxySchemas() throws Exception {
        doTestProxySchemas(false);
    }

    @Test
    // deploy another contrib where TestDoc4 also has the proxy schema
    @Deploy("org.nuxeo.ecm.core.storage.sql.test.tests:OSGI-INF/test-backend-core-types-contrib-2.xml")
    public void testProxySchemasShadowing() throws Exception {
        doTestProxySchemas(true);
    }

    protected void doTestProxySchemas(boolean shadow) throws Exception {
        assumeTrue(isProxiesEnabled());

        String type;
        if (shadow) {
            type = "TestDoc4";
        } else {
            type = "TestDoc2";
        }

        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node folder = session.addChildNode(root, "folder", null, "TestDoc3", false);
        Node doc = session.addChildNode(root, "file", null, type, false);
        Node version = session.checkIn(doc, "v1", "");
        Node proxy = session.addProxy(version.getId(), doc.getId(), folder, "proxy", null);
        session.save();
        try {
            doc.setSimpleProperty("info:info", "docinfo");
            session.save();
        } catch (PropertyNotFoundException e) {
            if (shadow) {
                // base doc should have the property
                throw e;
            } else {
                assertTrue(e.getMessage().contains("info:info"));
            }
        }

        assertNull(proxy.getSimpleProperty("info:info").getString());
        proxy.setSimpleProperty("info:info", "proxyinfo");
        session.save();
        session.close();

        // new session
        session = repository.getConnection();
        folder = session.getNodeById(folder.getId());
        proxy = session.getNodeById(proxy.getId());
        assertEquals("proxyinfo", proxy.getSimpleProperty("info:info").getString());

        // test a query
        String nxql;
        PartialList<Serializable> plist;
        nxql = "SELECT * FROM Document WHERE info:info = 'proxyinfo' AND ecm:isProxy = 1";
        plist = session.query(nxql, QueryFilter.EMPTY, false);
        assertEquals(1, plist.size());

        nxql = "SELECT * FROM Document WHERE info:info = 'proxyinfo'";
        plist = session.query(nxql, QueryFilter.EMPTY, false);
        assertEquals(1, plist.size());

        nxql = "SELECT * FROM Document WHERE info:info = 'proxyinfo' AND ecm:isProxy = 0";
        plist = session.query(nxql, QueryFilter.EMPTY, false);
        assertEquals(0, plist.size());

        // queryAndFetch
        IterableQueryResult it;
        nxql = "SELECT ecm:uuid, info:info FROM Document WHERE info:info IS NOT NULL";
        it = session.queryAndFetch(nxql, "NXQL", QueryFilter.EMPTY);
        Map<Serializable, String> expected = new HashMap<>();
        if (shadow) {
            expected.put(String.valueOf(doc.getId()), "docinfo");
        }
        expected.put(String.valueOf(proxy.getId()), "proxyinfo");
        Map<Serializable, String> actual = new HashMap<>();
        for (Map<String, Serializable> map : it) {
            Serializable uuid = map.get("ecm:uuid");
            String info = (String) map.get("info:info");
            actual.put(uuid, info);
        }
        it.close();
        assertEquals(actual, expected);

        // test that the copy has the extra schema values
        Node folderCopy = session.copy(folder, session.getRootNode(), "folderCopy");
        Node proxyCopy = session.getChildNode(folderCopy, "proxy", false);
        assertTrue(proxyCopy.isProxy());
        assertEquals("proxyinfo", proxyCopy.getSimpleProperty("info:info").getString());
    }

    @Test
    public void testProxyQueryStartsWith() throws Exception {
        assumeTrue(isProxiesEnabled());

        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node folder1 = session.addChildNode(root, "folder1", null, "TestDoc", false);
        Node folder2 = session.addChildNode(root, "folder2", null, "TestDoc", false);
        // create node in folder1
        Node node = session.addChildNode(folder1, "node", null, "TestDoc", false);
        // create proxy in folder2
        Node ver = session.checkIn(node, "foolab1", "desc1");
        session.addProxy(ver.getId(), node.getId(), folder2, "proxy2", null);
        session.save();

        // search for it
        PartialList<Serializable> res = session.query("SELECT * FROM TestDoc WHERE ecm:path STARTSWITH '/folder2'",
                QueryFilter.EMPTY, false);
        assertEquals(1, res.size());
    }

    @Test
    public void testProxyQueryProxyProp() throws Exception {
        assumeTrue(isProxiesEnabled());

        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node folder1 = session.addChildNode(root, "folder1", null, "TestDoc", false);
        Node folder2 = session.addChildNode(root, "folder2", null, "TestDoc", false);
        // create node in folder1
        Node node = session.addChildNode(folder1, "node", null, "TestDoc", false);
        // create proxy in folder2
        Node ver = session.checkIn(node, "foolab1", "desc1");
        Node proxy = session.addProxy(ver.getId(), node.getId(), folder2, "proxy2", null);
        session.save();

        String sql;
        PartialList<Serializable> res;

        // query without proxies (no result)
        sql = String.format("SELECT * FROM TestDoc WHERE ecm:proxyTargetId = '%s' AND ecm:isProxy = 0", ver.getId());
        res = session.query(sql, QueryFilter.EMPTY, false);
        assertEquals(0, res.size());
        sql = String.format("SELECT * FROM TestDoc WHERE ecm:proxyVersionableId = '%s' AND ecm:isProxy = 0",
                node.getId());
        res = session.query(sql, QueryFilter.EMPTY, false);
        assertEquals(0, res.size());

        // query just proxies
        sql = String.format("SELECT * FROM TestDoc WHERE ecm:proxyTargetId = '%s' AND ecm:isProxy = 1", ver.getId());
        res = session.query(sql, QueryFilter.EMPTY, false);
        assertEquals(1, res.size());
        assertEquals(proxy.getId(), res.get(0));
        sql = String.format("SELECT * FROM TestDoc WHERE ecm:proxyVersionableId = '%s' AND ecm:isProxy = 1",
                node.getId());
        res = session.query(sql, QueryFilter.EMPTY, false);
        assertEquals(1, res.size());
        assertEquals(proxy.getId(), res.get(0));

        // query all
        sql = String.format("SELECT * FROM TestDoc WHERE ecm:proxyTargetId = '%s'", ver.getId());
        res = session.query(sql, QueryFilter.EMPTY, false);
        assertEquals(1, res.size());
        assertEquals(proxy.getId(), res.get(0));
        sql = String.format("SELECT * FROM TestDoc WHERE ecm:proxyVersionableId = '%s'", node.getId());
        res = session.query(sql, QueryFilter.EMPTY, false);
        assertEquals(1, res.size());
        assertEquals(proxy.getId(), res.get(0));
    }

    private static void assertSameSet(Collection<Node> actual, Node... expected) {
        assertEquals(idSet(Arrays.asList(expected)), idSet(actual));
    }

    private static Set<Serializable> idSet(Collection<Node> nodes) {
        Set<Serializable> set = new HashSet<>();
        for (Node node : nodes) {
            set.add(node.getId());
        }
        return set;
    }

    @Test
    public void testDelete() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node nodea = session.addChildNode(root, "foo", null, "TestDoc", false);
        Serializable ida = nodea.getId();
        nodea.setSimpleProperty("tst:title", "foo");
        Node nodeb = session.addChildNode(nodea, "bar", null, "TestDoc", false);
        Serializable idb = nodeb.getId();
        nodeb.setSimpleProperty("tst:title", "bar");
        Node nodec = session.addChildNode(nodeb, "gee", null, "TestDoc", false);
        Serializable idc = nodec.getId();
        nodec.setSimpleProperty("tst:title", "gee");
        session.save();
        // delete foo after having modified some of the deleted children
        nodea.setSimpleProperty("tst:title", "foo2");
        nodeb.setSimpleProperty("tst:title", "bar2");
        nodec.setSimpleProperty("tst:title", "gee2");
        session.removeNode(nodea);
        session.save();

        // now from another session
        session.close();
        session = repository.getConnection();
        root = session.getRootNode();

        // no more docs
        nodea = session.getChildNode(root, "foo", false);
        assertNull(nodea);
        nodea = session.getNodeById(ida);
        assertNull(nodea);
        nodeb = session.getNodeById(idb);
        assertNull(nodeb);
        nodec = session.getNodeById(idc);
        assertNull(nodec);

        // and with a query
        PartialList<Serializable> res;
        res = session.query("SELECT * FROM TestDoc WHERE ecm:isProxy = 0", QueryFilter.EMPTY, false);
        assertEquals(0, res.size());
        res = session.query("SELECT * FROM TestDoc", QueryFilter.EMPTY, false);
        assertEquals(0, res.size());
    }

    @Test
    public void testBulkUpdates() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();

        // bulk insert
        Node nodea = session.addChildNode(root, "foo", null, "TestDoc", false);
        Node nodeb = session.addChildNode(nodea, "bar", null, "TestDoc", false);
        Node nodec = session.addChildNode(nodeb, "gee", null, "TestDoc", false);
        nodea.setSimpleProperty("tst:title", "foo");
        nodeb.setSimpleProperty("tst:title", "bar");
        nodec.setSimpleProperty("tst:title", "gee");
        nodea.setCollectionProperty("tst:subjects", new String[] { "a", "b", "c" });
        nodeb.setCollectionProperty("tst:subjects", new String[] { "d", "e", "f" });
        nodec.setCollectionProperty("tst:subjects", new String[] { "g", "h" });
        session.save();

        // bulk update
        nodea.setSimpleProperty("tst:title", "foo2");
        nodeb.setSimpleProperty("tst:title", "bar2");
        nodec.setSimpleProperty("tst:title", "gee2");
        nodea.setCollectionProperty("tst:subjects", new String[] { "a2", "b2", "c2" });
        nodeb.setCollectionProperty("tst:subjects", new String[] { "d2", "e2" });
        nodec.setCollectionProperty("tst:subjects", new String[] {});
        session.save();

        // bulk update, not identical groups of keys
        nodea.setSimpleProperty("tst:title", "foo3");
        nodea.setSimpleProperty("tst:count", Long.valueOf(333));
        nodeb.setSimpleProperty("tst:title", "bar3");
        nodec.setSimpleProperty("tst:title", "gee3");
        session.save();

        // bulk update, ACLs
        ACLRow acl1 = new ACLRow(1, "test", true, "Write", "steve", null);
        ACLRow acl2 = new ACLRow(0, "test", true, "Read", null, "Members");
        ACLRow acl3 = new ACLRow(2, "local", true, "ReadWrite", "bob", null);
        nodea.getCollectionProperty(Model.ACL_PROP).setValue(new ACLRow[] { acl1, acl2, acl3 });
        nodeb.getCollectionProperty(Model.ACL_PROP).setValue(new ACLRow[] { acl2 });
        nodec.getCollectionProperty(Model.ACL_PROP).setValue(new ACLRow[] { acl3 });
        session.save();

        // bulk delete
        session.removeNode(nodea);
        session.removeNode(nodeb);
        session.removeNode(nodec);
        session.save();
    }

    @Test
    public void testBulkFetch() throws Exception {
        Session session = repository.getConnection();
        Model model = ((SessionImpl) session).getModel();

        // fragments that are always present
        Set<String> alwaysFragments = new HashSet<>();
        if (!model.miscInHierarchy) {
            alwaysFragments.add(Model.MISC_TABLE_NAME);
        }

        boolean subjectsIsArray = !model.getFragmentNames().contains("tst:subjects");
        boolean tagsIsArray = !model.getFragmentNames().contains("tst:tags");

        // check computed prefetch info
        HashSet<String> expectedFragments;
        expectedFragments = new HashSet<>(Arrays.asList("testschema", "tst:bignotes", "acls", "versions"));
        expectedFragments.addAll(alwaysFragments);
        if (!subjectsIsArray) {
            expectedFragments.add("tst:subjects");
        }
        if (!tagsIsArray) {
            expectedFragments.add("tst:tags");
        }
        assertEquals(expectedFragments, model.getTypePrefetchedFragments("TestDoc"));

        expectedFragments = new HashSet<>(Arrays.asList("testschema2", "acls", "versions"));
        expectedFragments.addAll(alwaysFragments);
        assertEquals(expectedFragments, model.getTypePrefetchedFragments("TestDoc2"));

        expectedFragments = new HashSet<>(Arrays.asList("acls", "versions"));
        expectedFragments.addAll(alwaysFragments);
        if (subjectsIsArray) {
            expectedFragments.add("testschema");
        } else {
            expectedFragments.add("tst:subjects");
        }
        assertEquals(expectedFragments, model.getTypePrefetchedFragments("TestDoc3"));

        Node root = session.getRootNode();

        Node node1 = session.addChildNode(root, "n1", null, "TestDoc", false);
        node1.setSimpleProperty("tst:title", "one");
        node1.setCollectionProperty("tst:subjects", new String[] { "a", "b" });
        node1.setCollectionProperty("tst:tags", new String[] { "foo" });
        node1.setSimpleProperty("tst:count", Long.valueOf(123));
        node1.setSimpleProperty("tst:rate", Double.valueOf(3.14));
        CollectionProperty aclProp = node1.getCollectionProperty(Model.ACL_PROP);
        ACLRow acl = new ACLRow(1, "test", true, "Write", "steve", null);
        aclProp.setValue(new ACLRow[] { acl });

        Node node2 = session.addChildNode(root, "n2", null, "TestDoc2", false);
        node2.setSimpleProperty("tst2:title", "two");
        aclProp = node2.getCollectionProperty(Model.ACL_PROP);
        acl = new ACLRow(0, "test", true, "Read", null, "Members");
        aclProp.setValue(new ACLRow[] { acl });

        session.save();
        session.close();
        session = repository.getConnection();

        List<Node> nodes = session.getNodesByIds(Arrays.asList(node1.getId(), node2.getId()));

        assertEquals(2, nodes.size());
        node1 = nodes.get(0);
        node2 = nodes.get(1);
        if (node1.getName().equals("n2")) {
            // swap
            Node n = node1;
            node1 = node2;
            node2 = n;
        }
        assertEquals(Arrays.asList("a", "b"), Arrays.asList(node1.getCollectionProperty("tst:subjects").getStrings()));
        assertEquals(Arrays.asList("foo"), Arrays.asList(node1.getCollectionProperty("tst:tags").getStrings()));
        aclProp = node1.getCollectionProperty(Model.ACL_PROP);
        ACLRow[] acls = (ACLRow[]) aclProp.getValue();
        assertEquals(1, acls.length);
        assertEquals("Write", acls[0].permission);

        assertEquals("two", node2.getSimpleProperty("tst2:title").getString());
        aclProp = node2.getCollectionProperty(Model.ACL_PROP);
        acls = (ACLRow[]) aclProp.getValue();
        assertEquals(1, acls.length);
        assertEquals("Read", acls[0].permission);
    }

    @Test
    public void testBulkFetchProxies() throws Exception {
        assumeTrue(isProxiesEnabled());

        Session session = repository.getConnection();
        Node root = session.getRootNode();

        Node node0 = session.addChildNode(root, "n0", null, "TestDoc", false);
        node0.setSimpleProperty("tst:title", "zero");
        Node node1 = session.addChildNode(root, "n1", null, "TestDoc", false);
        node1.setSimpleProperty("tst:title", "one");
        Node node2 = session.addChildNode(root, "n2", null, "TestDoc", false);
        node2.setSimpleProperty("tst:title", "two");
        Node version1 = session.checkIn(node1, "v1", "");
        Node version2 = session.checkIn(node2, "v2", "");
        Node proxy1 = session.addProxy(version1.getId(), node1.getId(), root, "proxy1", null);
        Node proxy2 = session.addProxy(version2.getId(), node2.getId(), root, "proxy2", null);
        session.save();

        session.close();
        session = repository.getConnection();

        @SuppressWarnings("unused")
        List<Node> nodes = session.getNodesByIds(Arrays.asList(node0.getId(), proxy1.getId(), proxy2.getId()));

        // check logs by hand to see that data fragments are bulk fetched
    }

    @Test
    public void testBulkFetchMany() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node node1 = session.addChildNode(root, "n1", null, "TestDoc", false);
        Node node2 = session.addChildNode(root, "n2", null, "TestDoc2", false);
        session.save();

        // another session
        session.close();
        session = repository.getConnection();

        List<Serializable> ids = new ArrayList<>();
        ids.add(node2.getId());
        ids.add(node1.getId());
        int size = 2000; // > dialect.getMaximumArgsForIn()
        for (int i = 0; i < size; i++) {
            ids.add(generateMissingId(root, i));
        }
        List<Node> nodes = session.getNodesByIds(ids);
        assertEquals(2 + size, nodes.size());
        assertEquals(node2.getId(), nodes.get(0).getId());
        assertEquals(node1.getId(), nodes.get(1).getId());
        for (int i = 0; i < size; i++) {
            assertNull(nodes.get(2 + i));
        }
    }

    private Serializable generateMissingId(Node root, int i) {
        if (root.getId() instanceof String) {
            if (Dialect.DEBUG_UUIDS) {
                if (Dialect.DEBUG_REAL_UUIDS) {
                    return String.format("00000000-ffff-ffff-0000-%012x", Integer.valueOf(i));
                } else {
                    return "NO_SUCH_UUID_" + i;
                }
            } else {
                return UUID.randomUUID().toString();
            }
        } else { // Long
            return Long.valueOf(9999900000L + i);
        }
    }

    protected void waitForIndexing() {
        Framework.getService(EventService.class).waitForAsyncCompletion();
        DatabaseHelper.DATABASE.sleepForFulltext();
    }

    @Test
    public void testFulltextDisabled() throws Exception {
        // repository is already configured with fulltext disabled
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node node = session.addChildNode(root, "foo", null, "TestDoc", false);
        node.setSimpleProperty("tst:title", "hello world");
        session.save();
        try {
            session.query("SELECT * FROM TestDoc WHERE ecm:fulltext = 'world'", QueryFilter.EMPTY, false);
            fail("Expected fulltext to be disabled and throw an exception");
        } catch (NuxeoException e) {
            if (!e.getMessage().contains("disabled")) {
                fail("Expected fulltext to be disabled, got: " + e);
            }
            // ok
        }
    }

    @Test
    public void testRelation() throws Exception {
        PartialList<Serializable> res;

        Session session = repository.getConnection();
        Node rel1 = session.addChildNode(null, "rel", null, "Relation", false);
        rel1.setSimpleProperty("relation:source", "123");
        rel1.setSimpleProperty("relation:target", "456");
        Node rel2 = session.addChildNode(null, "rel", null, "Relation2", false);
        rel2.setSimpleProperty("relation:source", "123");
        rel2.setSimpleProperty("relation:target", "789");
        rel2.setSimpleProperty("tst:title", "yo");
        session.save();

        res = session.query("SELECT * FROM Document WHERE relation:source = '123'", QueryFilter.EMPTY, false);
        assertEquals(0, res.size()); // Relation is not a Document
        res = session.query("SELECT * FROM Relation WHERE relation:source = '123'", QueryFilter.EMPTY, false);
        assertEquals(2, res.size());
        res = session.query("SELECT * FROM Relation2", QueryFilter.EMPTY, false);
        assertEquals(1, res.size());
        res = session.query("SELECT * FROM Relation2 WHERE tst:title = 'yo'", QueryFilter.EMPTY, false);
        assertEquals(1, res.size());

        // remove
        session.removeNode(rel1);
        session.save();
        res = session.query("SELECT * FROM Relation WHERE relation:source = '123'", QueryFilter.EMPTY, false);
        assertEquals(1, res.size());
    }

    @Test
    public void testMixinAPI() throws Exception {
        PartialList<Serializable> res;
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node node = session.addChildNode(root, "foo", null, "TestDoc", false);
        session.save();

        assertFalse(node.hasMixinType("Aged"));
        assertFalse(node.hasMixinType("Orderable"));
        assertEquals(0, node.getMixinTypes().length);

        res = session.query("SELECT * FROM TestDoc WHERE ecm:mixinType = 'Aged'", QueryFilter.EMPTY, false);
        assertEquals(0, res.size());
        res = session.query("SELECT * FROM TestDoc WHERE ecm:mixinType <> 'Aged'", QueryFilter.EMPTY, false);
        assertEquals(1, res.size());

        assertTrue(session.addMixinType(node, "Aged"));
        session.save();

        assertTrue(node.hasMixinType("Aged"));
        assertFalse(node.hasMixinType("Orderable"));
        assertEquals(1, node.getMixinTypes().length);

        res = session.query("SELECT * FROM TestDoc WHERE ecm:mixinType = 'Aged'", QueryFilter.EMPTY, false);
        assertEquals(1, res.size());
        res = session.query("SELECT * FROM TestDoc WHERE ecm:mixinType <> 'Aged'", QueryFilter.EMPTY, false);
        assertEquals(0, res.size());

        assertFalse(session.addMixinType(node, "Aged"));
        assertEquals(1, node.getMixinTypes().length);

        assertTrue(session.addMixinType(node, "Orderable"));
        assertTrue(node.hasMixinType("Aged"));
        assertTrue(node.hasMixinType("Orderable"));
        assertEquals(2, node.getMixinTypes().length);

        try {
            session.addMixinType(node, "nosuchmixin");
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        assertEquals(2, node.getMixinTypes().length);

        assertTrue(session.removeMixinType(node, "Aged"));
        session.save();
        assertFalse(node.hasMixinType("Aged"));
        assertTrue(node.hasMixinType("Orderable"));
        assertEquals(1, node.getMixinTypes().length);
        res = session.query("SELECT * FROM TestDoc WHERE ecm:mixinType = 'Aged'", QueryFilter.EMPTY, false);
        assertEquals(0, res.size());
        res = session.query("SELECT * FROM TestDoc WHERE ecm:mixinType <> 'Aged'", QueryFilter.EMPTY, false);
        assertEquals(1, res.size());

        assertFalse(session.removeMixinType(node, "Aged"));
        assertEquals(1, node.getMixinTypes().length);

        assertFalse(session.removeMixinType(node, "nosuchmixin"));
        assertEquals(1, node.getMixinTypes().length);

        assertTrue(session.removeMixinType(node, "Orderable"));
        assertFalse(node.hasMixinType("Aged"));
        assertFalse(node.hasMixinType("Orderable"));
        assertEquals(0, node.getMixinTypes().length);
    }

    @Test
    public void testMixinIncludedInPrimaryType() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node node = session.addChildNode(root, "foo", null, "DocWithAge", false);

        node.setSimpleProperty("age:age", "123");
        assertEquals("123", node.getSimpleProperty("age:age").getValue());
        session.save();

        // another session
        session.close();
        session = repository.getConnection();
        root = session.getRootNode();
        node = session.getNodeById(node.getId());
        assertEquals("123", node.getSimpleProperty("age:age").getValue());

        // API on doc whose type has a mixin (facet)
        assertEquals(0, node.getMixinTypes().length); // instance mixins
        assertEquals(Collections.singleton("Aged"), node.getAllMixinTypes());
        assertTrue(node.hasMixinType("Aged"));
        assertFalse(session.addMixinType(node, "Aged"));
        assertFalse(session.removeMixinType(node, "Aged"));

    }

    @Test
    public void testMixinAddRemove() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node node = session.addChildNode(root, "foo", null, "TestDoc", false);
        session.save();

        // mixin not there
        try {
            node.getSimpleProperty("age:age");
            fail();
        } catch (PropertyNotFoundException e) {
            // ok
        }

        // add
        session.addMixinType(node, "Aged");
        SimpleProperty p = node.getSimpleProperty("age:age");
        assertNotNull(p);
        p.setValue("123");
        session.save();

        // remove
        session.removeMixinType(node, "Aged");
        session.save();

        // mixin not there anymore
        try {
            node.getSimpleProperty("age:age");
            fail();
        } catch (PropertyNotFoundException e) {
            // ok
        }
    }

    // mixin on doc with same schema in primary type does no harm
    @Test
    public void testMixinAddRemove2() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node node = session.addChildNode(root, "foo", null, "DocWithAge", false);

        node.setSimpleProperty("age:age", "456");
        session.save();

        session.addMixinType(node, "Aged");
        SimpleProperty p = node.getSimpleProperty("age:age");
        assertEquals("456", p.getValue());

        session.removeMixinType(node, "Aged");
        p = node.getSimpleProperty("age:age");
        assertEquals("456", p.getValue());
        session.save();
    }

    @Test
    public void testMixinWithSamePropertyName() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node node = session.addChildNode(root, "foo", null, "TestDoc", false);
        node.setSimpleProperty("tst:title", "bar");
        session.save();

        session.addMixinType(node, "Aged");
        node.setSimpleProperty("age:title", "gee");
        session.save();

        assertEquals("bar", node.getSimpleProperty("tst:title").getValue());
        assertEquals("gee", node.getSimpleProperty("age:title").getValue());
    }

    @Test
    public void testMixinCopy() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node node = session.addChildNode(root, "foo", null, "TestDoc", false);
        session.addMixinType(node, "Aged");
        node.setSimpleProperty("age:age", "123");
        node.setCollectionProperty("age:nicknames", new String[] { "bar", "gee" });
        session.save();

        // copy the doc
        Node copy = session.copy(node, root, "foo2");
        SimpleProperty p = copy.getSimpleProperty("age:age");
        assertEquals("123", p.getValue());
        CollectionProperty p2 = copy.getCollectionProperty("age:nicknames");
        assertEquals(Arrays.asList("bar", "gee"), Arrays.asList(p2.getValue()));
    }

    // copy of a facet that holds only a complex list
    @Test
    public void testMixinCopyComplexList() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node node = session.addChildNode(root, "foo", null, "TestDoc", false);
        session.addMixinType(node, "Templated");
        Node t = session.addChildNode(node, "template", Long.valueOf(0), "template", true);
        t.setSimpleProperty("templateId", "123");
        session.save();

        // copy the doc
        Node copy = session.copy(node, root, "foo2");
        Node tcopy = session.getChildNode(copy, "template", true);
        SimpleProperty p = tcopy.getSimpleProperty("templateId");
        assertEquals("123", p.getValue());
    }

    @Test
    public void testMixinCopyDeep() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();

        Node folder = session.addChildNode(root, "folder", null, "TestDoc", false);
        session.save();

        Node node = session.addChildNode(folder, "foo", null, "TestDoc", false);
        session.addMixinType(node, "Aged");
        node.setSimpleProperty("age:age", "123");
        node.setCollectionProperty("age:nicknames", new String[] { "bar", "gee" });
        session.save();

        // copy the folder
        session.copy(folder, root, "folder2");

        Node copy = session.getNodeByPath("/folder2/foo", null);
        SimpleProperty p = copy.getSimpleProperty("age:age");
        assertEquals("123", p.getValue());
        CollectionProperty p2 = copy.getCollectionProperty("age:nicknames");
        assertEquals(Arrays.asList("bar", "gee"), Arrays.asList(p2.getValue()));
    }

    @Test
    public void testMixinQueryContent() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node node = session.addChildNode(root, "foo", null, "TestDoc", false);
        session.addMixinType(node, "Aged");
        node.setSimpleProperty("age:age", "barbar");
        session.save();

        PartialList<Serializable> res = session.query("SELECT * FROM TestDoc WHERE age:age = 'barbar'",
                QueryFilter.EMPTY, false);
        assertEquals(1, res.size());
    }

    @Test
    public void testLocking() throws Exception {
        Session session = repository.getConnection();
        LockManager lockManager = session.getLockManager();
        Node root = session.getRootNode();
        Node node = session.addChildNode(root, "foo", null, "TestDoc", false);
        Serializable nodeId = node.getId();
        String id = nodeId.toString();
        assertNull(lockManager.getLock(id));
        session.save();

        session.close();
        session = repository.getConnection();
        node = session.getNodeById(nodeId);

        Lock lock = lockManager.setLock(id, new Lock("bob", null));
        assertNull(lock);
        assertNotNull(lockManager.getLock(id));

        lock = lockManager.setLock(id, new Lock("john", null));
        assertEquals("bob", lock.getOwner());

        lock = lockManager.removeLock(id, "steve");
        assertEquals("bob", lock.getOwner());
        assertTrue(lock.getFailed());
        assertNotNull(lockManager.getLock(id));

        lock = lockManager.removeLock(id, null);
        assertEquals("bob", lock.getOwner());
        assertFalse(lock.getFailed());
        assertNull(lockManager.getLock(id));

        lock = lockManager.removeLock(id, null);
        assertNull(lock);
    }

    @Test
    public void testLockingParallel() throws Throwable {
        Serializable nodeId = createNode();
        runParallelLocking(nodeId, repository, repository);
    }

    @Test
    @ConditionalIgnoreRule.Ignore(condition = ConditionalIgnoreRule.NXP10926H2Upgrade.class)
    public void testLockingParallelClustered() throws Throwable {
        if (!DatabaseHelper.DATABASE.supportsClustering()) {
            System.out.println("Skipping clustered locking test for unsupported database: "
                    + DatabaseHelper.DATABASE.getClass().getName());
            return;
        }

        Serializable nodeId = createNode();

        // get two clustered repositories
        repository.close();
        long DELAY = 50; // ms
        repository = newRepository(DELAY);
        Repository repository2 = newRepository(DELAY);

        runParallelLocking(nodeId, repository, repository2);
    }

    protected Serializable createNode() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node node = session.addChildNode(root, "foo", null, "TestDoc", false);
        session.save();
        Serializable nodeId = node.getId();
        session.close();
        return nodeId;
    }

    protected static void runParallelLocking(Serializable nodeId, Repository repository1, Repository repository2)
            throws Throwable {
        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch firstReady = new CountDownLatch(1);
        long TIME = 1000; // ms
        LockingJob r1 = new LockingJob(repository1, "t1-", nodeId, TIME, firstReady, barrier);
        LockingJob r2 = new LockingJob(repository2, "t2-", nodeId, TIME, null, barrier);
        Thread t1 = null;
        Thread t2 = null;
        try {
            t1 = new Thread(r1, "t1");
            t2 = new Thread(r2, "t2");
            t1.start();
            if (firstReady.await(60, TimeUnit.SECONDS)) {
                t2.start();

                t1.join();
                t1 = null;
                t2.join();
                t2 = null;
                if (r1.throwable != null) {
                    throw r1.throwable;
                }
                if (r2.throwable != null) {
                    throw r2.throwable;
                }
                int count = r1.count + r2.count;
                log.info("Parallel locks per second: " + count);
            } // else timed out
        } finally {
            // error condition recovery
            if (t1 != null) {
                t1.interrupt();
            }
            if (t2 != null) {
                t2.interrupt();
            }
        }
    }

    protected static class LockingJob implements Runnable {

        protected final Repository repository;

        protected final String namePrefix;

        protected final Serializable nodeId;

        protected final long waitMillis;

        public CountDownLatch ready;

        public CyclicBarrier barrier;

        public Throwable throwable;

        public int count;

        public LockingJob(Repository repository, String namePrefix, Serializable nodeId, long waitMillis,
                CountDownLatch ready, CyclicBarrier barrier) {
            this.repository = repository;
            this.namePrefix = namePrefix;
            this.nodeId = nodeId;
            this.waitMillis = waitMillis;
            this.ready = ready;
            this.barrier = barrier;
        }

        @Override
        public void run() {
            try {
                doHeavyLockingJob();
            } catch (Throwable t) {
                t.printStackTrace();
                throwable = t;
            } finally {
                // error recovery
                // still count down as main thread is awaiting us
                if (ready != null) {
                    ready.countDown();
                }
                // break barrier for other thread
                if (barrier != null) {
                    barrier.reset(); // break barrier
                }
            }
        }

        protected void doHeavyLockingJob() throws Exception {
            Session session = repository.getConnection();
            LockManager lockManager = session.getLockManager();
            if (ready != null) {
                ready.countDown();
                ready = null;
            }
            barrier.await(30, TimeUnit.SECONDS); // throws on timeout
            barrier = null;
            // System.err.println(namePrefix + " starting");
            long start = System.currentTimeMillis();
            do {
                String name = namePrefix + count++;

                // lock
                while (true) {
                    Lock lock = lockManager.setLock(nodeId.toString(), new Lock(name, null));
                    if (lock == null) {
                        break;
                    }
                    // System.err.println(name + " waiting, already locked by "
                    // + lock.getOwner());
                }
                // System.err.println(name + " locked");

                // unlock
                Lock lock = lockManager.removeLock(nodeId.toString(), null);
                assertNotNull("got no lock, expected " + name, lock);
                assertEquals(name, lock.getOwner());
                // System.err.println(name + " unlocked");
            } while (System.currentTimeMillis() - start < waitMillis);
            session.close();
        }
    }

    @Test
    public void testCacheInvalidations() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Session s2 = repository.getConnection();

        // no invalidations yet
        assertSessionInvalidations(s2, false, -1, -1);

        // creation
        Node doc = session.addChildNode(root, "doc", null, "Relation", false);
        session.save(); // send invalidations
        // 2 invalidations: root __PARENT__ and doc hierarchy
        assertSessionInvalidations(s2, false, 2, -1);
        s2.save(); // process (clear) invalidations

        // modification
        doc.setSimpleProperty("relation:predicate", "foo");
        session.save(); // send invalidations
        // 1 invalidation: doc relation
        assertSessionInvalidations(s2, false, 1, -1);
        s2.save(); // process (clear) invalidations

        // delete
        session.removeNode(doc);
        session.save(); // send invalidations
        assertSessionInvalidations(s2, false, isProxiesEnabled() ? 5 : 3, useArrayColumns() ? 6 : 8);

        s2.close();
        session.close();
    }

    /**
     * Tests that invalidations don't grow indefinitely (and consume memory).
     */
    @Test
    public void testCacheInvalidationsUnlimitedGrowth() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Session s2 = repository.getConnection();

        // no invalidations yet
        assertSessionInvalidations(s2, false, -1, -1);

        // now create many docs, accumulating invalidations
        int ndocs = Invalidations.MAX_SIZE + 1;
        for (int i = 0; i < ndocs; i++) {
            session.addChildNode(root, "doc" + i, null, "Relation", false);
        }
        session.save(); // send invalidations
        // "all" invalidations flagged
        assertSessionInvalidations(s2, true, -1, -1);

        s2.close();
        session.close();
    }

    protected static void assertSessionInvalidations(Session session, boolean all, int modified, int deleted) {
        SoftRefCachingMapper mapper = (SoftRefCachingMapper) ((SessionImpl) session).getMapper();
        Invalidations invalidations = mapper.cacheQueue.queue;
        assertTrue(all == invalidations.all);
        if (modified == -1) {
            if (invalidations.modified != null) {
                // if() prevents NPE when computing the message
                assertNull(String.valueOf(invalidations.modified.size()), invalidations.modified);
            }
        } else {
            assertEquals(modified, invalidations.modified.size());
        }
        if (deleted == -1) {
            if (invalidations.deleted != null) {
                assertNull(String.valueOf(invalidations.deleted.size()), invalidations.deleted);
            }
        } else {
            assertEquals(deleted, invalidations.deleted.size());
        }
    }

    @Test
    public void testCacheInvalidationsPropagatorLeak() throws Exception {
        assertEquals(0, getInvalidationsPropagatorSize());
        Session session = repository.getConnection();
        assertEquals(1, getInvalidationsPropagatorSize());
        session.close();
        assertEquals(0, getInvalidationsPropagatorSize());
        Session s1 = repository.getConnection();
        Session s2 = repository.getConnection();
        Session s3 = repository.getConnection();
        assertEquals(3, getInvalidationsPropagatorSize());
        s1.close();
        s2.close();
        s3.close();
        assertEquals(0, getInvalidationsPropagatorSize());
    }

    @Test
    public void testClusterInvalidationsPropagatorLeak() throws Exception {
        if (!DatabaseHelper.DATABASE.supportsClustering()) {
            System.out.println("Skipping clustering test for unsupported database: "
                    + DatabaseHelper.DATABASE.getClass().getName());
            return;
        }

        repository.close();
        // get a clustered repository
        long DELAY = 500; // ms
        repository = newRepository(DELAY);

        assertEquals(0, getInvalidationsPropagatorSize());
        Session session = repository.getConnection();
        assertEquals(1, getInvalidationsPropagatorSize());
        session.close();
        assertEquals(0, getInvalidationsPropagatorSize());
    }

    protected int getInvalidationsPropagatorSize() throws Exception {
        Field propagatorField = RepositoryImpl.class.getDeclaredField("invalidationsPropagator");
        propagatorField.setAccessible(true);
        InvalidationsPropagator propagator = (InvalidationsPropagator) propagatorField.get(repository);
        return propagator.queues.size();
    }

    protected List<Serializable> makeComplexDoc(Session session) {
        Node root = session.getRootNode();

        Node doc = session.addChildNode(root, "doc", null, "TestDoc", false);
        Serializable docId = doc.getId();

        // tst:title = 'hello world'
        doc.setSimpleProperty("tst:title", "hello world");
        // tst:subjects = ['foo', 'bar', 'moo']
        // tst:subjects/item[0] = 'foo'
        // tst:subjects/0 = 'foo'
        doc.setCollectionProperty("tst:subjects", new String[] { "foo", "bar", "moo" });

        Node owner = session.getChildNode(doc, "tst:owner", true);
        // tst:owner/firstname = 'Bruce'
        owner.setSimpleProperty("firstname", "Bruce");
        // tst:owner/lastname = 'Willis'
        owner.setSimpleProperty("lastname", "Willis");

        Node duo = session.getChildNode(doc, "tst:couple", true);
        Node first = session.getChildNode(duo, "first", true);
        Node second = session.getChildNode(duo, "second", true);
        // tst:couple/first/firstname = 'Steve'
        first.setSimpleProperty("firstname", "Steve");
        // tst:couple/first/lastname = 'Jobs'
        first.setSimpleProperty("lastname", "Jobs");
        // tst:couple/second/firstname = 'Steve'
        second.setSimpleProperty("firstname", "Steve");
        // tst:couple/second/lastname = 'McQueen'
        second.setSimpleProperty("lastname", "McQueen");

        Node friend0 = session.addChildNode(doc, "tst:friends", 0L, "person", true);
        Node friend1 = session.addChildNode(doc, "tst:friends", 1L, "person", true);
        // tst:friends/item[0]/firstname = 'John'
        // tst:friends/0/firstname = 'John'
        friend0.setSimpleProperty("firstname", "John");
        // tst:friends/0/lastname = 'Lennon'
        friend0.setSimpleProperty("lastname", "Lennon");
        // tst:friends/1/firstname = 'John'
        friend1.setSimpleProperty("firstname", "John");
        // tst:friends/1/lastname = 'Smith'
        friend1.setSimpleProperty("lastname", "Smith");

        // this one doesn't have a schema prefix
        Node person = session.getChildNode(doc, "animal", true);
        // animal/race = 'dog'
        person.setSimpleProperty("race", "dog");
        // animal/name = 'Scooby'
        person.setSimpleProperty("name", "Scooby");

        session.save();

        return Arrays.asList(docId);
    }

    protected static String FROM_WHERE = " FROM TestDoc WHERE ecm:isProxy = 0 AND ";

    protected static String SELECT_WHERE = "SELECT *" + FROM_WHERE;

    protected static String SELECT_TITLE_WHERE = "SELECT tst:title" + FROM_WHERE;

    @Test
    public void testQueryComplexMakeDoc() throws Exception {
        Session session = repository.getConnection();
        List<Serializable> oneDoc = makeComplexDoc(session);

        String clause;
        PartialList<Serializable> res;
        IterableQueryResult it;

        clause = "tst:title = 'hello world'";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);
        it = session.queryAndFetch(SELECT_TITLE_WHERE + clause, "NXQL", QueryFilter.EMPTY);
        assertEquals(1, it.size());
        assertEquals("hello world", it.iterator().next().get("tst:title"));
        it.close();

        clause = "tst:subjects = 'foo'";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);
        it = session.queryAndFetch(SELECT_TITLE_WHERE + clause, "NXQL", QueryFilter.EMPTY);
        assertEquals(1, it.size());
        assertEquals("hello world", it.iterator().next().get("tst:title"));
        it.close();

        clause = "tst:subjects IN ('foo', 'bar')";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);
        it = session.queryAndFetch(SELECT_TITLE_WHERE + clause, "NXQL", QueryFilter.EMPTY);
        assertEquals(1, it.size());
        assertEquals("hello world", it.iterator().next().get("tst:title"));
        it.close();
    }

    @Test
    public void testQueryComplexWhere() throws Exception {
        Session session = repository.getConnection();
        List<Serializable> oneDoc = makeComplexDoc(session);

        String clause;
        PartialList<Serializable> res;
        IterableQueryResult it;

        // hierarchy h
        // JOIN hierarchy h2 ON h2.parentid = h.id
        // LEFT JOIN person p ON p.id = h2.id
        // WHERE h2.name = 'tst:owner'
        // AND p.firstname = 'Bruce'
        clause = "tst:owner/firstname = 'Bruce'";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);
        it = session.queryAndFetch("SELECT tst:title, tst:owner/lastname" + FROM_WHERE + clause, "NXQL",
                QueryFilter.EMPTY);
        assertEquals(1, it.size());
        assertEquals("Willis", it.iterator().next().get("tst:owner/lastname"));
        it.close();

        // check other operators

        clause = "tst:owner/firstname LIKE 'B%'";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);

        clause = "tst:owner/firstname IS NOT NULL";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);

        clause = "tst:owner/firstname IN ('Bruce', 'Bilbo')";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);

        // hierarchy h
        // JOIN hierarchy h2 ON h2.parentid = h.id
        // JOIN hierarchy h3 ON h3.parentid = h2.id
        // LEFT JOIN person p ON p.id = h3.id
        // WHERE h2.name = 'tst:couple'
        // AND h3.name = 'first'
        // AND p.firstname = 'Steve'
        clause = "tst:couple/first/firstname = 'Steve'";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);
        it = session.queryAndFetch("SELECT tst:title, tst:couple/first/lastname" + FROM_WHERE + clause, "NXQL",
                QueryFilter.EMPTY);
        assertEquals(1, it.size());
        assertEquals("Jobs", it.iterator().next().get("tst:couple/first/lastname"));
        it.close();

        // hierarchy h
        // JOIN hierarchy h2 ON h2.parentid = h.id
        // LEFT JOIN person p ON p.id = h2.id
        // WHERE h2.name = 'tst:friends' AND h2.pos = 0
        // AND p.firstname = 'John'
        clause = "tst:friends/0/firstname = 'John'";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);
        it = session.queryAndFetch("SELECT tst:title, tst:friends/0/lastname" + FROM_WHERE + clause, "NXQL",
                QueryFilter.EMPTY);
        assertEquals(1, it.size());
        assertEquals("Lennon", it.iterator().next().get("tst:friends/0/lastname"));
        it.close();

        // alternate xpath syntax
        clause = "tst:friends/item[0]/firstname = 'John'";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);

        // hierarchy h
        // JOIN hierarchy h2 ON h2.parentid = h.id
        // LEFT JOIN person p ON p.id = h2.id
        // WHERE h2.name = 'tst:friends'
        // AND p.firstname = 'John'
        clause = "tst:friends/*/firstname = 'John'";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);
        it = session.queryAndFetch(SELECT_TITLE_WHERE + clause, "NXQL", QueryFilter.EMPTY);
        assertEquals(2, it.size()); // two uncorrelated stars
        it.close();

        // alternate xpath syntax
        clause = "tst:friends/item[*]/firstname = 'John'";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);

        // hierarchy h
        // JOIN hierarchy h2 ON h2.parentid = h.id
        // LEFT JOIN person p ON p.id = h2.id
        // WHERE h2.name = 'tst:friends'
        // AND p.firstname = 'John'
        // AND p.lastname = 'Smith'
        clause = "tst:friends/*1/firstname = 'John'" + " AND tst:friends/*1/lastname = 'Smith'";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);
        it = session.queryAndFetch("SELECT tst:title, tst:friends/*1/lastname" + FROM_WHERE + clause, "NXQL",
                QueryFilter.EMPTY);
        assertEquals(1, it.size()); // correlated stars
        assertEquals("Smith", it.iterator().next().get("tst:friends/*1/lastname"));
        it.close();

        // alternate xpath syntax
        clause = "tst:friends/item[*1]/firstname = 'John'" + " AND tst:friends/item[*1]/lastname = 'Smith'";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);
    }

    @Test
    public void testQueryComplexPrefix() throws Exception {
        Session session = repository.getConnection();
        List<Serializable> oneDoc = makeComplexDoc(session);
        String clause;
        PartialList<Serializable> res;

        // schema with a prefix
        clause = "tst:owner/firstname = 'Bruce'";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);

        // use of prefix is mandatory if defined
        try {
            clause = "owner/firstname = 'Bruce'";
            session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
            fail("Should fail on missing prefix");
        } catch (Exception e) {
            assertEquals("No such property: owner/firstname", e.getMessage());
        }

        // schema without a prefix
        clause = "animal/race = 'dog'";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);

        // allow use with schema-name-as-prefix
        clause = "testschema3:animal/race = 'dog'";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);
    }

    @Test
    public void testQueryComplexReturned() throws Exception {
        Session session = repository.getConnection();
        List<Serializable> oneDoc = makeComplexDoc(session);

        String clause;
        PartialList<Serializable> res;
        IterableQueryResult it;
        Set<String> set;

        // SELECT p.lastname
        // FROM hierarchy h
        // JOIN hierarchy h2 ON h2.parentid = h.id
        // LEFT JOIN person p ON p.id = h2.id
        // WHERE h2.name = 'tst:friends'
        clause = "tst:title = 'hello world'";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);
        it = session.queryAndFetch("SELECT tst:friends/*/lastname" + FROM_WHERE + clause, "NXQL", QueryFilter.EMPTY);
        assertEquals(2, it.size());
        set = new HashSet<>();
        for (Map<String, Serializable> map : it) {
            set.add((String) map.get("tst:friends/*/lastname"));
        }
        assertEquals(new HashSet<>(Arrays.asList("Lennon", "Smith")), set);
        it.close();

        // SELECT p.firstname, p.lastname
        // FROM hierarchy h
        // JOIN hierarchy h2 ON h2.parentid = h.id
        // LEFT JOIN person p ON p.id = h2.id
        // WHERE h2.name = 'tst:friends'
        clause = "tst:title = 'hello world'";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);
        it = session.queryAndFetch("SELECT tst:friends/*1/firstname, tst:friends/*1/lastname" + FROM_WHERE + clause,
                "NXQL", QueryFilter.EMPTY);
        assertEquals(2, it.size());
        Set<String> fn = new HashSet<>();
        Set<String> ln = new HashSet<>();
        for (Map<String, Serializable> map : it) {
            fn.add((String) map.get("tst:friends/*1/firstname"));
            ln.add((String) map.get("tst:friends/*1/lastname"));
        }
        assertEquals(Collections.singleton("John"), fn);
        assertEquals(new HashSet<>(Arrays.asList("Lennon", "Smith")), ln);
        it.close();

    }

    @Test
    public void testQueryComplexListElement() throws Exception {
        Session session = repository.getConnection();
        List<Serializable> oneDoc = makeComplexDoc(session);

        String clause;
        PartialList<Serializable> res;
        IterableQueryResult it;
        Set<String> set;

        // hierarchy h
        // JOIN tst_subjects s ON h.id = s.id // not LEFT JOIN
        // WHERE s.pos = 0
        // AND s.item = 'foo'
        clause = "tst:subjects/0 = 'foo'";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);
        clause = "tst:subjects/0 = 'bar'";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(0, res.size());

        // SELECT s.item
        // FROM hierarchy h
        // JOIN tst_subjects s ON h.id = s.id // not LEFT JOIN
        // WHERE s.pos = 0
        // AND s.item = 'bar'
        clause = "tst:subjects/0 = 'foo'";
        it = session.queryAndFetch("SELECT tst:subjects/0" + FROM_WHERE + clause, "NXQL", QueryFilter.EMPTY);
        assertEquals(1, it.size());
        assertEquals("foo", it.iterator().next().get("tst:subjects/0"));
        it.close();

        // SELECT s1.item
        // FROM hierarchy h
        // JOIN tst_subjects s0 ON h.id = s0.id // not LEFT JOIN
        // JOIN tst_subjects s1 ON h.id = s1.id // not LEFT JOIN
        // WHERE s0.pos = 0 AND s1.pos = 1
        // AND s0.item LIKE 'foo%'
        clause = "tst:subjects/0 LIKE 'foo%'";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);
        it = session.queryAndFetch("SELECT tst:subjects/1" + FROM_WHERE + clause, "NXQL", QueryFilter.EMPTY);
        assertEquals(1, it.size());
        assertEquals("bar", it.iterator().next().get("tst:subjects/1"));
        it.close();

        // SELECT s.item
        // FROM hierarchy h
        // LEFT JOIN tst_subjects s ON h.id = s.id
        // WHERE s.item LIKE '%oo'
        clause = "tst:subjects/*1 LIKE '%oo'";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);
        it = session.queryAndFetch("SELECT tst:subjects/*1" + FROM_WHERE + clause, "NXQL", QueryFilter.EMPTY);
        assertEquals(2, it.size());
        set = new HashSet<>();
        for (Map<String, Serializable> map : it) {
            set.add((String) map.get("tst:subjects/*1"));
        }
        assertEquals(new HashSet<>(Arrays.asList("foo", "moo")), set);
        it.close();

        clause = "tst:subjects/* LIKE '%oo'";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);
        it = session.queryAndFetch("SELECT tst:subjects/*" + FROM_WHERE + clause, "NXQL", QueryFilter.EMPTY);
        // two uncorrelated stars, resulting in a cross join
        assertEquals(6, it.size());
        set = new HashSet<>();
        for (Map<String, Serializable> map : it) {
            set.add((String) map.get("tst:subjects/*"));
        }
        assertEquals(new HashSet<>(Arrays.asList("foo", "moo", "bar")), set);
        it.close();

        // WHAT
        clause = "tst:title = 'hello world'";
        it = session.queryAndFetch("SELECT tst:subjects/*" + FROM_WHERE + clause, "NXQL", QueryFilter.EMPTY);
        assertEquals(3, it.size());
        set = new HashSet<>();
        for (Map<String, Serializable> map : it) {
            set.add((String) map.get("tst:subjects/*"));
        }
        assertEquals(new HashSet<>(Arrays.asList("foo", "bar", "moo")), set);
        it.close();
    }

    @Test
    public void testQueryComplexOrderBy() throws Exception {
        Session session = repository.getConnection();
        List<Serializable> oneDoc = makeComplexDoc(session);

        String clause;
        PartialList<Serializable> res;
        IterableQueryResult it;
        List<String> list;

        clause = "tst:title LIKE '%' ORDER BY tst:owner/firstname";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);

        clause = "tst:owner/firstname = 'Bruce' ORDER BY tst:title";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);

        clause = "tst:owner/firstname = 'Bruce' ORDER BY tst:owner/firstname";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);

        // this produces a DISTINCT and adds tst:title to the select list
        clause = "tst:subjects/* = 'foo' ORDER BY tst:title";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);

        clause = "tst:friends/*/firstname = 'John' ORDER BY tst:title";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);

        // no wildcard index so no DISTINCT needed
        clause = "tst:title LIKE '%' ORDER BY tst:friends/0/lastname";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);
        clause = "tst:title LIKE '%' ORDER BY tst:subjects/0";
        res = session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);

        // SELECT * statement cannot ORDER BY array or complex list element
        clause = "tst:subjects/*1 = 'foo' ORDER BY tst:subjects/*1";
        try {
            session.query(SELECT_WHERE + clause, QueryFilter.EMPTY, false);
            fail();
        } catch (QueryParseException e) {
            String expected = "For SELECT * the ORDER BY columns cannot use wildcard indexes";
            assertEquals(expected, e.getMessage());
        }
        assertEquals(oneDoc, res);

        clause = "tst:title = 'hello world' ORDER BY tst:subjects/*1";
        it = session.queryAndFetch("SELECT tst:title" + FROM_WHERE + clause, "NXQL", QueryFilter.EMPTY);
        assertEquals(3, it.size());
        it.close();

        // same with DISTINCT, cannot work
        try {
            session.queryAndFetch("SELECT DISTINCT tst:title" + FROM_WHERE + clause, "NXQL", QueryFilter.EMPTY);
            fail();
        } catch (QueryParseException e) {
            String expected = "For SELECT DISTINCT the ORDER BY columns must be in the SELECT list, missing: [tst:subjects/*1]";
            assertEquals(expected, e.getMessage());
        }

        // ok if ORDER BY column added to SELECT columns
        it = session.queryAndFetch("SELECT DISTINCT tst:title, tst:subjects/*1" + FROM_WHERE + clause, "NXQL",
                QueryFilter.EMPTY);
        assertEquals(3, it.size());
        it.close();

        clause = "tst:title = 'hello world' ORDER BY tst:subjects/*1";
        it = session.queryAndFetch("SELECT tst:subjects/*1" + FROM_WHERE + clause, "NXQL", QueryFilter.EMPTY);
        assertEquals(3, it.size());
        list = new LinkedList<>();
        for (Map<String, Serializable> map : it) {
            list.add((String) map.get("tst:subjects/*1"));
        }
        assertEquals(Arrays.asList("bar", "foo", "moo"), list);
        it.close();

        clause = "tst:title = 'hello world' ORDER BY tst:subjects/*1";
        it = session.queryAndFetch("SELECT DISTINCT tst:subjects/*1" + FROM_WHERE + clause, "NXQL", QueryFilter.EMPTY);
        assertEquals(3, it.size());
        it.close();
    }

    @Test
    public void testQueryComplexOrderByProxies() throws Exception {
        Session session = repository.getConnection();
        List<Serializable> oneDoc = makeComplexDoc(session);

        String clause;
        PartialList<Serializable> res;

        clause = "tst:friends/*/firstname = 'John' ORDER BY tst:title";
        res = session.query("SELECT * FROM TestDoc WHERE " + clause, QueryFilter.EMPTY, false);
        assertEquals(oneDoc, res);
    }

    @Test
    public void testQueryComplexOr() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();

        // doc1 tst:title = 'hello world'
        Node doc1 = session.addChildNode(root, "doc1", null, "TestDoc", false);
        doc1.setSimpleProperty("tst:title", "hello world");

        // doc2 tst:owner/firstname = 'Bruce'
        Node doc2 = session.addChildNode(root, "doc2", null, "TestDoc", false);
        Node owner = session.getChildNode(doc2, "tst:owner", true); // complex prop auto-created by parent
        owner.setSimpleProperty("firstname", "Bruce");

        // doc3 tst:friends/0/firstname = 'John'
        Node doc3 = session.addChildNode(root, "doc3", null, "TestDoc", false);
        Node friend = session.addChildNode(doc3, "tst:friends", Long.valueOf(0), "person", true);
        friend.setSimpleProperty("firstname", "John");

        // doc4 tst:subjects/0 = 'foo'
        Node doc4 = session.addChildNode(root, "doc4", null, "TestDoc", false);
        doc4.setCollectionProperty("tst:subjects", new String[] { "foo" });

        session.save();

        String s1 = "SELECT * FROM TestDoc WHERE ecm:isProxy = 0 AND (";
        String s2 = ")";
        String o = " OR ";
        String c1 = "tst:title = 'hello world'";
        String c2 = "tst:owner/firstname = 'Bruce'";
        String c3 = "tst:friends/0/firstname = 'John'";
        String c4 = "tst:subjects/0 = 'foo'";
        PartialList<Serializable> res;

        res = session.query(s1 + c1 + s2, QueryFilter.EMPTY, false);
        assertEquals(Collections.singletonList(doc1.getId()), res);

        res = session.query(s1 + c2 + s2, QueryFilter.EMPTY, false);
        assertEquals(Collections.singletonList(doc2.getId()), res);

        res = session.query(s1 + c3 + s2, QueryFilter.EMPTY, false);
        assertEquals(Collections.singletonList(doc3.getId()), res);

        res = session.query(s1 + c4 + s2, QueryFilter.EMPTY, false);
        assertEquals(Collections.singletonList(doc4.getId()), res);

        res = session.query(s1 + c1 + o + c2 + s2, QueryFilter.EMPTY, false);
        assertEquals(2, res.size());

        res = session.query(s1 + c1 + o + c3 + s2, QueryFilter.EMPTY, false);
        assertEquals(2, res.size());

        res = session.query(s1 + c1 + o + c4 + s2, QueryFilter.EMPTY, false);
        assertEquals(2, res.size());

        res = session.query(s1 + c2 + o + c3 + s2, QueryFilter.EMPTY, false);
        assertEquals(2, res.size());

        res = session.query(s1 + c2 + o + c4 + s2, QueryFilter.EMPTY, false);
        assertEquals(2, res.size());

        res = session.query(s1 + c3 + o + c4 + s2, QueryFilter.EMPTY, false);
        assertEquals(2, res.size());

        res = session.query(s1 + c1 + o + c2 + o + c3 + s2, QueryFilter.EMPTY, false);
        assertEquals(3, res.size());

        res = session.query(s1 + c1 + o + c2 + o + c4 + s2, QueryFilter.EMPTY, false);
        assertEquals(3, res.size());

        res = session.query(s1 + c1 + o + c3 + o + c4 + s2, QueryFilter.EMPTY, false);
        assertEquals(3, res.size());

        res = session.query(s1 + c2 + o + c3 + o + c4 + s2, QueryFilter.EMPTY, false);
        assertEquals(3, res.size());

        res = session.query(s1 + c1 + o + c2 + o + c3 + o + c4 + s2, QueryFilter.EMPTY, false);
        assertEquals(4, res.size());
    }

    @Test
    public void testQueryAggregatesErrors() throws Exception {
        Session session = repository.getConnection();
        try {
            session.queryAndFetch("SELECT tst:title FROM TestDoc WHERE COUNT(tst:title) = 1", "NXQL",
                    QueryFilter.EMPTY);
            fail("Should fail");
        } catch (QueryParseException e) {
            String msg = e.getMessage();
            assertTrue(msg, msg.contains("Function not supported in WHERE clause"));
        }
        try {
            session.queryAndFetch("SELECT COUNT(*) FROM TestDoc", "NXQL", QueryFilter.EMPTY);
            fail("Should fail");
        } catch (QueryParseException e) {
            String msg = e.getMessage();
            assertTrue(msg, msg.contains("Syntax error"));
        }
    }

    @Test
    public void testQueryAggregates() throws Exception {
        IterableQueryResult it;
        Map<String, Serializable> map;

        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node doc1 = session.addChildNode(root, "doc1", null, "TestDoc", false);
        doc1.setSimpleProperty("tst:title", "hello1");
        doc1.setSimpleProperty("tst:count", Long.valueOf(100));
        doc1.setSimpleProperty("tst:rate", Double.valueOf(1));
        Node doc2 = session.addChildNode(root, "doc2", null, "TestDoc", false);
        doc2.setSimpleProperty("tst:title", "hello2");
        doc2.setSimpleProperty("tst:count", Long.valueOf(300));
        doc2.setSimpleProperty("tst:rate", Double.valueOf(3));
        session.save();

        it = session.queryAndFetch("SELECT COUNT(tst:title) FROM TestDoc WHERE ecm:isProxy = 0", "NXQL",
                QueryFilter.EMPTY);
        assertEquals(1, it.size());
        map = it.iterator().next();
        assertEquals(Long.valueOf(2), map.get("COUNT(tst:title)"));
        it.close();

        it = session.queryAndFetch("SELECT SUM(tst:count), AVG(tst:count), MIN(tst:count), MAX(tst:count)"
                + " FROM TestDoc WHERE ecm:isProxy = 0", "NXQL", QueryFilter.EMPTY);
        assertEquals(1, it.size());
        map = it.iterator().next();
        assertEquals(Long.valueOf(400), map.get("SUM(tst:count)"));
        assertEquals(Double.valueOf(200.0), map.get("AVG(tst:count)"));
        assertEquals(Long.valueOf(100), map.get("MIN(tst:count)"));
        assertEquals(Long.valueOf(300), map.get("MAX(tst:count)"));
        it.close();

        it = session.queryAndFetch("SELECT SUM(tst:rate), AVG(tst:rate), MIN(tst:rate), MAX(tst:rate)"
                + " FROM TestDoc WHERE ecm:isProxy = 0", "NXQL", QueryFilter.EMPTY);
        assertEquals(1, it.size());
        map = it.iterator().next();
        assertEquals(Double.valueOf(4.0), map.get("SUM(tst:rate)"));
        assertEquals(Double.valueOf(2.0), map.get("AVG(tst:rate)"));
        assertEquals(Double.valueOf(1.0), map.get("MIN(tst:rate)"));
        assertEquals(Double.valueOf(3.0), map.get("MAX(tst:rate)"));
        it.close();
    }

    @Test
    public void testQueryAncestorId() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node folder1 = session.addChildNode(root, "folder1", null, "Folder", false);
        Node folder2 = session.addChildNode(folder1, "folder2", null, "Folder", false);
        session.addChildNode(folder2, "doc", null, "File", false);
        session.save();

        String query = "SELECT * FROM Document WHERE ecm:ancestorId = '%s'";
        PartialList<Serializable> res;

        res = session.query(String.format(query, "nosuchid"), QueryFilter.EMPTY, false);
        assertEquals(0, res.size());

        res = session.query(String.format(query, root.getId()), QueryFilter.EMPTY, false);
        assertEquals(3, res.size());

        res = session.query(String.format(query, folder1.getId()), QueryFilter.EMPTY, false);
        assertEquals(2, res.size());

        res = session.query(String.format(query, folder2.getId()), QueryFilter.EMPTY, false);
        assertEquals(1, res.size());
    }

    @Test
    public void testPath() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();

        // /foo
        Node foo = session.addChildNode(root, "foo", null, "TestDoc", false);
        // /foo/bar
        Node bar = session.addChildNode(foo, "bar", null, "TestDoc", false);
        // /foo/bar/gee
        Node gee = session.addChildNode(bar, "gee", null, "TestDoc", false);
        // /foo/moo
        Node moo = session.addChildNode(foo, "moo", null, "TestDoc", false);

        session.save();
        session.close();
        session = repository.getConnection();

        List<Node> nodes = session.getNodesByIds(Arrays.asList(gee.getId(), moo.getId()));
        assertEquals("/foo/bar/gee", nodes.get(0).getPath());
        assertEquals("/foo/moo", nodes.get(1).getPath());
    }

    @Test
    public void testNormalizedName() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();

        String originalName = "e\u0301cran";
        assertEquals(6, originalName.length());

        String normalizedName = "\u00e9cran";
        assertEquals(5, normalizedName.length());

        // Add a document with a non normalized name
        Node doc = session.addChildNode(root, originalName, null, "TestDoc", false);
        assertFalse(originalName.equals(doc.getName()));

        assertTrue(session.hasChildNode(root, normalizedName, false));
    }

    @Test
    public void testPathCached() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node foo = session.addChildNode(root, "foo", null, "TestDoc", false);
        Node bar = session.addChildNode(foo, "bar", null, "TestDoc", false);
        session.save();
        session.close();
        session = repository.getConnection();

        Node node = session.getNodeById(bar.getId());
        assertEquals("/foo/bar", node.getPath());

        // clear context, the mapper cache should still be used
        ((SessionImpl) session).context.pristine.clear();
        JDBCConnection jdbc = (JDBCConnection) JDBCMapperConnector.unwrap(
                ((SoftRefCachingMapper) ((SessionImpl) session).getMapper()).mapper);
        jdbc.countExecutes = true;
        jdbc.executeCount = 0;

        node = session.getNodeById(bar.getId());
        assertEquals("/foo/bar", node.getPath());
        assertEquals(0, jdbc.executeCount);
    }

    @Test
    public void testPathDeep() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node r1 = session.addChildNode(root, "r1", null, "TestDoc", false);
        Node r2 = session.addChildNode(root, "r2", null, "TestDoc", false);
        for (int i = 0; i < 10; i++) {
            r1 = session.addChildNode(r1, "node" + i, null, "TestDoc", false);
            r2 = session.addChildNode(r2, "node" + i, null, "TestDoc", false);
        }
        Node last1 = r1;
        Node last2 = r2;

        session.save();
        session.close();
        session = repository.getConnection();

        // fetch last
        List<Node> nodes = session.getNodesByIds(Arrays.asList(last1.getId(), last2.getId()));
        assertEquals("/r1/node0/node1/node2/node3/node4" + "/node5/node6/node7/node8/node9", nodes.get(0).getPath());
        assertEquals("/r2/node0/node1/node2/node3/node4" + "/node5/node6/node7/node8/node9", nodes.get(1).getPath());
    }

    @Test
    public void testPathOptimizationsActivation() throws Exception {
        repository.close();
        // open a repository without path optimization
        pathOptimizationsEnabled = false;
        repository = newRepository(-1);
        Session session = repository.getConnection();
        PartialList<Serializable> res;
        Node root = session.getRootNode();
        List<Serializable> ids = new ArrayList<>();
        Node node = session.addChildNode(root, "r1", null, "TestDoc", false);
        for (int i = 0; i < 4; i++) {
            node = session.addChildNode(node, "node" + i, null, "TestDoc", false);
        }
        ids.add(node.getId()); // keep the latest
        session.save();
        List<Node> nodes = session.getNodesByIds(ids);
        assertEquals(1, nodes.size());
        String sql = "SELECT * FROM TestDoc WHERE ecm:path STARTSWITH '/r1'";
        res = session.query(sql, QueryFilter.EMPTY, false);
        assertEquals(4, res.size());

        // reopen repository with path optimization to populate the ancestors table
        repository.close();

        pathOptimizationsEnabled = true;
        repository = newRepository(-1);
        session = repository.getConnection();
        // this query will use nx_ancestors to bulk load the path
        nodes = session.getNodesByIds(ids);
        assertEquals(1, nodes.size());
        res = session.query(sql, QueryFilter.EMPTY, false);
        assertEquals(4, res.size());
    }

    @Test
    public void testParallelPrepareUserReadAcls() throws Throwable {
        assumeTrue(!(DatabaseHelper.DATABASE instanceof DatabaseOracle)); // NXP-18684

        Session session = repository.getConnection();
        Node root = session.getRootNode();
        session.addChildNode(root, "foo", null, "TestDoc", false);
        session.save();
        session.close();

        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch firstReady = new CountDownLatch(1);
        PrepareUserReadAclsJob r1 = new PrepareUserReadAclsJob(firstReady, barrier);
        PrepareUserReadAclsJob r2 = new PrepareUserReadAclsJob(null, barrier);
        Thread t1 = null;
        Thread t2 = null;
        try {
            t1 = new Thread(r1, "t1");
            t2 = new Thread(r2, "t2");
            t1.start();
            if (firstReady.await(60, TimeUnit.SECONDS)) {
                t2.start();

                t1.join();
                t1 = null;
                t2.join();
                t2 = null;
                if (r1.throwable != null) {
                    throw r1.throwable;
                }
                if (r2.throwable != null) {
                    throw r2.throwable;
                }
            } // else timed out
        } finally {
            // error condition recovery
            if (t1 != null) {
                t1.interrupt();
            }
            if (t2 != null) {
                t2.interrupt();
            }
        }

        // after both threads have run, check that we don't see
        // duplicate documents
        session = repository.getConnection();
        checkOneDoc(session);
        session.close();

    }

    protected static void checkOneDoc(Session session) {
        String query = "SELECT * FROM TestDoc WHERE ecm:isProxy = 0";
        PartialList<Serializable> res = session.query(query, QueryFilter.EMPTY, true);
        assertEquals(1, res.size());
    }

    protected class PrepareUserReadAclsJob implements Runnable {

        public CountDownLatch ready;

        public CyclicBarrier barrier;

        public Throwable throwable;

        public PrepareUserReadAclsJob(CountDownLatch ready, CyclicBarrier barrier) {
            this.ready = ready;
            this.barrier = barrier;
        }

        @Override
        public void run() {
            Session session = null;
            try {
                session = repository.getConnection();
                if (ready != null) {
                    ready.countDown();
                    ready = null;
                }
                barrier.await(30, TimeUnit.SECONDS); // throws on timeout
                barrier = null;
                checkOneDoc(session);
            } catch (Throwable t) {
                t.printStackTrace();
                throwable = t;
            } finally {
                if (session != null) {
                    try {
                        session.close();
                    } catch (ResourceException e) {
                        e.printStackTrace();
                    }
                }
                // error recovery
                // still count down as main thread is awaiting us
                if (ready != null) {
                    ready.countDown();
                }
                // break barrier for other thread
                if (barrier != null) {
                    barrier.reset(); // break barrier
                }
            }
        }
    }

}
