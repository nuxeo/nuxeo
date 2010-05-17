package org.nuxeo.apidoc.tree;

import org.nuxeo.apidoc.api.AssociatedDocuments;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.ecm.core.api.CoreSession;

public class VirtualNode implements NuxeoArtifact {

    public static final String Services_VNODE = "ServicesContainer";
    public static final String ExtensionPoints_VNODE = "ExtensionPointsContainer";
    public static final String Contributions_VNODE = "ContributionsContainer";

    protected String cid;
    protected String version;
    protected final String type;
    protected String id;

    public VirtualNode(ComponentInfo ci, String type, String id) {
        cid=ci.getId();
        version = ci.getVersion();
        this.type=type;
        this.id=id;
    }

    public String getComponentId() {
        return cid;
    }

    public String getArtifactType() {
        return type;
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

}
