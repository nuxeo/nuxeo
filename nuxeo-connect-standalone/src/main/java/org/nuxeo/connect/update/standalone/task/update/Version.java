/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.connect.update.standalone.task.update;

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.common.utils.FileVersion;

/**
 * The version correspond to a JAR version that is required by some package. An
 * update version is defined by the JAR version, a relative path to the JAR file
 * and a list of packages requiring this version.
 * 
 * The path points to a copy of the JAR version in the update manager storage.
 * (thus the path is relative to the update manager root)
 * 
 * Let say you install a package pkg1 that requires the version 1.0 for the jar
 * X. If this version is not yet provided by another package a new version will
 * be created and the jar file copied in the update manager storage under the
 * destination 'path' (e.g. bundles/X-1.0.jar).
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
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
    protected Set<String> packages;

    public Version(String version) {
        this.version = version;
        packages = new HashSet<String>();
    }

    public final Set<String> getPackages() {
        return packages;
    }

    public boolean hasPackage(String pkgId) {
        return packages.contains(pkgId);
    }

    public boolean removePackage(String pkgId) {
        return packages.remove(pkgId);
    }

    public boolean addPackage(String pkgId) {
        return packages.add(pkgId);
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
