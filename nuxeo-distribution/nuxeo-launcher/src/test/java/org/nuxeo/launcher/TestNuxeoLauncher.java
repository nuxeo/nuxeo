/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.nuxeo.launcher.NuxeoLauncher.SolarisProcessManager;

public class TestNuxeoLauncher {

    private static final String NUXEO_PATH = "/opt/nuxeo";

    private static final String STARTUP_CLASS = "org.apache.catalina.startup.Bootstrap";

    // USER PID %CPU %MEM SZ RSS TT S START TIME COMMAND

    private static final String SOL_PS1_CMD = "fsflush";

    private static final String SOL_PS1 = "root         3  0.2  0.0    0    0 ?        S 16:02:16  0:00 "
            + SOL_PS1_CMD;

    private static final String SOL_PS2_CMD = "/usr/lib/rad/rad -m /usr/lib/rad/transport -m /usr/lib/rad/protocol -m /usr/lib/rad/module -m /usr/lib/rad/site-modules -t pipe:fd=3,exit -e 180";

    private static final String SOL_PS2 = "fguillau  1786  0.0  0.219136 3908 ?        S 16:05:41  0:00 "
            + SOL_PS2_CMD;

    private static final String SOL_PS3_CMD = "gnome-terminal";

    private static final String SOL_PS3 = "fguillau  1921  2.2  0.913130018836 ?        S 16:08:28  0:00 "
            + SOL_PS3_CMD;

    private static final String SOL_PS4_CMD = "/usr/jdk/instances/jdk1.7.0/jre/bin/java -server -Xms512m -Xmx1024m -XX:MaxPermSize=512m -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000 -Dfile.encoding=UTF-8 -Dmail.mime.decodeparameters=true -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n -Djava.net.preferIPv4Stack=true -Djava.awt.headless=true -cp .:/opt/nuxeo/nxserver/lib:/opt/nuxeo/bin/bootstrap.jar:/opt/nuxeo/bin/tomcat-juli.jar -Dnuxeo.home=/opt/nuxeo -Dnuxeo.conf=/opt/nuxeo/bin/nuxeo.conf -Dnuxeo.log.dir=/opt/nuxeo/log -Dnuxeo.data.dir=/opt/nuxeo/nxserver/data -Dnuxeo.tmp.dir=/opt/nuxeo/tmp -Djava.io.tmpdir=/opt/nuxeo/tmp -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager -Dcatalina.base=/opt/nuxeo -Dcatalina.home=/opt/nuxeo -Djava.endorsed.dirs=/opt/nuxeo/endorsed org.apache.catalina.startup.Bootstrap start";

    private static final String SOL_PS4 = "fguillau  2788  1.2 19.5699008405296 pts/2    S 18:54:51  1:26 "
            + SOL_PS4_CMD;

    protected class MockSolarisProcessManager extends SolarisProcessManager {

        protected Map<String, List<String>> commands = new HashMap<String, List<String>>();

        public void setLines(String command, List<String> lines) {
            commands.put(command, lines);
        }

        @Override
        protected List<String> execute(String... command) throws IOException {
            for (Entry<String, List<String>> es : commands.entrySet()) {
                String c = es.getKey();
                if (command[0].contains(c)) {
                    return es.getValue();
                }
            }
            fail("Bad command: " + Arrays.toString(command));
            return null;
        }
    }

    /** Code from {@link NuxeoLauncher#init} */
    protected String getRegex() {
        return "^(?!/bin/sh).*" + Pattern.quote(NUXEO_PATH) + ".*"
                + Pattern.quote(STARTUP_CLASS) + ".*$";
    }

    protected static void assertSolarisMatch(MockSolarisProcessManager pm,
            String expectedPid, String expectedCommand, String line) {
        Matcher lineMatcher = pm.getLineMatcher(line);
        assertTrue(lineMatcher.matches());
        String pid = lineMatcher.group(1);
        String command = lineMatcher.group(2);
        assertEquals(expectedPid, pid);
        assertEquals(expectedCommand, command);
    }

    @Test
    public void testSolarisProcessManagerParsing() throws Exception {
        MockSolarisProcessManager pm = new MockSolarisProcessManager();
        pm.setLines("uname", Collections.singletonList("5.11"));
        assertEquals("5.11", pm.getSolarisVersion());
        assertSolarisMatch(pm, "3", SOL_PS1_CMD, SOL_PS1);
        assertSolarisMatch(pm, "1786", SOL_PS2_CMD, SOL_PS2);
        assertSolarisMatch(pm, "1921", SOL_PS3_CMD, SOL_PS3);
        assertSolarisMatch(pm, "2788", SOL_PS4_CMD, SOL_PS4);

        pm.setLines("ps", Arrays.asList(SOL_PS1, SOL_PS2, SOL_PS3, SOL_PS4));
        assertEquals("2788", pm.findPid(getRegex()));
    }

}
