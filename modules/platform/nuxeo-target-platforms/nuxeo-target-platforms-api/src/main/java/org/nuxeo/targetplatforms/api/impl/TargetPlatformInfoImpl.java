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
        return new ArrayList<>(availablePackagesInfo.keySet());
    }

    @Override
    public Map<String, TargetPackageInfo> getAvailablePackagesInfo() {
        // dereference
        if (availablePackagesInfo == null) {
            return Collections.<String, TargetPackageInfo> emptyMap();
        }
        return new LinkedHashMap<>(availablePackagesInfo);
    }

    @Override
    public void addAvailablePackageInfo(TargetPackageInfo packInfo) {
        if (packInfo == null) {
            return;
        }
        if (availablePackagesInfo == null) {
            availablePackagesInfo = new LinkedHashMap<>();
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
