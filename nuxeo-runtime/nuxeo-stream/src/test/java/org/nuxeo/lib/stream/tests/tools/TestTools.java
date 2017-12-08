/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.lib.stream.tests.tools;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.lib.stream.tools.Main;

/**
 * @since 9.3
 */
public abstract class TestTools {
    protected static final int NB_RECORD = 50;

    protected static final String LOG_NAME = "myLog";

    protected boolean initialized;

    public abstract String getManagerOptions();

    public abstract void createContent() throws Exception;

    @Before
    public void initContent() throws Exception {
        if (!initialized) {
            createContent();
            initialized = true;
        }
    }

    @Test
    public void testCat() {
        run(String.format("cat %s --log-name %s --lines 4", getManagerOptions(), LOG_NAME));
    }

    @Test
    public void testCatWithGroup() {
        run(String.format("cat %s -l %s -n 1 --group aGroup", getManagerOptions(), LOG_NAME));
    }

    @Test
    public void testCatMd() {
        run(String.format("cat %s -l %s -n 4 --render markdown", getManagerOptions(), LOG_NAME));
    }

    @Test
    public void testTail() {
        run(String.format("tail %s -l %s -n 5 --render text", getManagerOptions(), LOG_NAME));
    }

    @Test
    public void testTailAndFollow() {
        run(String.format("tail %s -f -l %s -n 2 --render text --timeout 1", getManagerOptions(), LOG_NAME));
    }

    @Test
    public void testLag() {
        run(String.format("lag %s", getManagerOptions()));
    }

    @Test
    public void testLagForLog() {
        run(String.format("lag %s --log-name %s", getManagerOptions(), LOG_NAME));
    }

    @Test
    public void testReset() {
        run(String.format("reset %s --log-name %s --group anotherGroup", getManagerOptions(), LOG_NAME));
    }

    @Test
    public void testCopy() {
        run(String.format("copy %s --src %s --dest %s", getManagerOptions(), LOG_NAME,
                LOG_NAME + "-" + System.currentTimeMillis()));
    }

    @Test
    public void testHelpOption() {
        run("-h");
    }

    @Test
    public void testHelpCommand() {
        run("help");
    }

    @Test
    public void testHelpOnCommand() {
        run("help tail");
    }

    @Test
    public void testUnknownCommand() {
        runShouldFail("unknown-command");
    }

    @Test
    public void testUnknownOptions() {
        runShouldFail(
                String.format("cat %s --invalid-option %s -n 4 --render markdown", getManagerOptions(), LOG_NAME));
    }

    @Test
    public void testEmpty() {
        run("");
    }


    protected void run(String commandLine) {
        boolean result = runCommand(commandLine);
        assertTrue(String.format("Unexpected failure in command: \"%s\"", commandLine), result);
    }

    protected void runShouldFail(String commandLine) {
        boolean result = runCommand(commandLine);
        assertFalse(String.format("Expecting failure on command: \"%s\"", commandLine), result);
    }

    private boolean runCommand(String commandLine) {
        System.out.println("# stream.sh " + commandLine);
        String[] args = commandLine.split(" ");
        return (boolean) new Main().run(args);
    }

}
