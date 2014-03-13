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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for target platform contributions.
 *
 * @since 2.18
 */
@XObject("platform")
public class TargetPlatformDescriptor extends TargetDescriptor {

    @XNode("fastTrack")
    Boolean fastTrack;

    @XNodeList(value = "testVersions/version", type = ArrayList.class, componentType = String.class)
    List<String> testVersions;

    public boolean isFastTrackSet() {
        return fastTrack != null;
    }

    public boolean isFastTrack() {
        return Boolean.TRUE.equals(fastTrack);
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
        if (testVersions != null) {
            clone.testVersions = new ArrayList<>(testVersions);
        }
    }

}
