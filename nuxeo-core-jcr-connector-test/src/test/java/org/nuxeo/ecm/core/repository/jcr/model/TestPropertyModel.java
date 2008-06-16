/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.repository.jcr.testing.CoreJCRConnectorTestConstants;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Anahide Tchertchian
 *
 */
@SuppressWarnings("unchecked")
public class TestPropertyModel extends RepositoryOSGITestCase {

    DocumentModel doc;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        deployContrib(CoreJCRConnectorTestConstants.TESTS_BUNDLE,
                "test-core-types.xml");

        openRepository();
        doc = coreSession.createDocumentModel("TestDocument");
        doc.setPathInfo("/", "doc");
        doc = coreSession.createDocument(doc);
    }

    // NXP-2467
    public void testCreationWithDefaultPrefetch() throws Exception {
        DocumentModel doc = coreSession.createDocumentModel("TestDocumentWithDefaultPrefetch");
        doc.setPathInfo("/", "docwithDefaultPrefetch");
        coreSession.createDocument(doc);
    }

    public void testStringArray() throws Exception {
        assertNull(doc.getPropertyValue("tp:stringArray"));
        String[] values = new String[] { "foo", "bar" };
        doc.setPropertyValue("tp:stringArray", values);
        doc = coreSession.saveDocument(doc);
        assertTrue(Arrays.equals(values,
                (Object[]) doc.getPropertyValue("tp:stringArray")));
    }

    // NXP-2454
    public void testDateArray() throws Exception {
        assertNull(doc.getPropertyValue("tp:dateArray"));
        Calendar cal = Calendar.getInstance();
        cal.set(2008, 6, 10);
        Calendar[] values = new Calendar[] { cal };
        doc.setPropertyValue("tp:dateArray", values);
        doc = coreSession.saveDocument(doc);
        // currently returning long[] instead of Calendar[]
        assertTrue(Arrays.equals(values,
                (Object[]) doc.getPropertyValue("tp:dateArray")));
    }

    // NXP-2454
    public void testIntArray() throws Exception {
        assertNull(doc.getPropertyValue("tp:intArray"));
        Long[] values = new Long[] { 1L, 2L, 3L };
        doc.setPropertyValue("tp:intArray", values);
        doc = coreSession.saveDocument(doc);
        // currently returning long[], maybe this is the wanted behaviour (?)
        assertTrue(Arrays.equals(values,
                (Object[]) doc.getPropertyValue("tp:intArray")));
    }

    public void testComplexList() throws Exception {
        // not null on list
        assertTrue(doc.getPropertyValue("tp:complexList") instanceof List);
        assertEquals(0, ((List) doc.getPropertyValue("tp:complexList")).size());
        ArrayList<Map<String, Serializable>> values = new ArrayList<Map<String, Serializable>>();
        Map<String, Serializable> item = new HashMap<String, Serializable>();
        item.put("string", "foo");
        Calendar cal = Calendar.getInstance();
        cal.set(2008, 6, 10);
        item.put("date", cal);
        item.put("int", 3L);
        values.add(item);
        doc.setPropertyValue("tp:complexList", values);
        doc = coreSession.saveDocument(doc);

        Serializable actual = doc.getPropertyValue("tp:complexList");
        assertTrue(actual instanceof List);
        assertEquals(1, ((List) actual).size());
        assertEquals(item, ((List) actual).get(0));
    }

    // NXP-912, still to discuss if this is the wanted behaviour
    public void XXXtestNewBlob() throws Exception {
        // simple
        Object value = null;
        SchemaManager tm = Framework.getService(SchemaManager.class);
        Field field = tm.getField("tp:fileList");
        Type type = field.getType();
        Type itemType = ((ListType) type).getFieldType();
        value = itemType.newInstance();
        assertEquals(null, value);

        // complex
        field = tm.getField("tp:fileComplexList");
        type = field.getType();
        itemType = ((ListType) type).getFieldType();
        Map<String, Serializable> map = (Map) itemType.newInstance();
        assertEquals(2, map.size());
        assertTrue(map.containsKey("filename"));
        assertTrue(map.containsKey("blob"));
        assertEquals(null, map.get("filename"));
        assertEquals(null, map.get("blob"));
    }

    // NXP-2468
    public void XXXtestBlobListValue() throws Exception {
        // not null on list
        assertTrue(doc.getPropertyValue("tp:fileList") instanceof List);
        assertEquals(0, ((List) doc.getPropertyValue("tp:fileList")).size());
        ArrayList<Blob> values = new ArrayList<Blob>();
        StringBlob blob = new StringBlob("My content");
        values.add(blob);
        doc.setPropertyValue("tp:fileList", values);
        doc = coreSession.saveDocument(doc);

        Serializable actual = doc.getPropertyValue("tp:fileList");
        assertTrue(actual instanceof List);
        List<Blob> blobs = (List) actual;
        assertEquals(1, blobs.size());
        assertNotNull(blobs.get(0));
        assertTrue(blobs.get(0) instanceof Blob);
    }

    // NXP-2301
    public void testSubBlobValue() throws Exception {
        // not null on list
        assertTrue(doc.getPropertyValue("tp:fileComplexList") instanceof List);
        assertEquals(0,
                ((List) doc.getPropertyValue("tp:fileComplexList")).size());
        ArrayList<Map<String, Serializable>> values = new ArrayList<Map<String, Serializable>>();
        Map<String, Serializable> item = new HashMap<String, Serializable>();
        StringBlob blob = new StringBlob("My content");
        item.put("blob", blob);
        item.put("filename", "My filename");
        values.add(item);
        doc.setPropertyValue("tp:fileComplexList", values);
        doc = coreSession.saveDocument(doc);

        Object actual = doc.getPropertyValue("tp:fileComplexList");
        assertTrue(actual instanceof List);
        List<Map<String, Serializable>> items = (List) actual;
        assertEquals(1, items.size());
        assertNotNull(items.get(0));
        Map<String, Serializable> actualItem = items.get(0);
        assertEquals("My filename", actualItem.get("filename"));
        assertTrue(actualItem.get("blob") instanceof Blob);

        Object actualBlob = doc.getProperty("tp:fileComplexList/0/blob").getValue(
                Blob.class);
        assertTrue(actualBlob instanceof Blob);
    }

    // NXP-2318: i don't get what's supposed to be answered to these questions
    public void XXXtestArrayOrListProperties() throws Exception {
        Property prop = doc.getProperty("tp:stringArray");
        assertFalse(prop.isContainer());
        assertFalse(prop.isList());
        assertTrue(prop.isScalar());

        prop = doc.getProperty("tp:dateArray");
        assertFalse(prop.isContainer());
        assertFalse(prop.isList());
        assertTrue(prop.isScalar());

        prop = doc.getProperty("tp:intArray");
        assertFalse(prop.isContainer());
        assertFalse(prop.isList());
        assertTrue(prop.isScalar());

        prop = doc.getProperty("tp:complex");
        assertTrue(prop.isContainer());
        assertFalse(prop.isList());
        assertFalse(prop.isScalar());

        prop = doc.getProperty("tp:complexList");
        assertTrue(prop.isContainer());
        assertTrue(prop.isList());
        assertFalse(prop.isScalar());
    }

}
