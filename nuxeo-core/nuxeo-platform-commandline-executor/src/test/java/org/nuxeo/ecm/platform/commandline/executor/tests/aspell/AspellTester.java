/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
        CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
        assertNotNull(cles);

        CommandAvailability ca = cles.getCommandAvailability("aspell");
        Assume.assumeTrue("aspell is not available, skipping test", ca.isAvailable());

        CmdParameters params = cles.getDefaultCmdParameters();
        params.addNamedParameter("lang", "en_US");
        params.addNamedParameter("encoding", "utf-8");

        String text2Check = "this is a teste with a typo";
        File file2Check = Framework.createTempFile("nuxeo-spell-check-in", "txt");
        PrintWriter printout = new PrintWriter(new BufferedWriter(new FileWriter(file2Check)));
        printout.print(text2Check);
        printout.flush();
        printout.close();

        params.addNamedParameter("textFile", file2Check);

        ExecResult result = cles.execCommand("aspell", params);

        assertTrue(result.isSuccessful());
        assertSame(0, result.getReturnCode());

        List<String> lines = result.getOutput();
        log.trace(lines);
        assertTrue(checkOutput(lines));

        params.addNamedParameter("textFile", "");
        result = cles.execCommand("aspell", params);
        assertFalse(result.isSuccessful());
        assertNotSame(0, result.getReturnCode());
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
