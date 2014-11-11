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
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr.model;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ListDiff;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.jcr.testing.CoreJCRConnectorTestConstants;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;

/**
 * Warning: this is more integration test than unit tests.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TestRepository extends RepositoryTestCase {

    Document doc;

    Session session;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib(CoreJCRConnectorTestConstants.BUNDLE,
                "CoreTestExtensions.xml");

        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        assertNotNull(ctx);
        session = getRepository().getSession(ctx);
        Document root = session.getRootDocument();

        if (root.hasChild("myDoc")) {
            doc = root.getChild("myDoc");
        } else {
            doc = root.addChild("myDoc", "MyDocType");
        }
    }

    @Override
    public void tearDown() throws Exception {
        doc.remove();
        session.close();

        super.tearDown();
    }

    public void testScalarProperties() throws Exception {
        doc.setString("my:string", "myValue");
        assertEquals("myValue", doc.getString("my:string"));

        doc.setDouble("my:double", 1.2);
        assertEquals(1.2, doc.getDouble("my:double"));

        Calendar now = Calendar.getInstance();
        doc.setDate("my:date", now);
        assertEquals(doc.getDate("my:date"), now);

        doc.setLong("my:long", 123);
        assertEquals(123, doc.getLong("my:long"));

        doc.setBoolean("my:boolean", true);
        assertTrue(doc.getBoolean("my:boolean"));

        doc.setDouble("my:double", 222.3);
        assertEquals(222.3, doc.getDouble("my:double"));

        doc.setString("my:string", "modified string");
        assertEquals("modified string", doc.getString("my:string"));

        assertTrue(doc.isPropertySet("my:string"));
        doc.removeProperty("my:string");
        assertTrue(!doc.isPropertySet("my:string"));
    }

    public void testComplexProperties() throws Exception {
        Property name = doc.getProperty("my:name");
        name.getProperty("FirstName").setValue("val1");
        name.getProperty("LastName").setValue("val2");

        // this sort of constructs are no more working
        // Assert.assertEquals("val1", doc.getString("my:name/FirstName"));
        // Assert.assertEquals("val2", doc.getString("my:name/LastName"));

        assertEquals("val1",
                doc.getProperty("my:name").getProperty("FirstName").getValue());
        assertEquals("val2",
                doc.getProperty("my:name").getProperty("LastName").getValue());

        doc.getProperty("my:name").getProperty("LastName").setValue(
                "val2-modified");

        assertEquals("val1",
                doc.getProperty("my:name").getProperty("FirstName").getValue());
        assertEquals("val2-modified", doc.getProperty("my:name").getProperty(
                "LastName").getValue());
    }

    @SuppressWarnings("unchecked")
    public void testComplexListProperties() throws Exception {
        Property atts = doc.getProperty("attachments");
        List list = (List) atts.getValue();
        assertNull(list);
        ListDiff diff = new ListDiff();
        Blob blob = new StringBlob("value1");
        diff.add(blob);
        atts.setValue(diff);

        list = (List) atts.getValue();
        assertNotNull(list);
        assertEquals(1, list.size());
        String v = ((Blob) list.get(0)).getString();
        assertEquals("value1", v);

        // add a new element
        diff = new ListDiff();
        diff.add(new StringBlob("value2"));
        atts.setValue(diff);

        list = (List) atts.getValue();
        assertEquals(2, list.size());
        v = ((Blob) list.get(0)).getString();
        assertEquals("value1", v);
        v = ((Blob) list.get(1)).getString();
        assertEquals("value2", v);

        // move elements
        diff = new ListDiff();
        diff.move(0, 1);
        atts.setValue(diff);

        list = (List) atts.getValue();
        assertEquals(2, list.size());
        v = ((Blob) list.get(0)).getString();
        assertEquals("value2", v);
        v = ((Blob) list.get(1)).getString();
        assertEquals("value1", v);

        // move elements
        diff = new ListDiff();
        diff.move(1, 0);
        atts.setValue(diff);

        list = (List) atts.getValue();
        assertEquals(2, list.size());
        v = ((Blob) list.get(0)).getString();
        assertEquals("value1", v);
        v = ((Blob) list.get(1)).getString();
        assertEquals("value2", v);

        // modify elements, non regression test for NXP-1024
        diff = new ListDiff();
        diff.modify(0, new StringBlob("value1-mod"));
        atts.setValue(diff);

        list = (List) atts.getValue();
        assertEquals(2, list.size());
        v = ((Blob) list.get(0)).getString();
        assertEquals("value1-mod", v);
        v = ((Blob) list.get(1)).getString();
        assertEquals("value2", v);

        // modify + remove element
        diff = new ListDiff();
        diff.modify(0, new StringBlob("value2-mod"));
        diff.remove(1);
        atts.setValue(diff);

        list = (List) atts.getValue();
        assertNotNull(list);
        assertEquals(1, list.size());
        v = ((Blob) list.get(0)).getString();
        assertEquals("value2-mod", v);

        // insert a new element
        diff = new ListDiff();
        diff.insert(0, new StringBlob("value1"));
        atts.setValue(diff);

        list = (List) atts.getValue();
        assertEquals(2, list.size());
        v = ((Blob) list.get(0)).getString();
        assertEquals("value1", v);
        v = ((Blob) list.get(1)).getString();
        assertEquals("value2-mod", v);

        diff = new ListDiff();
        diff.removeAll();
        atts.setValue(diff);

        list = (List) atts.getValue();
        assertEquals(0, list.size());
    }

}
