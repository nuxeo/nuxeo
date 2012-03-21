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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.nuxeo.common.utils.FileVersion;

/**
 * Versions are stored in the same order they are registered (in historical
 * package install order).
 * 
 * That means when when rollbacking an update the file system will be modified
 * only if the last version was rollbacked. And the version that will become the
 * current version will be the last version in the version list.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class Entry implements Iterable<Version> {

    protected String key;

    protected Version baseVersion;

    protected List<Version> versions;

    public Entry(String key) {
        this.key = key;
        versions = new ArrayList<Version>();
    }

    public final boolean isEmpty() {
        return versions.isEmpty();
    }

    public final Version getFirstVersion() {
        return versions.isEmpty() ? null : versions.get(0);
    }

    public final Version getLastVersion() {
        return versions.isEmpty() ? null : versions.get(versions.size() - 1);
    }

    public final String getKey() {
        return key;
    }

    public final void setBaseVersion(Version baseVersion) {
        this.baseVersion = baseVersion;
    }

    public final boolean hasBaseVersion() {
        return baseVersion != null;
    }

    public final Version getBaseVersion() {
        return baseVersion;
    }

    public final boolean isLastVersion(Version v) {
        return v == getLastVersion();
    }

    public boolean removeVersion(Version version) {
        return versions.remove(version);
    }

    public Version addVersion(Version version) {
        if (versions.contains(version)) {
            throw new VersionAlreadyExistException(version.getVersion());
        }
        versions.add(version);
        return version;
    }

    public Version getVersion(String version) {
        for (Version v : versions) {
            if (v.getVersion().equals(version)) {
                return v;
            }
        }
        return null;
    }

    public Version getGreatestVersion() {
        Version result = null;
        FileVersion fv = null;
        for (Version v : versions) {
            if (result == null) {
                result = v;
                fv = v.getFileVersion();
            } else {
                FileVersion fv2 = v.getFileVersion();
                if (fv.lessThan(fv2)) {
                    result = v;
                    fv = fv2;
                }
            }
        }
        return result;
    }

    /**
     * @throws IncompatibleVersionException if version <= base version
     * @param version
     * @return
     */
    public Version getOrCreateVersion(String version) {
        Version v = getVersion(version);
        if (v == null) {
            return addVersion(new Version(version));
        }
        return v;
    }

    public List<Version> getVersions() {
        return versions;
    }

    @Override
    public Iterator<Version> iterator() {
        return versions.iterator();
    }

    @Override
    public String toString() {
        return key;
    }
}
