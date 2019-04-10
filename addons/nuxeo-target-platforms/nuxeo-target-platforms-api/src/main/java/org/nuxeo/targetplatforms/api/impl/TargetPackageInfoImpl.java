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

import org.nuxeo.targetplatforms.api.TargetPackageInfo;

/**
 * Describes a target package
 *
 * @since 5.7.1
 */
public class TargetPackageInfoImpl extends TargetInfoImpl implements TargetPackageInfo {

    private static final long serialVersionUID = 1L;

    protected List<String> dependencies;

    // needed by GWT serialization
    protected TargetPackageInfoImpl() {
    }

    public TargetPackageInfoImpl(String id, String name, String version, String refVersion, String label) {
        super(id, name, version, refVersion, label);
    }

    @Override
    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

}
