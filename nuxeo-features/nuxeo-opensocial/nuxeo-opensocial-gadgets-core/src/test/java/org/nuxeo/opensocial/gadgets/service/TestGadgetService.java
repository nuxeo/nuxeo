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

import org.nuxeo.opensocial.gadgets.service.api.GadgetDeclaration;
import org.nuxeo.opensocial.gadgets.service.api.GadgetService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestGadgetService extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.opensocial.gadgets.core");
    }

    public void testServiceRegistration() throws Exception {
        GadgetService service = Framework.getService(GadgetService.class);
        assertNotNull(service);
        deployContrib("org.nuxeo.opensocial.gadgets.core.test",
                "OSGI-INF/gadget-contrib.xml");
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

        // service.registerNewGadget(external);
    }
}
