package org.nuxeo.apidoc.search;

import org.nuxeo.apidoc.api.NuxeoArtifact;

public class ArtifactWithWeight implements Comparable<ArtifactWithWeight> {

    protected NuxeoArtifact artifact;

    protected int hits=0;

    public ArtifactWithWeight(NuxeoArtifact artifact) {
        this.artifact=artifact;
        hits=1;
    }
    public NuxeoArtifact getArtifact() {
        return artifact;
    }

    public int getHitNumbers() {
        return hits;
    }

    public void addHit() {
        hits+=1;
    }

    public int compareTo(ArtifactWithWeight other) {
        return new Integer(hits).compareTo(new Integer(other.getHitNumbers()));
    }

}
