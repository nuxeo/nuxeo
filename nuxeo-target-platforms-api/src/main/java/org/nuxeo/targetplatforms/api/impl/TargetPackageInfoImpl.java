/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
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
 * @since 2.18
 */
public class TargetPackageInfoImpl extends TargetInfoImpl implements
        TargetPackageInfo {

    private static final long serialVersionUID = 1L;

    protected List<String> dependencies;

    protected TargetPackageInfoImpl() {
    }

    public TargetPackageInfoImpl(String id, String name, String version,
            String refVersion, String label) {
        super(id, name, version, refVersion, label);
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

}
