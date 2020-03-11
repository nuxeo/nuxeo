/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.connect.client;

import org.nuxeo.connect.CallbackHolder;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.runtime.api.Framework;

/**
 * Provide access to Nuxeo Framework from Nuxeo Connect Client
 *
 * @author tiry
 */
public class NuxeoCallbackHolder implements CallbackHolder {

    @Override
    public String getHomePath() {
        return Framework.getRuntime().getHome().getAbsolutePath();
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return Framework.getProperty(key, defaultValue);
    }

    @Override
    public boolean isTestModeSet() {
        return Framework.isTestModeSet();
    }

    @Override
    public PackageUpdateService getUpdateService() {
        return Framework.getService(PackageUpdateService.class);
    }

}
