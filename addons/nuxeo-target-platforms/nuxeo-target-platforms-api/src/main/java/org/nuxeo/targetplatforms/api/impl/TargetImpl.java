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
 *     mcedica@nuxeo.com
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.api.impl;

import org.nuxeo.targetplatforms.api.Target;

/**
 * Common class to describe a target platform or package.
 *
 * @since 5.7.1
 */
public class TargetImpl extends TargetInfoImpl implements Target {

    private static final long serialVersionUID = 1L;

    // needed by GWT serialization
    protected TargetImpl() {
        super();
    }

    public TargetImpl(String id) {
        super(id);
    }

    public TargetImpl(String id, String name, String version, String refVersion, String label) {
        super(id, name, version, refVersion, label);
    }

    @Override
    public boolean isAfterVersion(String version) {
        if (version == null || version.trim().length() == 0 || getRefVersion().equals(version)) {
            return true;
        }

        String[] components1 = getRefVersion().split("\\.");
        String[] components2 = version.split("\\.");
        int length = Math.min(components1.length, components2.length);
        for(int i = 0; i < length; i++) {
            int result = Integer.compare(Integer.valueOf(components1[i]), Integer.valueOf(components2[i]));
            if (result != 0) {
                return result > 0;
            }
        }
        return components1.length > components2.length;
    }

    @Override
    public boolean isStrictlyBeforeVersion(String version) {
        if (version == null || version.trim().length() == 0) {
            return true;
        }

        String[] components1 = getRefVersion().split("\\.");
        String[] components2 = version.split("\\.");
        int length = Math.min(components1.length, components2.length);
        for(int i = 0; i < length; i++) {
            int result = Integer.compare(Integer.valueOf(components1[i]), Integer.valueOf(components2[i]));
            if (result != 0) {
                return result < 0;
            }
        }
        return components1.length < components2.length;
    }

    @Override
    public boolean isVersion(String version) {
        if (version == null || version.trim().length() == 0) {
            return false;
        }
        return version.compareTo(getRefVersion()) == 0;
    }

    @Override
    public boolean isStrictlyBeforeVersion(Target version) {
        return isStrictlyBeforeVersion(version.getRefVersion());
    }

    @Override
    public boolean isAfterVersion(Target version) {
        return isAfterVersion(version.getRefVersion());
    }

    @Override
    public boolean isVersion(Target version) {
        return isVersion(getRefVersion());
    }

}