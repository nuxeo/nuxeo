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

/**
 * @author Florent Guillaume
 */
public class TestBasics extends SQLStorageTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.core.storage.sql.tests",
                "OSGI-INF/test-core-types-contrib.xml");
    }

    public void testBasics() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        assertNotNull(root);
        assertEquals("", root.getName());
        assertEquals("Root",
                root.getSingleProperty("ecm:primaryType").getString());

        Node nodea = session.addNode(root, "foo", "TestDoc");
        assertEquals("TestDoc", nodea.getType().getName());
        nodea.setSingleProperty("tst:title", "hello world");
        nodea.setCollectionProperty("tst:subjects", new String[] { "a", "b",
                "c" });
        assertEquals("hello world",
                nodea.getSingleProperty("tst:title").getString());
        String[] subjects = nodea.getCollectionProperty("tst:subjects").getStrings();
        assertEquals(Arrays.asList("a", "b", "c"), Arrays.asList(subjects));

        Node nodeabis = session.getNode(root, "foo");
        Node nodeb = session.addNode(nodea, "bar", "TestDoc");
        session.save();

        // now modify a property and re-save
        nodea.setSingleProperty("tst:title", "another");
        nodea.setCollectionProperty("tst:subjects", new String[] { "z", "c" });
        session.save();
        session.close();

        // now read from another session
        Session session2 = repository.getConnection();
        Node root2 = session2.getRootNode();
        Node nodea2 = session2.getNode(root2, "foo");
        assertEquals("another",
                nodea2.getSingleProperty("tst:title").getString());
        subjects = nodea2.getCollectionProperty("tst:subjects").getStrings();
        assertEquals(Arrays.asList("z", "c"), Arrays.asList(subjects));
        // delete the node
        session2.removeNode(nodea2);
        session2.save();

    }

}
