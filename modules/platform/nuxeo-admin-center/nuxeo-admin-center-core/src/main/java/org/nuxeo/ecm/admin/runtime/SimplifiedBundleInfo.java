/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.admin.runtime;

import java.util.Optional;

import javax.annotation.Nullable;

/**
 * Very simplified representation of a Nuxeo Bundle used for displaying in the admin screens.
 *
 * @author tiry
 */
public class SimplifiedBundleInfo implements Comparable<SimplifiedBundleInfo> {

    protected final String name;

    protected final String version;

    @Nullable
    protected final String revision;

    /** @deprecated since 2025.12, use {@link #SimplifiedBundleInfo(String, String,String)} */
    @Deprecated(since = "2025.12", forRemoval = true)
    public SimplifiedBundleInfo(String name, String version) {
        this(name, version, null);
    }

    public SimplifiedBundleInfo(String name, String version, @Nullable String revision) {
        this.name = name;
        this.version = version;
        this.revision = revision;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    /** @since 2025.12 */
    public Optional<String> getRevision() {
        return Optional.ofNullable(revision);
    }

    @Override
    public int compareTo(SimplifiedBundleInfo other) {
        return this.getName().compareTo(other.getName());
    }
}
