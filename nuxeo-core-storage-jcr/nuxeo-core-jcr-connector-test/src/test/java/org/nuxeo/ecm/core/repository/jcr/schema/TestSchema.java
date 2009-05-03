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

import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.jcr.testing.CoreJCRConnectorTestConstants;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;

/**
 * Tests schemas and doc types in the case of
 * automatic registration (via XSD files) of types.
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TestSchema extends RepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib(CoreJCRConnectorTestConstants.BUNDLE,
                "CoreTestExtensions.xml");
    }


    public void testXSDTypeRegistration() throws Exception {

        // start the repository -> types will be automatically imported on the first start of the repo

        Session session = getRepository().getSession(null);
        Document root = session.getRootDocument();

        Document doc = root.addChild("doc1", "MyDocType");
        doc.setString("my:string", "string value");
        doc.setLong("my:integer", 10);

        Property name = doc.getProperty("my:name");
        name.getProperty("FirstName").setValue("Bogdan");
        name.getProperty("LastName").setValue("Stefanescu");

        try {
            doc.setString("dummy", "bla bla");
            fail("Test failed: dummy properties was not defined by this schema");
        } catch (Exception e) {}

        assertEquals("string value", doc.getString("my:string"));
        assertEquals(10, doc.getLong("my:integer"));
        name = doc.getProperty("my:name");
        assertEquals("Bogdan", name.getProperty("FirstName").getValue());
        assertEquals("Stefanescu", name.getProperty("LastName").getValue());

        try {
            doc.setString("Nickname", "manzu");
            fail("Test failed: Nickname was not defined in this complex type");
        } catch (Exception e) {}
    }

    public void testDefaultValue() throws Exception {
        Session session = getRepository().getSession(null);
        Document root = session.getRootDocument();

        Document doc = root.addChild("doc2", "MyDocType");
        String value = doc.getString("my:testDefault");
        assertEquals("the default value", value);

        doc = root.addChild("doc3", "MyDocType");
        value = (String) doc.getProperty("my:testDefault").getValue();
        assertEquals("the default value", value);
    }

    public void testSharedNamespaces() throws Exception {
        Session session = getRepository().getSession(null);
        Document root = session.getRootDocument();

        Document doc = root.addChild("doc-ns", "SharedNS");
        doc.setString("ns:field1", "v1");
        String value = doc.getString("ns:field1");
        assertEquals("v1", value);
        doc.setString("ns:field2", "v2");
        value = doc.getString("ns:field2");
        assertEquals("v2", value);
    }

    public void testDirtyFlag() throws Exception {
        Session session = getRepository().getSession(null);
        Document root = session.getRootDocument();

        Document doc = root.addChild("doc-dirty", "File");
        assertTrue(doc.isDirty());

        doc.setDirty(false);
        assertFalse(doc.isDirty());

        doc.setDirty(true);
        assertTrue(doc.isDirty());
    }

}
