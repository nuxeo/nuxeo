package org.nuxeo.apidoc.api;

import java.util.Comparator;

public class NuxeoArtifactComparator implements Comparator<NuxeoArtifact> {

    @Override
    public int compare(NuxeoArtifact o1, NuxeoArtifact o2) {
        return o1.getId().compareTo(o2.getId());
    }

}
