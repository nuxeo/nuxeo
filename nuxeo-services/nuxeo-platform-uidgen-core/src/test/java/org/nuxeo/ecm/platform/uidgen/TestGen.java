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

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventImpl;
import org.nuxeo.ecm.platform.uidgen.corelistener.DocUIDGeneratorListener;

public class TestGen extends UIDGeneratorTestCase {

    @Test
    public void testUIDGenerator() throws Exception {
        // create Geide doc
        String docTypeName = "GeideDoc";
        DocumentModel gdoc = createDocumentModel(docTypeName);
        gdoc.setProperty("dublincore", "title", "testGdoc_Title");
        gdoc.setProperty("dublincore", "description", "testGdoc_description");
        gdoc.setProperty("geide", "application_emetteur", "T4");

        final UIDGenerator generator = UIDGenFactory.createGeneratorForDocType(docTypeName);
        String uid = generator.createUID(gdoc);

        final int year = new GregorianCalendar().get(Calendar.YEAR);
        final String expected = "T4" + year + "00001";
        assertEquals(expected, uid);
    }

    @Test
    public void testUIDGenerator2() throws Exception {
        String docTypeName = "GeideDoc";
        DocumentModel gdoc = createDocumentModel(docTypeName);
        gdoc.setProperty("dublincore", "title", "testGdoc_Title");
        gdoc.setProperty("dublincore", "description", "testGdoc_description");
        gdoc.setProperty("geide", "application_emetteur", "T4");
        gdoc.setProperty("geide", "atelier_emetteur", "ATELIER");

        final UIDGenerator generator = UIDGenFactory.createGeneratorForDocType("GeideDoc");

        String uid = generator.createUID(gdoc);

        final int year = new GregorianCalendar().get(Calendar.YEAR);
        final String expected = "ATELIER" + year + "00001";
        assertEquals(expected, uid);
    }

    @Test
    public void testUIDGenerator3() throws Exception {
        // create Geide doc
        String docTypeName = "GeideDoc";
        DocumentModel gdoc = createDocumentModel(docTypeName);
        gdoc.setProperty("dublincore", "title", "testGdoc_Title");
        gdoc.setProperty("dublincore", "description", "testGdoc_description");
        gdoc.setProperty("geide", "application_emetteur", "T4");
        gdoc.setProperty("geide", "atelier_emetteur", "ATELIER3_");

        final UIDGenerator generator = UIDGenFactory.createGeneratorForDocType(docTypeName);

        for (int i = 1; i < 100; i++) {
            String uid = generator.createUID(gdoc);

            final int year = new GregorianCalendar().get(Calendar.YEAR);
            final String suffix = String.format("%05d", i);
            final String expected = "ATELIER3_" + year + suffix;
            assertEquals(expected, uid);
        }
    }

    /**
     * Test multiple UID properties set.
     */
    @Test
    public void testUIDGenerator3_multi() throws Exception {
        // create Geide doc
        String docTypeName = "GeideDoc";
        DocumentModel gdoc = createDocumentModel(docTypeName);
        gdoc.setProperty("dublincore", "title", "testGdoc_Title");
        gdoc.setProperty("dublincore", "description", "testGdoc_description");
        gdoc.setProperty("geide", "application_emetteur", "T4");
        gdoc.setProperty("geide", "atelier_emetteur", "ATELIER4_");

        for (int i = 1; i < 100; i++) {
            // local instantiation
            // TODO make it real

            EventContext ctx = new DocumentEventContext(null, null, gdoc);
            Event event = new EventImpl(DocumentEventTypes.DOCUMENT_CREATED, ctx);
            new DocUIDGeneratorListener().handleEvent(event);

            String uid = (String) gdoc.getProperty("uid", "uid");
            String uid2 = (String) gdoc.getProperty("other_uid_schema", "uid2");

            final int year = new GregorianCalendar().get(Calendar.YEAR);
            final String suffix = String.format("%05d", i);
            final String expected = "ATELIER4_" + year + suffix;
            assertEquals(expected, uid);
            assertEquals(expected, uid2);
        }
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

        Map<String, Object> uidMap = new HashMap<String, Object>();
        uidMap.put("uid", null);
        docModel.addDataModel(new DataModelImpl("uid", uidMap));

        Map<String, Object> uid2Map = new HashMap<String, Object>();
        uid2Map.put("uid2", null);
        docModel.addDataModel(new DataModelImpl("other_uid_schema", uid2Map));

        return docModel;
    }

}
