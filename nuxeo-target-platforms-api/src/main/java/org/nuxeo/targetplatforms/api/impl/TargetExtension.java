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

import org.nuxeo.targetplatforms.api.Target;

/**
 * {@link Target} implementation relying on an original implementation, useful
 * for override when adding additional metadata.
 *
 * @since 5.7.1
 */
public class TargetExtension extends TargetInfoExtension implements Target {

    private static final long serialVersionUID = 1L;

    protected Target origTarget;

    // needed by GWT serialization
    protected TargetExtension() {
        super();
    }

    public TargetExtension(Target orig) {
        super(orig);
        this.origTarget = orig;
    }

    @Override
    public boolean isAfterVersion(String version) {
        return origTarget.isAfterVersion(version);
    }

    @Override
    public boolean isStrictlyBeforeVersion(String version) {
        return origTarget.isStrictlyBeforeVersion(version);
    }

    @Override
    public boolean isVersion(String version) {
        return origTarget.isVersion(version);
    }

    @Override
    public boolean isStrictlyBeforeVersion(Target version) {
        return origTarget.isStrictlyBeforeVersion(version);
    }

    @Override
    public boolean isAfterVersion(Target version) {
        return origTarget.isAfterVersion(version);
    }

    @Override
    public boolean isVersion(Target version) {
        return origTarget.isAfterVersion(version);
    }

}
