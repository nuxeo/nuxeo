/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Laurent Doguin - unit test
 */

package org.nuxeo.opensocial.gadgets.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.opensocial.gadgets.service.api.GadgetDeclaration;
import org.nuxeo.opensocial.gadgets.service.api.GadgetService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestGadgetService extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.opensocial.gadgets.core");
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.directory.api");
        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.directory.sql");
        deployBundle("org.nuxeo.opensocial.service");
        deployContrib("org.nuxeo.opensocial.gadgets.core.test",
                "OSGI-INF/gadget-contrib.xml");
        deployContrib("org.nuxeo.opensocial.gadgets.core.test",
        "OSGI-INF/directory-test-config.xml");
    }

    public void testServiceRegistration() throws Exception {
        GadgetService service = Framework.getService(GadgetService.class);
        assertNotNull(service);
        GadgetDeclaration meteoGadget = service.getGadget("meteo");
        assertNotNull(meteoGadget);
        deployContrib("org.nuxeo.opensocial.gadgets.core.test",
                "OSGI-INF/gadget-override-contrib.xml");
        service = Framework.getService(GadgetService.class);
        meteoGadget = service.getGadget("meteo");
        assertNull(meteoGadget);
    }

    public void testRegisterExternal() throws Exception {
        GadgetServiceImpl service = (GadgetServiceImpl) Framework.getService(GadgetService.class);
        assertNotNull(service);
    }

    public void testSpecParsing() throws Exception {

        GadgetService service = Framework.getService(GadgetService.class);
        assertNotNull(service);
        GadgetDeclaration meteoGadget = service.getGadget("meteo");
        assertNotNull(meteoGadget);

        GadgetSpec spec = meteoGadget.getGadgetSpec();
        assertNotNull(spec);
        System.out.println(spec.getModulePrefs());


        DirectoryService ds = Framework.getService(DirectoryService.class);
        Session session = ds.open(GadgetServiceImpl.GADGET_DIRECTORY);
        Map<String, Object> init = new HashMap<String, Object>();

        init.put("id", "buddy");
        init.put("label", "buddy");
        init.put("enabled", "1");
        init.put("category", "secret");
        init.put("url", "http://buddypoke.s3.amazonaws.com/hi5.xml");
        init.put("iconUrl", "");
        DocumentModel entry = session.createEntry(init);
        session.updateEntry(entry);

        GadgetDeclaration buddyGadget = service.getGadget("buddy");
        assertNotNull(buddyGadget);

        // only testable online
        //spec = buddyGadget.getGadgetSpec();
        //assertNotNull(spec);
        //System.out.println(spec.getModulePrefs());


    }
}
