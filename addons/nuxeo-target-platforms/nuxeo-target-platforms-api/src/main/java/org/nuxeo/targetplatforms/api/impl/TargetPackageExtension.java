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
