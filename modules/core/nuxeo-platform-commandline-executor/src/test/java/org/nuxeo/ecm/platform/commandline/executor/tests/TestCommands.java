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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.commandline.executor.service.executors.ShellExecutor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * Tests commands parsing.
 *
 * @author tiry
 * @author Vincent Dutat
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
public class TestCommands {

    @Inject
    public CommandLineExecutorService cles;

    @Test
    public void testReplaceParams() throws Exception {
        CmdParameters params = cles.getDefaultCmdParameters();

        // test default param
        List<String> res = ShellExecutor.replaceParams("-tmp=#{java.io.tmpdir} -nuxeo.tmp=#{nuxeo.tmp.dir}", params);
        List<String> exp = Collections.singletonList(String.format("-tmp=%s -nuxeo.tmp=%s",
                System.getProperty("java.io.tmpdir"), Environment.getDefault().getTemp().getPath()));
        assertEquals(exp, res);

        // test String param
        params.addNamedParameter("foo", "/some/path");
        res = ShellExecutor.replaceParams("foo=#{foo}", params);
        assertEquals(Arrays.asList("foo=/some/path"), res);
        params.addNamedParameter("width", "320");
        params.addNamedParameter("height", "200");
        res = ShellExecutor.replaceParams("#{width}x#{height}", params);
        assertEquals(Arrays.asList("320x200"), res);

        // test File param
        File tmp = Framework.createTempFile("testCommands", "txt");
        tmp.delete();
        params.addNamedParameter("foo", tmp);
        res = ShellExecutor.replaceParams("-file=#{foo}[0]", params);
        assertEquals(Arrays.asList("-file=" + tmp.getAbsolutePath() + "[0]"), res);

        // test List param
        params.addNamedParameter("tags", Arrays.asList("-foo", "-bar", "-baz"));
        res = ShellExecutor.replaceParams("#{tags}", params);
        assertEquals(Arrays.asList("-foo", "-bar", "-baz"), res);
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.commandline.executor:OSGI-INF/commandline-env-test-contrib.xml")
    public void testCmdEnvironment() throws Exception {
        List<String> cmds = cles.getRegistredCommands();
        assertNotNull(cmds);
        assertTrue(cmds.contains("echo"));

        ExecResult result = cles.execCommand("echo", cles.getDefaultCmdParameters());
        assertTrue(result.isSuccessful());
        assertSame(0, result.getReturnCode());
        assertTrue(
                String.format("Output should contain %s:\n%s", Environment.getDefault().getTemp().getPath(),
                        result.getOutput()),
                String.join("", result.getOutput()).contains(Environment.getDefault().getTemp().getPath()));
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.commandline.executor:OSGI-INF/commandline-env-test-contrib.xml")
    public void testCmdPipe() throws Exception {

        ExecResult result = cles.execCommand("pipe", cles.getDefaultCmdParameters());
        assertTrue(result.isSuccessful());
        assertEquals(0, result.getReturnCode());
        String line = String.join("", result.getOutput());
        // window's echo displays things exactly as is including quotes
        String expected = SystemUtils.IS_OS_WINDOWS ? "\"a   b\" \"c   d\" e" : "a   b c   d e";
        assertEquals(expected, line);
    }

}
