/*
 * (C) Copyright 2013-2020 Nuxeo (http://nuxeo.com/) and others.
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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nuxeo.common.Environment;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier;
import org.nuxeo.launcher.config.AbstractConfigurationTest;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.launcher.config.TomcatConfigurator;
import org.nuxeo.launcher.info.InstanceInfo;

public class TestNuxeoLauncher extends AbstractConfigurationTest {

    private static final String TEST_INSTANCE_CLID = "/opt/build/hudson/instance.clid";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

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
     *
     * @deprecated since 9.3
     */
    @Test
    @Deprecated
    public void testRegisterTrialIsDeprecated() throws Exception {
        thrown.expect(NuxeoLauncherException.class);
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

    private void checkParsing(NuxeoLauncher launcher) throws GeneralSecurityException {
        assertTrue(launcher.commandIs("encrypt"));
        assertTrue(launcher.cmdLine.hasOption(NuxeoLauncher.OPTION_ENCRYPT));
        assertEquals("AES/ECB/PKCS5Padding",
                launcher.cmdLine.getOptionValue(NuxeoLauncher.OPTION_ENCRYPT, "AES/ECB/PKCS5Padding"));
        assertArrayEquals(new String[] { "value1", "value2" }, launcher.params);
        launcher.encrypt();
    }

}
