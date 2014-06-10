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
import java.util.List;

import org.nuxeo.targetplatforms.api.TargetPackage;

/**
 * @since 5.7.1
 */
public class TargetPackageImpl extends TargetImpl implements TargetPackage,
        Comparable<TargetPackage> {

    private static final long serialVersionUID = 1L;

    protected List<String> dependencies;

    protected TargetPackage parent;

    // needed by GWT serialization
    protected TargetPackageImpl() {
        super();
    }

    public TargetPackageImpl(String id, String name, String version,
            String refVersion, String label) {
        super(id, name, version, refVersion, label);
        this.dependencies = new ArrayList<String>();
    }

    public void addDependency(String dependency) {
        dependencies.add(dependency);
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        if (dependencies == null) {
            this.dependencies = null;
        } else {
            this.dependencies = new ArrayList<String>(dependencies);
        }
    }

    public TargetPackage getParent() {
        return parent;
    }

    public void setParent(TargetPackage parent) {
        this.parent = parent;
    }

    @Override
    public int compareTo(TargetPackage o) {
        // compare first on name, then on version
        int comp = getName().compareTo(o.getName());
        if (comp == 0) {
            comp = getVersion().compareTo(o.getVersion());
        }
        return comp;
    }

}