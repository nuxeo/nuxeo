/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.api.impl;

import java.util.List;

import org.nuxeo.targetplatforms.api.TargetPackage;
import org.nuxeo.targetplatforms.api.TargetPlatform;

/**
 * {@link TargetPlatform} implementation relying on an original implementation,
 * useful for override when adding additional metadata.
 *
 * @since 5.7.1
 */
public class TargetPlatformExtension extends TargetExtension implements
        TargetPlatform, Comparable<TargetPlatform> {

    private static final long serialVersionUID = 1L;

    protected TargetPlatform origPlatform;

    // needed by GWT serialization
    protected TargetPlatformExtension() {
        super();
    }

    public TargetPlatformExtension(TargetPlatform orig) {
        super(orig);
        this.origPlatform = orig;
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

    public TargetPlatform getParent() {
        return origPlatform.getParent();
    }

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
