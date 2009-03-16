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

import java.util.Calendar;

import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.jcr.testing.CoreJCRConnectorTestConstants;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;

/**
 * @author <a href="mailto:lgiura@nuxeo.com">Leonard Giura</a>
 *
 */

public class TestMapField extends RepositoryTestCase {

    private Document doc;

    private Document parent;

    private Document root;
    private Session session;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib(CoreJCRConnectorTestConstants.BUNDLE,
                "CoreTestExtensions.xml");
        // creating the session
        session = getRepository().getSession(null);
        root = session.getRootDocument();
        // adding a folder and a child doc
        parent = root.addChild("child_folder_from_test", "Folder");
        doc = parent.addChild("child_document_from_test", "MyDocType");
    }

    @Override
    public void tearDown() throws Exception {
        parent.remove();
        session.close();
        doc = null;
        parent = null;
        root = null;
        session = null;
        super.tearDown();
    }

    /**
     * Closes the session, then opens it again in order to test
     * the preservation of the actions made in the first step of the testing.
     */
    public void prepareReTest() throws Exception {
        session.close();
        session = getRepository().getSession(null);
        root = session.getRootDocument();
        parent = root.getChild("child_folder_from_test");
        doc = parent.getChild("child_document_from_test");
    }

    public void testHasField() throws Exception {
        String testField = "title";
        // test for document
        doc.setString(testField, "");
        assertTrue(doc.isPropertySet(testField));
        // assertFalse(metaData.hasField(testField+" "));
        // TODO - DocumentException instead of false
        prepareReTest();
        //retest for document
        assertTrue(doc.isPropertySet(testField));
    }

    public void testGetField() throws Exception {
        String testField = "title";
        //test for document
        doc.setString(testField, "");
        assertNotNull(doc.getString(testField));
        prepareReTest();
        //retest for document
        assertNotNull(doc.getString(testField));
    }

    public void testGetString() throws Exception {
        String testField = "title";
        String testValue = "test value";
        //test for document
        doc.setString(testField, testValue);
        assertEquals(testValue, doc.getString(testField));
        prepareReTest();
        //retest for document
        assertEquals(testValue, doc.getString(testField));
    }

    public void testGetBoolean() throws Exception {
        String testField = "my:boolean";
        boolean testValue = true;
        //test for document
        doc.setBoolean(testField, testValue);
        assertTrue(doc.getBoolean(testField));
        prepareReTest();
        //retest for document
        assertTrue(doc.getBoolean(testField));
    }

    public void testGetDouble() throws Exception {
        String testField = "my:double";
        double testValue = 0.5;
        //test for document
        doc.setDouble(testField, testValue);
        assertEquals(testValue, doc.getDouble(testField), 0);
        prepareReTest();
        //retest for document
        assertEquals(testValue, doc.getDouble(testField), 0);
    }

    public void testGetLong() throws Exception {
        String testField = "my:long";
        long testValue = Long.MAX_VALUE;
        //test for document
        doc.setLong(testField, testValue);
        assertEquals(testValue, doc.getLong(testField));
        prepareReTest();
        //retest for document
        assertEquals(testValue, doc.getLong(testField));
    }

    public void testGetDate() throws Exception {
        String testField = "my:date";
        Calendar testValue = Calendar.getInstance();
        //test for document
        doc.setDate(testField, testValue);
        assertEquals(testValue, doc.getDate(testField));
        prepareReTest();
        //retest for document
        assertEquals(testValue, doc.getDate(testField));
    }


/*
    public void testGetArray() throws Exception {
     String testField = "testField";
     Object[] testValue={Calendar.getInstance(),Calendar.getInstance()};
     //test for root
     rootMetaData.setArray(testField, testValue);
     assertNotNull(rootMetaData.getArray(testField));
     //test for document
     //docMetaData.setArray(testField, testValue);
     //assertNotNull(docMetaData.getArray(testField));
     //prepareReTest();
     //retest for document
     //assertNotNull(docMetaData.getArray(testField));

     }*/

    /* public void testSetArray() { fail("Not yet implemented"); // TODO }
     */
}
