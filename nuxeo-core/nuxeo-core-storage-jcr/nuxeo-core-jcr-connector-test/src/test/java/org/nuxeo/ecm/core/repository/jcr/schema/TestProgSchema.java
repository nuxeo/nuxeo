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
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;
import org.nuxeo.ecm.core.schema.BuiltinTypes;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.DocumentTypeImpl;
import org.nuxeo.ecm.core.schema.NXSchema;
import org.nuxeo.ecm.core.schema.Namespace;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.SchemaNames;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.ComplexTypeImpl;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.SchemaImpl;
import org.nuxeo.ecm.core.schema.types.SimpleType;
import org.nuxeo.ecm.core.schema.types.SimpleTypeImpl;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;

/**
 * Tests schemas and doc types in the case of
 * programmatic registration of types.
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TestProgSchema extends RepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();

        SchemaManager typeMgr = NXSchema.getSchemaManager();

        // simple types don't need to be registered in JCR
        SimpleType stype = new SimpleTypeImpl(StringType.INSTANCE, SchemaNames.BUILTIN,  "MyString");

        ComplexType ctype = new ComplexTypeImpl((ComplexType) null, SchemaNames.BUILTIN,  "Name");
        ctype.addField("FirstName", stype.getRef());
        ctype.addField("LastName", StringType.INSTANCE.getRef());

        Schema schema = new SchemaImpl("MySchema",
                new Namespace("http://www.nuxeo.org/ecm/schemas/MySchema", "my"));
        schema.addField("string", StringType.INSTANCE.getRef());
        schema.addField("integer", LongType.INSTANCE.getRef());
        schema.addField("name", ctype.getRef());

        Schema defSchema = new SchemaImpl("common", Namespace.DEFAULT_NS);
        defSchema.addField("title", StringType.INSTANCE.getRef());
        defSchema.addField("description", StringType.INSTANCE.getRef());

        DocumentType docType = new DocumentTypeImpl(
                BuiltinTypes.T_DOCUMENT, "MyDocType",
                new String[] {defSchema.getName(), schema.getName()}, null);

        // register our custom types
        typeMgr.registerType(stype);
        typeMgr.registerType(ctype);
        typeMgr.registerSchema(schema);
        typeMgr.registerSchema(defSchema);
        typeMgr.registerDocumentType(docType);

        // force loading new types into the repository by re-initializing the repo
        getRepository().initialize();
    }

    public void testTypeRegistration() throws Exception {

        // start the repository -> types will be automatically imported on the first start of the repo

        Session session = getRepository().getSession(null);
        Document root = session.getRootDocument();

        Document doc = root.addChild("doc1", "MyDocType");
        doc.setString("my:string", "string value");
        doc.setLong("my:integer", 10L);

        Property name = doc.getProperty("my:name");
        name.getProperty("FirstName").setValue("Bogdan");
        name.getProperty("LastName").setValue("Stefanescu");

        try {
            doc.setString("dummy", "bla bla");
            fail("Test failed: dummy properties was not defined by this schema");
        } catch (Exception e) {}

        assertEquals("string value", doc.getString("my:string"));
        assertEquals(10L, doc.getLong("my:integer"));
        name = doc.getProperty("my:name");
        assertEquals("Bogdan", name.getProperty("FirstName").getValue());
        assertEquals("Stefanescu", name.getProperty("LastName").getValue());

        try {
            doc.setString("Nickname", "manzu");
            fail("Test failed: Nickname was not defined in this complex type");
        } catch (Exception e) {}
    }

}
