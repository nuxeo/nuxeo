/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bstefanescu, jcarsique
 */
package org.nuxeo.connect.update.task.guards;

import org.nuxeo.launcher.config.ConfigurationGenerator;

/**
 * This class can be used to check if the current platform match a given
 * platform. For example in a command you may want a guard that define the
 * platform string format.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PlatformHelper {

    protected final String name;

    protected final String version;

    private ConfigurationGenerator cg;

    public PlatformHelper() {
        cg = new ConfigurationGenerator();
        cg.init();
        name = cg.getUserConfig().getProperty(
                ConfigurationGenerator.PARAM_PRODUCT_NAME);
        version = cg.getUserConfig().getProperty(
                ConfigurationGenerator.PARAM_PRODUCT_VERSION);
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    /**
     * Test whether or not the current platform is same as (or compatible) with
     * the given one.
     *
     * @return not implemented, always return true
     */
    public boolean matches(String platform) {
        return true;
    }

    public boolean isTomcat() {
        return cg.isTomcat;
    }

    public boolean isJBoss() {
        return cg.isJBoss;
    }

    public boolean isJetty() {
        return cg.isJetty;
    }

    public boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().indexOf("win") >= 0;
    }

    public boolean isNotWindows() {
        return !isWindows();
    }

    public static String getFullName(String platform) {
        return null;
    }

    public static String getPlatformKey(String platform, String version) {
        return null;
    }

}
