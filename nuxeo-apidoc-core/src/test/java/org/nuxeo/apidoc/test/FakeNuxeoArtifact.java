package org.nuxeo.apidoc.test;

import org.nuxeo.apidoc.api.AssociatedDocuments;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.ecm.core.api.CoreSession;

public class FakeNuxeoArtifact implements NuxeoArtifact {

    public String id;

    public String version;

    public String type;

    public FakeNuxeoArtifact(NuxeoArtifact artifact) {
        this.id = artifact.getId();
        this.version = artifact.getVersion();
        this.type = artifact.getArtifactType();
    }

    public AssociatedDocuments getAssociatedDocuments(CoreSession session) {
        return null;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public String getArtifactType() {
        return type;
    }

}
