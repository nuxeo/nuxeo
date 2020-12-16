/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.launcher.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.launcher.config.ConfigurationGenerator.JVMCHECK_FAIL;
import static org.nuxeo.launcher.config.ConfigurationGenerator.JVMCHECK_NOFAIL;
import static org.nuxeo.launcher.config.ConfigurationGenerator.JVMCHECK_PROP;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.Test;

/**
 * @since 11.5
 */
public class JVMVersionTest {

    @Test
    public void testCheckJavaVersionFail() {
        testCheckJavaVersion(true);
    }

    @Test
    public void testCheckJavaVersionNoFail() {
        testCheckJavaVersion(false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongJavaVersionFail() {
        ConfigurationGenerator.checkJavaVersion("1.not-a-version", "1.8.0_40", false, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongPreJdk9VersionFail() {
        ConfigurationGenerator.checkJavaVersion("1.not-a-version", "1.8.0_40", false, false);
    }

    @Test
    public void testWrongJavaVersionNoFail() {
        runJVMCheck(false, () -> ConfigurationGenerator.checkJavaVersion("not-a-version", "1.8.0_40", true, true));
    }

    protected void testCheckJavaVersion(boolean fail) {
        runJVMCheck(fail, () -> checkJavaVersions(!fail));
    }

    protected void runJVMCheck(boolean fail, Runnable runnable) {
        String old = System.getProperty(JVMCHECK_PROP);
        try {
            System.setProperty(JVMCHECK_PROP, fail ? JVMCHECK_FAIL : JVMCHECK_NOFAIL);
            runnable.run();
        } finally {
            if (old == null) {
                System.clearProperty(JVMCHECK_PROP);
            } else {
                System.setProperty(JVMCHECK_PROP, old);
            }
        }
    }

    protected void checkJavaVersions(boolean compliant) {
        // ok
        checkJavaVersion(true, "1.7.0_10", "1.7.0_1");
        checkJavaVersion(true, "1.8.0_92", "1.7.0_1");
        checkJavaVersion(true, "1.8.0_40", "1.8.0_40");
        checkJavaVersion(true, "1.8.0_45", "1.8.0_40");
        checkJavaVersion(true, "1.8.0_101", "1.8.0_40");
        checkJavaVersion(true, "1.8.0_400", "1.8.0_40");
        checkJavaVersion(true, "1.8.0_72-internal", "1.8.0_40");
        checkJavaVersion(true, "1.8.0-internal", "1.8.0");
        checkJavaVersion(true, "1.9.0_1", "1.8.0_40");
        // compliant if jvmcheck=nofail
        checkJavaVersion(compliant, "1.7.0_1", "1.8.0_40");
        checkJavaVersion(compliant, "1.7.0_40", "1.8.0_40");
        checkJavaVersion(compliant, "1.7.0_101", "1.8.0_40");
        checkJavaVersion(compliant, "1.7.0_400", "1.8.0_40");
        checkJavaVersion(compliant, "1.8.0_1", "1.8.0_40");
        checkJavaVersion(compliant, "1.8.0_25", "1.8.0_40");
        checkJavaVersion(compliant, "1.8.0_39", "1.8.0_40");
    }

    protected void checkJavaVersion(boolean compliant, String version, String requiredVersion) {
        assertEquals(version + " vs " + requiredVersion, compliant,
                ConfigurationGenerator.checkJavaVersion(version, requiredVersion, true, false));
    }

    @Test
    public void testParseJVMVersion() throws Exception {
        checkParsed("7.10", "1.7.0_10");
        checkParsed("8.45", "1.8.0_45");
        checkParsed("8.72", "1.8.0_72-internal");
        checkParsed("9.0", "9");
        checkParsed("9.0", "9.0");
        checkParsed("9.0", "9.0.1");
        checkParsed("9.0", "9.0.1.15");
        checkParsed("9.4", "9.4.5.6");
        checkParsed("10.0", "10.0.1");
        checkParsed("15.0", "15");
    }

    protected void checkParsed(String expected, String version) throws Exception {
        JVMVersion v = JVMVersion.parse(version);
        assertEquals(expected, v.toString());
    }

    @Test
    public void testCheckJavaVersionCompliant() throws Exception {
        LogCaptureAppender logCaptureAppender = new LogCaptureAppender(Level.WARN, ConfigurationGenerator.class);
        logCaptureAppender.start();
        Logger rootLogger = LoggerContext.getContext(false).getRootLogger();
        rootLogger.addAppender(logCaptureAppender);
        try {
            // Nuxeo 6.0 case
            ConfigurationGenerator.checkJavaVersion("1.7.0_10", new String[] { "1.7.0_1", "1.8.0_1" });
            assertTrue(logCaptureAppender.isEmpty());
            ConfigurationGenerator.checkJavaVersion("1.8.0_92", new String[] { "1.7.0_1", "1.8.0_1" });
            assertTrue(logCaptureAppender.isEmpty());
            // Nuxeo 7.10/8.10 case
            ConfigurationGenerator.checkJavaVersion("1.8.0_50", new String[] { "1.8.0_40" });
            assertTrue(logCaptureAppender.isEmpty());

            // may log warn message cases
            ConfigurationGenerator.checkJavaVersion("1.8.0_92", new String[] { "1.7.0_1" });
            assertEquals(1, logCaptureAppender.size());
            assertEquals("Nuxeo requires Java 1.7.0_1+ (detected 1.8.0_92).", logCaptureAppender.get(0));
            logCaptureAppender.clear();

            ConfigurationGenerator.checkJavaVersion("1.8.0_92", new String[] { "1.6.0_1", "1.7.0_1" });
            assertEquals(1, logCaptureAppender.size());
            assertEquals("Nuxeo requires Java 1.7.0_1+ (detected 1.8.0_92).", logCaptureAppender.get(0));
            logCaptureAppender.clear();

            // jvmcheck=nofail case
            runJVMCheck(false, () -> {
                try {
                    ConfigurationGenerator.checkJavaVersion("1.6.0_1", new String[] { "1.7.0_1" });
                    assertEquals(1, logCaptureAppender.size());
                    assertEquals("Nuxeo requires Java 1.7.0_1+ (detected 1.6.0_1).", logCaptureAppender.get(0));
                    logCaptureAppender.clear();
                } catch (Exception e) {
                    fail("Exception thrown " + e.getMessage());
                }
            });

            // fail case
            try {
                ConfigurationGenerator.checkJavaVersion("1.6.0_1", new String[] { "1.7.0_1" });
            } catch (ConfigurationException ce) {
                assertEquals(
                        "Nuxeo requires Java {1.7.0_1} (detected 1.6.0_1). See 'jvmcheck' option to bypass version check.",
                        ce.getMessage());
            }
        } finally {
            logCaptureAppender.stop();
            rootLogger.removeAppender(logCaptureAppender);
        }
    }
}
