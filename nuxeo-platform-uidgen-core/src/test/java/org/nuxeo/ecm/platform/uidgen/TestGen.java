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

package org.nuxeo.ecm.platform.uidgen;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.SchemaNames;
import org.nuxeo.ecm.core.schema.TypeRef;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.core.schema.types.SchemaImpl;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestGen extends NXRuntimeTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployBundle("nuxeo-core-schema");
        deployBundle("nuxeo-core"); // for dublincore
        // define geide schema
        SchemaImpl sch = new SchemaImpl("geide");
        sch.addField(QName.valueOf("application_emetteur"), new TypeRef<Type>(SchemaNames.BUILTIN, StringType.ID));
        sch.addField(QName.valueOf("atelier_emetteur"), new TypeRef<Type>(SchemaNames.BUILTIN, StringType.ID));
        Framework.getLocalService(SchemaManager.class).registerSchema(sch);
    }

    private static DocumentModel createDocumentModel(String type) {
        DocumentModelImpl docModel = new DocumentModelImpl(type);
        Map<String, Object> dcMap = new HashMap<String, Object>();
        dcMap.put("title", null);
        dcMap.put("description", null);
        docModel.addDataModel(new DataModelImpl("dublincore", dcMap));
        Map<String, Object> geideMap = new HashMap<String, Object>();
        geideMap.put("application_emetteur", null);
        geideMap.put("atelier_emetteur", null);
        docModel.addDataModel(new DataModelImpl("geide", geideMap));
        return docModel;
    }

    public void testUIDGenerator() throws Exception {
        // create Geide doc
        String docTypeName = "GeideDoc";
        DocumentModel gdoc = createDocumentModel(docTypeName);
        gdoc.setProperty("dublincore", "title", "testGdoc_Title");
        gdoc.setProperty("dublincore", "description", "testGdoc_description");
        gdoc.setProperty("geide", "application_emetteur", "T4");

        final UIDSequencer sequencer = new DummySequencer();
        final UIDGenerator generator = UIDGenFactory.createGeneratorForDocType(
                docTypeName, sequencer);
        String uid = generator.createUID(gdoc);

        final int year = new GregorianCalendar().get(Calendar.YEAR);
        final String expected = "T4" + year + "00001";
        assertEquals(expected, uid);
    }

    public void testUIDGenerator2() throws Exception {
        String docTypeName = "GeideDoc";
        DocumentModel gdoc = createDocumentModel(docTypeName);
        gdoc.setProperty("dublincore", "title", "testGdoc_Title");
        gdoc.setProperty("dublincore", "description", "testGdoc_description");
        gdoc.setProperty("geide", "application_emetteur", "T4");
        gdoc.setProperty("geide", "atelier_emetteur", "ATELIER");

        final UIDSequencer sequencer = new DummySequencer();
        final UIDGenerator generator = UIDGenFactory.createGeneratorForDocType(
                "GeideDoc", sequencer);

        String uid = generator.createUID(gdoc);

        final int year = new GregorianCalendar().get(Calendar.YEAR);
        final String expected = "ATELIER" + year + "00001";
        assertEquals(expected, uid);
    }

    public void testUIDGenerator3() throws Exception {
        // create Geide doc
        String docTypeName = "GeideDoc";
        DocumentModel gdoc = createDocumentModel(docTypeName);
        gdoc.setProperty("dublincore", "title", "testGdoc_Title");
        gdoc.setProperty("dublincore", "description", "testGdoc_description");
        gdoc.setProperty("geide", "application_emetteur", "T4");
        gdoc.setProperty("geide", "atelier_emetteur", "ATELIER3_");

        final UIDSequencer sequencer = new DummySequencer();
        final UIDGenerator generator = UIDGenFactory.createGeneratorForDocType(
                docTypeName, sequencer);

        for (int i = 1; i < 100; i++) {
            String uid = generator.createUID(gdoc);

            final int year = new GregorianCalendar().get(Calendar.YEAR);
            final String suffix = String.format("%05d", i);
            final String expected = "ATELIER3_" + year + suffix;
            assertEquals(expected, uid);
        }
    }

}
