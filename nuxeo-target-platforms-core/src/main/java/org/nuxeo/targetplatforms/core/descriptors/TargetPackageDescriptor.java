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
package org.nuxeo.targetplatforms.core.descriptors;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for target package contributions.
 *
 * @since 5.7.1
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