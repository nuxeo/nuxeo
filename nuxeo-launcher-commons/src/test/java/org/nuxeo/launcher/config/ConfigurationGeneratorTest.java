/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Julien Carsique
 *
 */

package org.nuxeo.launcher.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

public class ConfigurationGeneratorTest extends AbstractConfigurationTest {
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
        FileUtils.copyDirectory(getResourceFile("templates/jboss"), new File(
                nuxeoHome, "templates"));
        System.setProperty("jboss.home.dir", nuxeoHome.getPath());
        configGenerator = new ConfigurationGenerator();
    }

    @Test
    public void testEvalDynamicProperties() {
        assertTrue(configGenerator.init());
        assertEquals(
                "Bad loop back URL",
                "http://127.0.0.1:8080/nuxeo",
                configGenerator.getUserConfig().getProperty(
                        ConfigurationGenerator.PARAM_LOOPBACK_URL));
        try {
            testAddress("10.213.2.105", "http://10.213.2.105:8080/nuxeo");
        } catch (ConfigurationException e) {
            log.error(e);
        }
        log.debug("Force IPv6");
        System.setProperty("java.net.preferIPv4Stack", "false");
        System.setProperty("java.net.preferIPv6Addresses", "true");
        try {
            testAddress("::", "http://[0:0:0:0:0:0:0:1]:8080/nuxeo");
        } catch (ConfigurationException e) {
            log.error(e);
        }
        try {
            testAddress("2a01:240:fe8e::226:bbff:fe09:55cd",
                    "http://[2a01:240:fe8e:0:226:bbff:fe09:55cd]:8080/nuxeo");
        } catch (ConfigurationException e) {
            log.error(e);
        }
    }

    private void testAddress(String bindAddress, String expectedLoopback)
            throws ConfigurationException {
        configGenerator.getUserConfig().setProperty(
                ConfigurationGenerator.PARAM_BIND_ADDRESS, bindAddress);
        log.debug("Test with "
                + configGenerator.getUserConfig().getProperty(
                        ConfigurationGenerator.PARAM_BIND_ADDRESS));
        configGenerator.getUserConfig().remove(
                ConfigurationGenerator.PARAM_LOOPBACK_URL);
        configGenerator.evalDynamicProperties();
        assertEquals(
                "Bad loop back URL",
                expectedLoopback,
                configGenerator.getUserConfig().getProperty(
                        ConfigurationGenerator.PARAM_LOOPBACK_URL));
    }
}
