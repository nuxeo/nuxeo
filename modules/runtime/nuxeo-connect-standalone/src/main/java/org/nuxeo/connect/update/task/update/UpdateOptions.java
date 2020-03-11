/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.connect.update.task.update;

import java.io.File;

import org.nuxeo.connect.update.task.update.JarUtils.Match;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class UpdateOptions {

    public static UpdateOptions newInstance(String pkgId, File file, File targetDir) {
        // compute JAR name without version and the the JAR version
        String name = file.getName();
        Match<String> match = JarUtils.findJarVersion(name);
        // FIXME: hack to get the Studio snapshot version...
        if (match == null && pkgId != null && pkgId.endsWith("-" + UpdateManager.STUDIO_SNAPSHOT_VERSION)) {
            match = new Match<>();
            match.object = pkgId.substring(0, pkgId.length() - ("-" + UpdateManager.STUDIO_SNAPSHOT_VERSION).length());
            match.version = UpdateManager.STUDIO_SNAPSHOT_VERSION;
        }
        if (match == null) {
            return null;
        }
        UpdateOptions up = new UpdateOptions();
        up.pkgId = pkgId;
        up.file = file;
        up.nameWithoutVersion = match.object;
        up.version = match.version;
        up.targetDir = targetDir;
        up.targetFile = new File(targetDir, name);
        return up;
    }

    /**
     * TYhe package ID
     */
    protected String pkgId;

    /**
     * The jar file to be installed for this version
     */
    protected File file;

    /**
     * The file name without the version
     */
    protected String nameWithoutVersion;

    /**
     * The version of this update file (including classifier)
     */
    protected String version;

    /**
     * Where the update file will be installed
     */
    protected File targetFile;

    /**
     * The directory where this file will be installed
     */
    protected File targetDir;

    /**
     * Allow install of a lower version
     */
    protected boolean allowDowngrade;

    /**
     * Install only if already installed
     */
    protected boolean upgradeOnly = false;

    protected boolean deleteOnExit = false;

    protected UpdateOptions() {
    }

    public File getFile() {
        return file;
    }

    public File getTargetDir() {
        return targetDir;
    }

    public File getTargetFile() {
        return targetFile;
    }

    public String getVersion() {
        return version;
    }

    public boolean isSnapshotVersion() {
        return version.contains("-SNAPSHOT");
    }

    public String getPackageId() {
        return pkgId;
    }

    public void setUpgradeOnly(boolean upgradeOnly) {
        this.upgradeOnly = upgradeOnly;
    }

    public void setAllowDowngrade(boolean allowDowngrade) {
        this.allowDowngrade = allowDowngrade;
    }

    public boolean isUpgradeOnly() {
        return upgradeOnly;
    }

    public boolean isAllowDowngrade() {
        return allowDowngrade;
    }

    public void setDeleteOnExit(boolean deleteOnExit) {
        this.deleteOnExit = deleteOnExit;
    }

    public boolean isDeleteOnExit() {
        return deleteOnExit;
    }
}
