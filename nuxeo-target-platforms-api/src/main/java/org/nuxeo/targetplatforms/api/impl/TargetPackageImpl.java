/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.api.impl;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.targetplatforms.api.TargetPackage;


/**
 * @since 2.18
 */
public class TargetPackageImpl extends TargetImpl implements TargetPackage,
        Comparable<TargetPackage> {

    private static final long serialVersionUID = 1L;

    protected List<String> dependencies;

    protected TargetPackage parent;

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
            this.dependencies = new ArrayList<>(dependencies);
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