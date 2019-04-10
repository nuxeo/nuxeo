/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 * </ul>
 *
 * @author Antoine Taillefer
 */
public class NuxeoDriveClientUpdateInfo {

    protected String serverVersion;

    protected String updateSiteURL;

    protected NuxeoDriveClientUpdateInfo() {
        // Needed for JSON deserialization
    }

    public NuxeoDriveClientUpdateInfo(String serverVersion, String updateSiteURL) {
        this.serverVersion = serverVersion;
        this.updateSiteURL = updateSiteURL;
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

}