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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for target platform contributions.
 *
 * @since 5.7.1
 */
@XObject("platform")
public class TargetPlatformDescriptor extends TargetDescriptor {

    @XNode("fastTrack")
    Boolean fastTrack;

    @XNode("trial")
    Boolean trial;

    @XNode("default")
    Boolean isDefault;

    @XNodeList(value = "testVersions/version", type = ArrayList.class, componentType = String.class)
    List<String> testVersions;

    public boolean isFastTrackSet() {
        return fastTrack != null;
    }

    public boolean isFastTrack() {
        return Boolean.TRUE.equals(fastTrack);
    }

    public boolean isTrialSet() {
        return trial != null;
    }

    public boolean isTrial() {
        return Boolean.TRUE.equals(trial);
    }

    public boolean isDefaultSet() {
        return isDefault != null;
    }

    public boolean isDefault() {
        return Boolean.TRUE.equals(isDefault);
    }

    public List<String> getTestVersions() {
        return testVersions;
    }

    @Override
    public TargetPlatformDescriptor clone() {
        TargetPlatformDescriptor clone = new TargetPlatformDescriptor();
        doClone(clone);
        return clone;
    }

    protected void doClone(TargetPlatformDescriptor clone) {
        super.doClone(clone);
        clone.fastTrack = fastTrack;
        clone.trial = trial;
        clone.isDefault = isDefault;
        if (testVersions != null) {
            clone.testVersions = new ArrayList<>(testVersions);
        }
    }

}
