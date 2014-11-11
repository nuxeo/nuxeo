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

package org.nuxeo.ecm.core.repository.jcr.versioning;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;
import org.nuxeo.ecm.core.versioning.DocumentVersion;
import org.nuxeo.ecm.core.versioning.DocumentVersionIterator;

public class TestVersioning extends RepositoryTestCase {

    protected Document doc;
    protected Document parent;
    protected Session session;
    protected Document root;

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        if (parent != null) {
            parent.remove();
        }
        if (session != null) {
            session.close();
        }
        doc = null;
        parent = null;
        session = null;
        root = null;
        super.tearDown();
    }

    public void prepareTest(String childName, String parentName)
            throws Exception {
        // creating the session
        session = getRepository().getSession(null);
        root = session.getRootDocument();
        // adding a folder and a child doc
        parent = root.addChild(parentName, "Folder");
        doc = parent.addChild(childName, "File");
    }

    public void prepareReTest(String childName, String parentName)
            throws Exception {
        session.close();
        session = getRepository().getSession(null);
        root = session.getRootDocument();
        parent = root.getChild(parentName);
        doc = parent.getChild(childName);
    }

    public void testCheckIn() throws Exception {
        prepareTest("child_from_testCheckIn", "parent_from_testCheckIn");
        try {
            doc.checkIn("firstCheckIn");
            //TODO: this is not true because the checkIn method is now autosaving the doc.
            //fail("checkIn should fail on a node with pending changes");
        } catch (DocumentException e) {
            // ok
        }
    }

    public void testIsCheckOut() throws Exception {
        prepareTest("child_from_testIsCheckOut", "parent_from_testIsCheckOut");

        // true on a new node
        assertTrue(doc.isCheckedOut());
        session.save();
        // true on a node without history version
        assertTrue(doc.isCheckedOut());

        doc.checkIn("1stcheckIn");
        // false after checkIn
        assertFalse(doc.isCheckedOut());

        doc.checkOut();
        // true after checkOut
        assertTrue(doc.isCheckedOut());

        doc.checkIn("2stcheckIn");
        // false after second checkIn
        assertFalse(doc.isCheckedOut());
    }

    public void testVersionsList() throws Exception {
        prepareTest("child_from_testVersionsList",
                "parent_from_testVersionsList");

        session.save();

        // XXX versionHistory is created after first save - JCR,
        // we do it at first checkin
        /// assertTrue(doc.getVersions().hasNext());

        doc.checkIn("label1");

        doc.checkOut();
        doc.setString("title", "the title");
        doc.save();
        doc.checkIn("label2", "desc2");

        doc.checkOut();
        doc.setString("description", "the description");
        doc.save();
        doc.checkIn(null, "desc3");

        DocumentVersionIterator iterator = doc.getVersions();
        // FIXME ... order is not guaranteed
        DocumentVersion v0 = iterator.nextDocumentVersion();
        assertNull("label not null: " + v0.getLabel(), v0.getLabel());
        assertNull("description not null: " + v0.getDescription(), v0.getDescription());

        DocumentVersion v1 = iterator.nextDocumentVersion();
        assertEquals("label1", v1.getLabel());
        assertNull(v1.getDescription());

        DocumentVersion v2 = iterator.nextDocumentVersion();
        assertEquals("label2", v2.getLabel());
        assertEquals("desc2", v2.getDescription());

        DocumentVersion v3 = iterator.nextDocumentVersion();
        assertNull(v3.getLabel());
        assertEquals("desc3", v3.getDescription());
    }

    public void testRestore() throws Exception {
        prepareTest("child_from_testRestore", "parent_from_testRestore");
        // create two versions
        session.save();
        doc.checkIn("one");

        doc.checkOut();
        doc.setString("title", "firstValue");
        doc.save();
        doc.checkIn("two");

        doc.checkOut();
        doc.removeProperty("title");
        doc.setString("title", "secondValue");
        doc.save();
        doc.checkIn("three");
        prepareReTest("child_from_testRestore", "parent_from_testRestore");
        assertEquals("secondValue", doc.getString("title"));

        doc.restore("two");
        assertEquals("firstValue", doc.getString("title"));

        doc.restore("three");
        assertEquals("secondValue", doc.getString("title"));
    }

    public void testReadOldVersion() throws Exception {
        prepareTest("child_from_ReadOldVersion", "parent_from_ReadOldVersion");
        session.save();
        doc.checkIn("one");

        doc.checkOut();
        doc.setString("title", "firstTitleValue");
        doc.save();
        doc.checkIn("two");

        doc.checkOut();
        doc.removeProperty("title");
        doc.setString("title", "secondTitleValue");
        doc.save();
        doc.checkIn("three");

        Document oldDoc = doc.getVersion("two");
        assertNotNull(oldDoc);
        assertEquals("firstTitleValue", oldDoc.getString("title"));
        assertEquals("secondTitleValue", doc.getString("title"));
    }

    public void testParent() throws Exception {
        prepareTest("child_from_testIsCheckOut", "parent_from_testIsCheckOut");

        session.save();

        Document parent = doc.getParent();

        parent.setPropertyValue("title", "parent_title");

        doc.checkIn("1stcheckIn");
        doc.checkOut();

        Document ver = doc.getVersion("1stcheckIn");

        Document verParent = ver.getParent();

        assertEquals("parent_title", verParent.getPropertyValue("title"));
    }

    public void testDeleteVersion() throws Exception {
        prepareTest("child_from_testIsCheckOut", "parent_from_testIsCheckOut");

        session.save();

        Document parent = doc.getParent();

        parent.setPropertyValue("title", "parent_title");

        doc.checkIn("1stcheckIn");
        doc.checkOut();

        Document ver = doc.getVersion("1stcheckIn");

        assertNotNull(ver);

        session.save();
        ver.remove();
        session.save();
    }
}
