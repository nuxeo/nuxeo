/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     tdelprat
 *
 */
package org.nuxeo.wizard.helpers;

import java.io.File;
import java.io.IOException;

import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.wizard.context.Context;
import org.nuxeo.wizard.context.ParamCollector;
import org.nuxeo.wizard.download.PackageDownloader;

public class PackageDownloaderHelper {

    public static final String MARKER_FILE = "packageSelection.done";

    protected static File getMarkerFile(Context ctx) {
        ParamCollector collector = ctx.getCollector();
        ConfigurationGenerator cg = collector.getConfigurationGenerator();
        File nxHome = cg.getNuxeoHome();
        File dir = new File(nxHome, PackageDownloader.WORKDING_DIR_NAME);
        return new File(dir, MARKER_FILE);
    }

    public static void markPackageSelectionDone(Context ctx) throws IOException {
        File marker = getMarkerFile(ctx);
        marker.createNewFile();
    }

    public static boolean isPackageSelectionDone(Context ctx) {
        File marker = getMarkerFile(ctx);
        return marker.exists();
    }

}
