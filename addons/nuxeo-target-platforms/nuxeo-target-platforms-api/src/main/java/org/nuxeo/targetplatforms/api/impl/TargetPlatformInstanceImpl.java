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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.targetplatforms.api.TargetPackage;
import org.nuxeo.targetplatforms.api.TargetPlatform;
import org.nuxeo.targetplatforms.api.TargetPlatformInstance;

/**
 * @since 5.7.1
 */
public class TargetPlatformInstanceImpl extends TargetImpl implements
        TargetPlatformInstance {

    private static final long serialVersionUID = 1L;

    protected TargetPlatform parent;

    protected Map<String, TargetPackage> enabledPackages;

    protected TargetPlatformInstanceImpl() {
        super();
    }

    public TargetPlatformInstanceImpl(String id) {
        super(id);
    }

    public TargetPlatformInstanceImpl(String id, String name, String version,
            String refVersion, String label) {
        super(id, name, version, refVersion, label);
    }

    @Override
    public List<String> getEnabledPackagesIds() {
        if (enabledPackages == null) {
            return Collections.emptyList();
        }
        return new ArrayList<String>(enabledPackages.keySet());
    }

    @Override
    public Map<String, TargetPackage> getEnabledPackages() {
        if (enabledPackages == null) {
            return Collections.emptyMap();
        }
        return enabledPackages;
    }

    public void addEnabledPackage(TargetPackage pack) {
        if (pack == null) {
            return;
        }
        if (enabledPackages == null) {
            enabledPackages = new LinkedHashMap<String, TargetPackage>();
        }
        enabledPackages.put(pack.getId(), pack);
    }

    public void setEnabledPackages(Map<String, TargetPackage> packages) {
        if (enabledPackages == null) {
            enabledPackages = new LinkedHashMap<String, TargetPackage>();
        } else {
            enabledPackages.clear();
        }
        if (packages != null) {
            enabledPackages.putAll(packages);
        }
    }

    @Override
    public boolean hasEnabledPackageWithName(String packageName) {
        if (packageName == null || enabledPackages == null) {
            return false;
        }
        for (TargetPackage pkg : enabledPackages.values()) {
            if (pkg != null && packageName.equals(pkg.getName())) {
                return true;
            }
        }
        return false;
    }

    public TargetPlatform getParent() {
        return parent;
    }

    public void setParent(TargetPlatform parent) {
        this.parent = parent;
    }

}