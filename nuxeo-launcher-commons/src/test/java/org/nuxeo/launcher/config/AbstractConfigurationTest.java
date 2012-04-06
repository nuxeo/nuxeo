/*
 * (C) Copyright 2011-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
        nuxeoHome = File.createTempFile("nuxeo", null);
        nuxeoHome.delete();
        nuxeoHome.mkdirs();
        File nuxeoConf = getResourceFile("configurator/nuxeo.conf");
        FileUtils.copyFileToDirectory(nuxeoConf, nuxeoHome);
        System.setProperty(ConfigurationGenerator.NUXEO_CONF, new File(
                nuxeoHome, nuxeoConf.getName()).getPath());
        System.setProperty(Environment.NUXEO_HOME,
                nuxeoHome.getPath());
        System.setProperty(org.nuxeo.common.Environment.NUXEO_DATA_DIR,
                new File(nuxeoHome, "data").getPath());
        System.setProperty(org.nuxeo.common.Environment.NUXEO_LOG_DIR,
                new File(nuxeoHome, "log").getPath());
    }

}