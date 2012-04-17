/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Mathieu Guillaume
 *
 */

package org.nuxeo.launcher.connect;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.connect.CallbackHolder;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.standalone.StandaloneUpdateService;
import org.nuxeo.launcher.NuxeoLauncher;

public class StandaloneCallbackHolder implements CallbackHolder {

    static final Log log = LogFactory.getLog(StandaloneCallbackHolder.class);

    protected PackageUpdateService pus = null;

    protected Environment env = null;

    protected boolean testMode = false;

    public StandaloneCallbackHolder(Environment env) {
        this.env = env;
        pus = (PackageUpdateService) new StandalonePackageManager(env);
    }

    public StandaloneCallbackHolder(Environment env, StandaloneUpdateService pus) {
        this.env = env;
        this.pus = pus;
    }

    public PackageUpdateService getUpdateService() {
        return pus;
    }

    public boolean isTestModeSet() {
        return testMode;
    }

    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }

    public String getProperty(String key, String defaultValue) {
        return env.getProperty(key, defaultValue);
    }

    public String getHomePath() {
        try {
            return env.getServerHome().getCanonicalPath();
        } catch (IOException e) {
            log.error("Cannot get home path");
            return null;
        }
    }
}
