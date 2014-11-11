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
import static org.junit.Assert.assertNotNull;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.platform.uidgen.service.ServiceHelper;
import org.nuxeo.ecm.platform.uidgen.service.UIDGeneratorService;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestUIDGeneratorService extends NXRuntimeTestCase {

    final Log log = LogFactory.getLog(TestUIDGeneratorService.class);

    UIDGeneratorService service;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        NuxeoContainer.installNaming();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.event");
        deployBundle("org.nuxeo.ecm.core"); // for dublincore
        deployBundle("org.nuxeo.ecm.core.persistence");
        deployBundle("org.nuxeo.ecm.platform.uidgen.core");

        deployContrib("org.nuxeo.ecm.platform.uidgen.core.tests",
                "nxuidgenerator-test-contrib.xml");

        service = ServiceHelper.getUIDGeneratorService();
        assertNotNull(service);
    }

    @After
    public void tearDown() throws Exception {
        NuxeoContainer.uninstallNaming();
        super.tearDown();
    }

    @Test
    public void testUIDGenerator() throws Exception {
        String docTypeName = "GeideDoc";
        // create Geide doc
        DocumentModel gdoc = createDocumentModel(docTypeName);
        gdoc.setProperty("dublincore", "title", "testGdoc_Title");
        gdoc.setProperty("dublincore", "description", "testGdoc_description");
        gdoc.setProperty("geide", "application_emetteur", "T5");

        String uid = service.createUID(gdoc);

        final int year = new GregorianCalendar().get(Calendar.YEAR);
        final String expected = "T5" + year + "00001";
        assertEquals(expected, uid);
    }

    private static DocumentModel createDocumentModel(String type) {
        DocumentModelImpl docModel = new DocumentModelImpl(type);
        Map<String, Object> dcMap = new HashMap<String, Object>();
        dcMap.put("title", null);
        dcMap.put("description", null);
        docModel.addDataModel(new DataModelImpl("dublincore", dcMap));
        Map<String, Object> geideMap = new HashMap<String, Object>();
        geideMap.put("application_emetteur", null);
        docModel.addDataModel(new DataModelImpl("geide", geideMap));
        return docModel;
    }

}
