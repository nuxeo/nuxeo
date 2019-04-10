/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Olivier Grisel <ogrisel@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.seam;

/**
 * Data transfer object to display the list of available packages for download in the User Center.
 *
 * @since 5.7
 */
public class DesktopPackageDefinition {

    protected final String name;

    protected final String platform;

    protected final String url;

    public DesktopPackageDefinition(String url, String name, String platform) {
        this.name = name;
        this.platform = platform;
        this.url = url;
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

    public String getURL() {
        return url;
    }

}
