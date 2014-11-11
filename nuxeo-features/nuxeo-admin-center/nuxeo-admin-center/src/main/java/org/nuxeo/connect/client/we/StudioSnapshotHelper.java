/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.connect.client.we;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.connect.data.DownloadablePackage;

public class StudioSnapshotHelper {

    public static final String SNAPSHOT_SUFFIX = "0.0.0-SNAPSHOT";

    private StudioSnapshotHelper() {
    }

    public static boolean isSnapshot(DownloadablePackage pkg) {
        return pkg.getVersion() != null
                && pkg.getVersion().toString().endsWith(SNAPSHOT_SUFFIX);
    }

    public static List<DownloadablePackage> removeSnapshot(List<DownloadablePackage> pkgs) {
        List<DownloadablePackage> result = new ArrayList<DownloadablePackage>();

        for (DownloadablePackage pkg : pkgs) {
            if (!isSnapshot(pkg)) {
                result.add(pkg);
            }
        }

        return result;
    }

    public static DownloadablePackage getSnapshot(List<DownloadablePackage> pkgs) {
        for (DownloadablePackage pkg : pkgs) {
            if (isSnapshot(pkg)) {
                return pkg;
            }
        }
        return null;
    }

}
