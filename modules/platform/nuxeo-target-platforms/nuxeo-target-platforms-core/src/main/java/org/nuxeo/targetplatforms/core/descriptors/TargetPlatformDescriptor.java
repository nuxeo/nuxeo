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
