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

package org.nuxeo.ecm.core.repository.jcr.schema;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.naming.OperationNotSupportedException;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.NoSuchPropertyException;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.jcr.testing.CoreJCRConnectorTestConstants;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;
import org.nuxeo.ecm.core.schema.SchemaManager;

/**
 * @author  <a href="mailto:lgiura@nuxeo.com">Leonard Giura</a>
 *
 */
public class TestProperty extends RepositoryTestCase {

    SchemaManager typeMgr;
    Session session;
    Document root;
    Document doc;
    Map map;
    Property author;
    Property name;

    @SuppressWarnings("unchecked")
    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib(CoreJCRConnectorTestConstants.BUNDLE,
                "CoreTestExtensions.xml");
        // start the repository -> types will be automatically imported on
        // the first start of the repo
        session = getRepository().getSession(null);
        root =  session.getRootDocument();
        doc = root.addChild("doc1", "Book");
        author = doc.getProperty("book:author");
        name = author.getProperty("pName");
    }

    @Override
    protected void tearDown() throws Exception {
        doc.remove();
        session.close();
        session = null;
        root = null;
        doc = null;
        author = null;
        name = null;
        super.tearDown();
    }

    public void testSimpleProperty() throws DocumentException {
        Property price = doc.getProperty("book:price");

        // a non set property has isNull() - true
        assertTrue(author.isNull());
        assertTrue(price.isNull());
        assertNull(price.getValue());

        // when putting a value, isNull should return false
        price.setValue(800);
        assertEquals(800L, price.getValue());
        assertFalse(price.isNull());

        // set a String value instead of Long
        boolean found = true;
        try {
            price.setValue("800");
        } catch (ClassCastException e) {
            found = false;
        }
        assertFalse(found);

        // a nonexistent property is not found
        found = true;
        try {
            doc.getPropertyValue("inexistent");
        } catch (NoSuchPropertyException e) {
            found = false;
        }
        assertFalse(found);
    }

    public void testComplexProperty() throws DocumentException {
        // isNull() for the property returns true until a value is assigned to its descendants
        assertTrue(name.isNull());
        assertTrue(name.getProperty("FirstName").isNull());
        assertTrue(name.getProperty("LastName").isNull());

        name.getProperty("FirstName").setValue("Bogdan");
        name.getProperty("LastName").setValue("Stefanescu");

        // after setting the values, the isNull returns false
        assertFalse(name.isNull());
        assertFalse(name.getProperty("FirstName").isNull());
        assertFalse(name.getProperty("LastName").isNull());

        // the values assigned are well preserved and found
        assertEquals("Bogdan", name.getProperty("FirstName").getValue());
        assertEquals("Stefanescu", name.getProperty("LastName").getValue());
    }

    public void testGetProperties()
            throws DocumentException, OperationNotSupportedException {
        name.getProperty("FirstName").setValue("Bogdan");
        name.getProperty("LastName").setValue("Stefanescu");

        boolean found;
        Collection<Property> props = name.getProperties();
        // the function returns an iterator
        assertNotNull(props);
        found = false;
        // try to find in the returned iterator the FirstName
        for (Property p : props) {
            if (p.getName().equalsIgnoreCase("FirstName")) {
                found = true;
                // get the value of the found property
                assertEquals(name.getProperty("FirstName").getValue(), p.getValue());
            }
        }
        assertTrue(found);
    }

    @SuppressWarnings("unchecked")
    public void testExportMap()
            throws DocumentException, OperationNotSupportedException {
        doc.addChild("subdoc", "Book");

        name.getProperty("FirstName").setValue("Bogdan");
        name.getProperty("LastName").setValue("Stefanescu");

        // get the map with values
        map = doc.exportFlatMap(null);
        assertTrue(map.containsKey("book:author"));
        // map contains also unset components if they are declared by the schema
        assertTrue(map.containsKey("book:price"));

        // value found in the map should correspond to those in the property
        Map author = (Map) map.get("book:author");
        Map aname =  (Map) author.get("pName");

        assertNull(map.get("book:price"));
        assertNull(author.get("pAge"));
        assertEquals("Bogdan", aname.get("FirstName").toString());
        assertEquals("Stefanescu", aname.get("LastName").toString());
    }

    public void testImportMap()
            throws DocumentException, OperationNotSupportedException {
        Map<String, String> nameMap = new HashMap<String, String>();
        nameMap.put("FirstName", "NewBogdan");
        nameMap.put("LastName", "NewStefanescu");

        Map<String, Object> persMap = new HashMap<String, Object>();
        persMap.put("pName", nameMap);
        Map<String, Object> docmap = new HashMap<String, Object>();
        docmap.put("book:author", persMap);
        doc.importFlatMap(docmap);

        author = doc.getProperty("book:author");
        name = author.getProperty("pName");
        // check for the new values taken from the map
        assertEquals("NewBogdan",
                name.getProperty("FirstName").getValue());
    }

    public void testScalarList() throws Exception {
        Document mydoc = root.addChild("docWithList", "MyDocType");
        mydoc.setPropertyValue("participants", new String[] {"me", "you"});
        String[] values = (String[]) mydoc.getPropertyValue("participants");
        assertEquals(2, values.length);
        assertEquals("me", values[0]);
        assertEquals("you", values[1]);

        mydoc.setPropertyValue("participants", new String[] {});
        values = (String[]) mydoc.getPropertyValue("participants");
        assertEquals(0, values.length);

        mydoc.setPropertyValue("participants", null);
        values = (String[]) mydoc.getPropertyValue("participants");
        assertNull(values);
    }

}
