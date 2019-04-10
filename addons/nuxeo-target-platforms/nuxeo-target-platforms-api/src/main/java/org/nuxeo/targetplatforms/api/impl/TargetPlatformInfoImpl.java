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
 *     bstefanescu
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.api.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.targetplatforms.api.TargetPackageInfo;
import org.nuxeo.targetplatforms.api.TargetPlatformInfo;

/**
 * Describe a target platform: name, version
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @since 5.7.1
 */
public class TargetPlatformInfoImpl extends TargetInfoImpl implements TargetPlatformInfo {

    private static final long serialVersionUID = 1L;

    protected Map<String, TargetPackageInfo> availablePackagesInfo;

    // needed by GWT serialization
    protected TargetPlatformInfoImpl() {
    }

    public TargetPlatformInfoImpl(String id, String name, String version, String refVersion, String label) {
        super(id, name, version, refVersion, label);
    }

    @Override
    public List<String> getAvailablePackagesIds() {
        if (availablePackagesInfo == null) {
            return Collections.emptyList();
        }
        return new ArrayList<String>(availablePackagesInfo.keySet());
    }

    @Override
    public Map<String, TargetPackageInfo> getAvailablePackagesInfo() {
        // dereference
        if (availablePackagesInfo == null) {
            return Collections.<String, TargetPackageInfo> emptyMap();
        }
        return new LinkedHashMap<String, TargetPackageInfo>(availablePackagesInfo);
    }

    @Override
    public void addAvailablePackageInfo(TargetPackageInfo packInfo) {
        if (packInfo == null) {
            return;
        }
        if (availablePackagesInfo == null) {
            availablePackagesInfo = new LinkedHashMap<String, TargetPackageInfo>();
        }
        availablePackagesInfo.put(packInfo.getId(), packInfo);
    }

    @Override
    public void setAvailablePackagesInfo(Map<String, TargetPackageInfo> packages) {
        if (availablePackagesInfo != null) {
            availablePackagesInfo.clear();
        }
        if (packages != null) {
            for (TargetPackageInfo packInfo : packages.values()) {
                addAvailablePackageInfo(packInfo);
            }
        }
    }

    @Override
    public int compareTo(TargetPlatformInfo o) {
        // compare first on name, then on version
        int comp = getName().compareTo(o.getName());
        if (comp == 0) {
            comp = getVersion().compareTo(o.getVersion());
        }
        return comp;
    }

}