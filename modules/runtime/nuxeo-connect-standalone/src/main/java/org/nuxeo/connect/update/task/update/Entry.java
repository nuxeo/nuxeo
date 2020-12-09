/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.nuxeo.common.utils.FileVersion;

/**
 * Versions are stored in the same order they are registered (in historical package install order). That means when
 * rollbacking an update the file system will be modified only if the last version was rollbacked. And the version that
 * will become the current version will be the last version in the version list.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Entry implements Iterable<Version> {

    protected String key;

    protected Version baseVersion;

    protected List<Version> versions;

    public Entry(String key) {
        this.key = key;
        versions = new ArrayList<>();
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

    /**
     * @return Last version not deployed in upgradeOnly mode
     * @since 5.7
     */
    public Version getLastVersion(boolean includeUpgradeOnly) {
        if (includeUpgradeOnly) {
            return getLastVersion();
        }
        for (int i = versions.size() - 1; i >= 0; i--) {
            for (UpdateOptions opt : versions.get(i).packages.values()) {
                if (!opt.upgradeOnly) {
                    return versions.get(i);
                }
            }
        }
        return null;
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
            String ov = v.getVersion();
            if ((ov != null && ov.equals(version)) || (ov == null && version == null)) {
                return v;
            }
        }
        return null;
    }

    public Version getGreatestVersion() {
        Version result = null;
        FileVersion fv = null;
        for (Version v : versions) {
            if (fv == null) {
                fv = v.getFileVersion();
                result = v;
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
