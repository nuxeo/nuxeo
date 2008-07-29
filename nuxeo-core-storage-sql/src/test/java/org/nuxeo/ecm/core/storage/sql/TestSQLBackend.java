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

import java.util.Arrays;
import java.util.List;

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

}
