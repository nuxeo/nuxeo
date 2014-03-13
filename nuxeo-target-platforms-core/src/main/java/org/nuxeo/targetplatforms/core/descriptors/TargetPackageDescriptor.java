/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.core.descriptors;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for target package contributions.
 *
 * @since 2.18
 */
@XObject("package")
public class TargetPackageDescriptor extends TargetDescriptor {

    @XNodeList(value = "targetPlatforms/platform", type = ArrayList.class, componentType = String.class)
    public List<String> targetPlatforms;

    @XNodeList(value = "dependencies/package", type = ArrayList.class, componentType = String.class)
    public List<String> dependencies;

    public List<String> getTargetPlatforms() {
        return targetPlatforms;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    @Override
    public TargetPackageDescriptor clone() {
        TargetPackageDescriptor clone = new TargetPackageDescriptor();
        doClone(clone);
        return clone;
    }

    protected void doClone(TargetPackageDescriptor clone) {
        super.doClone(clone);
        if (targetPlatforms != null) {
            clone.targetPlatforms = new ArrayList<>(targetPlatforms);
        }
        if (dependencies != null) {
            clone.dependencies = new ArrayList<>(dependencies);
        }
    }

}