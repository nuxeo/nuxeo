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

import java.io.File;
import java.util.List;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.service.CommandLineDescriptor;
import org.nuxeo.ecm.platform.commandline.executor.service.CommandLineExecutorComponent;
import org.nuxeo.ecm.platform.commandline.executor.service.executors.AbstractExecutor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * Tests commands parsing.
 *
 * @author tiry
 * @author Vincent Dutat
 */
public class TestCommands extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.commandline.executor");
    }

    public void testCmdParamatersParsing() throws Exception {
        CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
        assertNotNull(cles);

        deployContrib("org.nuxeo.ecm.platform.commandline.executor",
                "OSGI-INF/commandline-aspell-test-contribs.xml");
        List<String> cmds = cles.getRegistredCommands();
        assertNotNull(cmds);
        assertEquals(1, cmds.size());
        assertTrue(cmds.contains("aspell"));

        CommandLineDescriptor cmdDesc = CommandLineExecutorComponent.getCommandDescriptor("aspell");

        // System.out.println(cmdDesc.getParametersString());

        File textFile = File.createTempFile("testMe", "txt");
        String textFilePath = "/tmp/textMe.txt";

        CmdParameters params = new CmdParameters();
        params.addNamedParameter("lang", "fr_FR");
        params.addNamedParameter("encoding", "utf-8");

        // test String params
        params.addNamedParameter("textFile", textFilePath);
        String parsedParamString = AbstractExecutor.getParametersString(
                cmdDesc, params);
        assertEquals(
                "-a --lang=fr_FR --encoding=utf-8 -H --rem-sgml-check=alt < /tmp/textMe.txt",
                parsedParamString);

        // test with File param
        params.addNamedParameter("textFile", textFile);
        parsedParamString = AbstractExecutor.getParametersString(cmdDesc,
                params);
        // System.out.println("command:" + parsedParamString);
        assertTrue(parsedParamString.startsWith("-a --lang=fr_FR --encoding=utf-8 -H --rem-sgml-check=alt < "));
        assertTrue(parsedParamString.contains(System.getProperties().getProperty(
                "java.io.tmpdir")));
    }

}
