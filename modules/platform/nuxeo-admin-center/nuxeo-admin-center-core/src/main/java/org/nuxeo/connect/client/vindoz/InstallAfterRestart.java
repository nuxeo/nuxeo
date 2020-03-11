/*
 * (C) Copyright 2010-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.connect.client.vindoz;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.connect.update.Package;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper class used to manage packages installation issue under windows systems.
 * <p>
 * Because the Windows OS locks all the jar files loaded by the JVM, we can not do proper installation. So installation
 * is delayed until next restart where installation is done before Nuxeo starts (and loads the jars).
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public class InstallAfterRestart {

    public static final String FILE_NAME = "installAfterRestart.log";

    public static final String FAKE_VIDOZ = "org.nuxeo.fake.vindoz";

    protected static final List<String> pkgNameOrIds = new ArrayList<>();

    protected static final List<String> uninstallpkgNameOrIds = new ArrayList<>();

    protected static final Log log = LogFactory.getLog(InstallAfterRestart.class);

    protected static boolean isNeededByOs() {
        return Framework.isBooleanPropertyTrue(FAKE_VIDOZ) || SystemUtils.IS_OS_WINDOWS;
    }

    /**
     * Returns true if a restart should be triggered after install
     */
    public static boolean isNeededForPackage(Package pkg) {
        if (!Framework.isDevModeSet()) {
            return true;
        }
        boolean isNotStudioOrWindows = PackageType.STUDIO != pkg.getType() && isNeededByOs();
        boolean isHotFix = PackageType.HOT_FIX == pkg.getType();
        boolean isAddonAndNoHotReload = PackageType.ADDON == pkg.getType() && !pkg.supportsHotReload();
        return isNotStudioOrWindows || isHotFix || isAddonAndNoHotReload;
    }

    protected static boolean isDevMode() {
        return Framework.isDevModeSet();
    }

    /**
     * @deprecated Since 7.4. Use {@link SystemUtils#IS_OS_WINDOWS}
     */
    @Deprecated
    protected static boolean isVindozBox() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    public static void addPackageForInstallation(String pkgNameOrId) {
        if (!pkgNameOrIds.contains(pkgNameOrId)) {
            pkgNameOrIds.add(pkgNameOrId);
            savePkgList();
        }
    }

    public static void addPackageForUnInstallation(String pkgNameOrId) {
        if (!pkgNameOrIds.contains(pkgNameOrId) && !(uninstallpkgNameOrIds.contains(pkgNameOrId))) {
            pkgNameOrIds.add(pkgNameOrId);
            uninstallpkgNameOrIds.add(pkgNameOrId);
            savePkgList();
        }
    }

    public static boolean isMarkedForInstallAfterRestart(String pkgNameOrId) {
        return pkgNameOrIds.contains(pkgNameOrId);
    }

    protected static void savePkgList() {
        String path = Framework.getProperty(Environment.NUXEO_DATA_DIR);
        File installFile = new File(path, FILE_NAME);
        List<String> cmds = new ArrayList<>();
        for (String pkgNameOrId : pkgNameOrIds) {
            String cmd = pkgNameOrId;
            if (uninstallpkgNameOrIds.contains(pkgNameOrId)) {
                cmd = "uninstall " + pkgNameOrId;
            }
            cmds.add(cmd);
        }

        try {
            FileUtils.writeLines(installFile, cmds);
        } catch (IOException e) {
            log.error("Unable to same listing of packages to install on restart", e);
        }
    }

    /**
     * @since 5.6
     */
    public static boolean isMarkedForUninstallAfterRestart(String pkgName) {
        return uninstallpkgNameOrIds.contains(pkgName);
    }

}
