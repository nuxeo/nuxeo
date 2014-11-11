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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.jcr.testing.CoreJCRConnectorTestConstants;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;
import org.nuxeo.ecm.core.schema.TypeConstants;

/**
 * Unit tests for the Document implementation in the JCR.
 *
 * @author <a href="mailto:lgiura@nuxeo.com">Leonard Giura</a>
 */
public class TestDocument extends RepositoryTestCase {
    private Session session;

    private Document root;

    private Document doc;

    private Document parent;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib(CoreJCRConnectorTestConstants.BUNDLE,
                "CoreTestExtensions.xml");
        prepareTest();
    }

    @Override
    public void tearDown() throws Exception {
        finalizeTest();
        root = null;
        session = null;
        doc = null;
        parent = null;
        super.tearDown();
    }

    /**
     * Prepares each test by getting the root doc, adding a a folder under the
     * root and a document under this folder.
     *
     * @throws Exception
     */

    public void prepareTest() throws Exception {
        // creating the session
        session = getRepository().getSession(null);
        root = session.getRootDocument();
        // adding a folder and a child doc
        parent = root.addChild("parent", "Folder");
        doc = parent.addChild("child", "MyDocType");
    }

    /**
     * Closes the session, then opens it again in order to test the preservation
     * of the actions made in the first step of the testing.
     *
     * @throws Exception
     */
    public void prepareReTest() throws Exception {
        session.close();
        session = getRepository().getSession(null);
        root = session.getRootDocument();
        parent = root.getChild("parent");
        doc = parent.getChild("child");
    }

    /**
     * Closes the session, after deleting newly created documents.
     *
     * @throws Exception
     */
    public void finalizeTest() throws Exception {
        if (parent != null) {
            parent.remove();
        }
        if (session != null) {
            session.close();
        }
    }

    public void testGetSession() throws Exception {
        // testing for root document
        assertEquals(session, root.getSession());
        prepareReTest();
        // retesting for a non root document
        assertEquals(session, doc.getSession());
    }

    public void testFolder() throws Exception {
        assertEquals("parent", parent.getName());
        // assertEquals(workspace, parent.getParent());
    }

    public void testDocument() throws Exception {
        assertEquals("child", doc.getName());
        // assertEquals(parent, doc.getParent());
        assertFalse(doc.hasChild("dummy"));
        assertFalse(doc.hasChildren());
        Iterator<Document> children = doc.getChildren();
        assertFalse(children.hasNext());
        List<String> childIds = doc.getChildrenIds();
        assertEquals(0, childIds.size());
    }

    public void testGetName() throws Exception {
        // testing for a non root document
        assertEquals("child", doc.getName());
        prepareReTest();
        // retesting for a non root document
        assertEquals("child", doc.getName());
    }

    public void testGetUUID() throws Exception {
        String rootUUID = root.getUUID();
        String docUUID = doc.getUUID();
        prepareReTest();
        // testing for root document
        assertEquals(rootUUID, root.getUUID());
        // testing for a non root document
        assertEquals(docUUID, doc.getUUID());
    }

    public void testGetParent() throws Exception {
        // testing for root document
        assertNull(root.getParent());
        // testing for a non root document
        assertEquals("parent", doc.getParent().getName());
        assertEquals(root, parent.getParent());
        prepareReTest();
        // testing again for a non root document
        assertEquals("parent", parent.getChild("child").getParent().getName());
    }

    public void testPathWalk() throws Exception {
        Document p = doc;
        List<String> path = new ArrayList<String>();
        while (p != null) {
            path.add(p.getName());
            p = p.getParent();
        }
        assertEquals(3, path.size());
        assertEquals(doc.getName(), path.get(0));
        assertEquals(parent.getName(), path.get(1));
        assertEquals(root.getName(), path.get(2));

    }

    public void testPathResolver() throws Exception {

        assertEquals(root, root.resolvePath("/"));
        assertEquals(root, parent.resolvePath("/"));
        assertEquals(parent, doc.resolvePath("../"));
        assertEquals(root, doc.resolvePath("../../"));
        assertEquals(doc, doc.resolvePath(""));
        assertEquals(parent, root.resolvePath("/parent"));
        assertEquals(parent, root.resolvePath(parent.getPath()));

    }

    public void testGetPath() throws Exception {
        assertEquals("/", root.getPath());
        assertEquals("/" + parent.getName(), parent.getPath());
        assertEquals("/" + parent.getName() + "/" + doc.getName(), doc
                .getPath());
    }

    public void testGetType() throws Exception {
        // testing for root document
        assertNotNull(root.getType());
        assertEquals("Root", root.getType().getName());
        // testing for non root document
        assertNotNull(doc.getType());
        assertEquals("MyDocType", doc.getType().getName());
        prepareReTest();
        // retesting for non root document
        assertNotNull(doc.getType());
        assertEquals("MyDocType", doc.getType().getName());
    }

    public void testGetState() {
        // TODO returns 0 ???
    }

    public void testIsFolder() throws Exception {
        // testing for root document
        assertTrue(root.isFolder());
        // testing for a non root document
        assertFalse(doc.isFolder());
        // testing for a non root folder
        assertTrue(parent.isFolder());
        prepareReTest();
        // retesting for a non root document
        assertFalse(doc.isFolder());
        // retesting for a non root folder
        assertTrue(parent.isFolder());
    }

    public void testResolve() throws Exception {
        // testing for a non root document
        assertEquals("child", root.resolvePath("parent/child").getName());
        prepareTest();
        // testing for a non root document
        assertEquals("child", parent.resolvePath("/child").getName());
    }

    public void testGetChild() throws Exception {
        // testing for root document
        assertEquals(parent.getName(), root.getChild("parent").getName());
        // testing for non root document
        assertEquals(doc.getName(), parent.getChild("child").getName());
        // testing for null param
        // assertEquals(doc.getName(), parent.getChild("").getName());
        prepareReTest();
        // retesting for non root document
        assertEquals(doc.getName(), parent.getChild("child").getName());
    }

    public void testGetChildren() throws Exception {
        // adding one more doc to the folder
        parent.addChild("child_01", TypeConstants.DOCUMENT);
        prepareReTest();
        // testing for a root folder
        List<String> rootChildrenIds = root.getChildrenIds();
        assertEquals(1, rootChildrenIds.size());
        assertEquals(parent.getUUID(), rootChildrenIds.get(0));
        Iterator<Document> iterator = root.getChildren();
        assertTrue(iterator.hasNext());
        assertEquals("parent", iterator.next().getName());
        assertFalse(iterator.hasNext());
        // testing for a non root folder
        List<String> childrenIds = parent.getChildrenIds();
        assertEquals(2, childrenIds.size());
        iterator = parent.getChildren();
        for (int i = 0; i < 2; i++) {
            assertTrue(iterator.hasNext());
            String name = iterator.next().getName();
            if (!"child".equals(name) && !"child_01".equals(name)) {
                fail(name);
            }
        }
        assertFalse(iterator.hasNext());
    }

    public void testHasChild() throws Exception {
        // testing for a root folder
        assertTrue(root.hasChild("parent"));
        // testing for a non root folder
        assertTrue(parent.hasChild("child"));
        prepareReTest();
        // testing for a non root folder
        assertTrue(parent.hasChild("child"));
    }

    public void testHasChildren() throws Exception {
        // testing for a root folder
        assertTrue(root.hasChildren());
        // testing for a non root folder
        assertTrue(parent.hasChildren());
        // testing for a non root document
        // assertFalse(doc.hasChildren());//TODO
        prepareReTest();
        // retesting for a non root folder
        assertTrue(parent.hasChildren());
        // retesting for a non root document
        // assertFalse(doc.hasChildren());//TODO
    }

    public void testAddChild() throws Exception {
        // testing for a non root folder
        assertNotNull(parent.addChild("child_01", TypeConstants.DOCUMENT));
        // testing for a non root document
        // assertNull(doc.addChild("child_from_testAddChild()_02",ITypeNames.DOCUMENT));//TODO
        prepareReTest();
    }

    public void testRemove() throws Exception {
        // test for a non root document
        doc.remove();
        // if we can add again the deleted doc, means that
        // the former does not exist anymore
        assertNotNull(parent.addChild("child", "File"));
    }

    public void testSave() throws Exception {
        // test for root document}
        doc.setString("title", "this is a test");
        session.save();
        assertNotNull(doc.getString("title"));
        // test for non root document
        doc.setString("title", "this is a test");
        doc.save();
        assertNotNull(doc.getString("title"));
    }

    public void testPath() throws Exception {
        String childName = "folderX";

        // test for root document
        Document child = root.addChild(childName, "Folder");
        assertEquals("/" + child.getName(), child.getPath());
        session.save();

        child = root.resolvePath("/" + child.getName());
        assertEquals("/" + child.getName(), child.getPath());

        prepareReTest();

        child = root.getChild(childName);
        assertEquals("/" + child.getName(), child.getPath());

        child = root.resolvePath("/" + child.getName());
        assertEquals("/" + child.getName(), child.getPath());

        child.remove();
    }

    public void testGetRepository() throws Exception {
        String childName = "folderX";
        Document child = root.addChild(childName, "Folder");
        assertNotNull(child.getRepository());
    }

}
