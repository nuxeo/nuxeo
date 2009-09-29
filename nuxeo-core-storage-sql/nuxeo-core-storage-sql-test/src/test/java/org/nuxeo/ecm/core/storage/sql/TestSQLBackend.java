/*
 * (C) Copyright 2008-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.nuxeo.ecm.core.storage.StorageException;

/**
 * @author Florent Guillaume
 */
public class TestSQLBackend extends SQLBackendTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.core.storage.sql.test.tests",
                "OSGI-INF/test-backend-core-types-contrib.xml");
    }

    public void testRootNode() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        assertNotNull(root);
        assertEquals("", root.getName());
        assertEquals("/", session.getPath(root));
        assertEquals("Root",
                root.getSimpleProperty("ecm:primaryType").getString());
        try {
            root.getSimpleProperty("tst:title");
            fail("Property should not exist");
        } catch (IllegalArgumentException e) {
            // ok
        }
        session.save();
        session.close();
    }

    protected int getChildrenHardSize(Session session) {
        Context context = ((SessionImpl) session).getContext("hierarchy");
        return ((HierarchyContext) context).childrenRegularHard.size();
    }

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
        Node nodeb = session.addChildNode(nodefoo, "bar", null, "TestDoc",
                false);
        assertEquals("/foo/bar", session.getPath(nodeb));
        assertEquals(nodefoo.getId(), session.getParentNode(nodeb).getId());
        assertEquals(nodeb.getId(),
                session.getNodeByPath("/foo/bar", null).getId());

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
        Node nodec = session.addChildNode(nodefoo, "gee", null, "TestDoc",
                false);
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

    public void testChildrenRemoval() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Serializable fooId = session.addChildNode(root, "foo", null, "TestDoc",
                false).getId();
        Serializable barId = session.addChildNode(root, "bar", null, "TestDoc",
                false).getId();
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
            node = session.addChildNode(node, String.valueOf(i), null,
                    "TestDoc", false);
            ids[i] = node.getId();
        }
        session.save(); // TODO shouldn't be needed
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

    public void testBasics() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node nodea = session.addChildNode(root, "foo", null, "TestDoc", false);

        nodea.setSingleProperty("tst:title", "hello world");
        nodea.setSingleProperty("tst:rate", Double.valueOf(1.5));
        nodea.setSingleProperty("tst:count", Long.valueOf(123456789));
        Calendar cal = new GregorianCalendar(2008, Calendar.JULY, 14, 12, 34,
                56);
        nodea.setSingleProperty("tst:created", cal);
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
        nodea.setSingleProperty("tst:title", "another");
        nodea.setSingleProperty("tst:rate", Double.valueOf(3.14));
        nodea.setSingleProperty("tst:count", Long.valueOf(1234567891234L));
        nodea.setCollectionProperty("tst:subjects", new String[] { "z", "c" });
        nodea.setCollectionProperty("tst:tags", new String[] { "3" });
        session.save();

        // again
        nodea.setSingleProperty("tst:created", null);
        session.save();

        // check the logs to see that the following doesn't do anything because
        // the value is unchanged since the last save (UPDATE optimizations)
        nodea.setSingleProperty("tst:title", "blah");
        nodea.setSingleProperty("tst:title", "another");
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

    public void testBasicsUpgrade() throws Exception {
        try {
            Mapper.debugTestUpgrade = true;
            testBasics();
        } finally {
            Mapper.debugTestUpgrade = false;
        }
    }

    public void testBigText() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node nodea = session.addChildNode(root, "foo", null, "TestDoc", false);

        StringBuilder buf = new StringBuilder(5000);
        for (int i = 0; i < 1000; i++) {
            buf.append(String.format("%-5d", Integer.valueOf(i)));
        }
        String bigtext = buf.toString();
        assertEquals(5000, bigtext.length());
        nodea.setSingleProperty("tst:bignote", bigtext);
        assertEquals(bigtext, nodea.getSimpleProperty("tst:bignote").getString());
        session.save();

        // now read from another session
        session.close();
        session = repository.getConnection();
        root = session.getRootNode();
        assertNotNull(root);
        nodea = session.getChildNode(root, "foo", false);
        String readtext = nodea.getSimpleProperty("tst:bignote").getString();
        assertEquals(bigtext, readtext);
    }

    public void testPropertiesSameName() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node nodea = session.addChildNode(root, "foo", null, "TestDoc", false);

        nodea.setSingleProperty("tst:title", "hello world");
        assertEquals("hello world",
                nodea.getSimpleProperty("tst:title").getString());

        try {
            nodea.setSingleProperty("tst2:title", "aha");
            fail("shouldn't allow setting property from foreign schema");
        } catch (Exception e) {
            // ok
        }

        session.save();
    }

    public void testBinary() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node nodea = session.addChildNode(root, "foo", null, "TestDoc", false);

        InputStream in = new ByteArrayInputStream("abc".getBytes("UTF-8"));
        Binary bin = session.getBinary(in);
        assertEquals(3, bin.getLength());
        assertEquals("900150983cd24fb0d6963f7d28e17f72", bin.getDigest());
        assertEquals("abc", readAllBytes(bin.getStream()));
        assertEquals("abc", readAllBytes(bin.getStream())); // readable twice
        nodea.setSingleProperty("tst:bin", bin);
        session.save();
        session.close();

        // now read from another session
        session = repository.getConnection();
        root = session.getRootNode();
        nodea = session.getChildNode(root, "foo", false);
        SimpleProperty binProp = nodea.getSimpleProperty("tst:bin");
        assertNotNull(binProp);
        Serializable value = binProp.getValue();
        assertTrue(value instanceof Binary);
        bin = (Binary) value;
        in = bin.getStream();
        assertEquals(3, bin.getLength());
        assertEquals("900150983cd24fb0d6963f7d28e17f72", bin.getDigest());
        assertEquals("abc", readAllBytes(bin.getStream()));
        assertEquals("abc", readAllBytes(bin.getStream())); // readable twice

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

    public void testACLs() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        CollectionProperty prop = root.getCollectionProperty(Model.ACL_PROP);
        assertNotNull(prop);
        assertEquals(4, prop.getValue().length); // root acls preexist
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

    public void testCrossSessionInvalidations() throws Exception {
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
        session2.save(); // and notifies invalidations
        // in non-transaction mode, session1 has not processed its invalidations
        // yet, call save() to process them artificially
        session1.save();
        // session2 save wins
        assertEquals("glop", title1.getString());
        assertEquals("glop", title2.getString());
    }

    public void testConcurrentNameCreation() throws Exception {
        // two docs with same name (possible at this low level)
        Session session1 = repository.getConnection();
        Node root1 = session1.getRootNode();
        Node foo1 = session1.addChildNode(root1, "foo", null, "TestDoc", false);
        session1.save();
        Session session2 = repository.getConnection();
        Node root2 = session2.getRootNode();
        Node foo2 = session2.addChildNode(root2, "foo", null, "TestDoc", false);
        session2.save();
        // on read we get one or the other, but no crash
        Session session3 = repository.getConnection();
        Node root3 = session3.getRootNode();
        Node foo3 = session3.getChildNode(root3, "foo", false);
        assertTrue(foo3.getId().equals(foo1.getId())
                || foo3.getId().equals(foo2.getId()));
        // try again, has been fixed (only one error in logs)
        Session session4 = repository.getConnection();
        Node root4 = session4.getRootNode();
        Node foo4 = session4.getChildNode(root4, "foo", false);
        assertEquals(foo3.getId(), foo4.getId());
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

    public void testCrossSessionChildrenInvalidationAdd() throws Exception {
        // in first session, create base folder
        Session session1 = repository.getConnection();
        Node root1 = session1.getRootNode();
        Node folder1 = session1.addChildNode(root1, "foo", null, "TestDoc",
                false);
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

    public void testCrossSessionChildrenInvalidationRemove() throws Exception {
        // in first session, create base folder and doc
        Session session1 = repository.getConnection();
        Node root1 = session1.getRootNode();
        Node folder1 = session1.addChildNode(root1, "foo", null, "TestDoc",
                false);
        Node doc1 = session1.addChildNode(folder1, "gee", null, "TestDoc",
                false);
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
    }

    public void testCrossSessionChildrenInvalidationMove() throws Exception {
        // in first session, create base folders and doc
        Session session1 = repository.getConnection();
        Node root1 = session1.getRootNode();
        Node foldera1 = session1.addChildNode(root1, "foo", null, "TestDoc",
                false);
        Node folderb1 = session1.addChildNode(root1, "bar", null, "TestDoc",
                false);
        Node doc1 = session1.addChildNode(foldera1, "gee", null, "TestDoc",
                false);
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

    public void testCrossSessionChildrenInvalidationCopy() throws Exception {
        // in first session, create base folders and doc
        Session session1 = repository.getConnection();
        Node root1 = session1.getRootNode();
        Node foldera1 = session1.addChildNode(root1, "foo", null, "TestDoc",
                false);
        Node folderb1 = session1.addChildNode(root1, "bar", null, "TestDoc",
                false);
        Node doc1 = session1.addChildNode(foldera1, "gee", null, "TestDoc",
                false);
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
        repository2 = newRepository(DELAY);

        Session session1 = repository.getConnection();
        // session1 creates root node and does a save
        // which resets invalidation timeout
        Session session2 = repository2.getConnection();
        session2.save(); // save resets invalidations timeout

        // in session1, create base folder
        Node root1 = session1.getRootNode();
        Node folder1 = session1.addChildNode(root1, "foo", null, "TestDoc",
                false);
        SimpleProperty title1 = folder1.getSimpleProperty("tst:title");
        session1.save();

        // in session2, retrieve folder and check children
        Node root2 = session2.getRootNode();
        Node folder2 = session2.getChildNode(root2, "foo", false);
        SimpleProperty title2 = folder2.getSimpleProperty("tst:title");
        session2.getChildren(folder2, null, false);

        // in session1, add document
        session1.addChildNode(folder1, "gee", null, "TestDoc", false);
        session1.save();

        // in session2, try to get document
        // immediate check, invalidation delay means not done yet
        session2.save();
        Node doc2 = session2.getChildNode(folder2, "gee", false);
        assertNull(doc2);
        Thread.sleep(DELAY + 1); // wait invalidation delay
        session2.save(); // process invalidations (non-transactional)
        doc2 = session2.getChildNode(folder2, "gee", false);
        assertNotNull(doc2);

        // in session1 change title
        title1.setValue("yo");
        assertNull(title2.getString());
        // save session1 (queues its invalidations to others)
        session1.save();
        // session2 has not saved (committed) yet, so still unmodified
        assertNull(title2.getString());
        // immediate check, invalidation delay means not done yet
        session2.save();
        assertNull(title2.getString());
        Thread.sleep(DELAY + 1); // wait invalidation delay
        session2.save();
        // after commit, invalidations have been processed
        assertEquals("yo", title2.getString());

        // written properties aren't shared
        title1.setValue("mama");
        title2.setValue("glop");
        session1.save();
        assertEquals("mama", title1.getString());
        assertEquals("glop", title2.getString());
        session2.save(); // and notifies invalidations
        // in non-transaction mode, session1 has not processed
        // its invalidations yet, call save() to process them artificially
        Thread.sleep(DELAY + 1); // wait invalidation delay
        session1.save();
        // session2 save wins
        assertEquals("glop", title1.getString());
        assertEquals("glop", title2.getString());
    }

    public void testRollback() throws Exception {
        Session session = repository.getConnection();
        XAResource xaresource = ((SessionImpl) session).getXAResource();
        Node root = session.getRootNode();
        Node nodea = session.addChildNode(root, "foo", null, "TestDoc", false);
        nodea.setSingleProperty("tst:title", "old");
        assertEquals("old", nodea.getSimpleProperty("tst:title").getString());
        session.save();

        /*
         * rollback before save (underlying XAResource saw no updates)
         */
        Xid xid = new DummyXid("1");
        xaresource.start(xid, XAResource.TMNOFLAGS);
        nodea = session.getNodeByPath("/foo", null);
        nodea.setSingleProperty("tst:title", "new");
        xaresource.end(xid, XAResource.TMSUCCESS);
        xaresource.prepare(xid);
        xaresource.rollback(xid);
        nodea = session.getNodeByPath("/foo", null);
        assertEquals("old", nodea.getSimpleProperty("tst:title").getString());

        /*
         * rollback after save (underlying XAResource does a rollback too)
         */
        xid = new DummyXid("2");
        xaresource.start(xid, XAResource.TMNOFLAGS);
        nodea = session.getNodeByPath("/foo", null);
        nodea.setSingleProperty("tst:title", "new");
        session.save();
        xaresource.end(xid, XAResource.TMSUCCESS);
        xaresource.prepare(xid);
        xaresource.rollback(xid);
        nodea = session.getNodeByPath("/foo", null);
        assertEquals("old", nodea.getSimpleProperty("tst:title").getString());
    }

    public void testSaveOnCommit() throws Exception {
        Session session = repository.getConnection(); // init
        session.save();

        XAResource xaresource = ((SessionImpl) session).getXAResource();

        // first transaction
        Xid xid = new DummyXid("1");
        xaresource.start(xid, XAResource.TMNOFLAGS);
        Node root = session.getRootNode();
        assertNotNull(root);
        session.addChildNode(root, "foo", null, "TestDoc", false);
        // let end do an implicit save
        xaresource.end(xid, XAResource.TMSUCCESS);
        xaresource.prepare(xid);
        xaresource.commit(xid, false);

        // should have saved, clearing caches should be harmless
        ((SessionImpl) session).clearCaches();

        // second transaction
        xid = new DummyXid("2");
        xaresource.start(xid, XAResource.TMNOFLAGS);
        Node foo = session.getNodeByPath("/foo", null);
        assertNotNull(foo);
        xaresource.end(xid, XAResource.TMSUCCESS);
        xaresource.prepare(xid);
        // Oracle needs a rollback as prepare() returns XA_RDONLY
        xaresource.rollback(xid);
    }

    public void testMove() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node foldera = session.addChildNode(root, "folder_a", null, "TestDoc",
                false);
        Serializable prevId = foldera.getId();
        Node nodea = session.addChildNode(foldera, "node_a", null, "TestDoc",
                false);
        Node nodeac = session.addChildNode(nodea, "node_a_complex", null,
                "TestDoc", true);
        assertEquals("/folder_a/node_a/node_a_complex", session.getPath(nodeac));
        Node folderb = session.addChildNode(root, "folder_b", null, "TestDoc",
                false);
        session.addChildNode(folderb, "node_b", null, "TestDoc", false);
        session.save();

        // cannot move under itself
        try {
            session.move(foldera, nodea, "abc");
            fail();
        } catch (StorageException e) {
            // ok
        }

        // cannot move to name that already exists
        try {
            session.move(foldera, folderb, "node_b");
            fail();
        } catch (StorageException e) {
            // ok
        }

        // do normal move
        Node node = session.move(foldera, folderb, "yo");
        assertEquals(prevId, node.getId());
        assertEquals("yo", node.getName());
        assertEquals("/folder_b/yo", session.getPath(node));
        assertEquals("/folder_b/yo/node_a/node_a_complex",
                session.getPath(nodeac));

        // move higher is allowed
        node = session.move(node, root, "underr");
        assertEquals(prevId, node.getId());
        assertEquals("underr", node.getName());
        assertEquals("/underr", session.getPath(node));
        assertEquals("/underr/node_a/node_a_complex", session.getPath(nodeac));

        session.save();
    }

    public void testCopy() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node foldera = session.addChildNode(root, "folder_a", null, "TestDoc",
                false);
        Serializable prevFolderaId = foldera.getId();
        Node nodea = session.addChildNode(foldera, "node_a", null, "TestDoc",
                false);
        Serializable prevNodeaId = nodea.getId();
        Node nodeac = session.addChildNode(nodea, "node_a_complex", null,
                "TestDoc", true);
        Node nodead = session.addChildNode(nodea, "node_a_duo", null, "duo",
                true);
        Serializable prevNodeacId = nodeac.getId();
        nodea.setSingleProperty("tst:title", "hello world");
        nodea.setCollectionProperty("tst:subjects", new String[] { "a", "b",
                "c" });
        nodea.setSingleProperty("ecm:lifeCycleState", "foostate"); // misc table
        assertEquals("/folder_a/node_a/node_a_complex", session.getPath(nodeac));
        Node folderb = session.addChildNode(root, "folder_b", null, "TestDoc",
                false);
        session.addChildNode(folderb, "node_b", null, "TestDoc", false);
        Node folderc = session.addChildNode(root, "folder_c", null, "TestDoc",
                false);
        session.save();

        // cannot copy under itself
        try {
            session.copy(foldera, nodea, "abc");
            fail();
        } catch (StorageException e) {
            // ok
        }

        // cannot copy to name that already exists
        try {
            session.copy(foldera, folderb, "node_b");
            fail();
        } catch (StorageException e) {
            // ok
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
        assertEquals("hello world",
                nodea2.getSimpleProperty("tst:title").getString());
        assertEquals("foostate",
                nodea2.getSimpleProperty("ecm:lifeCycleState").getString());
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

    public void testVersioning() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node foldera = session.addChildNode(root, "folder_a", null, "TestDoc",
                false);
        Node nodea = session.addChildNode(foldera, "node_a", null, "TestDoc",
                false);
        Node nodeac = session.addChildNode(nodea, "node_a_complex", null,
                "TestDoc", true);
        nodea.setSingleProperty("tst:title", "hello world");
        nodea.setCollectionProperty("tst:subjects", new String[] { "a", "b",
                "c" });
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
        assertEquals(Boolean.TRUE,
                nodea.getSimpleProperty("ecm:isCheckedIn").getValue());
        assertEquals(version.getId(),
                nodea.getSimpleProperty("ecm:baseVersion").getString());
        // the version info
        assertEquals("node_a", version.getName()); // keeps name
        assertNull(session.getParentNode(version));
        assertEquals("hello world",
                version.getSimpleProperty("tst:title").getString());
        assertNull(version.getSimpleProperty("ecm:baseVersion").getString());
        assertNull(version.getSimpleProperty("ecm:isCheckedIn").getValue());
        assertEquals(nodea.getId(), version.getSimpleProperty(
                "ecm:versionableId").getString());
        // assertEquals(Long.valueOf(1), version.getSimpleProperty(
        // "ecm:majorVersion").getLong());
        // assertEquals(Long.valueOf(0), version.getSimpleProperty(
        // "ecm:minorVersion").getLong());
        assertNotNull(version.getSimpleProperty("ecm:versionCreated").getValue());
        assertEquals("foolab",
                version.getSimpleProperty("ecm:versionLabel").getValue());
        assertEquals("bardesc", version.getSimpleProperty(
                "ecm:versionDescription").getValue());
        // the version child (complex prop)
        Node nodeacv = session.getChildNode(version, "node_a_complex", true);
        assertNotNull(nodeacv);
        assertNotSame(nodeacId, nodeacv.getId());

        /*
         * Check out.
         */
        session.checkOut(nodea);
        assertEquals(Boolean.FALSE,
                nodea.getSimpleProperty("ecm:isCheckedIn").getValue());
        assertEquals(version.getId(),
                nodea.getSimpleProperty("ecm:baseVersion").getString());
        nodea.setSingleProperty("tst:title", "blorp");
        nodea.setCollectionProperty("tst:subjects", new String[] { "x", "y" });
        Node nodeac2 = session.getChildNode(nodea, "node_a_complex", true);
        nodeac2.setSingleProperty("tst:title", "comp");
        session.save();

        /*
         * Restore.
         */
        session.restoreByLabel(nodea, "foolab");
        assertEquals("hello world",
                nodea.getSimpleProperty("tst:title").getString());
        assertEquals(
                Arrays.asList("a", "b", "c"),
                Arrays.asList(nodea.getCollectionProperty("tst:subjects").getStrings()));
        Node nodeac3 = session.getChildNode(nodea, "node_a_complex", true);
        assertNotNull(nodeac3);
        SimpleProperty sp = nodeac3.getSimpleProperty("tst:title");
        assertNotNull(sp);
        assertNull(sp.getString());

    }

    public void testProxies() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node foldera = session.addChildNode(root, "foldera", null, "TestDoc",
                false);
        Node nodea = session.addChildNode(foldera, "nodea", null, "TestDoc",
                false);
        Node folderb = session.addChildNode(root, "folderb", null, "TestDoc",
                false);

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
        Node proxy = session.addProxy(version.getId(), nodea.getId(), folderb,
                "proxy1", null);
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

    public void testDelete() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node nodea = session.addChildNode(root, "foo", null, "TestDoc", false);
        nodea.setSingleProperty("tst:title", "foo");
        Node nodeb = session.addChildNode(nodea, "bar", null, "TestDoc", false);
        nodeb.setSingleProperty("tst:title", "bar");
        Node nodec = session.addChildNode(nodeb, "gee", null, "TestDoc", false);
        nodec.setSingleProperty("tst:title", "gee");
        session.save();
        // delete foo after having modified some of the deleted children
        nodea.setSingleProperty("tst:title", "foo2");
        nodeb.setSingleProperty("tst:title", "bar2");
        nodec.setSingleProperty("tst:title", "gee2");
        session.removeNode(nodea);
        session.save();
    }

    public void testSystemProperties() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node nodea = session.addChildNode(root, "foo", null, "TestDoc", false);
        nodea.setSingleProperty("ecm:wfInProgress", Boolean.TRUE);
        nodea.setSingleProperty("ecm:wfIncOption", "beeep");
        session.save();
        session.close();
        session = repository.getConnection();
        root = session.getRootNode();
        assertNotNull(root);
        nodea = session.getChildNode(root, "foo", false);
        assertEquals(Boolean.TRUE,
                nodea.getSimpleProperty("ecm:wfInProgress").getValue());
        assertEquals("beeep",
                nodea.getSimpleProperty("ecm:wfIncOption").getValue());
    }
}

class DummyXid implements Xid {

    private final byte[] gtrid;

    private final byte[] bqual;

    public DummyXid(String id) {
        gtrid = id.getBytes();
        // MySQL JDBC driver needs a non 0-length branch qualifier
        bqual = new byte[] { '0' };
    }

    public int getFormatId() {
        return 0;
    }

    public byte[] getGlobalTransactionId() {
        return gtrid;
    }

    public byte[] getBranchQualifier() {
        return bqual;
    }
}
