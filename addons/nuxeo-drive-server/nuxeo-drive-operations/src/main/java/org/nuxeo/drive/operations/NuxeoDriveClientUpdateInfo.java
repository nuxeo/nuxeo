/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.operations;

/**
 * Information needed for the Nuxeo Drive client update:
 * <ul>
 * <li>Server version</li>
 * <li>Nuxeo Drive update site URL</li>
 * <li>Nuxeo Drive beta update site URL</li>
 * </ul>
 *
 * @author Antoine Taillefer
 */
public class NuxeoDriveClientUpdateInfo {

    protected String serverVersion;

    protected String updateSiteURL;

    protected String betaUpdateSiteURL;

    protected NuxeoDriveClientUpdateInfo() {
        // Needed for JSON deserialization
    }

    public NuxeoDriveClientUpdateInfo(String serverVersion, String updateSiteURL, String betaUpdateSiteURL) {
        this.serverVersion = serverVersion;
        this.updateSiteURL = updateSiteURL;
        this.betaUpdateSiteURL = betaUpdateSiteURL;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    public String getUpdateSiteURL() {
        return updateSiteURL;
    }

    public void setUpdateSiteURL(String updateSiteURL) {
        this.updateSiteURL = updateSiteURL;
    }

    public String getBetaUpdateSiteURL() {
        return betaUpdateSiteURL;
    }

    public void setBetaUpdateSiteURL(String betaUpdateSiteURL) {
        this.betaUpdateSiteURL = betaUpdateSiteURL;
    }

}
