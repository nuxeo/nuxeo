/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Mathieu Guillaume
 *
 */

package org.nuxeo.launcher.connect;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.connect.CallbackHolder;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.standalone.StandaloneUpdateService;

public class StandaloneCallbackHolder implements CallbackHolder {

    static final Log log = LogFactory.getLog(StandaloneCallbackHolder.class);

    protected PackageUpdateService pus = null;

    protected Environment env = null;

    protected boolean testMode = false;

    public StandaloneCallbackHolder(Environment env) throws IOException, PackageException {
        this.env = env;
        pus = new ConnectBroker(env).getUpdateService();
    }

    public StandaloneCallbackHolder(Environment env, StandaloneUpdateService pus) {
        this.env = env;
        this.pus = pus;
    }

    @Override
    public PackageUpdateService getUpdateService() {
        return pus;
    }

    @Override
    public boolean isTestModeSet() {
        return testMode;
    }

    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return env.getProperty(key, defaultValue);
    }

    @Override
    public String getHomePath() {
        try {
            return env.getServerHome().getCanonicalPath();
        } catch (IOException e) {
            log.error("Cannot get home path");
            return null;
        }
    }
}
