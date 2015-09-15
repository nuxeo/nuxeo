/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.platform.commandline.executor.tests.aspell;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * Test Aspell command line.
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
@LocalDeploy("org.nuxeo.ecm.platform.commandline.executor:OSGI-INF/commandline-aspell-test-contribs.xml")
public class AspellTester {

    private static final Log log = LogFactory.getLog(AspellTester.class);

    @Test
    public void testAspellExec() throws Exception {
        CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
        assertNotNull(cles);

        CommandAvailability ca = cles.getCommandAvailability("aspell");
        Assume.assumeTrue("aspell is not available, skipping test", ca.isAvailable());

        CmdParameters params = cles.getDefaultCmdParameters();
        params.addNamedParameter("lang", "en_US");
        params.addNamedParameter("encoding", "utf-8");

        String text2Check = "this is a teste with a typo";
        File file2Check = File.createTempFile("nuxeo-spell-check-in", "txt");
        PrintWriter printout = new PrintWriter(new BufferedWriter(new FileWriter(file2Check)));
        printout.print(text2Check);
        printout.flush();
        printout.close();

        params.addNamedParameter("textFile", file2Check);

        ExecResult result = cles.execCommand("aspell", params);

        assertTrue(result.isSuccessful());
        assertSame(0, result.getReturnCode());

        List<String> lines = result.getOutput();

        // System.out.println(lines);

        assertTrue(checkOutput(lines));

        params.addNamedParameter("textFile", "");
        result = cles.execCommand("aspell", params);
        assertFalse(result.isSuccessful());
        assertNotSame(0, result.getReturnCode());
        // System.out.println(result.getOutput());
    }

    protected boolean checkOutput(List<String> lines) {
        for (String line : lines) {
            if (line.contains("& teste") && line.contains("tested")) {
                return true;
            }
        }
        return false;
    }

}
