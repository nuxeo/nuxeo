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
