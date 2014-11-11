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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.jcr.properties.JCRComplexListProperty;
import org.nuxeo.ecm.core.repository.jcr.testing.CoreJCRConnectorTestConstants;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;

/**
 * Unit test for ComplexProperty.
 *
 * @author <a href="mailto:lgiura@nuxeo.com">Leonard Giura</a>
 */
public class TestComplexProperty extends RepositoryTestCase {

    private Document doc;

    private Document parent;

    private Session session;

    private Document root;

    private Property docProp;

// TODO this test must be refactored
// ------------------------------------------------------

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
        // adding complex property to a normal doc
        docProp = doc.getProperty("my:name");
    }

    @Override
    public void tearDown() throws Exception {
        parent.remove();
        session.close();
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
        docProp = doc.getProperty("my:name");
    }

    public void testGetType() throws Exception {
        assertEquals("Name", docProp.getType().getName());
        prepareReTest();
        assertEquals("Name", docProp.getType().getName());
    }

    public void testIsNull() throws Exception {
        String testName = "FirstName";
        assertTrue(!docProp.isPropertySet(testName));
        assertTrue(docProp.isNull());
        docProp.getProperty(testName).setValue("");
        assertTrue(docProp.isPropertySet(testName));
        assertTrue(!docProp.isNull());
    }

    @SuppressWarnings("unchecked")
    public void testComplexList() throws Exception {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("firstname", "foo");
        map.put("lastname", "hah");
        list.add(map);
        Property prop = doc.getProperty("testlist:persons");
        prop.setValue(list);
        session.save();

        // close and reopen
        session.close();
        session = getRepository().getSession(null);
        root = session.getRootDocument();
        parent = root.getChild("child_folder_from_test");
        doc = parent.getChild("child_document_from_test");
        prop = doc.getProperty("testlist:persons");
        assertNotNull(prop);
        JCRComplexListProperty clprop = (JCRComplexListProperty) prop;
        List<Object> cl = clprop.getList();
        assertNotNull(cl);
        Map<String, String> m = (Map<String, String>) cl.get(0);
        assertEquals("foo", m.get("firstname"));

        // rewrite the list
        list = new ArrayList<Map<String, Object>>();
        map = new HashMap<String, Object>();
        map.put("firstname", "bar");
        list.add(map);
        prop.setValue(list);
        session.save();
    }

//    public void testAddComplexProperty() throws Exception {
//        String testName = "testPropertyName";
//        // test root
//        assertNotNull(wsProp.getProperty(testName, TypeNames.MAP));
//        wsProp.removeProperty(testName);
//        // test non root
//        assertNotNull(docProp.getProperty(testName, TypeNames.MAP));
//        docProp.removeProperty(testName);
//        prepareReTest();
//        // test non root
//        assertNotNull(docProp.getProperty(testName, TypeNames.MAP));
//        docProp.getCompositeProperty(testName);
//
//    }
//
//
//    public void testGetBoolean() throws Exception {
//        String name = "testPropertyName";
//        // test root
//        wsProp.setBoolean(name, true);
//        assertTrue(wsProp.getBoolean(name));
//        wsProp.removeProperty(name);
//        // test non root
//        docProp.setBoolean(name, true);
//        assertTrue(docProp.getBoolean(name));
//        prepareReTest();
//        // retest non root
//        assertTrue(docProp.getBoolean(name));
//        docProp.removeProperty(name);
//    }
//
//    public void testGetDate() throws Exception {
//        String name = "testPropertyName";
//        Calendar now = Calendar.getInstance();
//        // test root
//        wsProp.setDate(name, now);
//        assertEquals(now, wsProp.getDate(name));
//        wsProp.removeProperty(name);
//        // test non root
//        docProp.setDate(name, now);
//        assertEquals(now, docProp.getDate(name));
//        prepareReTest();
//        // retest non root
//        assertEquals(now, docProp.getDate(name));
//        docProp.removeProperty(name);
//    }
//
//    public void testGetDouble() throws Exception {
//        String name = "testPropertyName";
//        Double dblNr = Double.MAX_VALUE;
//        // test root
//        wsProp.setDouble(name, dblNr);
//        assertEquals(dblNr, wsProp.getDouble(name));
//        wsProp.removeProperty(name);
//        // test non root
//        docProp.setDouble(name, dblNr);
//        assertEquals(dblNr, docProp.getDouble(name));
//        prepareReTest();
//        // retest non root
//        assertEquals(dblNr, docProp.getDouble(name));
//        docProp.removeProperty(name);
//    }
//
//    public void testGetProperty() throws Exception {
//        String name = "testPropertyName";
//        String value = "value";
//        // test root
//        wsProp.setString(name, value);
//        assertNotNull(wsProp.getString(name));
//        wsProp.removeProperty(name);
//        // test non root
//        docProp.setString(name, value);
//        assertNotNull(docProp.getString(name));
//        prepareReTest();
//        // retest non root
//        assertNotNull(docProp.getString(name));
//        docProp.removeProperty(name);
//    }
//
//    public void testGetLong() throws Exception {
//        String name = "testPropertyName";
//        long lngNr = Long.MAX_VALUE;
//        // test root
//        wsProp.setLong(name, lngNr);
//        assertEquals(lngNr, wsProp.getLong(name));
//        wsProp.removeProperty(name);
//        // test non root
//        docProp.setLong(name, lngNr);
//        assertEquals(lngNr, docProp.getLong(name));
//        prepareReTest();
//        // retest non root
//        assertEquals(lngNr, docProp.getLong(name));
//        docProp.removeProperty(name);
//    }
//
//    public void testGetComplexProperty() throws Exception {
//        testAddComplexProperty();
//    }
//
//    public void testGetSimpleProperty() throws Exception {
//        String name = "testPropertyName";
//        String value = "value";
//        // test root
//        wsProp.setString(name, value);
//        assertNotNull(wsProp.getString(name));
//        wsProp.removeProperty(name);
//        // test non root
//        docProp.setString(name, value);
//        assertNotNull(docProp.getString(name));
//        prepareReTest();
//        // retest non root
//        assertNotNull(docProp.getString(name));
//        docProp.removeProperty(name);
//
//    }
//
//    public void testGetString() throws Exception {
//        String name = "testPropertyName";
//        String value = "value";
//        // test root
//        wsProp.setString(name, value);
//        assertEquals(value, wsProp.getString(name));
//        wsProp.removeProperty(name);
//        // test non root
//        docProp.setString(name, value);
//        assertEquals(value, docProp.getString(name));
//        prepareReTest();
//        // retest non root
//        assertEquals(value, docProp.getString(name));
//        docProp.removeProperty(name);
//
//    }
//
//
//    public void testSetBoolean() throws Exception {
//        testGetBoolean();
//    }
//
//    public void testSetDate() throws Exception {
//        testGetDate();
//    }
//
//    public void testSetDouble() throws Exception {
//        testGetDouble();
//    }
//
//    public void testSetLong() throws Exception {
//        testGetLong();
//    }
//
//    public void testSetString() throws Exception {
//        testGetString();
//    }
//
//    public void testRemoveProperty() throws Exception {
//        String name = "testPropertyName";
//        // test root
//        wsProp.getProperty(name,TypeNames.MAP);
//        wsProp.removeProperty(name);
//        // test non root
//        docProp.getProperty(name, TypeNames.MAP);
//        prepareReTest();
//        //retest non root
//        docProp.removeProperty(name);
//    }

}
