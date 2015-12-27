/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tdelprat
 *
 */
package org.nuxeo.wizard.helpers;

import java.io.File;
import java.io.IOException;

import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.wizard.context.Context;
import org.nuxeo.wizard.context.ParamCollector;

public class PackageDownloaderHelper {

    public static final String MARKER_FILE = "packageSelection.done";

    protected static File getMarkerFile(Context ctx) {
        ParamCollector collector = ctx.getCollector();
        ConfigurationGenerator cg = collector.getConfigurationGenerator();
        File mpDir = cg.getDistributionMPDir();
        return new File(mpDir, MARKER_FILE);
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
