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
public class TargetPlatformInstanceImpl extends TargetImpl implements TargetPlatformInstance {

    private static final long serialVersionUID = 1L;

    protected TargetPlatform parent;

    protected Map<String, TargetPackage> enabledPackages;

    protected TargetPlatformInstanceImpl() {
        super();
    }

    public TargetPlatformInstanceImpl(String id) {
        super(id);
    }

    public TargetPlatformInstanceImpl(String id, String name, String version, String refVersion, String label) {
        super(id, name, version, refVersion, label);
    }

    @Override
    public List<String> getEnabledPackagesIds() {
        if (enabledPackages == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(enabledPackages.keySet());
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
            enabledPackages = new LinkedHashMap<>();
        }
        enabledPackages.put(pack.getId(), pack);
    }

    public void setEnabledPackages(Map<String, TargetPackage> packages) {
        if (enabledPackages == null) {
            enabledPackages = new LinkedHashMap<>();
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

    @Override
    public TargetPlatform getParent() {
        return parent;
    }

    public void setParent(TargetPlatform parent) {
        this.parent = parent;
    }

}
