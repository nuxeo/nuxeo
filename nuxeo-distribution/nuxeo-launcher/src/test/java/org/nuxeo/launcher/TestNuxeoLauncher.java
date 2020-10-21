/*
 * (C) Copyright 2013-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.launcher;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nuxeo.common.Environment;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier;
import org.nuxeo.launcher.config.AbstractConfigurationTest;
import org.nuxeo.launcher.config.ConfigurationException;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.launcher.config.TomcatConfigurator;
import org.nuxeo.launcher.info.InstanceInfo;
import org.nuxeo.launcher.info.PackageInfo;
import org.nuxeo.launcher.process.SolarisProcessManager;

public class TestNuxeoLauncher extends AbstractConfigurationTest {

    private static final String TEST_INSTANCE_CLID = "/opt/build/hudson/instance.clid";

    private static final String NUXEO_PATH = "/opt/nuxeo";

    private static final String STARTUP_CLASS = "org.apache.catalina.startup.Bootstrap";

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

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    protected class MockSolarisProcessManager extends SolarisProcessManager {

        protected Map<String, List<String>> commands = new HashMap<>();

        public void setLines(String command, List<String> lines) {
            commands.put(command, lines);
        }

        @Override
        protected List<String> execute(String... command) {
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
        return "^(?!/bin/sh).*" + Pattern.quote(NUXEO_PATH) + ".*" + Pattern.quote(STARTUP_CLASS) + ".*$";
    }

    protected static void assertSolarisMatch(MockSolarisProcessManager pm, String expectedPid, String expectedCommand,
            String line) {
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

    @Override
    @Before
    public void setUp() throws Exception {
        Environment.setDefault(null);
        nuxeoHome = new File("target/launcher");
        FileUtils.deleteQuietly(nuxeoHome);
        Files.createDirectories(nuxeoHome.toPath());
        File nuxeoConf = getResourceFile("config/nuxeo.conf");
        FileUtils.copyFileToDirectory(nuxeoConf, nuxeoHome);
        FileUtils.copyDirectory(getResourceFile("templates"), new File(nuxeoHome, "templates"));
        setSystemProperty(Environment.NUXEO_HOME, nuxeoHome.getPath());
        setSystemProperty(ConfigurationGenerator.NUXEO_CONF, new File(nuxeoHome, nuxeoConf.getName()).getPath());
        setSystemProperty(TomcatConfigurator.TOMCAT_HOME, Environment.getDefault().getServerHome().getPath());
        configGenerator = new ConfigurationGenerator();
        assertTrue(configGenerator.init());
    }

    @Test
    public void testClidOption() throws Exception {
        Path instanceClid = Paths.get(TEST_INSTANCE_CLID);
        if (!Files.exists(instanceClid)) {
            throw new AssumptionViolatedException("No test CLID available");
        }
        String[] args = new String[] { "--clid", instanceClid.toString(), "showconf" };
        final NuxeoLauncher launcher = NuxeoLauncher.createLauncher(args);
        InstanceInfo info = launcher.getInfo();
        assertNotNull("Failed to get instance info", info);
        List<String> clidLines = Files.readAllLines(instanceClid, UTF_8);
        LogicalInstanceIdentifier expectedClid = new LogicalInstanceIdentifier(
                clidLines.get(0) + LogicalInstanceIdentifier.ID_SEP + clidLines.get(1), "expected clid");
        assertEquals("Not the right instance.clid file: ", expectedClid.getCLID(), info.clid);
    }

    @Test
    public void testRegisterTrialIsValidCommand() throws Exception {
        String[] args = { "register-trial" };
        NuxeoLauncher launcher = NuxeoLauncher.createLauncher(args);
        assertTrue(launcher.commandIs("register-trial"));
        assertTrue(launcher.commandRequiresNoGUI());
    }

    /**
     * Command "register-trial" is deprecated since 9.3.
     * <p>
     * Set timeout to 1 second, {@code timeout = 1000}, to prevent this test takes too long time to finish. The only
     * case it can happen is when Nuxeo Launcher waits for user value from standard input (stdin). It means somebody has
     * changed the implementation of #registerTrial.
     */
    @Test(timeout = 1000) // 1s. Explanation in Javadoc.
    @SuppressWarnings("deprecation")
    public void testRegisterTrialIsDeprecated() throws Exception {
        thrown.expect(UnsupportedOperationException.class);
        thrown.expectMessage("deprecated");
        thrown.expectMessage("https://connect.nuxeo.com/register");
        NuxeoLauncher.createLauncher(new String[] { "register-trial" }).registerTrial();
    }

    /**
     * NXP-19071: avoid confusion with the command parameters when passing an argument to an option, or when calling
     * without argument an option which accepts optional arguments.
     *
     * @since 8.2
     */
    @Test
    public void testParamSeparator() throws Exception {
        // failing syntax: "value1" is parsed as an argument to "--encrypt" option
        NuxeoLauncher launcher = NuxeoLauncher.createLauncher(
                new String[] { "encrypt", "--encrypt", "value1", "value2" });
        assertTrue(launcher.commandIs("encrypt"));
        assertTrue(launcher.cmdLine.hasOption(NuxeoLauncher.OPTION_ENCRYPT));
        assertEquals("value1", launcher.cmdLine.getOptionValue(NuxeoLauncher.OPTION_ENCRYPT));
        assertArrayEquals(new String[] { "value2" }, launcher.params);
        try {
            launcher.encrypt();
            fail("Expected 'java.security.NoSuchAlgorithmException: Cannot find any provider supporting value1'");
        } catch (NoSuchAlgorithmException e) {
            assertEquals("Cannot find any provider supporting value1", e.getMessage());
        }

        // working syntax: "value1" is a parsed as a parameter to "encrypt" command
        // 1) option without argument placed at the end
        launcher = NuxeoLauncher.createLauncher(new String[] { "encrypt", "value1", "value2", "--encrypt" });
        checkParsing(launcher);
        // 2) option with an argument
        launcher = NuxeoLauncher.createLauncher(
                new String[] { "encrypt", "--encrypt", "AES/ECB/PKCS5Padding", "value1", "value2" });
        checkParsing(launcher);
        // 3) option without argument separated with "--"
        launcher = NuxeoLauncher.createLauncher(new String[] { "encrypt", "--encrypt", "--", "value1", "value2" });
        checkParsing(launcher);

        // Check specific case of the "--set" option
        launcher = NuxeoLauncher.createLauncher(new String[] { "config", "--set", "someTemplate", "someKey" });
        assertTrue(launcher.commandIs("config"));
        assertTrue(launcher.cmdLine.hasOption(NuxeoLauncher.OPTION_SET));
        assertEquals("someTemplate", launcher.cmdLine.getOptionValue(NuxeoLauncher.OPTION_SET));
        assertArrayEquals(new String[] { "someKey" }, launcher.params);

        launcher = NuxeoLauncher.createLauncher(new String[] { "config", "--set", "--", "someKey" });
        assertTrue(launcher.commandIs("config"));
        assertTrue(launcher.cmdLine.hasOption(NuxeoLauncher.OPTION_SET));
        assertNull(launcher.cmdLine.getOptionValue(NuxeoLauncher.OPTION_SET));
        assertArrayEquals(new String[] { "someKey" }, launcher.params);

        launcher = NuxeoLauncher.createLauncher(new String[] { "config", "someKey", "--set" });
        assertTrue(launcher.commandIs("config"));
        assertTrue(launcher.cmdLine.hasOption(NuxeoLauncher.OPTION_SET));
        assertNull(launcher.cmdLine.getOptionValue(NuxeoLauncher.OPTION_SET));
        assertArrayEquals(new String[] { "someKey" }, launcher.params);
    }

    private void checkParsing(NuxeoLauncher launcher) throws ConfigurationException, GeneralSecurityException {
        assertTrue(launcher.commandIs("encrypt"));
        assertTrue(launcher.cmdLine.hasOption(NuxeoLauncher.OPTION_ENCRYPT));
        assertEquals("AES/ECB/PKCS5Padding",
                launcher.cmdLine.getOptionValue(NuxeoLauncher.OPTION_ENCRYPT, "AES/ECB/PKCS5Padding"));
        assertArrayEquals(new String[] { "value1", "value2" }, launcher.params);
        launcher.encrypt();
    }

    /**
     * NXP-29401
     */
    @Test
    public void testPrintInstanceXMLOutputInJSON() throws Exception {
        NuxeoLauncher launcher = NuxeoLauncher.createLauncher(new String[] { "showconf", "--json" });

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.description = "Download and install the latest hotfix to keep your Nuxeo up-to-date.\n    "
                + "Changes will take effects after restart.";

        InstanceInfo instanceInfo = new InstanceInfo();
        instanceInfo.packages.add(packageInfo);

        try (OutputStream os = new ByteArrayOutputStream()) {
            launcher.printInstanceXMLOutput(instanceInfo, os);
            String json = os.toString();
            assertEquals("{\"packages\":{\"package\":{\"supportsHotReload\":\"false\",\"description\":\"Download and "
                    + "install the latest hotfix to keep your Nuxeo up-to-date.\\n    Changes will take "
                    + "effects after restart.\"}}}", json);
        }
    }

}
