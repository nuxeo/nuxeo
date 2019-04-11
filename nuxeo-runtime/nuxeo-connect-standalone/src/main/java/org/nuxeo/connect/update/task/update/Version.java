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

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.utils.FileVersion;

/**
 * The version correspond to a JAR version that is required by some package. An update version is defined by the JAR
 * version, a relative path to the JAR file and a list of packages requiring this version. The path points to a copy of
 * the JAR version in the update manager storage. (thus the path is relative to the update manager root) Let say you
 * install a package pkg1 that requires the version 1.0 for the jar X. If this version is not yet provided by another
 * package a new version will be created and the jar file copied in the update manager storage under the destination
 * 'path' (e.g. bundles/X-1.0.jar).
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Version {

    /**
     * The version name (including classifier)
     */
    protected String version;

    /**
     * The path of the backup file
     */
    protected String path;

    /**
     * The packages requiring this version
     */
    protected Map<String, UpdateOptions> packages;

    public Version(String version) {
        this.version = version;
        packages = new HashMap<>();
    }

    public final Map<String, UpdateOptions> getPackages() {
        return packages;
    }

    public boolean hasPackage(String pkgId) {
        return packages.containsKey(pkgId);
    }

    public boolean removePackage(String pkgId) {
        return packages.remove(pkgId) != null;
    }

    public boolean addPackage(UpdateOptions opt) {
        return packages.put(opt.getPackageId(), opt) != null;
    }

    public boolean hasPackages() {
        return !packages.isEmpty();
    }

    public final String getPath() {
        return path;
    }

    public final void setPath(String path) {
        this.path = path;
    }

    public final String getVersion() {
        return version;
    }

    public final FileVersion getFileVersion() {
        return new FileVersion(version);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Version) {
            return ((Version) obj).version.equals(version);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return version.hashCode();
    }

    @Override
    public String toString() {
        return version;
    }

}
