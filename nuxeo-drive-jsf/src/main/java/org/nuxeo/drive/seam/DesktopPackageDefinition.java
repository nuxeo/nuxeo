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
 *     Olivier Grisel <ogrisel@nuxeo.com>
 */
package org.nuxeo.drive.seam;

/**
 * Data transfer object to display the list of available packages for download
 * in the User Center.
 * 
 * @since 5.7
 */
public class DesktopPackageDefinition {

    protected final String url;

    protected final String platform;

    protected final String name;

    public DesktopPackageDefinition(String path, String baseURL)
            throws IllegalArgumentException {
        url = baseURL + path.substring(1);
        if (path.endsWith(".msi")) {
            platform = "windows";
        } else if (path.endsWith(".dmg")) {
            platform = "osx";
        } else if (path.endsWith(".deb")) {
            platform = "ubuntu";
        } else {
            throw new IllegalArgumentException("Unsupported package name: "
                    + path);
        }
        name = path.substring("/nuxeo-drive/".length());
    }

    public String getDownloadURL() {
        return url;
    }

    public String getName() {
        return name;
    }

    public String getPlatformLabel() {
        return "user.center.nuxeoDrive.platform." + platform;
    }

    public String getPlatformId() {
        return platform;
    }

}
