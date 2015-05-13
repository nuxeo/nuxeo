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

import org.junit.Assert;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.platform.uidgen.jpa.JPAUIDSequencerImpl;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@LocalDeploy("org.nuxeo.ecm.platform.uidgen.core:nxuidgenerator-seqgen-test-contrib.xml")
public class TestCustomGen extends UIDGeneratorTestCase {

    @Test
    public void testUIDGenerator() throws Exception {

        UIDSequencer seq = service.getSequencer("hibernateSequencer");
        Assert.assertNotNull(seq);
        Assert.assertTrue(seq.getClass().isAssignableFrom(JPAUIDSequencerImpl.class));


        seq = service.getSequencer("dummySequencer");
        Assert.assertNotNull(seq);
        Assert.assertTrue(seq.getClass().isAssignableFrom(DummyUIDSequencerImpl.class));

        seq = service.getSequencer();
        Assert.assertNotNull(seq);
        Assert.assertTrue(seq.getClass().isAssignableFrom(DummyUIDSequencerImpl.class));


        String docTypeName = "GeideDoc";
        DocumentModel gdoc = createDocumentModel(docTypeName);
        gdoc.setProperty("dublincore", "title", "testGdoc_Title");
        gdoc.setProperty("dublincore", "description", "testGdoc_description");
        gdoc.setProperty("geide", "application_emetteur", "T4");

        final UIDGenerator generator = UIDGenFactory.createGeneratorForDocType(docTypeName);
        String uid = generator.createUID(gdoc);

        final int year = new GregorianCalendar().get(Calendar.YEAR);
        final String expected = "T4" + year + "00001";
        Assert.assertEquals(expected, uid);
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
