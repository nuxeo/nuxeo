/*
 * (C) Copyright 2011-2019 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Julien Carsique
 *     Frantz Fischer <ffischer@nuxeo.com>
 *
 */

package org.nuxeo.launcher.config;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.nuxeo.common.Environment;

public abstract class AbstractConfigurationTest {

    static final Log log = LogFactory.getLog(AbstractConfigurationTest.class);

    protected ConfigurationGenerator configGenerator;

    protected File nuxeoHome;

    protected File nuxeoBinDir;

    protected Map<String, String> originSystemProps = new HashMap<>();

    public File getResourceFile(String resource) {
        URL url = getClass().getClassLoader().getResource(resource);
        try {
            return new File(URLDecoder.decode(url.getPath(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            log.error(e);
            return null;
        }
    }

    @Before
    public void setUp() throws Exception {
        nuxeoHome = new File("target/launcher");
        nuxeoHome.delete();
        nuxeoHome.mkdirs();
        File nuxeoConf = getResourceFile("configurator/nuxeo.conf");
        FileUtils.copyFileToDirectory(nuxeoConf, nuxeoHome);

        nuxeoBinDir = new File(nuxeoHome, "bin");
        nuxeoBinDir.mkdir();

        setSystemProperty(ConfigurationGenerator.NUXEO_CONF, new File(nuxeoHome, nuxeoConf.getName()).getPath());
        setSystemProperty(Environment.NUXEO_HOME, nuxeoHome.getPath());
        setSystemProperty(Environment.NUXEO_DATA_DIR, new File(nuxeoHome, "data").getPath());
        setSystemProperty(Environment.NUXEO_LOG_DIR, new File(nuxeoHome, "log").getPath());
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteQuietly(nuxeoHome);

        // Restore or clear all the system properties manipulated by the current test
        originSystemProps.forEach((key, value) -> {
            if (value == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, value);
            }
        });
        originSystemProps.clear();
    }

    /**
     * Sets a specific {@link System} property before the test.
     * <p>
     * The original property value will be stored in {@link #originSystemProps}.
     *
     * @param key The name of the system property.
     * @param newValue The new value of the system property.
     * @since 9.3
     */
    protected void setSystemProperty(String key, String newValue) {
        originSystemProps.put(key, System.setProperty(key, newValue));
    }

}
