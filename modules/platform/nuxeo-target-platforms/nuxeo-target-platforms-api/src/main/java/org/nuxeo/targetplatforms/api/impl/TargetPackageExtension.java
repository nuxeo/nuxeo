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

import org.nuxeo.targetplatforms.api.TargetPackage;

/**
 * {@link TargetPackage} implementation relying on an original implementation, useful for override when adding
 * additional metadata.
 *
 * @since 5.7.1
 */
public class TargetPackageExtension extends TargetExtension implements TargetPackage, Comparable<TargetPackage> {

    private static final long serialVersionUID = 1L;

    protected TargetPackage origPackage;

    // needed by GWT serialization
    protected TargetPackageExtension() {
        super();
    }

    public TargetPackageExtension(TargetPackage orig) {
        super(orig);
        origPackage = orig;
    }

    @Override
    public List<String> getDependencies() {
        return origPackage.getDependencies();
    }

    @Override
    public TargetPackage getParent() {
        return origPackage.getParent();
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
