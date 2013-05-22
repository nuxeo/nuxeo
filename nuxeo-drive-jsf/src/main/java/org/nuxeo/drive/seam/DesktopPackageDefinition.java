/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Olivier Grisel <ogrisel@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.seam;

import java.io.File;

/**
 * Data transfer object to display the list of available packages for download
 * in the User Center.
 *
 * @since 5.7
 */
public class DesktopPackageDefinition {

    protected final File file;

    protected final String name;

    protected final String platform;

    protected final boolean isLink;

    protected final String url;

    public DesktopPackageDefinition(File file, String name, String platform) {
        this.file = file;
        this.name = name;
        this.platform = platform;
        this.isLink = false;
        this.url = null;
    }

    public DesktopPackageDefinition(String url, String name, String platform) {
        this.file = null;
        this.name = name;
        this.platform = platform;
        this.isLink = true;
        this.url = url;
    }

    public File getFile() {
        return file;
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

    public boolean isLink() {
        return isLink;
    }

    public String getURL() {
        return url;
    }

}
