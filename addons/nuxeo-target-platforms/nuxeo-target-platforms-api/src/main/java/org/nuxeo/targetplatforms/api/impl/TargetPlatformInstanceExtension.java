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
import java.util.Map;

import org.nuxeo.targetplatforms.api.TargetPackage;
import org.nuxeo.targetplatforms.api.TargetPlatform;
import org.nuxeo.targetplatforms.api.TargetPlatformInstance;

/**
 * {@link TargetPlatform} implementation relying on an original implementation, useful for override when adding
 * additional metadata.
 *
 * @since 5.7.1
 */
public class TargetPlatformInstanceExtension extends TargetExtension implements TargetPlatformInstance {

    private static final long serialVersionUID = 1L;

    protected TargetPlatformInstance origInstance;

    // needed by GWT serialization
    protected TargetPlatformInstanceExtension() {
        super();
    }

    public TargetPlatformInstanceExtension(TargetPlatformInstance orig) {
        super(orig);
        origInstance = orig;
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

    @Override
    public TargetPlatform getParent() {
        return origInstance.getParent();
    }

}
