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
