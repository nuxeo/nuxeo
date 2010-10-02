/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.convert.tests;

import java.io.FileInputStream;

import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.junit.*;
import org.nuxeo.ecm.platform.convert.ooomanager.OOoManagerComponent;
import org.nuxeo.ecm.platform.convert.ooomanager.OOoManagerDescriptor;
import org.nuxeo.ecm.platform.convert.ooomanager.OOoManagerService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

@Ignore
public class TestOOoServiceManagerService extends Assert {

    NXRuntimeTestCase tc = new NXRuntimeTestCase();

    OOoManagerService ods;

    @Before
    public void setUp() throws Exception {
        tc.setUp();
        tc.deployBundle("org.nuxeo.ecm.platform.convert");
        tc.deployBundle("org.nuxeo.ecm.platform.convert.test");
        tc.deployContrib("org.nuxeo.ecm.platform.convert.test",
                "test-ooo-manager-contrib.xml");
    }

    @After
    public void tearDown() throws Exception {
        ods.stopOOoManager();
        tc.tearDown();
    }

    @Test
    public void testServiceRegistration() throws Exception {
        ods = Framework.getLocalService(OOoManagerService.class);
        assertNotNull(ods);

        ods.startOOoManager();
        OfficeDocumentConverter converter = ods.getDocumentConverter();
        assertNotNull(converter);

        OOoManagerComponent odc = (OOoManagerComponent) ods;
        OOoManagerDescriptor desc = odc.getDescriptor();
        String[] pipes = desc.getPipeNames();
        assertEquals("pipe1", pipes[0]);
        assertEquals("pipe2", pipes[1]);
        assertEquals("pipe3", pipes[2]);

        int[] ports = desc.getPortNumbers();
        assertEquals(2003, ports[0]);
        assertEquals(2004, ports[1]);
        assertEquals(2005, ports[2]);
    }

    @Test
    public void testSocketConnection() throws Exception {
        Framework.getProperties().load(
                new FileInputStream(tc.getResource("jodSocket.properties").getFile()));
        ods = Framework.getLocalService(OOoManagerService.class);
        assertNotNull(ods);

        ods.startOOoManager();
        OfficeDocumentConverter converter = ods.getDocumentConverter();
        assertNotNull(converter);
    }

    @Test
    public void testPipeConnection() throws Exception {
        Framework.getProperties().load(
                new FileInputStream(tc.getResource("jodPipe.properties").getFile()));
        ods = Framework.getLocalService(OOoManagerService.class);
        assertNotNull(ods);

        ods.startOOoManager();
        OfficeDocumentConverter converter = ods.getDocumentConverter();
        assertNotNull(converter);
    }

}
