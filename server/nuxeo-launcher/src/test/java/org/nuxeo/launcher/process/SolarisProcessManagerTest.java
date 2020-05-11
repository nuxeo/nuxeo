/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.launcher.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.launcher.config.ServerConfigurator.TOMCAT_STARTUP_CLASS;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.nuxeo.launcher.NuxeoLauncher;

/**
 * @since 11.1 (moved from NuxeoLauncherTest)
 */
public class SolarisProcessManagerTest {

    private static final String NUXEO_PATH = "/opt/nuxeo";

    // USER PID %CPU %MEM SZ RSS TT S START TIME COMMAND

    private static final String SOL_PS1_CMD = "fsflush";

    private static final String SOL_PS1 = "root         3  0.2  0.0    0    0 ?        S 16:02:16  0:00 " + SOL_PS1_CMD;

    private static final String SOL_PS2_CMD = "/usr/lib/rad/rad -m /usr/lib/rad/transport -m /usr/lib/rad/protocol -m /usr/lib/rad/module -m /usr/lib/rad/site-modules -t pipe:fd=3,exit -e 180";

    private static final String SOL_PS2 = "fguillau  1786  0.0  0.219136 3908 ?        S 16:05:41  0:00 " + SOL_PS2_CMD;

    private static final String SOL_PS3_CMD = "gnome-terminal";

    private static final String SOL_PS3 = "fguillau  1921  2.2  0.913130018836 ?        S 16:08:28  0:00 "
            + SOL_PS3_CMD;

    private static final String SOL_PS4_CMD = "/usr/jdk/instances/jdk1.7.0/jre/bin/java -server -Xms512m -Xmx1024m -XX:MaxPermSize=512m -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000 -Dfile.encoding=UTF-8 -Dmail.mime.decodeparameters=true -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n -Djava.net.preferIPv4Stack=true -Djava.awt.headless=true -cp .:/opt/nuxeo/nxserver/lib:/opt/nuxeo/bin/bootstrap.jar:/opt/nuxeo/bin/tomcat-juli.jar -Dnuxeo.home=/opt/nuxeo -Dnuxeo.conf=/opt/nuxeo/bin/nuxeo.conf -Dnuxeo.log.dir=/opt/nuxeo/log -Dnuxeo.data.dir=/opt/nuxeo/nxserver/data -Dnuxeo.tmp.dir=/opt/nuxeo/tmp -Djava.io.tmpdir=/opt/nuxeo/tmp -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager -Dcatalina.base=/opt/nuxeo -Dcatalina.home=/opt/nuxeo -Djava.endorsed.dirs=/opt/nuxeo/endorsed org.apache.catalina.startup.Bootstrap start";

    private static final String SOL_PS4 = "fguillau  2788  1.2 19.5699008405296 pts/2    S 18:54:51  1:26 "
            + SOL_PS4_CMD;

    @Test
    public void testSolarisProcessManagerParsing() throws Exception {
        var pm = new MockSolarisProcessManager(getRegex(), "5.11",
                Map.of("ps", List.of(SOL_PS1, SOL_PS2, SOL_PS3, SOL_PS4)));
        assertEquals(Long.valueOf(2788), pm.findPid().orElseThrow(() -> new AssertionError("No PID found")));
    }

    @Test
    public void testSolarisLineMatcher() {
        assertSolarisMatch("3", SOL_PS1_CMD, SOL_PS1);
        assertSolarisMatch("1786", SOL_PS2_CMD, SOL_PS2);
        assertSolarisMatch("1921", SOL_PS3_CMD, SOL_PS3);
        assertSolarisMatch("2788", SOL_PS4_CMD, SOL_PS4);
    }

    protected static void assertSolarisMatch(String expectedPid, String expectedCommand, String line) {
        Matcher lineMatcher = SolarisProcessManager.PS_OUTPUT_LINE.matcher(line);
        assertTrue(lineMatcher.matches());
        String pid = lineMatcher.group(1);
        String command = lineMatcher.group(2);
        assertEquals(expectedPid, pid);
        assertEquals(expectedCommand, command);
    }

    /** Code from {@link NuxeoLauncher#init} */
    protected Pattern getRegex() {
        return Pattern.compile(
                "^(?!/bin/sh).*" + Pattern.quote(NUXEO_PATH) + ".*" + Pattern.quote(TOMCAT_STARTUP_CLASS) + ".*$");
    }

    protected static class MockSolarisProcessManager extends SolarisProcessManager {

        protected final Map<String, List<String>> commands;

        protected MockSolarisProcessManager(Pattern processPattern, String solarisVersion,
                Map<String, List<String>> commands) {
            super(processPattern, solarisVersion);
            this.commands = commands;
        }

        @Override
        protected List<String> execute0(String... command) {
            for (Map.Entry<String, List<String>> es : commands.entrySet()) {
                String c = es.getKey();
                if (command[0].contains(c)) {
                    return es.getValue();
                }
            }
            fail("Bad command: " + String.join(" ", command));
            return null;
        }
    }
}
