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
import java.util.Map;

import org.nuxeo.targetplatforms.api.TargetPackage;
import org.nuxeo.targetplatforms.api.TargetPlatform;
import org.nuxeo.targetplatforms.api.TargetPlatformInstance;

/**
 * {@link TargetPlatform} implementation relying on an original implementation,
 * useful for override when adding additional metadata.
 *
 * @since 5.7.1
 */
public class TargetPlatformInstanceExtension extends TargetExtension implements
        TargetPlatformInstance {

    private static final long serialVersionUID = 1L;

    protected TargetPlatformInstance origInstance;

    // needed by GWT serialization
    protected TargetPlatformInstanceExtension() {
        super();
    }

    public TargetPlatformInstanceExtension(TargetPlatformInstance orig) {
        super(orig);
        this.origInstance = orig;
    }

    @Override
    public boolean isFastTrack() {
        return origInstance.isFastTrack();
    }

    @Override
    public List<String> getEnabledPackagesIds() {
        return origInstance.getEnabledPackagesIds();
    }

    @Override
    public Map<String, TargetPackage> getEnabledPackages() {
        return origInstance.getEnabledPackages();
    }

    @Override
    public boolean hasEnabledPackageWithName(String packageName) {
        return origInstance.hasEnabledPackageWithName(packageName);
    }

    public TargetPlatform getParent() {
        return origInstance.getParent();
    }

}
