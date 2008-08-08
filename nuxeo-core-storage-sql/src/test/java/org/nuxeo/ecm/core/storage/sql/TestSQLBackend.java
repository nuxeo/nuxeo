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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.core.storage.StorageException;

/**
 * @author Florent Guillaume
 */
public class TestSQLBackend extends SQLBackendTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.core.storage.sql.tests",
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

        SimpleProperty titleProp = root.getSimpleProperty("tst:title");
        assertNull(titleProp.getValue());
        titleProp.setValue("the root");

        session.save();
        session.close();
    }

    public void testChildren() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();

        // root doc /foo
        Node nodefoo = session.addChildNode(root, "foo", "TestDoc", false);
        assertEquals(root.getId(), session.getParentNode(nodefoo).getId());
        assertEquals("TestDoc", nodefoo.getType().getName());
        assertEquals("/foo", session.getPath(nodefoo));
        Node nodeabis = session.getChildNode(root, "foo", false);
        assertEquals(nodefoo.getId(), nodeabis.getId());

        // first child /foo/bar
        Node nodeb = session.addChildNode(nodefoo, "bar", "TestDoc", false);
        assertEquals("/foo/bar", session.getPath(nodeb));
        assertEquals(nodefoo.getId(), session.getParentNode(nodeb).getId());
        assertEquals(nodeb.getId(),
                session.getNodeByPath("/foo/bar", null).getId());

        session.save();
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
        Node nodec = session.addChildNode(nodefoo, "gee", "TestDoc", false);
        assertEquals("/foo/gee", session.getPath(nodec));
        List<Node> children = session.getChildren(nodefoo, false, null);
        assertEquals(2, children.size());

        session.save();

        children = session.getChildren(nodefoo, false, null);
        assertEquals(2, children.size());

        // delete bar
        session.removeNode(nodefoo);
        session.save();

    }

    public void testBasics() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node nodea = session.addChildNode(root, "foo", "TestDoc", false);

        nodea.setSingleProperty("tst:title", "hello world");
        nodea.setCollectionProperty("tst:subjects", new String[] { "a", "b",
                "c" });
        nodea.setCollectionProperty("tst:tags", new String[] { "1", "2" });

        assertEquals("hello world",
                nodea.getSimpleProperty("tst:title").getString());
        String[] subjects = nodea.getCollectionProperty("tst:subjects").getStrings();
        String[] tags = nodea.getCollectionProperty("tst:tags").getStrings();
        assertEquals(Arrays.asList("a", "b", "c"), Arrays.asList(subjects));
        assertEquals(Arrays.asList("1", "2"), Arrays.asList(tags));

        session.save();

        // now modify a property and re-save
        nodea.setSingleProperty("tst:title", "another");
        nodea.setCollectionProperty("tst:subjects", new String[] { "z", "c" });
        nodea.setCollectionProperty("tst:tags", new String[] { "3" });

        session.save();
        session.close();

        // now read from another session
        session = repository.getConnection();
        root = session.getRootNode();
        assertNotNull(root);
        nodea = session.getChildNode(root, "foo", false);
        assertEquals("another",
                nodea.getSimpleProperty("tst:title").getString());
        subjects = nodea.getCollectionProperty("tst:subjects").getStrings();
        tags = nodea.getCollectionProperty("tst:tags").getStrings();
        assertEquals(Arrays.asList("z", "c"), Arrays.asList(subjects));
        assertEquals(Arrays.asList("3"), Arrays.asList(tags));

        // delete the node
        session.removeNode(nodea);
        session.save();
    }

    public void testBinary() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node nodea = session.addChildNode(root, "foo", "TestDoc", false);

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
        ACLRow acl1 = new ACLRow(0, "test", 1, true, "Write", "steve", null);
        ACLRow acl2 = new ACLRow(0, "test", 0, true, "Read", null, "Members");
        prop.setValue(new ACLRow[] { acl1, acl2 });
        session.save();
        session.close();
        session = repository.getConnection();
        root = session.getRootNode();
        prop = root.getCollectionProperty(Model.ACL_PROP);
        ACLRow[] acls = (ACLRow[]) prop.getValue();
        assertEquals(2, acls.length);
        assertEquals("Members", acls[0].group);
        assertEquals("test", acls[0].aclname);
        assertEquals("steve", acls[1].user);
        assertEquals("test", acls[1].aclname);
    }

    public void testCrossSessionInvalidations() throws Exception {
        Session session1 = repository.getConnection();
        Node root1 = session1.getRootNode();
        SimpleProperty title1 = root1.getSimpleProperty("tst:title");

        Session session2 = repository.getConnection();
        Node root2 = session2.getRootNode();
        SimpleProperty title2 = root2.getSimpleProperty("tst:title");

        // change title1
        title1.setValue("yo");
        assertEquals(null, title2.getString());
        // save session1 and queue its invalidations to others
        session1.save();
        // session2 has not saved (committed) yet, so still unmodified
        assertEquals(null, title2.getString());
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

        // TODO test collections, and children
    }

    public void testMove() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node foldera = session.addChildNode(root, "folder_a", "TestDoc", false);
        Serializable prevId = foldera.getId();
        Node nodea = session.addChildNode(foldera, "node_a", "TestDoc", false);
        Node nodeac = session.addChildNode(nodea, "node_a_complex", "TestDoc",
                true);
        assertEquals("/folder_a/node_a/node_a_complex", session.getPath(nodeac));
        Node folderb = session.addChildNode(root, "folder_b", "TestDoc", false);
        session.addChildNode(folderb, "node_b", "TestDoc", false);
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
        Node foldera = session.addChildNode(root, "folder_a", "TestDoc", false);
        Serializable prevFolderaId = foldera.getId();
        Node nodea = session.addChildNode(foldera, "node_a", "TestDoc", false);
        Serializable prevNodeaId = nodea.getId();
        Node nodeac = session.addChildNode(nodea, "node_a_complex", "TestDoc",
                true);
        Serializable prevNodeacId = nodeac.getId();
        nodea.setSingleProperty("tst:title", "hello world");
        nodea.setCollectionProperty("tst:subjects", new String[] { "a", "b",
                "c" });
        assertEquals("/folder_a/node_a/node_a_complex", session.getPath(nodeac));
        Node folderb = session.addChildNode(root, "folder_b", "TestDoc", false);
        session.addChildNode(folderb, "node_b", "TestDoc", false);
        Node folderc = session.addChildNode(root, "folder_c", "TestDoc", false);
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
        Node foldera = session.addChildNode(root, "folder_a", "TestDoc", false);
        Node nodea = session.addChildNode(foldera, "node_a", "TestDoc", false);
        Node nodeac = session.addChildNode(nodea, "node_a_complex", "TestDoc",
                true);
        nodea.setSingleProperty("tst:title", "hello world");
        nodea.setCollectionProperty("tst:subjects", new String[] { "a", "b",
                "c" });
        // nodea.setSingleProperty("ecm:majorVersion", Long.valueOf(1));
        // nodea.setSingleProperty("ecm:minorVersion", Long.valueOf(0));

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
        assertEquals("foolab", version.getSimpleProperty("ecm:versionLabel").getValue());
        assertEquals("bardesc", version.getSimpleProperty("ecm:versionDescription").getValue());

        /*
         * Check out.
         */
        session.checkOut(nodea);
        assertEquals(Boolean.FALSE,
                nodea.getSimpleProperty("ecm:isCheckedIn").getValue());
        assertEquals(version.getId(),
                nodea.getSimpleProperty("ecm:baseVersion").getString());
    }
}
