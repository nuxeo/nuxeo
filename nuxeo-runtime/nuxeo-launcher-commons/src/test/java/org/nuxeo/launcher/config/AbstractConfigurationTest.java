/*
 * (C) Copyright 2011-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.launcher.config;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

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
        System.setProperty(ConfigurationGenerator.NUXEO_CONF, new File(nuxeoHome, nuxeoConf.getName()).getPath());
        System.setProperty(Environment.NUXEO_HOME, nuxeoHome.getPath());
        System.setProperty(Environment.NUXEO_DATA_DIR, new File(nuxeoHome, "data").getPath());
        System.setProperty(Environment.NUXEO_LOG_DIR, new File(nuxeoHome, "log").getPath());
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(nuxeoHome);
        System.clearProperty(ConfigurationGenerator.NUXEO_CONF);
        System.clearProperty(Environment.NUXEO_HOME);
        System.clearProperty(Environment.NUXEO_DATA_DIR);
        System.clearProperty(Environment.NUXEO_LOG_DIR);
    }

}
