/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 */

package org.nuxeo.ecm.platform.commandline.executor.tests;

import java.util.List;

import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;


/**
 * Test service and EPs.
 *
 * @author tiry
 */
public class TestService extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.commandline.executor");
    }

    public void testServiceExist() {
        CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
        assertNotNull(cles);
    }

    public void testCmdRegistration() throws Exception {
        CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
        assertNotNull(cles);

        deployContrib("org.nuxeo.ecm.platform.commandline.executor",
                "OSGI-INF/commandline-aspell-test-contribs.xml");
        List<String> cmds = cles.getRegistredCommands();
        assertNotNull(cmds);
        assertEquals(1, cmds.size());
        assertTrue(cmds.contains("aspell"));

        deployContrib("org.nuxeo.ecm.platform.commandline.executor",
                "OSGI-INF/commandline-imagemagic-test-contrib.xml");
        cmds = cles.getRegistredCommands();
        assertNotNull(cmds);
        assertEquals(2, cmds.size());
        assertTrue(cmds.contains("identify"));

        deployContrib("org.nuxeo.ecm.platform.commandline.executor",
                "OSGI-INF/commandline-imagemagic-test-contrib2.xml");
        cmds = cles.getRegistredCommands();
        assertNotNull(cmds);
        assertEquals(1, cmds.size());
        assertFalse(cmds.contains("identify"));
    }

    public void testCmdAvailable() {
        CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
        assertNotNull(cles);

        /**
         deployContrib("org.nuxeo.ecm.platform.commandline.executor","OSGI-INF/commandline-aspell-test-contribs.xml");
         List<String> cmds = cles.getAvailableCommands();
         assertNotNull(cmds);
         assertEquals(1, cmds.size());
         assertTrue(cmds.contains("aspell"));


         deployContrib("org.nuxeo.ecm.platform.commandline.executor","OSGI-INF/commandline-imagemagic-test-contrib.xml");
         cmds = cles.getAvailableCommands();
         assertNotNull(cmds);
         assertEquals(2, cmds.size());
         assertTrue(cmds.contains("identify"));

         deployContrib("org.nuxeo.ecm.platform.commandline.executor","OSGI-INF/commandline-dummy-test-contrib.xml");
         cmds = cles.getAvailableCommands();
         assertEquals(2, cmds.size());
         assertFalse(cmds.contains("cmdThatDoNotExist"));

         CommandAvailability ca =  cles.getCommandAvailability("cmdThatDoNotExist");
         assertFalse(ca.isAvailable());
         assertNotNull(ca.getErrorMessage());
         System.out.println(ca.getErrorMessage());
         assertNotNull(ca.getInstallMessage());
         assertTrue(ca.getInstallMessage().contains("need to install this command that does not"));
         System.out.println(ca.getInstallMessage());

         **/
    }

    public void testCmdExecption() {
        CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
        assertNotNull(cles);

        try {
            cles.execCommand("IDon'tExist", null);
            fail("No Exception has been raised");
        } catch (CommandNotAvailable e) {
            String msg = e.getErrorMessage();
            assertNotNull(msg);
        }
    }

}
