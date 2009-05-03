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

import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.jcr.JCRSession;
import org.nuxeo.ecm.core.repository.jcr.TypeImporter;
import org.nuxeo.ecm.core.repository.jcr.testing.CoreJCRConnectorTestConstants;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.NXSchema;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.SchemaNames;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.ComplexTypeImpl;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;

/**
 *
 * @author <a href="mailto:lgiura@nuxeo.com">Leonard Giura</a>
 */
public class TestReRegister extends RepositoryTestCase {
    private Session session;
    private Document root;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        session = getRepository().getSession(null);
        root = session.getRootDocument();
        assertNotNull(root);
    }

    @Override
    public void tearDown() throws Exception {
        session.close();
        session = null;
        root = null;
        super.tearDown();
    }

    public void testReregister() throws Exception  {
        SchemaManager typeMgr = NXSchema.getSchemaManager();

        DocumentType docType =  typeMgr.getDocumentType("MyDocType");
        assertNull(docType); // MyDocType doesn't exists

        // close repository and reopen it
        session.close();
        releaseRepository();
        undeployContrib(CoreJCRConnectorTestConstants.BUNDLE,
                "DemoRepository.xml");
        getRepository(); // reopen the same repository as before

        // deploy a new doctype (MyDocType)
        deployContrib(CoreJCRConnectorTestConstants.BUNDLE, "CoreTestExtensions.xml");

        // it should be registered in type manager
        docType =  typeMgr.getDocumentType("MyDocType");
        assertNotNull(docType);

        // create a new session and re-register types
        session = getSession();
        TypeImporter ti = new TypeImporter((JCRSession) session);
        ti.registerTypes(typeMgr);

        // test that the corresponding jcr node type was registered
        assertTrue(ti.isDocTypeRegistered(docType));
    }

    public void testComplexType() throws Exception {
        SchemaManager typeMgr = NXSchema.getSchemaManager();
        TypeImporter ti = new TypeImporter((JCRSession) session);

        // manually create a type
        ComplexTypeImpl ctype = new ComplexTypeImpl((ComplexType) null,
                SchemaNames.BUILTIN, "myNewType");

        assertFalse(ti.isTypeRegistered(ctype));

        typeMgr.registerType(ctype);

        // type is not existting in JCR
        assertFalse(ti.isTypeRegistered(ctype));

        ti.registerTypes(typeMgr);

        // now type should exists in JCR too
        assertTrue(ti.isTypeRegistered(ctype));
    }

    public void testModify() throws Exception {
        root = session.getRootDocument();
        root.addChild("theDocToTest", "File");

        SchemaManager typeMgr = NXSchema.getSchemaManager();
        TypeImporter ti = new TypeImporter((JCRSession) session);

        Schema schema = typeMgr.getSchema("common");
        // now type should exists in JCR too
        assertTrue(ti.isSchemaRegistered(schema));

        Document doc = root.getChild("theDocToTest");
        try {
            doc.getString("theNewField");
            fail();
        } catch (Exception e) {
            // this should throw an exception since the property doesn't exist in the schema
        }

        // let's modify the type
        schema.addField("theNewField", StringType.INSTANCE.getRef());
        typeMgr.registerSchema(schema);

        ti = new TypeImporter((JCRSession) session);
        ti.registerTypes(typeMgr);

        doc = root.getChild("theDocToTest");
        // now the doc should have a new field
        doc.setString("theNewField", "theValue");

        session.save();

        assertEquals("theValue", doc.getString("theNewField"));


        restartRepository();

        // now the property should be removed by the common schema from the deployment files
        doc = root.addChild("theDocToTest2", "File");
        try {
            doc.getString("theNewField");
            fail();
        } catch (Exception e) {
            // this should throw an exception since the property doesn't exists in the schema
        }
    }

    private void restartRepository() throws Exception {
        // close repository and reopen it
        session.close();
        releaseRepository();
        //redeploy components
        undeployContrib(CoreJCRConnectorTestConstants.BUNDLE,
                "DemoRepository.xml");
        undeployContrib(CoreJCRConnectorTestConstants.BUNDLE,
                "test-CoreExtensions.xml");
        undeployContrib(CoreJCRConnectorTestConstants.CORE_BUNDLE, "OSGI-INF/CoreService.xml");
        undeployContrib(CoreJCRConnectorTestConstants.BUNDLE, "TypeService.xml");
        undeployContrib(CoreJCRConnectorTestConstants.CORE_BUNDLE, "OSGI-INF/RepositoryService.xml");

        deployContrib(CoreJCRConnectorTestConstants.CORE_BUNDLE, "OSGI-INF/CoreService.xml");
        deployContrib(CoreJCRConnectorTestConstants.BUNDLE, "TypeService.xml");
        deployContrib(CoreJCRConnectorTestConstants.CORE_BUNDLE, "OSGI-INF/RepositoryService.xml");
        deployContrib(CoreJCRConnectorTestConstants.BUNDLE,
                "test-CoreExtensions.xml");

        getRepository(); // reopen the same repository as before
        session = getSession();
        root = session.getRootDocument();
    }

}
