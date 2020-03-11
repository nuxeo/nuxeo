/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.api.impl;

import java.util.List;

import org.nuxeo.targetplatforms.api.TargetPackage;
import org.nuxeo.targetplatforms.api.TargetPlatform;

/**
 * {@link TargetPlatform} implementation relying on an original implementation, useful for override when adding
 * additional metadata.
 *
 * @since 5.7.1
 */
public class TargetPlatformExtension extends TargetExtension implements TargetPlatform, Comparable<TargetPlatform> {

    private static final long serialVersionUID = 1L;

    protected TargetPlatform origPlatform;

    // needed by GWT serialization
    protected TargetPlatformExtension() {
        super();
    }

    public TargetPlatformExtension(TargetPlatform orig) {
        super(orig);
        origPlatform = orig;
    }

    @Override
    public boolean isFastTrack() {
        return origPlatform.isFastTrack();
    }

    @Override
    public List<String> getAvailablePackagesIds() {
        return origPlatform.getAvailablePackagesIds();
    }

    @Override
    public List<TargetPackage> getAvailablePackages() {
        return origPlatform.getAvailablePackages();
    }

    @Override
    public TargetPlatform getParent() {
        return origPlatform.getParent();
    }

    @Override
    public List<String> getTestVersions() {
        return origPlatform.getTestVersions();
    }

    @Override
    public int compareTo(TargetPlatform o) {
        // compare first on name, then on version
        int comp = getName().compareTo(o.getName());
        if (comp == 0) {
            comp = getVersion().compareTo(o.getVersion());
        }
        return comp;
    }

}
