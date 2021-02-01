/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.commandline.executor.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * Test service and EPs.
 *
 * @author tiry
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
public class TestService {

    @Inject
    protected CommandLineExecutorService cles;

    @Inject
    protected HotDeployer hotDeployer;

    @Test
    @Deploy("org.nuxeo.ecm.platform.commandline.executor:OSGI-INF/commandline-aspell-test-contribs.xml")
    public void testCmdRegistration() throws Exception {

        List<String> cmds = cles.getRegistredCommands();
        assertNotNull(cmds);
        assertEquals(1, cmds.size());
        assertTrue(cmds.contains("aspell"));

        hotDeployer.deploy(
                "org.nuxeo.ecm.platform.commandline.executor:OSGI-INF/commandline-imagemagic-test-contrib.xml");

        cmds = cles.getRegistredCommands();
        assertNotNull(cmds);
        assertEquals(2, cmds.size());
        assertTrue(cmds.contains("identify"));

        hotDeployer.deploy(
                "org.nuxeo.ecm.platform.commandline.executor:OSGI-INF/commandline-imagemagic-test-contrib2.xml");

        cmds = cles.getRegistredCommands();
        assertNotNull(cmds);
        assertEquals(1, cmds.size());
        assertFalse(cmds.contains("identify"));
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.commandline.executor:OSGI-INF/commandline-dummy-test-contrib.xml")
    public void testCmdAvailable() {
        assertEquals(List.of("cmdThatDoNotExist"), cles.getRegistredCommands());
        assertTrue(cles.getAvailableCommands().isEmpty());

        CommandAvailability ca1 = cles.getCommandAvailability("cmdThatDoNotExist");
        assertFalse(ca1.isAvailable());
        assertEquals("command cmdThatDoNotExistAtAllForSure not found in system path "
                + "(descriptor CommandLineDescriptor[available=false,command=cmdThatDoNotExistAtAllForSure,enabled=true,"
                + "installErrorMessage=<null>,installationDirective=You need to install this command that does not exist!"
                + ",name=cmdThatDoNotExist,parameterString=,readOutput=true,testParameterString=,tester=<null>,"
                + "winCommand=<null>,winParameterString=<null>,winTestParameterString=<null>])", ca1.getErrorMessage());
        assertEquals("You need to install this command that does not exist!", ca1.getInstallMessage());
        assertTrue(ca1.getInstallMessage().contains("need to install this command that does not"));

        CommandAvailability ca2 = cles.getCommandAvailability("foo");
        assertFalse(ca2.isAvailable());
        assertEquals("foo is not a registered command", ca2.getErrorMessage());
        assertNull(ca2.getInstallMessage());
    }

    @Test
    public void testCmdExecption() {
        try {
            cles.execCommand("IDon'tExist", null);
            fail("No Exception has been raised");
        } catch (CommandNotAvailable e) {
            String msg = e.getErrorMessage();
            assertNotNull(msg);
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.commandline.executor:OSGI-INF/commandline-env-test-contrib.xml")
    public void testEnvContributions() {
        String error = "Since version 11.5, contributions to extension point 'org.nuxeo.ecm.platform.commandline.executor.service.CommandLineExecutorComponent--environment'"
                + " do not accept comma-separated names to match multiple commands or command lines anymore: "
                + "contribution with name 'old,multiple,name' should be duplicated";
        List<String> errors = Framework.getRuntime().getMessageHandler().getMessages(Level.ERROR);
        assertEquals(List.of(error), errors);
    }

}
